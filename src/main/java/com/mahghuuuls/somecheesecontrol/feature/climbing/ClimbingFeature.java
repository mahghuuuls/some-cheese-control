package com.mahghuuuls.somecheesecontrol.feature.climbing;

import com.mahghuuuls.somecheesecontrol.config.Settings;
import com.mahghuuuls.somecheesecontrol.core.PlayerExemptions;
import com.mahghuuuls.somecheesecontrol.core.Refusals;
import com.mahghuuuls.somecheesecontrol.core.Resync;
import com.mahghuuuls.somecheesecontrol.core.Rule;
import com.mahghuuuls.somecheesecontrol.feature.climbing.ClimbingItemRules.Moment;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.item.ItemStack;
import net.minecraft.util.EnumActionResult;
import net.minecraft.util.math.BlockPos;
import net.minecraftforge.event.entity.living.LivingEntityUseItemEvent;
import net.minecraftforge.event.entity.player.ArrowLooseEvent;
import net.minecraftforge.event.entity.player.AttackEntityEvent;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;
import net.minecraftforge.fml.common.eventhandler.EventPriority;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.PlayerEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;

import javax.annotation.Nullable;

/**
 * Keeps the climbing state current and refuses melee attacks, ranged items and blocklisted items
 * in it.
 *
 * <p>Melee refusal uses the attack event, which RLCombat also posts for both hands (as a subclass)
 * before any damage, durability or cooldown work; Spartan Weaponry's melee runs only after an
 * attack event that nobody cancelled.</p>
 *
 * <p>Ranged refusal has two paths. An item that acts at once (a snowball, a pearl, a splash potion,
 * dynamite, a fishing rod) is refused at the right click. An item that is drawn or charged first (a
 * bow, a crossbow, a throwing weapon) is let start and is stopped at the start of the player's next tick, before any
 * use tick runs. The server cannot tell whether the client already refused the same click, so it
 * keeps this path for every such click. The same stop ends a draw that began before the climbing state, so one path covers
 * both. The client runs its own draw; the stop changes the server's hand state and resends the
 * inventory, either of which ends the client's draw, and a late resend corrects an arrow the client
 * spent on a release made before the stop reached it. Nothing is spent before release or finish, so
 * a stop costs nothing, and a loaded crossbow stays loaded. The use tick event is never cancelled:
 * Forge then finishes the use at once.</p>
 *
 * <p>A blocklisted item is refused at every way of using it: the right click in the air, the whole
 * click on a block or on an entity with the hand that holds it, and the start of a held use; a held use begun
 * before the climbing state stops at the next tick, as a draw does. Which rule an item breaks is
 * decided by {@link ClimbingItemRules}. One right click arrives as several steps; a refused step
 * refuses the whole click, and only its first refused step writes a line and a message
 * ({@link Refusals#refuseClickStep}). A drawn item that is let start still makes its click refused,
 * so the other hand does nothing in that click.</p>
 */
public final class ClimbingFeature {

    private final ClimbingItemRules rules;

    public ClimbingFeature(Settings settings) {
        this.rules = ClimbingItemRules.of(settings);
    }

    /**
     * True when any climbing restriction is on, so the state must be tracked. Every climbing
     * restriction added later must be listed here, or it gets no release delay. An empty blocklist
     * adds nothing.
     */
    public static boolean isNeeded(Settings settings) {
        return settings.climbingMelee() || settings.climbingRanged()
                || (settings.climbingItemBlocklist() && !settings.blockedItems().isEmpty());
    }

    @SubscribeEvent
    public void onPlayerTick(TickEvent.PlayerTickEvent event) {
        if (event.phase != TickEvent.Phase.END || event.player.world.isRemote) {
            return;
        }
        ClimbingTracker.update(event.player);
        if (rules.ranged() || rules.hasBlockedItems()) {
            LateResend.runDue(event.player);
        }
    }

    /**
     * Stops a refused use in progress (ranged or blocklisted) at the start of the player's tick,
     * before the use ticks and before other mods' handlers of this phase see it.
     */
    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public void onPlayerTickStart(TickEvent.PlayerTickEvent event) {
        if (event.phase == TickEvent.Phase.START && !event.player.world.isRemote) {
            stopRefusedUse(event.player);
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
        if (!rules.melee() || !(attacker instanceof EntityPlayerMP) || attacker.world.isRemote || !isRefused(attacker, Rule.CLIMBING_MELEE)) {
            return;
        }
        event.setCanceled(true);
        Refusals.refuse((EntityPlayerMP) attacker, Rule.CLIMBING_MELEE, Resync.inventory(), attacker.getHeldItemMainhand(),
                event.getTarget().getPosition());
    }

    /** Refuses a right click in the air; the client already ran its own copy, so the inventory is resent. */
    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public void onRightClickItem(PlayerInteractEvent.RightClickItem event) {
        if (refuseClick(event.getEntityPlayer(), event.getItemStack(), Moment.CLICK, Resync.inventory(), null)) {
            event.setCanceled(true);
            event.setCancellationResult(EnumActionResult.FAIL);
        }
    }

    /**
     * Refuses the whole click on a block while the clicking hand holds a blocklisted item, because
     * some blocks use the held item in their own reaction. Only the blocklist counts here: a ranged
     * item is refused when it is thrown, not when it clicks a door. The server resends the clicked
     * block and its neighbour after every such click; the blocks above and below are resent too,
     * because a door changes its other half on the client.
     */
    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public void onRightClickBlock(PlayerInteractEvent.RightClickBlock event) {
        BlockPos pos = event.getPos();
        if (refuseClick(event.getEntityPlayer(), event.getItemStack(), Moment.BLOCK_OR_ENTITY,
                Resync.inventoryAndBlocks(pos.up(), pos.down()), pos)) {
            event.setCanceled(true);
            event.setCancellationResult(EnumActionResult.FAIL);
        }
    }

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public void onEntityInteract(PlayerInteractEvent.EntityInteract event) {
        if (refuseOnEntity(event.getEntityPlayer(), event.getItemStack(), event.getTarget())) {
            event.setCanceled(true);
            event.setCancellationResult(EnumActionResult.FAIL);
        }
    }

    /** The click at a point on an entity, which the client sends first (armor stands act on it alone). */
    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public void onEntityInteractSpecific(PlayerInteractEvent.EntityInteractSpecific event) {
        if (refuseOnEntity(event.getEntityPlayer(), event.getItemStack(), event.getTarget())) {
            event.setCanceled(true);
            event.setCancellationResult(EnumActionResult.FAIL);
        }
    }

    /**
     * Refuses the start of a held use of a blocklisted item, however the use was started. A drawn
     * ranged item is let start here and stopped at the next tick, but its click counts as refused.
     */
    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public void onUseStart(LivingEntityUseItemEvent.Start event) {
        if (!(event.getEntityLiving() instanceof EntityPlayer)) {
            return;
        }
        EntityPlayer player = (EntityPlayer) event.getEntityLiving();
        if (refuseClick(player, event.getItem(), Moment.CLICK, Resync.inventory(), null)) {
            event.setCanceled(true);
        } else if (rules.isDrawnRanged(event.getItem()) && player instanceof EntityPlayerMP && !player.world.isRemote
                && isRefused(player, Rule.CLIMBING_RANGED)) {
            Refusals.markClickRefused((EntityPlayerMP) player);
        }
    }

    /** Refuses the release of a ranged or blocklisted item still in use; the use ends without effect. */
    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public void onUseStop(LivingEntityUseItemEvent.Stop event) {
        if (!(event.getEntityLiving() instanceof EntityPlayerMP)) {
            return;
        }
        EntityPlayerMP player = (EntityPlayerMP) event.getEntityLiving();
        Rule rule = rules.ruleFor(event.getItem(), Moment.IN_USE);
        if (rule == null || !isRefused(player, rule)) {
            return;
        }
        event.setCanceled(true);
        refuseUse(player, rule, Resync.inventory(), event.getItem(), null);
    }

    /**
     * Refuses a bow release reached without the stop event, for example when another mod releases
     * it. A bow release is ranged whatever the bow's class.
     */
    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public void onArrowLoose(ArrowLooseEvent event) {
        EntityPlayer player = event.getEntityPlayer();
        Rule rule = rules.ranged() ? Rule.CLIMBING_RANGED : rules.ruleFor(event.getBow(), Moment.IN_USE);
        if (rule == null || !(player instanceof EntityPlayerMP) || player.world.isRemote || !isRefused(player, rule)) {
            return;
        }
        event.setCanceled(true);
        refuseUse((EntityPlayerMP) player, rule, Resync.inventory(), event.getBow(), null);
    }

    private boolean refuseOnEntity(EntityPlayer player, ItemStack stack, Entity target) {
        return refuseClick(player, stack, Moment.BLOCK_OR_ENTITY, Resync.inventoryAndEntity(target), target.getPosition());
    }

    /** True when this click step must be refused; the caller cancels its event. */
    private boolean refuseClick(EntityPlayer player, ItemStack stack, Moment moment, Resync resync, @Nullable BlockPos target) {
        Rule rule = rules.ruleFor(stack, moment);
        if (rule == null || !(player instanceof EntityPlayerMP) || player.world.isRemote || !isRefused(player, rule)) {
            return false;
        }
        EntityPlayerMP mp = (EntityPlayerMP) player;
        Refusals.refuseClickStep(mp, rule, resync, stack, target);
        scheduleLateResend(mp, stack);
        return true;
    }

    /** Stops a ranged or blocklisted use in progress, as if it had never started. */
    private void stopRefusedUse(EntityPlayer player) {
        if (!player.isHandActive() || !(player instanceof EntityPlayerMP)) {
            return;
        }
        ItemStack active = player.getActiveItemStack();
        Rule rule = rules.ruleFor(active, Moment.IN_USE);
        if (rule == null || !isRefused(player, rule)) {
            return;
        }
        player.resetActiveHand();
        refuseUse((EntityPlayerMP) player, rule, Resync.inventory(), active, null);
    }

    /** One refusal of an item use, with the late resend for an item the client may be using on its own. */
    private static void refuseUse(EntityPlayerMP player, Rule rule, Resync resync, ItemStack stack, @Nullable BlockPos target) {
        Refusals.refuse(player, rule, resync, stack, target);
        scheduleLateResend(player, stack);
    }

    private static void scheduleLateResend(EntityPlayerMP player, ItemStack stack) {
        if (!RangedItems.actsAtOnce(stack)) {
            LateResend.schedule(player);
        }
    }

    private static boolean isRefused(EntityPlayer player, Rule rule) {
        return PlayerExemptions.applies(player, rule) && ClimbingTracker.isRestricted(player);
    }

    private static void forget(EntityPlayer player) {
        ClimbingTracker.forget(player);
        LateResend.forget(player);
    }
}
