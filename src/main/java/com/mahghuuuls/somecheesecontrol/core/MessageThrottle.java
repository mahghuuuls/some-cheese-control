package com.mahghuuuls.somecheesecontrol.core;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Allows at most one message per player per interval. Used on the server thread; its owner
 * {@link ServerState} is replaced at each server start and stop.
 */
final class MessageThrottle {

    private final Map<UUID, Long> lastSent = new HashMap<>();

    /** Returns true and records the time when a message may be sent now. */
    boolean tryAcquire(UUID player, long nowMillis, long intervalMillis) {
        Long last = lastSent.get(player);
        if (last != null && nowMillis - last < intervalMillis) {
            return false;
        }
        lastSent.put(player, nowMillis);
        return true;
    }
}
