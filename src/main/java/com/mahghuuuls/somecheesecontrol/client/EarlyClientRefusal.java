package com.mahghuuuls.somecheesecontrol.client;

import com.mahghuuuls.somecheesecontrol.config.Settings;
import com.mahghuuuls.somecheesecontrol.core.PlayerExemptions;
import com.mahghuuuls.somecheesecontrol.core.PlayerSituation;
import com.mahghuuuls.somecheesecontrol.core.Rule;
import com.mahghuuuls.somecheesecontrol.feature.climbing.ClimbingItemRules;
import com.mahghuuuls.somecheesecontrol.feature.climbing.ClimbingItemRules.Moment;
import com.mahghuuuls.somecheesecontrol.feature.climbing.ClimbingRules;
import com.mahghuuuls.somecheesecontrol.feature.climbing.ClimbingTracker;
import com.mahghuuuls.somecheesecontrol.feature.liquid.LiquidPlacement;
import net.minecraft.client.Minecraft;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.util.EnumActionResult;
import net.minecraft.util.NonNullList;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.entity.living.LivingEntityUseItemEvent;
import net.minecraftforge.event.entity.player.AttackEntityEvent;
import net.minecraftforge.event.entity.player.FillBucketEvent;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;
import net.minecraftforge.fml.common.eventhandler.EventPriority;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;
import net.minecraftforge.fml.common.network.FMLNetworkEvent;

import javax.annotation.Nullable;

/**
 * Refuses the client's own player's actions before the client shows them, by the rules of the
 * server it is connected to, so a refused action shows only the server's message.
 *
 * <p>The client runs its own copy of every action first. Cancelling the client's event stops that
 * copy (no liquid, no sound, no swing or item movement, no draw) while the click still reaches the
 * server, which makes the real decision, sends the message and corrects anything the two sides saw
 * differently. Nothing here sends a message or a packet of its own.</p>
 *
 * <p>One right click is one action: after a refused step, the remaining steps of the same click
 * (the other hand too) are refused, as the server does. A cancelled step must not answer
 * {@code SUCCESS}, which would end the click with a swing or an item movement.</p>
 *
 * <p>The server still resends the inventory after its own refusal, to correct a client that judged
 * differently. That resend replaces the held items with new copies, and Forge replays the hand's
 * equip movement for any new copy, even an identical one. For a short time after this client
 * refused something, a held item that came back unchanged is kept as the same copy, so the hand does
 * not move; a changed item still shows the correction.</p>
 *
 * <p>A melee attack is refused too; its arm swing and attack bar reset come from code no event
 * reaches, which the swing Mixins hide where MixinBooter is present ({@link SwingHiding}). Items the
 * server lets start and stops at once (bows, crossbows, throwing weapons) are refused here at the
 * click, so no draw shows; a use begun before the climbing state is ended with
 * {@code resetActiveHand}, which, unlike a stop, releases nothing.</p>
 *
 * <p>In single player the integrated server shares the event bus, so every handler acts only for
 * the client's own player in the client world. Before the server's rules arrive, and on a server
 * without this mod, nothing is refused.</p>
 */
public final class EarlyClientRefusal {

    /** How long after a refusal an unchanged held item keeps its copy: the server's immediate and late resends, with a margin. */
    private static final int KEEP_HELD_TICKS = 40;

    /** The rules the state below belongs to; a new connection or new rules start it again. */
    @Nullable
    private Settings settings;
    private ClimbingItemRules climbingRules;
    @Nullable
    private EntityPlayer trackedPlayer;
    private long lastHoldingTick = ClimbingRules.NEVER;
    private long refusedClickTick = Long.MIN_VALUE;
    private long keepHeldUntilTick = Long.MIN_VALUE;
    private ItemStack lastMainHand = ItemStack.EMPTY;
    private int lastSelectedSlot = -1;
    private ItemStack lastOffHand = ItemStack.EMPTY;

    private EarlyClientRefusal() {
    }

    /** Registers the handlers. Call once, at initialization, on the physical client only. */
    public static void register() {
        EarlyClientRefusal refusal = new EarlyClientRefusal();
        SwingHiding.attach(refusal);
        MinecraftForge.EVENT_BUS.register(refusal);
    }

    @SubscribeEvent
    public void onDisconnect(FMLNetworkEvent.ClientDisconnectionFromServerEvent event) {
        Settings.clearClientCopy();
    }

    /**
     * At the start of a client tick, after the packets that arrived were applied and before the hand
     * is drawn for this tick, an unchanged held item gets its previous copy back; at the end, the
     * held items are remembered for the next tick.
     */
    @SubscribeEvent
    public void onClientTick(TickEvent.ClientTickEvent event) {
        EntityPlayer player = Minecraft.getMinecraft().player;
        if (player == null || current(player) == null) {
            return;
        }
        if (event.phase == TickEvent.Phase.START) {
            if (now(player) <= keepHeldUntilTick && player.inventory.currentItem == lastSelectedSlot) {
                keepCopy(player.inventory.mainInventory, player.inventory.currentItem, lastMainHand);
                keepCopy(player.inventory.offHandInventory, 0, lastOffHand);
            }
            return;
        }
        lastMainHand = player.getHeldItemMainhand();
        lastSelectedSlot = player.inventory.currentItem;
        lastOffHand = player.getHeldItemOffhand();
        SwingHiding.clearAttack();
    }

    /** For the client's own player: a refused use in progress ends before its use tick; the climbing state is updated after the move. */
    @SubscribeEvent
    public void onPlayerTick(TickEvent.PlayerTickEvent event) {
        if (ownPlayerRules(event.player) == null) {
            return;
        }
        if (event.phase == TickEvent.Phase.START) {
            stopRefusedUse(event.player);
        } else {
            lastHoldingTick = ClimbingRules.update(lastHoldingTick, PlayerSituation.isOnClimbableBlock(event.player),
                    ClimbingTracker.isSupported(event.player), now(event.player));
        }
    }

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public void onFillBucket(FillBucketEvent event) {
        EntityPlayer player = event.getEntityPlayer();
        Settings rules = ownPlayerRules(player);
        if (rules != null && LiquidPlacement.check(rules, player, event.getWorld(), event.getEmptyBucket(), event.getTarget()) != null) {
            event.setCanceled(true);
            refusedClickTick = clickTick(player);
            keepHeldItems(player);
        }
    }

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public void onRightClickItem(PlayerInteractEvent.RightClickItem event) {
        if (refuseStep(event.getEntityPlayer(), event.getItemStack(), Moment.CLICK)) {
            cancel(event);
        }
    }

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public void onRightClickBlock(PlayerInteractEvent.RightClickBlock event) {
        if (refuseStep(event.getEntityPlayer(), event.getItemStack(), Moment.BLOCK_OR_ENTITY)) {
            cancel(event);
        }
    }

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public void onEntityInteract(PlayerInteractEvent.EntityInteract event) {
        if (refuseStep(event.getEntityPlayer(), event.getItemStack(), Moment.BLOCK_OR_ENTITY)) {
            cancel(event);
        }
    }

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public void onEntityInteractSpecific(PlayerInteractEvent.EntityInteractSpecific event) {
        if (refuseStep(event.getEntityPlayer(), event.getItemStack(), Moment.BLOCK_OR_ENTITY)) {
            cancel(event);
        }
    }

    /**
     * A held use the click did not start: the server's own start of a drawn item reaches this client
     * as its hand-active flag, and the client then starts the same use; refusing it here keeps the
     * draw hidden.
     */
    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public void onUseStart(LivingEntityUseItemEvent.Start event) {
        if (event.getEntityLiving() instanceof EntityPlayer
                && refuseStep((EntityPlayer) event.getEntityLiving(), event.getItem(), Moment.CLICK)) {
            event.setCanceled(true);
        }
    }

    /** The attack's own effects on the client; the swing itself happens before this event. */
    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public void onAttack(AttackEntityEvent event) {
        EntityPlayer player = event.getEntityPlayer();
        if (ownPlayerRules(player) != null && climbingRules.melee()
                && isClimbingRefused(player, Rule.CLIMBING_MELEE, clickTick(player))) {
            event.setCanceled(true);
            keepHeldItems(player);
            SwingHiding.markRefusedAttack();
        }
    }

    /** True when the climbing rule refuses a melee attack by the client's own player in this click. */
    boolean refusesMeleeNow(EntityPlayer player) {
        return ownPlayerRules(player) != null && climbingRules.melee()
                && isClimbingRefused(player, Rule.CLIMBING_MELEE, clickTick(player));
    }

    /**
     * True when this step of a right click is refused: an earlier step of the same click was, or
     * using the item now breaks a climbing rule. A drawn ranged item is refused at its click here.
     */
    private boolean refuseStep(EntityPlayer player, ItemStack stack, Moment moment) {
        if (ownPlayerRules(player) == null) {
            return false;
        }
        if (refusedClickTick == clickTick(player)) {
            return true;
        }
        Rule rule = moment == Moment.CLICK ? climbingRules.clientClickRule(stack) : climbingRules.ruleFor(stack, moment);
        if (rule == null || !isClimbingRefused(player, rule, clickTick(player))) {
            return false;
        }
        refusedClickTick = clickTick(player);
        keepHeldItems(player);
        return true;
    }

    /** Ends a refused use in progress without releasing it, before this tick's use tick runs. */
    private void stopRefusedUse(EntityPlayer player) {
        if (!player.isHandActive()) {
            return;
        }
        Rule rule = climbingRules.ruleFor(player.getActiveItemStack(), Moment.IN_USE);
        if (rule != null && isClimbingRefused(player, rule, now(player))) {
            player.resetActiveHand();
            keepHeldItems(player);
        }
    }

    /** Whether the climbing rule refuses this player at {@code tick} of the client clock. */
    private boolean isClimbingRefused(EntityPlayer player, Rule rule, long tick) {
        return PlayerExemptions.applies(player, rule)
                && ClimbingRules.isRestricted(PlayerSituation.isOnClimbableBlock(player), ClimbingTracker.isSupported(player),
                lastHoldingTick, tick, settings.climbingReleaseDelayTicks());
    }

    /**
     * The client's clock: the player's own tick count. The player's tick handlers run after this
     * tick's count; the steps of a click run before it, so a click belongs to the coming tick
     * ({@link #clickTick}). That matches the server, which handles the click one tick after its last
     * climbing update, and so ends the release delay on the same tick as the server.
     */
    private static long now(EntityPlayer player) {
        return player.ticksExisted;
    }

    /**
     * The tick a click step belongs to; every step of one click shares it. A use the server started
     * reaches this client between ticks and is refused under the same value as the next click; that
     * click is not refused by it, because the cancelled start still marks the hand active, and the
     * game ignores right clicks while a hand is active.
     */
    private static long clickTick(EntityPlayer player) {
        return player.ticksExisted + 1L;
    }

    /** After a refusal, keeps unchanged held items as the same copies through the server's resends. */
    private void keepHeldItems(EntityPlayer player) {
        keepHeldUntilTick = now(player) + KEEP_HELD_TICKS;
    }

    /** Puts {@code previous} back when the slot now holds a new but identical copy of it. */
    private static void keepCopy(NonNullList<ItemStack> slots, int index, ItemStack previous) {
        ItemStack now = slots.get(index);
        if (now != previous && !previous.isEmpty() && ItemStack.areItemStacksEqual(now, previous)) {
            slots.set(index, previous);
        }
    }

    /** The server's rules when {@code player} is the client's own player in the client world; otherwise null. */
    @Nullable
    private Settings ownPlayerRules(EntityPlayer player) {
        if (!player.world.isRemote || player != Minecraft.getMinecraft().player) {
            return null;
        }
        return current(player);
    }

    /**
     * The current server's rules, with this class's state kept for them and for this player entity
     * (a respawn or a dimension change makes a new one); null without rules.
     */
    @Nullable
    private Settings current(EntityPlayer player) {
        Settings received = Settings.forSide(true);
        if (received != settings || player != trackedPlayer) {
            settings = received;
            climbingRules = received != null ? ClimbingItemRules.of(received) : null;
            trackedPlayer = player;
            lastHoldingTick = ClimbingRules.NEVER;
            refusedClickTick = Long.MIN_VALUE;
            keepHeldUntilTick = Long.MIN_VALUE;
            lastMainHand = ItemStack.EMPTY;
            lastSelectedSlot = -1;
            lastOffHand = ItemStack.EMPTY;
        }
        return received;
    }

    private static void cancel(PlayerInteractEvent event) {
        event.setCanceled(true);
        event.setCancellationResult(EnumActionResult.FAIL);
    }
}
