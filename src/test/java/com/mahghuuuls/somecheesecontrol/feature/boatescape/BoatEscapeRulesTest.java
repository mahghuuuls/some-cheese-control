package com.mahghuuuls.somecheesecontrol.feature.boatescape;

import net.minecraft.entity.item.EntityBoat;
import net.minecraft.entity.item.EntityMinecartEmpty;
import org.junit.jupiter.api.Test;

import java.util.HashSet;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Covered boats, the exit spot search over a small block model, and the reboarding time. In the
 * model, a spot is free when no listed solid block is at its feet or head (a mob two blocks tall),
 * and supported when a solid block is below it.
 */
class BoatEscapeRulesTest {

    /** A modded boat or a ship that extends the vanilla boat. */
    private abstract static class ModdedShip extends EntityBoat {
        ModdedShip() {
            super(null);
        }
    }

    @Test
    void onlyTheVanillaBoatIsCovered() {
        assertTrue(BoatEscapeRules.isCoveredBoat(EntityBoat.class));
        assertFalse(BoatEscapeRules.isCoveredBoat(ModdedShip.class), "subclasses are not covered");
        assertFalse(BoatEscapeRules.isCoveredBoat(EntityMinecartEmpty.class), "minecarts are not affected");
    }

    @Test
    void theSpotsAreWithinReachNearestFirstAndNeverTheBoatsOwnBlock() {
        assertEquals(48, BoatEscapeRules.SPOTS.size(), "24 side blocks at two heights");
        int lastDistance = 0;
        for (BoatEscapeRules.Offset spot : BoatEscapeRules.SPOTS) {
            assertTrue(Math.abs(spot.dx) <= 2 && Math.abs(spot.dz) <= 2 && spot.dy >= 0 && spot.dy <= 1, spot.toString());
            assertFalse(spot.dx == 0 && spot.dz == 0, "the boat's own block is the last resort, not a spot");
            int distance = spot.dx * spot.dx + spot.dz * spot.dz;
            assertTrue(distance >= lastDistance, "nearest first: " + spot);
            lastDistance = distance;
        }
    }

    @Test
    void whenEverySpotIsFreeAndHasGroundTheNearestIsUsed() {
        Grid grid = new Grid();
        grid.floorEverywhere(-1); // ground under every side spot (water below is the game-side probe's case)
        assertEquals(BoatEscapeRules.SPOTS.get(0), BoatEscapeRules.choose(grid));
    }

    @Test
    void inAOneWideTrenchTheMobLeavesAlongTheTrenchNotIntoTheBanks() {
        // The boat floats in a trench along z, one block deep; the banks at x = -1 and x = 1 are solid at the boat's height.
        Grid grid = new Grid();
        for (int z = -2; z <= 2; z++) {
            for (int x = -2; x <= 2; x++) {
                if (x != 0) {
                    grid.solid(x, 0, z);
                }
            }
            grid.solid(0, -1, z); // the trench floor
        }
        BoatEscapeRules.Offset spot = BoatEscapeRules.choose(grid);
        // Along the trench at the boat's height is free and supported too, and nearer than the bank.
        assertEquals(new BoatEscapeRules.Offset(0, 0, -1), spot);
        assertTrue(grid.isFree(spot) && grid.isSupported(spot));
    }

    @Test
    void inAOneBlockPocketTheMobGoesUpOntoTheEdge() {
        // Solid all around at the boat's height; air one block higher.
        Grid grid = new Grid();
        for (int x = -2; x <= 2; x++) {
            for (int z = -2; z <= 2; z++) {
                if (x != 0 || z != 0) {
                    grid.solid(x, 0, z);
                }
            }
        }
        BoatEscapeRules.Offset spot = BoatEscapeRules.choose(grid);
        assertEquals(1, spot.dy, "one block up, standing on the edge");
        assertEquals(1, spot.dx * spot.dx + spot.dz * spot.dz, "next to the boat");
    }

    @Test
    void aFreeSpotWithNothingBelowIsUsedOnlyWhenNoSpotHasGround() {
        Grid grid = new Grid(); // nothing solid anywhere: every spot is free, none supported
        assertEquals(BoatEscapeRules.SPOTS.get(0), BoatEscapeRules.choose(grid));
        grid.solid(2, -1, 2); // one far spot has ground below
        assertEquals(new BoatEscapeRules.Offset(2, 0, 2), BoatEscapeRules.choose(grid));
    }

    @Test
    void withNoFreeSpotTheMobGoesToTheBoat() {
        Grid grid = new Grid();
        for (int x = -2; x <= 2; x++) {
            for (int z = -2; z <= 2; z++) {
                for (int y = 0; y <= 2; y++) {
                    if (x != 0 || z != 0) {
                        grid.solid(x, y, z);
                    }
                }
            }
        }
        assertNull(BoatEscapeRules.choose(grid));
    }

    @Test
    void aRefusalCountsAsThisModsOnlyAfterTheGamesOwnWait() {
        assertFalse(BoatEscapeRules.isPastVanillaCooldown(1059, 1000), "59 ticks: the game refuses too");
        assertFalse(BoatEscapeRules.isPastVanillaCooldown(1060, 1000), "60 ticks: the game's wait may still run");
        assertTrue(BoatEscapeRules.isPastVanillaCooldown(1061, 1000), "61 ticks: only this mod refuses");
    }

    @Test
    void boardingIsBlockedUntilTheTimeIsOver() {
        long until = BoatEscapeRules.blockedUntil(1000, 100);
        assertEquals(1100, until);
        assertTrue(BoatEscapeRules.isBlocked(1000, until));
        assertTrue(BoatEscapeRules.isBlocked(1040, until), "2 seconds later: still blocked");
        assertTrue(BoatEscapeRules.isBlocked(1099, until));
        assertFalse(BoatEscapeRules.isBlocked(1100, until), "after 5 seconds the mob can board");
        assertFalse(BoatEscapeRules.isBlocked(1120, until), "6 seconds later");
    }

    /** Solid blocks relative to the boat's block; a mob is two blocks tall. */
    private static final class Grid implements BoatEscapeRules.Probe {
        private final Set<String> solid = new HashSet<>();

        void solid(int x, int y, int z) {
            solid.add(x + "," + y + "," + z);
        }

        void floorEverywhere(int y) {
            for (int x = -2; x <= 2; x++) {
                for (int z = -2; z <= 2; z++) {
                    solid(x, y, z);
                }
            }
        }

        private boolean isSolid(int x, int y, int z) {
            return solid.contains(x + "," + y + "," + z);
        }

        @Override
        public boolean isFree(BoatEscapeRules.Offset spot) {
            return !isSolid(spot.dx, spot.dy, spot.dz) && !isSolid(spot.dx, spot.dy + 1, spot.dz);
        }

        @Override
        public boolean isSupported(BoatEscapeRules.Offset spot) {
            return isSolid(spot.dx, spot.dy - 1, spot.dz);
        }
    }
}
