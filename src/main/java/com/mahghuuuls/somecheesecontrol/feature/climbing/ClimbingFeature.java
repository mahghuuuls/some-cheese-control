package com.mahghuuuls.somecheesecontrol.feature.climbing;

import com.mahghuuuls.somecheesecontrol.config.Settings;
import com.mahghuuuls.somecheesecontrol.core.PlayerExemptions;
import com.mahghuuuls.somecheesecontrol.core.Refusals;
import com.mahghuuuls.somecheesecontrol.core.Resync;
import com.mahghuuuls.somecheesecontrol.core.Rule;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.item.ItemStack;
import net.minecraft.util.EnumActionResult;
import net.minecraftforge.event.entity.living.LivingEntityUseItemEvent;
import net.minecraftforge.event.entity.player.ArrowLooseEvent;
import net.minecraftforge.event.entity.player.AttackEntityEvent;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;
import net.minecraftforge.fml.common.eventhandler.EventPriority;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.PlayerEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;

/**
 * Keeps the climbing state current and refuses melee and ranged actions in it.
 *
 * <p>Melee refusal uses the attack event, which RLCombat also posts for both hands (as a subclass)
 * before any damage, durability or cooldown work; Spartan Weaponry's melee runs only after an
 * attack event that nobody cancelled.</p>
 *
 * <p>Ranged refusal has two paths. An item that acts at once (a snowball, a pearl, dynamite, a
 * fishing rod) is refused at the right click. An item with a use duration (a bow, a crossbow, a
 * throwing weapon) is let start and is stopped at the start of the player's next tick, before any
 * use tick runs. The same stop ends a draw that began before the climbing state, so one path covers
 * both. The client runs its own draw; the stop changes the server's hand state and resends the
 * inventory, either of which ends the client's draw, and a late resend corrects an arrow the client
 * spent on a release made before the stop reached it. Nothing is spent before release or finish, so
 * a stop costs nothing, and a loaded crossbow stays loaded. The use tick event is never cancelled:
 * Forge then finishes the use at once.</p>
 */
public final class ClimbingFeature {

    private final boolean melee;
    private final boolean ranged;

    public ClimbingFeature(Settings settings) {
        this.melee = settings.climbingMelee();
        this.ranged = settings.climbingRanged();
    }

    /**
     * True when any climbing restriction is on, so the state must be tracked. Every climbing
     * restriction added later must be listed here, or it gets no release delay.
     */
    public static boolean isNeeded(Settings settings) {
        return settings.climbingMelee() || settings.climbingRanged();
    }

    @SubscribeEvent
    public void onPlayerTick(TickEvent.PlayerTickEvent event) {
        if (event.phase != TickEvent.Phase.END || event.player.world.isRemote) {
            return;
        }
        ClimbingTracker.update(event.player);
        if (ranged) {
            LateResend.runDue(event.player);
        }
    }

    /**
     * Stops a ranged use at the start of the player's tick, before the use ticks and before other
     * mods' handlers of this phase see an active bow.
     */
    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public void onPlayerTickStart(TickEvent.PlayerTickEvent event) {
        if (ranged && event.phase == TickEvent.Phase.START && !event.player.world.isRemote) {
            stopRangedPreparation(event.player);
        }
    }

    @SubscribeEvent
    public void onLogin(PlayerEvent.PlayerLoggedInEvent event) {
        forget(event.player);
    }

    @SubscribeEvent
    public void onLogout(PlayerEvent.PlayerLoggedOutEvent event) {
        forget(event.player);
    }

    @SubscribeEvent
    public void onRespawn(PlayerEvent.PlayerRespawnEvent event) {
        forget(event.player);
    }

    @SubscribeEvent
    public void onDimensionChange(PlayerEvent.PlayerChangedDimensionEvent event) {
        forget(event.player);
    }

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public void onAttack(AttackEntityEvent event) {
        EntityPlayer attacker = event.getEntityPlayer();
        if (!melee || !(attacker instanceof EntityPlayerMP) || attacker.world.isRemote || !isRefused(attacker, Rule.CLIMBING_MELEE)) {
            return;
        }
        event.setCanceled(true);
        Refusals.refuse((EntityPlayerMP) attacker, Rule.CLIMBING_MELEE, Resync.inventory(), attacker.getHeldItemMainhand(),
                event.getTarget().getPosition());
    }

    /** Refuses a ranged item that acts at once; the client already spent one, so the inventory is resent. */
    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public void onRightClickItem(PlayerInteractEvent.RightClickItem event) {
        EntityPlayer player = event.getEntityPlayer();
        ItemStack stack = event.getItemStack();
        if (!ranged || !(player instanceof EntityPlayerMP) || player.world.isRemote || !RangedItems.actsAtOnce(stack)
                || !RangedItems.isRanged(stack) || !isRefused(player, Rule.CLIMBING_RANGED)) {
            return;
        }
        event.setCanceled(true);
        event.setCancellationResult(EnumActionResult.FAIL);
        Refusals.refuse((EntityPlayerMP) player, Rule.CLIMBING_RANGED, Resync.inventory(), stack, null);
    }

    /** Refuses the release of a ranged item still in use; the use ends without firing. */
    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public void onUseStop(LivingEntityUseItemEvent.Stop event) {
        if (!ranged || !(event.getEntityLiving() instanceof EntityPlayerMP)) {
            return;
        }
        EntityPlayerMP player = (EntityPlayerMP) event.getEntityLiving();
        if (!RangedItems.isRanged(event.getItem()) || !isRefused(player, Rule.CLIMBING_RANGED)) {
            return;
        }
        event.setCanceled(true);
        Refusals.refuse(player, Rule.CLIMBING_RANGED, Resync.inventory(), event.getItem(), null);
    }

    /** Refuses a bow release reached without the stop event, for example when another mod releases it. */
    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public void onArrowLoose(ArrowLooseEvent event) {
        EntityPlayer player = event.getEntityPlayer();
        if (!ranged || !(player instanceof EntityPlayerMP) || player.world.isRemote || !isRefused(player, Rule.CLIMBING_RANGED)) {
            return;
        }
        event.setCanceled(true);
        Refusals.refuse((EntityPlayerMP) player, Rule.CLIMBING_RANGED, Resync.inventory(), event.getBow(), null);
    }

    private static void stopRangedPreparation(EntityPlayer player) {
        if (!player.isHandActive() || !(player instanceof EntityPlayerMP)) {
            return;
        }
        ItemStack active = player.getActiveItemStack();
        if (!RangedItems.isRanged(active) || !isRefused(player, Rule.CLIMBING_RANGED)) {
            return;
        }
        player.resetActiveHand();
        Refusals.refuse((EntityPlayerMP) player, Rule.CLIMBING_RANGED, Resync.inventory(), active, null);
        LateResend.schedule(player);
    }

    private static boolean isRefused(EntityPlayer player, Rule rule) {
        return PlayerExemptions.applies(player, rule) && ClimbingTracker.isRestricted(player);
    }

    private static void forget(EntityPlayer player) {
        ClimbingTracker.forget(player);
        LateResend.forget(player);
    }
}
