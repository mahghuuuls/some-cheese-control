package com.mahghuuuls.somecheesecontrol.core;

import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** One right click is one action: the click record that ties its steps together on the server. */
class ClickRecordTest {

    private static final UUID PLAYER = UUID.randomUUID();
    private static final UUID OTHER = UUID.randomUUID();

    @Test
    void theFirstRefusedStepOfAClickIsTheDecision() {
        ClickRecord record = new ClickRecord();
        assertFalse(record.mark(PLAYER, 100), "first refused step: message and line");
        assertTrue(record.mark(PLAYER, 100), "a later refused step of the same click only corrects the client");
    }

    @Test
    void theStepsAfterARefusedStepAreRefused() {
        ClickRecord record = new ClickRecord();
        assertFalse(record.isRefused(PLAYER, 100), "nothing refused yet");
        record.mark(PLAYER, 100);
        assertTrue(record.isRefused(PLAYER, 100), "the offhand step of the same click");
    }

    @Test
    void aClickInALaterTickIsANewAction() {
        ClickRecord record = new ClickRecord();
        record.mark(PLAYER, 100);
        assertFalse(record.isRefused(PLAYER, 104), "a held right click repeats every 4 ticks");
        assertFalse(record.mark(PLAYER, 104), "the repeated click is decided again");
    }

    @Test
    void eachPlayerHasTheirOwnClick() {
        ClickRecord record = new ClickRecord();
        record.mark(PLAYER, 100);
        assertFalse(record.isRefused(OTHER, 100));
    }

    @Test
    void aForgottenPlayerHasNoRefusedClick() {
        ClickRecord record = new ClickRecord();
        record.mark(PLAYER, 100);
        record.forget(PLAYER);
        assertFalse(record.isRefused(PLAYER, 100));
    }
}
