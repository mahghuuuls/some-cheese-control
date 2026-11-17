package com.mahghuuuls.somecheesecontrol.integration.spartanshields;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ShieldBashRulesTest {

    @Test
    void theRefusalIsActiveWhenEverythingIsInPlace() {
        assertNull(ShieldBashRules.inactiveReason(true, true, true, true));
    }

    @Test
    void eachMissingPieceIsNamed() {
        assertEquals("climbing melee refusal is off", ShieldBashRules.inactiveReason(false, true, true, true));
        assertEquals("this Spartan Shields version has no bash handler to patch", ShieldBashRules.inactiveReason(true, false, true, true),
                "a handler missing wins over a marker");
        assertEquals("MixinBooter is not installed", ShieldBashRules.inactiveReason(true, true, false, false));
        assertEquals("the bash Mixin was not applied", ShieldBashRules.inactiveReason(true, true, false, true));
    }

    @Test
    void anAppliedMarkerWithoutMixinBooterStillCounts() {
        assertNull(ShieldBashRules.inactiveReason(true, true, true, false), "the marker proves the Mixin, whatever loaded it");
    }

    @Test
    void onlyTheSwitchIsADeliberateSetting() {
        assertTrue(ShieldBashRules.isSwitchedOff(false));
        assertFalse(ShieldBashRules.isSwitchedOff(true));
    }

    @Test
    void aBashSpartanShieldsWouldActOnIsRefusedWhileClimbing() {
        assertTrue(ShieldBashRules.refuses(true, true, false, true, true));
    }

    @Test
    void aBashSpartanShieldsIgnoresAnywayIsLeftAlone() {
        assertFalse(ShieldBashRules.refuses(false, true, false, true, true), "not blocking");
        assertFalse(ShieldBashRules.refuses(true, false, false, true, true), "no Spartan shield in the bash's hand");
        assertFalse(ShieldBashRules.refuses(true, true, true, true, true), "the shield's bash is cooling down");
    }

    @Test
    void theClimbingRuleDecides() {
        assertFalse(ShieldBashRules.refuses(true, true, false, false, true), "an exempt player");
        assertFalse(ShieldBashRules.refuses(true, true, false, true, false), "not climbing");
    }
}
