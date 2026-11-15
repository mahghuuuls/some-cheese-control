package com.mahghuuuls.somecheesecontrol.feature.vehiclefall;

import com.mahghuuuls.somecheesecontrol.core.PlayerSituation.Support;
import com.mahghuuuls.somecheesecontrol.feature.vehiclefall.VehicleFallRules.Action;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** The vehicle fall decisions: what counts as fall, when it is given, carried or dropped. */
class VehicleFallRulesTest {

    @Test
    void onlyDescentInTheAirAddsToTheFall() {
        assertEquals(20F, VehicleFallRules.afterMove(15F, 70.0, 65.0, false), 1e-4, "a pending 15 plus 5 blocks of descent");
        assertEquals(15F, VehicleFallRules.afterMove(15F, 65.0, 66.0, false), 1e-4, "a rise keeps the fall");
        assertEquals(0F, VehicleFallRules.afterMove(0F, 65.0, 64.0, true), 1e-4, "a step down from the ground is no fall");
    }

    @Test
    void aCliffRideCountsFromTheTickTheBoatLeftTheEdge() {
        float fall = 0F;
        double y = 80.0;
        boolean heldUp = true;
        double[] path = {79.9, 79.5, 78.7, 77.5, 75.9, 73.9, 71.5, 70.0};
        for (double next : path) {
            fall = VehicleFallRules.afterMove(fall, y, next, heldUp);
            heldUp = false;
            y = next;
        }
        assertEquals(9.9F, fall, 1e-3, "the first tick off the edge counts nothing; the rest of a 10-block cliff counts");
    }

    @Test
    void aRiddenBoatGivesItsFallOnABlockAndDropsItInWater() {
        assertEquals(Action.APPLY, VehicleFallRules.atBoatTick(Support.GROUND, false, 20F), "stone or grass");
        assertEquals(Action.CLEAR, VehicleFallRules.atBoatTick(Support.WATER, false, 20F), "water is safe");
        assertEquals(Action.KEEP, VehicleFallRules.atBoatTick(Support.NONE, false, 20F), "still in the air");
        assertEquals(Action.CLEAR, VehicleFallRules.atBoatTick(Support.GROUND, true, 20F), "the game already gave a fall this tick");
        assertEquals(Action.CLEAR, VehicleFallRules.atBoatTick(Support.GROUND, false, 0.5F), "a step down gives nothing");
    }

    @Test
    void leavingInTheAirCarriesTheFallAndOnTheLandingTickGivesIt() {
        assertEquals(Action.CARRY, VehicleFallRules.atLeaving(Support.NONE, false, 15F, false, false), "a boat in mid-air");
        assertEquals(Action.CARRY, VehicleFallRules.atLeaving(Support.NONE, false, 15F, true, false), "a minecart in mid-air");
        assertEquals(Action.APPLY, VehicleFallRules.atLeaving(Support.GROUND, false, 15F, false, false), "sneaking out as the boat lands");
        assertEquals(Action.CARRY, VehicleFallRules.atLeaving(Support.GROUND, false, 15F, true, false),
                "a minecart just above a block has not passed its fall yet");
        assertEquals(Action.CLEAR, VehicleFallRules.atLeaving(Support.GROUND, false, 15F, true, true), "a minecart on a rail is safe");
        assertEquals(Action.CLEAR, VehicleFallRules.atLeaving(Support.WATER, false, 15F, false, false), "water");
        assertEquals(Action.CLEAR, VehicleFallRules.atLeaving(Support.NONE, true, 15F, false, false), "the game already gave a fall this tick");
        assertEquals(Action.CLEAR, VehicleFallRules.atLeaving(Support.NONE, false, 0.2F, false, false), "no fall to carry");
    }

    @Test
    void aLongJumpInOneTickIsATeleport() {
        assertFalse(VehicleFallRules.isTeleport(0.3, -3.9, 0.2), "the fastest fall");
        assertFalse(VehicleFallRules.isTeleport(0, -11.7, 0), "three bunched movement packets of the fastest fall");
        assertTrue(VehicleFallRules.isTeleport(0, 0, 50), "a command teleport");
    }

    @Test
    void aMinecartResetByARailOrWaterEndsThePendingFall() {
        assertTrue(VehicleFallRules.minecartWasReset(0F, 12F), "rails reset the cart's fall");
        assertFalse(VehicleFallRules.minecartWasReset(14F, 12F), "the cart kept falling");
        assertFalse(VehicleFallRules.minecartWasReset(12F, 12F), "boarded this tick");
    }

    @Test
    void aMinecartKeepsOnlyItsOwnFallWhenTheTrackingEnds() {
        assertEquals(4F, VehicleFallRules.withoutPending(16F, 12F), 1e-4, "its own 4 blocks stay");
        assertEquals(0F, VehicleFallRules.withoutPending(5F, 12F), 1e-4, "never below 0");
    }
}
