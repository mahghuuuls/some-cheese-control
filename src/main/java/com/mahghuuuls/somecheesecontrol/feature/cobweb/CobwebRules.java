package com.mahghuuuls.somecheesecontrol.feature.cobweb;

/**
 * The cobweb release rules as plain decisions over flags and box edges. The feature
 * gathers the facts and removes the webs.
 */
final class CobwebRules {

    /**
     * How far inside the body box a block must reach to touch the body. The game uses the same
     * margin when it finds the blocks an entity is inside (a web slows only an entity that is inside
     * it by this test), so a web that only touches the side of the box stays.
     */
    static final double INSET = 0.001;

    private CobwebRules() {
    }

    /** True when the webs may be removed: always, unless the setting follows a false {@code mobGriefing}. */
    static boolean mayRemove(boolean followMobGriefing, boolean mobGriefing) {
        return !followMobGriefing || mobGriefing;
    }

    /** The block positions inside a body box, from corner to corner, both included. */
    static Range inside(double minX, double minY, double minZ, double maxX, double maxY, double maxZ) {
        return new Range(floor(minX + INSET), floor(minY + INSET), floor(minZ + INSET),
                floor(maxX - INSET), floor(maxY - INSET), floor(maxZ - INSET));
    }

    private static int floor(double value) {
        return (int) Math.floor(value);
    }

    /** A box of block positions; empty when a "to" corner is below its "from" corner. */
    static final class Range {
        final int fromX;
        final int fromY;
        final int fromZ;
        final int toX;
        final int toY;
        final int toZ;

        Range(int fromX, int fromY, int fromZ, int toX, int toY, int toZ) {
            this.fromX = fromX;
            this.fromY = fromY;
            this.fromZ = fromZ;
            this.toX = toX;
            this.toY = toY;
            this.toZ = toZ;
        }

        /** The number of block positions in the range. */
        long count() {
            if (toX < fromX || toY < fromY || toZ < fromZ) {
                return 0;
            }
            return (long) (toX - fromX + 1) * (toY - fromY + 1) * (toZ - fromZ + 1);
        }
    }
}
