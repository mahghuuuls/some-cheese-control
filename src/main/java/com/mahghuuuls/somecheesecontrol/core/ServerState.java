package com.mahghuuuls.somecheesecontrol.core;

import java.util.HashMap;
import java.util.Map;
import java.util.function.Supplier;

/**
 * Holder of every per-server, never-saved state of the mod.
 *
 * <p>A new holder is created at each server start and dropped at stop, so nothing leaks from one
 * world to the next in single player. A feature keeps its own state here under a {@link Slot} it
 * declares, so core never needs to know feature types.</p>
 */
public final class ServerState {

    private static ServerState current = new ServerState();

    private final MessageThrottle messageThrottle = new MessageThrottle();
    private final Map<Slot<?>, Object> slots = new HashMap<>();

    private ServerState() {
    }

    public static ServerState get() {
        return current;
    }

    /** Replaces the holder; called at server start and server stop. */
    public static void reset() {
        current = new ServerState();
    }

    /** The state under {@code slot}, created on first use for this server and kept until reset. */
    public <T> T get(Slot<T> slot) {
        Object value = slots.get(slot);
        if (value == null) {
            value = slot.factory.get();
            slots.put(slot, value);
        }
        return slot.type.cast(value);
    }

    MessageThrottle messageThrottle() {
        return messageThrottle;
    }

    /** A typed place for one owner's per-server state; declare it as a constant in the owning class. */
    public static final class Slot<T> {
        private final Class<T> type;
        private final Supplier<T> factory;

        public Slot(Class<T> type, Supplier<T> factory) {
            this.type = type;
            this.factory = factory;
        }
    }
}
