package com.mahghuuuls.somecheesecontrol.feature.boatescape;

import net.minecraft.entity.item.EntityBoat;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import javax.annotation.Nullable;

/**
 * The boat escape rules as plain decisions: which vehicles are covered, where an ejected mob goes,
 * and how long it cannot board a boat. The feature gathers the facts and acts.
 */
final class BoatEscapeRules {

    /** How far from the boat's block, in blocks along each side, an exit spot may be. */
    static final int REACH = 2;

    /**
     * Every exit spot to try, nearest first: each block position within {@link #REACH} of the boat's
     * block on the sides, at the boat's height and one block higher. The boat's own block is not a
     * spot; it is the last resort when no spot fits.
     */
    static final List<Offset> SPOTS = spots();

    private BoatEscapeRules() {
    }

    /**
     * True for the vanilla boat of any wood type. Subclasses (modded boats, Davinci's Vessels ships)
     * are not covered.
     */
    static boolean isCoveredBoat(Class<?> vehicleClass) {
        return vehicleClass == EntityBoat.class;
    }

    /** What the feature can tell about one exit spot in the world. */
    interface Probe {
        /** True when the mob's body at the spot is in no solid block, no fire and no lava. */
        boolean isFree(Offset spot);

        /** True when the mob would stand at the spot: a block with a solid top, or water, below it. */
        boolean isSupported(Offset spot);
    }

    /**
     * The exit spot for an ejected mob: the nearest free spot the mob can stand on, else the nearest
     * free spot, else null (the mob goes to the boat's own position).
     */
    @Nullable
    static Offset choose(Probe probe) {
        Offset firstFree = null;
        for (Offset spot : SPOTS) {
            if (!probe.isFree(spot)) {
                continue;
            }
            if (probe.isSupported(spot)) {
                return spot;
            }
            if (firstFree == null) {
                firstFree = spot;
            }
        }
        return firstFree;
    }

    /**
     * The game's own wait after any entity leaves a vehicle, in ticks: it cannot board anything until
     * it is over. A reboarding block shorter than this changes nothing.
     */
    static final int VANILLA_RIDE_COOLDOWN = 60;

    /** The server tick until which an ejected mob cannot board a boat. */
    static long blockedUntil(long now, int reboardTicks) {
        return now + reboardTicks;
    }

    /** True while boarding is blocked at {@code now}. */
    static boolean isBlocked(long now, long until) {
        return now < until;
    }

    /**
     * True once the game's own wait after the ejection is surely over, so a refused boarding is
     * refused by this mod alone. The wait counts down in the mob's own updates, which can start one
     * tick after the ejection (a hit handled after the world's entities), so the wait counts as over
     * one tick later than its length.
     */
    static boolean isPastVanillaCooldown(long now, long ejectedAt) {
        return now - ejectedAt > VANILLA_RIDE_COOLDOWN;
    }

    private static List<Offset> spots() {
        List<Offset> spots = new ArrayList<>();
        for (int dy = 0; dy <= 1; dy++) {
            for (int dx = -REACH; dx <= REACH; dx++) {
                for (int dz = -REACH; dz <= REACH; dz++) {
                    if (dx != 0 || dz != 0) {
                        spots.add(new Offset(dx, dy, dz));
                    }
                }
            }
        }
        spots.sort(Comparator.comparingInt((Offset o) -> o.dx * o.dx + o.dz * o.dz)
                .thenComparingInt(o -> o.dy)
                .thenComparingInt(o -> o.dx)
                .thenComparingInt(o -> o.dz));
        return Collections.unmodifiableList(spots);
    }

    /** A block position relative to the boat's block. */
    static final class Offset {
        final int dx;
        final int dy;
        final int dz;

        Offset(int dx, int dy, int dz) {
            this.dx = dx;
            this.dy = dy;
            this.dz = dz;
        }

        @Override
        public boolean equals(Object other) {
            if (!(other instanceof Offset)) {
                return false;
            }
            Offset o = (Offset) other;
            return dx == o.dx && dy == o.dy && dz == o.dz;
        }

        @Override
        public int hashCode() {
            return (dx * 31 + dy) * 31 + dz;
        }

        @Override
        public String toString() {
            return dx + "," + dy + "," + dz;
        }
    }
}
