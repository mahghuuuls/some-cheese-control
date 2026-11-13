package com.mahghuuuls.somecheesecontrol.feature.climbing;

import com.mahghuuuls.somecheesecontrol.config.Settings;
import com.mahghuuuls.somecheesecontrol.core.PlayerSituation;
import com.mahghuuuls.somecheesecontrol.core.ServerState;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.server.MinecraftServer;
import net.minecraftforge.fml.common.FMLCommonHandler;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * The only owner of the restricted climbing state: per player, the last server tick the player held
 * on to a climbable block, kept in the per-server holder and never saved.
 *
 * <p>Other packages only ask {@link #isRestricted(EntityPlayer)}. The answer also looks at the live
 * situation, so the one tick between a player grabbing a ladder and the next update cannot let an
 * action through.</p>
 */
public final class ClimbingTracker {

    private static final ServerState.Slot<LastHolding> SLOT = new ServerState.Slot<>(LastHolding.class, LastHolding::new);

    private ClimbingTracker() {
    }

    /**
     * True when the player is in the restricted climbing state now.
     *
     * <p>The release delay is only known while the tracker is registered, which
     * {@link ClimbingFeature#isNeeded} decides; without it this answers from the live situation only.</p>
     */
    public static boolean isRestricted(EntityPlayer player) {
        return ClimbingRules.isRestricted(PlayerSituation.isOnClimbableBlock(player), isSupported(player), last(player), now(),
                Settings.get().climbingReleaseDelayTicks());
    }

    /** Records this tick's facts for the player; called once per player per server tick. */
    static void update(EntityPlayer player) {
        Map<UUID, Long> map = ServerState.get().get(SLOT).byPlayer;
        long updated = ClimbingRules.update(last(player), PlayerSituation.isOnClimbableBlock(player), isSupported(player), now());
        if (updated == ClimbingRules.NEVER) {
            map.remove(player.getUniqueID());
        } else {
            map.put(player.getUniqueID(), updated);
        }
    }

    /** Forgets the player, so the state is computed again from the new position. */
    static void forget(EntityPlayer player) {
        ServerState.get().get(SLOT).byPlayer.remove(player.getUniqueID());
    }

    /** Standing on the ground, or riding: the server does not update the ground flag of a rider. */
    private static boolean isSupported(EntityPlayer player) {
        return player.onGround || player.isRiding();
    }

    private static long last(EntityPlayer player) {
        Long value = ServerState.get().get(SLOT).byPlayer.get(player.getUniqueID());
        return value != null ? value : ClimbingRules.NEVER;
    }

    private static long now() {
        MinecraftServer server = FMLCommonHandler.instance().getMinecraftServerInstance();
        return server != null ? server.getTickCounter() : 0L;
    }

    /** Per-server map from player to the last tick that player held on. */
    static final class LastHolding {
        final Map<UUID, Long> byPlayer = new HashMap<>();
    }
}
