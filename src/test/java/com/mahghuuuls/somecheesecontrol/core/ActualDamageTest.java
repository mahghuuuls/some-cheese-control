package com.mahghuuuls.somecheesecontrol.core;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Actual damage: health or absorption lost, and a hit during the invulnerability time. */
class ActualDamageTest {

    @Test
    void healthLostIsActualDamage() {
        assertTrue(ActualDamage.isActualDamage(3F, 0F, 0F));
    }

    @Test
    void aHitThatOnlyRemovesAbsorptionIsActualDamage() {
        assertTrue(ActualDamage.isActualDamage(0F, 4F, 1F), "absorption went from 4 to 1, no health lost");
    }

    @Test
    void aHitWithNothingLeftAndNoAbsorptionTakenIsNotActualDamage() {
        assertFalse(ActualDamage.isActualDamage(0F, 4F, 4F), "armor or resistance took the whole hit");
        assertFalse(ActualDamage.isActualDamage(0F, 0F, 0F));
    }

    @Test
    void onlyANoteFromThisTickIsUsed() {
        assertEquals(4F, ActualDamage.absorptionBefore(4F, 100, 100, 1F), "the hit's own hurt event, same tick");
        assertEquals(1F, ActualDamage.absorptionBefore(4F, 99, 100, 1F), "an older hit's note is not used");
    }

    @Test
    void aStrongerHitDuringTheInvulnerabilityTimeIsTold() {
        // A raised time counting down through exactly the maximum looks like a new hit here; no test can tell them apart.
        assertFalse(ActualDamage.isInvulnerabilityHit(20, 20), "a new hit sets the time to its maximum first");
        assertTrue(ActualDamage.isInvulnerabilityHit(19, 20));
        assertTrue(ActualDamage.isInvulnerabilityHit(11, 20));
        assertFalse(ActualDamage.isInvulnerabilityHit(10, 20), "in the second half a hit is a new hit");
        assertTrue(ActualDamage.isInvulnerabilityHit(60, 20), "a time set above the maximum, as for skeleton trap riders");
    }
}
