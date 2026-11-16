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

    @Test
    void theReleaseDelayLeftCountsDownAfterLettingGo() {
        assertEquals(11, ClimbingRules.releaseTicksLeft(false, false, 100, 100, DELAY), "the holding tick and the 10 delay ticks after it");
        assertEquals(4, ClimbingRules.releaseTicksLeft(false, false, 100, 107, DELAY));
        assertEquals(1, ClimbingRules.releaseTicksLeft(false, false, 100, 110, DELAY), "the last tick of the delay");
        assertEquals(0, ClimbingRules.releaseTicksLeft(false, false, 100, 111, DELAY), "the delay is over");
        assertEquals(0, ClimbingRules.releaseTicksLeft(true, false, 100, 105, DELAY), "holding on: no delay runs yet");
        assertEquals(0, ClimbingRules.releaseTicksLeft(false, true, 100, 105, DELAY), "standing on the ground ends it");
        assertEquals(0, ClimbingRules.releaseTicksLeft(false, false, ClimbingRules.NEVER, 105, DELAY), "never held on");
    }

    @Test
    void theStateIsOnExactlyWhileHoldingOrWhileDelayTicksAreLeft() {
        for (long now = 95; now <= 115; now++) {
            for (boolean holding : new boolean[] {false, true}) {
                for (boolean supported : new boolean[] {false, true}) {
                    boolean on = ClimbingRules.isRestricted(holding, supported, 100, now, DELAY);
                    long left = ClimbingRules.releaseTicksLeft(holding, supported, 100, now, DELAY);
                    assertEquals(!supported && (holding || left > 0), on, "tick " + now + " holding " + holding + " supported " + supported);
                }
            }
        }
    }
}
