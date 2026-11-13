package com.mahghuuuls.somecheesecontrol.core;

import org.junit.jupiter.api.Test;

import java.util.ArrayList;

import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertSame;

class ServerStateTest {

    @SuppressWarnings("rawtypes")
    private static final ServerState.Slot<ArrayList> SLOT = new ServerState.Slot<>(ArrayList.class, ArrayList::new);

    @Test
    void theSameSlotGivesTheSameStateUntilReset() {
        ServerState.reset();
        Object first = ServerState.get().get(SLOT);
        assertSame(first, ServerState.get().get(SLOT));
        ServerState.reset();
        assertNotSame(first, ServerState.get().get(SLOT));
    }
}
