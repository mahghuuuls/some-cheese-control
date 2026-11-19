package com.mahghuuuls.somecheesecontrol.feature.vehiclefall;

import com.mahghuuuls.somecheesecontrol.config.Settings;
import com.mahghuuuls.somecheesecontrol.core.DiagnosticLog;
import com.mahghuuuls.somecheesecontrol.core.PlayerSituation;
import com.mahghuuuls.somecheesecontrol.core.PlayerSituation.Support;
import com.mahghuuuls.somecheesecontrol.core.ServerState;
import com.mahghuuuls.somecheesecontrol.feature.vehiclefall.VehicleFallRules.Action;
import net.minecraft.entity.Entity;
import net.minecraft.entity.item.EntityBoat;
import net.minecraft.entity.item.EntityMinecart;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.world.WorldServer;
import net.minecraft.world.chunk.Chunk;
import net.minecraftforge.event.entity.EntityMountEvent;
import net.minecraftforge.event.entity.living.LivingDeathEvent;
import net.minecraftforge.event.entity.living.LivingFallEvent;
import net.minecraftforge.fml.common.eventhandler.EventPriority;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.PlayerEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;

import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;

import javax.annotation.Nullable;

/**
 * The only owner of vehicle falls: the pending fall of a player who boards a boat or minecart while
 * falling, the descent of a boat a player rides, and when and how that fall reaches the player. The
 * decisions are in {@link VehicleFallRules}; this class gathers the facts and keeps the state.
 *
 * <p>A riding player's own fall is reset every tick, and a boat throws its fall away when it lands,
 * so a boat's fall is counted here from the boat's height, tick by tick, independent of the boat's
 * own stored fall (which other mods change). When the boat lands on a block, the player takes the
 * whole fall through the game's normal fall method, so fall events, Feather Falling and other fall
 * protection apply as for a normal fall. Water ends a fall without damage.</p>
 *
 * <p>A minecart already passes its fall to its rider when it lands off rails, and rails and water
 * reset it. So the pending fall of a player boarding a minecart is added to the minecart's own fall,
 * and the game does the rest. When the tracking ends any other way, the added part is taken back
 * out, so the minecart (which the game saves with its rider and in its chunk) keeps only its own fall.</p>
 *
 * <p>A pending fall goes on when the player leaves the vehicle in the air (it becomes the player's
 * own fall) or boards another vehicle (the new boarding takes the old vehicle's fall). It is gone
 * after a teleport, a dimension change, death, logout, an orderly server stop, or in Creative or
 * Spectator. A fall reaches the player once: when the game already passed a fall to the player in
 * the same tick (another mod breaking the boat on landing), none is added.</p>
 *
 * <p>Inside one server tick, movement packets (a player-steered boat moves here) come first, then
 * the world's entities (minecarts, boats steered by nobody), then each player's tick, where this
 * class samples and decides.</p>
 */
public final class VehicleFallTracker {

    private static final ServerState.Slot<Falls> SLOT = new ServerState.Slot<>(Falls.class, Falls::new);

    /** Where a player's tracked fall is. */
    private enum Phase {
        /** Boarded this tick; the mount is confirmed at the end of the tick, or undone. */
        BOARDING,
        /** Riding a boat: the fall counts the boat's descent. */
        BOAT,
        /** Riding a minecart: {@code fall} was added to the minecart's own fall. */
        MINECART,
        /** Left the vehicle in the air this tick; the fall is now the player's own. */
        CARRIED
    }

    /** True while this tracker applies a fall, so its own fall event is not taken for the game's. */
    private boolean applying;

    /** True when vehicle fall damage is on, so the tracker must be registered. */
    public static boolean isNeeded(Settings settings) {
        return settings.vehicleFall();
    }

    /** The fall the player would take if the vehicle landed on a block now; 0 when none is tracked. */
    public static float pendingFall(EntityPlayer player) {
        Entry entry = falls().byPlayer.get(player.getUniqueID());
        return entry != null ? liveFall(entry, player.getRidingEntity()) : 0F;
    }

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public void onMount(EntityMountEvent event) {
        if (event.getWorldObj().isRemote || !(event.getEntityMounting() instanceof EntityPlayerMP)) {
            return;
        }
        EntityPlayerMP player = (EntityPlayerMP) event.getEntityMounting();
        if (event.isMounting()) {
            onBoarding(player, event.getEntityBeingMounted());
        } else {
            onLeaving(player, event.getEntityBeingMounted());
        }
    }

    @SubscribeEvent
    public void onPlayerTick(TickEvent.PlayerTickEvent event) {
        if (event.phase != TickEvent.Phase.END || event.player.world.isRemote || !(event.player instanceof EntityPlayerMP)
                || !event.player.isEntityAlive()) {
            return;
        }
        EntityPlayerMP player = (EntityPlayerMP) event.player;
        Map<UUID, Entry> byPlayer = falls().byPlayer;
        Entry entry = byPlayer.get(player.getUniqueID());
        Entity vehicle = player.getRidingEntity();
        if (entry == null) {
            if (isTrackedBoat(vehicle) && !isExempt(player)) {
                byPlayer.put(player.getUniqueID(), new Entry(Phase.BOAT, 0F, vehicle, vehicle));
            }
            return;
        }
        if (isExempt(player)) {
            end(player, entry, "creative or spectator");
            return;
        }
        switch (entry.phase) {
            case CARRIED:
                if (VehicleFallRules.isTeleport(player.posX - entry.x, player.posY - entry.y, player.posZ - entry.z)) {
                    player.fallDistance = 0F;
                    log("fall-cleared", player, entry.fall, "reason", "teleport");
                }
                byPlayer.remove(player.getUniqueID());
                return;
            case BOARDING:
                settleBoarding(player, entry, vehicle);
                return;
            default:
                break;
        }
        if (vehicle == null || !vehicle.getUniqueID().equals(entry.vehicle)) {
            end(player, entry, "not riding");
            return;
        }
        if (entry.phase == Phase.MINECART) {
            followMinecart(player, entry, vehicle);
        } else {
            followBoat(player, entry, vehicle);
        }
    }

    /** The game passed a fall to the player (a normal landing, a minecart, or another mod): remember it, so none is added. */
    @SubscribeEvent
    public void onFall(LivingFallEvent event) {
        if (applying || !(event.getEntityLiving() instanceof EntityPlayerMP) || event.getEntityLiving().world.isRemote) {
            return;
        }
        EntityPlayerMP player = (EntityPlayerMP) event.getEntityLiving();
        Falls falls = falls();
        falls.lastFallTick.put(player.getUniqueID(), ServerState.tick());
        Entry entry = falls.byPlayer.get(player.getUniqueID());
        if (entry == null) {
            return;
        }
        if (entry.phase == Phase.MINECART) {
            log("fall-applied", player, event.getDistance(), "by", "the game (minecart)");
            falls.byPlayer.remove(player.getUniqueID());
        } else if (entry.phase == Phase.BOAT && entry.fall >= VehicleFallRules.MIN_FALL) {
            log("fall-applied", player, event.getDistance(), "by", "the game (boat)");
            entry.fall = 0F;
        }
    }

    @SubscribeEvent
    public void onDeath(LivingDeathEvent event) {
        if (event.getEntityLiving() instanceof EntityPlayerMP && !event.getEntityLiving().world.isRemote) {
            forget((EntityPlayer) event.getEntityLiving(), "death");
        }
    }

    @SubscribeEvent
    public void onLogout(PlayerEvent.PlayerLoggedOutEvent event) {
        forget(event.player, "logout");
    }

    @SubscribeEvent
    public void onRespawn(PlayerEvent.PlayerRespawnEvent event) {
        forget(event.player, "respawn");
    }

    /** A fall carried into the new dimension would be a fall the player never made there. */
    @SubscribeEvent
    public void onDimensionChange(PlayerEvent.PlayerChangedDimensionEvent event) {
        Entry entry = falls().byPlayer.get(event.player.getUniqueID());
        if (entry != null && entry.phase == Phase.CARRIED) {
            event.player.fallDistance = 0F;
        }
        forget(event.player, "dimension change");
    }

    /**
     * Records the fall so far when a player boards a boat or minecart: the player's own fall plus the
     * fall of the vehicle the player rides now, if any (a switch from one vehicle to another). The
     * previous record is kept until the mount is confirmed. Boarding anything else is left to the game.
     */
    private void onBoarding(EntityPlayerMP player, Entity vehicle) {
        boolean boat = isTrackedBoat(vehicle);
        if (isExempt(player) || !(boat || vehicle instanceof EntityMinecart)) {
            return;
        }
        Map<UUID, Entry> byPlayer = falls().byPlayer;
        Entry previous = byPlayer.get(player.getUniqueID());
        float fall = player.fallDistance + (previous != null ? liveFall(previous, player.getRidingEntity()) : 0F);
        if (!boat && fall < VehicleFallRules.MIN_FALL) {
            return;
        }
        Entry boarding = new Entry(Phase.BOARDING, fall, vehicle, vehicle);
        boarding.heldUp = PlayerSituation.vehicleSupport(vehicle) != Support.NONE;
        boarding.previous = previous;
        byPlayer.put(player.getUniqueID(), boarding);
        if (fall >= VehicleFallRules.MIN_FALL) {
            log("fall-pending", player, fall, "vehicle", vehicle.getClass().getSimpleName());
        }
    }

    /**
     * A player leaving a vehicle: in the air the fall goes on as the player's own; a boat that just
     * landed gives its fall now; water, rails and a fall the game already passed end it. A switch to
     * another vehicle moved the fall to the new boarding already.
     */
    private void onLeaving(EntityPlayerMP player, Entity vehicle) {
        Map<UUID, Entry> byPlayer = falls().byPlayer;
        Entry entry = byPlayer.get(player.getUniqueID());
        if (entry == null) {
            return;
        }
        if (entry.phase == Phase.BOARDING && entry.previous != null && vehicle.getUniqueID().equals(entry.previous.vehicle)) {
            takeBackPending(entry.previous, vehicle);
            return;
        }
        if (entry.phase == Phase.CARRIED || entry.phase == Phase.BOARDING || !vehicle.getUniqueID().equals(entry.vehicle)) {
            return;
        }
        float fall = liveFall(entry, vehicle);
        takeBackPending(entry, vehicle);
        boolean minecart = entry.phase == Phase.MINECART;
        Action action = VehicleFallRules.atLeaving(PlayerSituation.vehicleSupport(vehicle), fellThisTick(player), fall,
                minecart, minecart && PlayerSituation.isOnRail(vehicle));
        switch (action) {
            case CARRY:
                player.fallDistance += fall;
                byPlayer.put(player.getUniqueID(), new Entry(Phase.CARRIED, fall, vehicle, player));
                log("fall-carried", player, fall, "vehicle", vehicle.getClass().getSimpleName());
                break;
            case APPLY:
                byPlayer.remove(player.getUniqueID());
                applyFall(player, fall);
                break;
            default:
                byPlayer.remove(player.getUniqueID());
                if (fall >= VehicleFallRules.MIN_FALL && !fellThisTick(player)) {
                    log("fall-cleared", player, fall, "reason", "left on water or rails");
                }
                break;
        }
    }

    /**
     * At the end of the boarding tick: a confirmed minecart gets the pending fall added; a failed
     * mount brings back the record of the vehicle the player still rides, or ends the tracking (the
     * player's own fall goes on as the game keeps it).
     */
    private void settleBoarding(EntityPlayerMP player, Entry entry, @Nullable Entity vehicle) {
        Map<UUID, Entry> byPlayer = falls().byPlayer;
        Entry previous = entry.previous;
        entry.previous = null;
        if (vehicle != null && vehicle.getUniqueID().equals(entry.vehicle)) {
            if (vehicle instanceof EntityMinecart && fellThisTick(player)) {
                // The minecart landed this tick, between the boarding and now, and passed its own fall.
                byPlayer.remove(player.getUniqueID());
            } else if (vehicle instanceof EntityMinecart) {
                vehicle.fallDistance += entry.fall;
                entry.phase = Phase.MINECART;
                entry.track(vehicle);
            } else {
                entry.phase = Phase.BOAT;
                followBoat(player, entry, vehicle);
            }
        } else if (previous != null && vehicle != null && vehicle.getUniqueID().equals(previous.vehicle)) {
            byPlayer.put(player.getUniqueID(), previous);
        } else {
            byPlayer.remove(player.getUniqueID());
        }
    }

    /** One tick of a ridden minecart: a teleport ends the pending fall; a rail or water reset it. */
    private static void followMinecart(EntityPlayerMP player, Entry entry, Entity cart) {
        if (VehicleFallRules.isTeleport(cart.posX - entry.x, cart.posY - entry.y, cart.posZ - entry.z)) {
            end(player, entry, "teleport");
        } else if (VehicleFallRules.minecartWasReset(cart.fallDistance, entry.fall)) {
            log("fall-cleared", player, entry.fall, "reason", "rail or water");
            falls().byPlayer.remove(player.getUniqueID());
        } else {
            entry.track(cart);
        }
    }

    /** One tick of a ridden boat: count its descent, and on landing give the fall once or drop it in water. */
    private void followBoat(EntityPlayerMP player, Entry entry, Entity boat) {
        if (boat.isRiding() || VehicleFallRules.isTeleport(boat.posX - entry.x, boat.posY - entry.y, boat.posZ - entry.z)) {
            // A boat carried by something else falls with it, as the game does not count it either; a teleport is no fall.
            if (!boat.isRiding() && entry.fall >= VehicleFallRules.MIN_FALL) {
                log("fall-cleared", player, entry.fall, "reason", "teleport");
            }
            entry.fall = 0F;
            entry.heldUp = true;
            entry.track(boat);
            return;
        }
        entry.fall = VehicleFallRules.afterMove(entry.fall, entry.y, boat.posY, entry.heldUp);
        entry.track(boat);
        Support support = PlayerSituation.vehicleSupport(boat);
        entry.heldUp = support != Support.NONE;
        switch (VehicleFallRules.atBoatTick(support, fellThisTick(player), entry.fall)) {
            case APPLY:
                applyFall(player, entry.fall);
                entry.fall = 0F;
                break;
            case CLEAR:
                if (entry.fall >= VehicleFallRules.MIN_FALL && !fellThisTick(player)) {
                    log("fall-cleared", player, entry.fall, "reason", "water");
                }
                entry.fall = 0F;
                break;
            default:
                break;
        }
    }

    /** Through the game's normal fall method, so fall events and fall protection apply. */
    private void applyFall(EntityPlayerMP player, float fall) {
        applying = true;
        try {
            player.fall(fall, 1.0F);
        } finally {
            applying = false;
        }
        log("fall-applied", player, fall, "by", "this mod (boat)");
    }

    /** Ends the tracking of a ridden vehicle, taking a minecart's added pending fall back out. */
    private static void end(EntityPlayer player, Entry entry, String reason) {
        Entity vehicle = vehicleOf(player, entry);
        float fall = liveFall(entry, vehicle);
        if (vehicle != null) {
            takeBackPending(entry, vehicle);
        }
        // A boarding that never settled, while the player still rides the minecart of the record
        // before it: that minecart keeps the added fall until it is taken back here. Once the player
        // has left it, the leaving already took the fall back out.
        Entity ridden = player.getRidingEntity();
        if (entry.previous != null && ridden != null && ridden.getUniqueID().equals(entry.previous.vehicle)) {
            takeBackPending(entry.previous, ridden);
        }
        falls().byPlayer.remove(player.getUniqueID());
        if (fall >= VehicleFallRules.MIN_FALL) {
            log("fall-cleared", player, fall, "reason", reason);
        }
    }

    /**
     * At an orderly server stop, before the game saves: ends every tracked ride, so a minecart is
     * saved with only its own fall and a pending fall does not outlive the server. The stop saves the
     * players (a ridden minecart with them) and the chunks without logging the players out first, so
     * the logout path does not run in time on a dedicated server.
     */
    public static void releaseAll(Iterable<EntityPlayerMP> online) {
        for (EntityPlayerMP player : online) {
            forget(player, "server stopping");
        }
    }

    private static void forget(EntityPlayer player, String reason) {
        Falls falls = falls();
        Entry entry = falls.byPlayer.get(player.getUniqueID());
        if (entry != null) {
            end(player, entry, reason);
        }
        falls.lastFallTick.remove(player.getUniqueID());
    }

    /**
     * A minecart keeps only its own fall once the tracker no longer follows its rider. Its chunk is
     * marked changed, so a save after this one writes the corrected minecart too.
     */
    private static void takeBackPending(Entry entry, Entity vehicle) {
        if (entry.phase == Phase.MINECART && vehicle.getUniqueID().equals(entry.vehicle)) {
            vehicle.fallDistance = VehicleFallRules.withoutPending(vehicle.fallDistance, entry.fall);
            // The chunk the minecart is registered in is the one that saves it.
            Chunk chunk = vehicle.addedToChunk ? vehicle.world.getChunkProvider().getLoadedChunk(vehicle.chunkCoordX, vehicle.chunkCoordZ) : null;
            if (chunk != null) {
                chunk.markDirty();
            }
        }
    }

    /** The fall the player would take from this record now. */
    private static float liveFall(Entry entry, @Nullable Entity vehicle) {
        boolean riding = vehicle != null && vehicle.getUniqueID().equals(entry.vehicle);
        switch (entry.phase) {
            case BOAT:
                return riding ? VehicleFallRules.afterMove(entry.fall, entry.y, vehicle.posY, entry.heldUp) : entry.fall;
            case MINECART:
                return riding ? vehicle.fallDistance : entry.fall;
            case BOARDING:
                return entry.fall;
            default:
                return 0F;
        }
    }

    /** The vehicle of this record: the one ridden, or found by its id in the player's world. */
    @Nullable
    private static Entity vehicleOf(EntityPlayer player, Entry entry) {
        Entity ridden = player.getRidingEntity();
        if (ridden != null && ridden.getUniqueID().equals(entry.vehicle)) {
            return ridden;
        }
        return player.world instanceof WorldServer ? ((WorldServer) player.world).getEntityFromUuid(entry.vehicle) : null;
    }

    private static boolean fellThisTick(EntityPlayer player) {
        Long tick = falls().lastFallTick.get(player.getUniqueID());
        return tick != null && tick == ServerState.tick();
    }

    /** Creative and Spectator players take no fall damage; their tracking ends. */
    private static boolean isExempt(EntityPlayer player) {
        return player.capabilities.isCreativeMode || player.isSpectator();
    }

    /**
     * A boat a fall is counted for: a vanilla boat or a modded boat built on it, except Davinci's
     * Vessels ships (MovingWorld), which are whole player-built ships that extend the boat class.
     */
    static boolean isTrackedBoat(@Nullable Entity vehicle) {
        if (!(vehicle instanceof EntityBoat)) {
            return false;
        }
        for (Class<?> type = vehicle.getClass(); type != null; type = type.getSuperclass()) {
            if ("EntityMovingWorld".equals(type.getSimpleName())) {
                return false;
            }
        }
        return true;
    }

    private static void log(String event, EntityPlayer player, float height, String key, Object value) {
        if (DiagnosticLog.enabled()) {
            DiagnosticLog.decision(event, "player", player.getName(), "height", String.format(Locale.ROOT, "%.1f", height), key, value);
        }
    }

    private static Falls falls() {
        return ServerState.get().get(SLOT);
    }

    /** One player's tracked fall. */
    private static final class Entry {
        Phase phase;
        float fall;
        final UUID vehicle;
        /** The last known position of what is followed: the vehicle while riding, the player when carried. */
        double x;
        double y;
        double z;
        /** For a boat: something held it up at the last sample, so its next drop is a step, not a fall. */
        boolean heldUp = true;
        /** For a boarding: the record of the vehicle ridden before, kept until the mount is confirmed. */
        @Nullable
        Entry previous;

        Entry(Phase phase, float fall, Entity vehicle, Entity followed) {
            this.phase = phase;
            this.fall = fall;
            this.vehicle = vehicle.getUniqueID();
            track(followed);
        }

        void track(Entity followed) {
            x = followed.posX;
            y = followed.posY;
            z = followed.posZ;
        }
    }

    /** Per-server state: tracked falls, and the tick of each player's last fall the game passed on. */
    static final class Falls {
        final Map<UUID, Entry> byPlayer = new HashMap<>();
        final Map<UUID, Long> lastFallTick = new HashMap<>();
    }
}
