package com.mahghuuuls.somecheesecontrol.core;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Per player, the server tick of the last refused step of a right click.
 *
 * <p>One right click reaches the server as several steps in the same tick: a click at a point on an
 * entity, a click on the entity, a click on a block and a click with the item, first for the main
 * hand and then for the offhand. A refused step makes the whole click refused, so the steps after it
 * are refused too, without a second message or line. Melee refusals and forced drops are not click
 * steps and are not recorded.</p>
 */
final class ClickRecord {

    private static final ServerState.Slot<ClickRecord> SLOT = new ServerState.Slot<>(ClickRecord.class, ClickRecord::new);

    private final Map<UUID, Long> lastRefusedTick = new HashMap<>();

    /** The record of the running server. */
    static ClickRecord get() {
        return ServerState.get().get(SLOT);
    }

    /** Records a refused step at {@code tick}; true when a step of the same click was already refused. */
    boolean mark(UUID player, long tick) {
        Long last = lastRefusedTick.put(player, tick);
        return last != null && last == tick;
    }

    /** True when a step of the player's click in {@code tick} was refused. */
    boolean isRefused(UUID player, long tick) {
        Long last = lastRefusedTick.get(player);
        return last != null && last == tick;
    }

    void forget(UUID player) {
        lastRefusedTick.remove(player);
    }
}
