package com.mahghuuuls.somecheesecontrol.feature.climbing;

import com.mahghuuuls.somecheesecontrol.core.ServerState;
import net.minecraft.entity.player.EntityPlayer;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Tells the first refused step of one right click from the steps that follow it.
 *
 * <p>One right click reaches the server as several packets in the same tick: a click at a point on
 * an entity, a click on the entity, a click on a block, and a click with the item, for each hand,
 * until one of them succeeds on the client. A client without this mod sees none of the refusals, so
 * it sends them all. Only the first refused step counts as the decision; the others still correct
 * the client but write no second line and send no second message.</p>
 */
final class ClickRepeats {

    private static final ServerState.Slot<LastClick> SLOT = new ServerState.Slot<>(LastClick.class, LastClick::new);

    private ClickRepeats() {
    }

    /** True when a refused click step of this player was already handled in this server tick; records this one. */
    static boolean isRepeat(EntityPlayer player) {
        Map<UUID, Long> map = ServerState.get().get(SLOT).byPlayer;
        long now = ClimbingTracker.now();
        Long last = map.put(player.getUniqueID(), now);
        return last != null && last == now;
    }

    static void forget(EntityPlayer player) {
        ServerState.get().get(SLOT).byPlayer.remove(player.getUniqueID());
    }

    /** Per-server map from player to the tick of that player's last refused click step. */
    static final class LastClick {
        final Map<UUID, Long> byPlayer = new HashMap<>();
    }
}
