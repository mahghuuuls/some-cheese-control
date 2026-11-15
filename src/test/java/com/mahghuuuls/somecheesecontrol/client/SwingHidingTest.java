package com.mahghuuuls.somecheesecontrol.client;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** The record the swing Mixins read: only an attack the client refused in this click hides its swing. */
class SwingHidingTest {

    @AfterEach
    void endClick() {
        SwingHiding.clearAttack();
    }

    @Test
    void anAttackIsShownUnlessTheClientRefusedIt() {
        SwingHiding.clearAttack();
        assertTrue(SwingHiding.showsAttack(), "a swing at the air, a block hit, an allowed attack");
        SwingHiding.markRefusedAttack();
        assertFalse(SwingHiding.showsAttack(), "the refused attack of this click");
    }

    @Test
    void theNextClickStartsWithNoRefusedAttack() {
        SwingHiding.markRefusedAttack();
        SwingHiding.clearAttack();
        assertTrue(SwingHiding.showsAttack(), "a swing at the air right after a refused hit still shows");
    }
}
