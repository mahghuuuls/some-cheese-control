package com.mahghuuuls.somecheesecontrol.feature.climbing;

import com.mahghuuuls.somecheesecontrol.core.Refusals;
import com.mahghuuuls.somecheesecontrol.core.Resync;
import com.mahghuuuls.somecheesecontrol.core.ServerState;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.EntityPlayerMP;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * One more inventory resend, a short time after the server stopped a draw.
 *
 * <p>The client learns of the stop only one round trip later. A release in that window makes the
 * client spend an arrow on its own while the server spends nothing; the late resend shows the true
 * inventory again. Each stop also resends at once, so repeated stops never hold back the correction
 * of an earlier release; the late resend only has to cover the last one.</p>
 */
final class LateResend {

    /** Half a second: longer than a normal round trip, within the one second the client may be wrong. */
    static final int DELAY_TICKS = 10;

    private static final ServerState.Slot<Due> SLOT = new ServerState.Slot<>(Due.class, Due::new);

    private LateResend() {
    }

    static void schedule(EntityPlayer player) {
        ServerState.get().get(SLOT).byPlayer.put(player.getUniqueID(), ClimbingTracker.now() + DELAY_TICKS);
    }

    /** Sends the resend when it is due; called once per player per server tick. */
    static void runDue(EntityPlayer player) {
        Map<UUID, Long> map = ServerState.get().get(SLOT).byPlayer;
        Long due = map.get(player.getUniqueID());
        if (due != null && ClimbingTracker.now() >= due && player instanceof EntityPlayerMP) {
            map.remove(player.getUniqueID());
            Refusals.correctClient((EntityPlayerMP) player, Resync.inventory());
        }
    }

    static void forget(EntityPlayer player) {
        ServerState.get().get(SLOT).byPlayer.remove(player.getUniqueID());
    }

    /** Per-server map from player to the tick the resend is due. */
    static final class Due {
        final Map<UUID, Long> byPlayer = new HashMap<>();
    }
}
