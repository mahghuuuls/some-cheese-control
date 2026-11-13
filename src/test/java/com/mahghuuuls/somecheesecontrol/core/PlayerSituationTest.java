package com.mahghuuuls.somecheesecontrol.core;

import net.minecraft.util.math.AxisAlignedBB;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * The airborne decision from given facts, and the shape of the probe under a vehicle. How the game
 * reports the facts (the player's ground flag, a vehicle's blocks below) is checked in a running game.
 */
class PlayerSituationTest {

    @Test
    void standingOnSomethingIsNotAirborne() {
        assertFalse(PlayerSituation.isAirborne(true, false));
    }

    @Test
    void neitherStandingNorInLiquidIsAirborne() {
        // A jump, a fall, a ladder above air, or a vehicle with nothing under it.
        assertTrue(PlayerSituation.isAirborne(false, false));
    }

    @Test
    void inLiquidIsNotAirborne() {
        // Swimming in water or lava, or a boat on water.
        assertFalse(PlayerSituation.isAirborne(false, true));
    }

    @Test
    void probeIsAThinSlightlyNarrowerBoxDirectlyUnderTheVehicle() {
        AxisAlignedBB box = new AxisAlignedBB(10.0, 64.0, 20.0, 11.0, 64.7, 21.0);
        AxisAlignedBB under = PlayerSituation.underneath(box);
        assertEquals(64.0, under.maxY, 1.0e-9);
        assertEquals(63.9, under.minY, 1.0e-9);
        assertEquals(10.001, under.minX, 1.0e-9);
        assertEquals(10.999, under.maxX, 1.0e-9);
        assertEquals(20.001, under.minZ, 1.0e-9);
        assertEquals(20.999, under.maxZ, 1.0e-9);
    }
}
