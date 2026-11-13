package com.mahghuuuls.somecheesecontrol.feature.climbing;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ClimbingRulesTest {

    private static final int DELAY = 10;

    @Test
    void standingOnTheGroundBesideALadderIsNotRestricted() {
        assertFalse(ClimbingRules.isRestricted(true, true, ClimbingRules.NEVER, 100, DELAY));
    }

    @Test
    void standingInALadderBlockClearsTheTimer() {
        assertEquals(ClimbingRules.NEVER, ClimbingRules.update(90, true, true, 100),
                "supported wins over climbable, so the base of a ladder does not refresh the delay");
    }

    @Test
    void onALadderAboveAirIsRestrictedBeforeAnyTickIsRecorded() {
        assertTrue(ClimbingRules.isRestricted(true, false, ClimbingRules.NEVER, 100, DELAY),
                "the live situation counts in the tick between grabbing and the next update");
    }

    @Test
    void theDelayCountsFromTheLastHoldingTick() {
        long last = ClimbingRules.update(ClimbingRules.NEVER, true, false, 100);
        assertEquals(100, last);
        last = ClimbingRules.update(last, false, false, 101);
        assertEquals(100, last, "letting go keeps the last holding tick");
        assertTrue(ClimbingRules.isRestricted(false, false, last, 110, DELAY), "10 ticks after the last holding tick");
        assertFalse(ClimbingRules.isRestricted(false, false, last, 111, DELAY), "11 ticks after the last holding tick");
    }

    @Test
    void beingSupportedEndsTheStateAtOnce() {
        assertEquals(ClimbingRules.NEVER, ClimbingRules.update(100, false, true, 102));
        assertFalse(ClimbingRules.isRestricted(false, true, 100, 102, DELAY));
    }

    @Test
    void zeroDelayEndsTheStateTheTickAfterTheLastHoldingTick() {
        assertTrue(ClimbingRules.isRestricted(false, false, 100, 100, 0));
        assertFalse(ClimbingRules.isRestricted(false, false, 100, 101, 0));
    }

    @Test
    void neverClimbedIsNeverRestricted() {
        assertFalse(ClimbingRules.isRestricted(false, false, ClimbingRules.NEVER, 5, DELAY));
    }
}
