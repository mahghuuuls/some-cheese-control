package com.mahghuuuls.somecheesecontrol.core;

/**
 * Holder of every per-server, never-saved state of the mod.
 *
 * <p>A new holder is created at each server start and dropped at stop, so nothing leaks from one
 * world to the next in single player.</p>
 */
public final class ServerState {

    private static ServerState current = new ServerState();

    private final MessageThrottle messageThrottle = new MessageThrottle();

    private ServerState() {
    }

    public static ServerState get() {
        return current;
    }

    /** Replaces the holder; called at server start and server stop. */
    public static void reset() {
        current = new ServerState();
    }

    MessageThrottle messageThrottle() {
        return messageThrottle;
    }
}
