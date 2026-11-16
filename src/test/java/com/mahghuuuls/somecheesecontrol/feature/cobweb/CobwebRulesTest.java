package com.mahghuuuls.somecheesecontrol.feature.cobweb;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** The mobGriefing setting, and which block positions are inside a body box. */
class CobwebRulesTest {

    @Test
    void mobGriefingMattersOnlyWhenTheSettingFollowsIt() {
        assertTrue(CobwebRules.mayRemove(false, false), "the default ignores the game rule");
        assertTrue(CobwebRules.mayRemove(false, true));
        assertTrue(CobwebRules.mayRemove(true, true));
        assertFalse(CobwebRules.mayRemove(true, false));
    }

    @Test
    void aZombieStandingInTheMiddleOfABlockIsInsideTwoBlocks() {
        // A zombie is 0.6 wide and 1.95 tall; standing at x=10.5, y=64, z=-3.5.
        CobwebRules.Range range = CobwebRules.inside(10.2, 64.0, -3.8, 10.8, 65.95, -3.2);
        assertEquals(10, range.fromX);
        assertEquals(10, range.toX);
        assertEquals(64, range.fromY);
        assertEquals(65, range.toY);
        assertEquals(-4, range.fromZ);
        assertEquals(-4, range.toZ);
        assertEquals(2, range.count(), "the feet block and the head block, not the block above");
    }

    @Test
    void aBlockThatOnlyTouchesTheSideOfTheBoxIsNotInside() {
        // A box from exactly x=10 to x=11 touches blocks 9 and 11 only at a face.
        CobwebRules.Range range = CobwebRules.inside(10.0, 64.0, 0.0, 11.0, 65.0, 1.0);
        assertEquals(10, range.fromX);
        assertEquals(10, range.toX);
        assertEquals(64, range.fromY);
        assertEquals(64, range.toY);
        assertEquals(1, range.count());
    }

    @Test
    void aBodyThatReachesLessThanTheMarginIntoABlockIsNotInIt() {
        // The box starts 0.0005 before block 10 and ends 0.0005 after block 11 starts.
        CobwebRules.Range range = CobwebRules.inside(9.9995, 64.0, 0.0, 11.0005, 65.0, 1.0);
        assertEquals(10, range.fromX, "block 9 is reached by less than the margin");
        assertEquals(10, range.toX, "block 11 is reached by less than the margin");
    }

    @Test
    void aWideBodyCoversEveryBlockItReaches() {
        // An iron golem is 1.4 wide and 2.7 tall; centered on a block corner it reaches 2 x 3 x 2 blocks.
        CobwebRules.Range range = CobwebRules.inside(4.3, 70.0, 4.3, 5.7, 72.7, 5.7);
        assertEquals(12, range.count());
    }

    @Test
    void aBoxWithNoInsideHasNoBlocks() {
        assertEquals(0, CobwebRules.inside(5.0, 5.0, 5.0, 5.0005, 6.0, 6.0).count());
        assertEquals(0, new CobwebRules.Range(5, 5, 5, 3, 3, 5).count(), "two inverted sides do not make blocks");
    }
}
