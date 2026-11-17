package com.mahghuuuls.somecheesecontrol.integration.wizardry;

import com.mahghuuuls.somecheesecontrol.core.CastingItems;
import com.mahghuuuls.somecheesecontrol.core.PlayerExemptions;
import com.mahghuuuls.somecheesecontrol.core.Refusals;
import com.mahghuuuls.somecheesecontrol.core.Resync;
import com.mahghuuuls.somecheesecontrol.core.Rule;
import com.mahghuuuls.somecheesecontrol.feature.climbing.ClimbingTracker;
import electroblob.wizardry.data.WizardData;
import electroblob.wizardry.event.SpellCastEvent;
import electroblob.wizardry.item.ISpellCastingItem;
import electroblob.wizardry.item.ItemScroll;
import electroblob.wizardry.spell.Spell;
import electroblob.wizardry.util.SpellModifiers;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.item.ItemStack;
import net.minecraft.util.EnumActionResult;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;
import net.minecraftforge.fml.common.eventhandler.EventPriority;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;
import net.minecraftforge.fml.relauncher.Side;

import javax.annotation.Nullable;

/**
 * Refuses Wizardry casts from a held spell-casting item (a wand, a scroll, an addon's casting item)
 * while the player is in the restricted climbing state, on the logical server.
 *
 * <p>Four paths, each before any cost:</p>
 * <ul>
 * <li>The right click with a spell-casting item, before the item acts. It covers every item that
 * casts from its right click, also the few addon items that cast without Wizardry's events. The
 * client refuses the same click early.</li>
 * <li>At the start of the player's tick, a held use that has not cast yet (a charge-up, or the
 * first tick of a channel, which Wizardry casts with no event) ends without a release: nothing is
 * cast, nothing is spent, no cooldown.</li>
 * <li>Wizardry's channel event ({@code Tick}) for a spell already channeling: Wizardry then stops it
 * as a release, so its end cooldown applies and a channeled scroll is used up, as Wizardry does.</li>
 * <li>Wizardry's cast event ({@code Pre}) for a cast from the item in use, for an addon item that
 * casts during its use by another way.</li>
 * </ul>
 *
 * <p>Only the server ends a use in progress; the client follows the server's hand state. A cast
 * belongs to a held item when its source can be one and the item in use (for the click, the clicked
 * item) is a spell-casting item whose current spell is the cast's spell. So casts by minions on the
 * owner's behalf, automatic triggers (artefacts, contingency spells) and the {@code /cast} command
 * pass, unless one casts the very spell the player is casting from the item in use.</p>
 */
public final class CastingRefusal {

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public void onRightClickItem(PlayerInteractEvent.RightClickItem event) {
        if (WizardryIntegration.failed()) {
            return;
        }
        try {
            EntityPlayer player = event.getEntityPlayer();
            ItemStack stack = event.getItemStack();
            if (!CastingItems.is(stack) || !(player instanceof EntityPlayerMP) || player.world.isRemote || !isRefused(player)) {
                return;
            }
            event.setCanceled(true);
            event.setCancellationResult(EnumActionResult.FAIL);
            Refusals.refuseClickStep((EntityPlayerMP) player, Rule.CLIMBING_WIZARDRY, Resync.inventory(), stack, null,
                    "spell", spellName(currentSpell(stack)), "at", "click");
        } catch (RuntimeException | LinkageError e) {
            WizardryIntegration.fail(e);
        }
    }

    /** Ends a held use that has not cast yet, before this tick's use update casts it. */
    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public void onPlayerTickStart(TickEvent.PlayerTickEvent event) {
        if (WizardryIntegration.failed() || event.phase != TickEvent.Phase.START || event.side != Side.SERVER) {
            return;
        }
        try {
            stopBeforeFirstCast(event.player);
        } catch (RuntimeException | LinkageError e) {
            WizardryIntegration.fail(e);
        }
    }

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public void onChannel(SpellCastEvent.Tick event) {
        refuseFromItemInUse(event, Resync.none(), "channel");
    }

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public void onCast(SpellCastEvent.Pre event) {
        refuseFromItemInUse(event, Resync.inventory(), "cast");
    }

    /**
     * A channel needs no resend: Wizardry's own stop changes the hand state the client follows. A
     * refused cast does: the client may have run the same cast and set a cooldown on its copy.
     */
    private static void refuseFromItemInUse(SpellCastEvent event, Resync resync, String at) {
        if (WizardryIntegration.failed()) {
            return;
        }
        try {
            EntityLivingBase caster = event.getCaster();
            SpellCastEvent.Source source = event.getSource();
            if (!(caster instanceof EntityPlayerMP) || caster.world.isRemote || !caster.isHandActive()
                    || !CastingRules.mayComeFromHeldItem(source != null ? source.name() : null)) {
                return;
            }
            EntityPlayerMP player = (EntityPlayerMP) caster;
            ItemStack active = player.getActiveItemStack();
            if (!(active.getItem() instanceof ISpellCastingItem) || currentSpell(active) != event.getSpell() || !isRefused(player)) {
                return;
            }
            event.setCanceled(true);
            Refusals.refuse(player, Rule.CLIMBING_WIZARDRY, resync, active, null,
                    "spell", spellName(event.getSpell()), "source", source, "at", at);
        } catch (RuntimeException | LinkageError e) {
            WizardryIntegration.fail(e);
        }
    }

    private static void stopBeforeFirstCast(EntityPlayer player) {
        if (!player.isHandActive() || !(player instanceof EntityPlayerMP)) {
            return;
        }
        ItemStack active = player.getActiveItemStack();
        if (!(active.getItem() instanceof ISpellCastingItem)) {
            return;
        }
        Spell spell = currentSpell(active);
        if (spell == null) {
            return;
        }
        int useTick = active.getMaxItemUseDuration() - player.getItemInUseCount();
        int chargeup = CastingRules.chargeupTicks(spell.getChargeup(), chargeupModifier(player), active.getItem() instanceof ItemScroll);
        if (!CastingRules.isBeforeFirstCast(useTick, chargeup, spell.isContinuous) || !isRefused(player)) {
            return;
        }
        player.resetActiveHand();
        Refusals.refuse((EntityPlayerMP) player, Rule.CLIMBING_WIZARDRY, Resync.inventory(), active, null,
                "spell", spellName(spell), "at", "chargeup");
    }

    @Nullable
    private static Spell currentSpell(ItemStack stack) {
        return stack.getItem() instanceof ISpellCastingItem ? ((ISpellCastingItem) stack.getItem()).getCurrentSpell(stack) : null;
    }

    /** The charge-up modifier Wizardry stored for the player's held use; 1 when it has none. */
    private static float chargeupModifier(EntityPlayer player) {
        WizardData data = WizardData.get(player);
        return data != null && data.itemCastingModifiers != null ? data.itemCastingModifiers.get(SpellModifiers.CHARGEUP) : 1.0F;
    }

    private static Object spellName(@Nullable Spell spell) {
        return spell != null ? spell.getRegistryName() : "none";
    }

    private static boolean isRefused(EntityPlayer player) {
        return PlayerExemptions.applies(player, Rule.CLIMBING_WIZARDRY) && ClimbingTracker.isRestricted(player);
    }
}
