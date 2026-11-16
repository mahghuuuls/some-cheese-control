package com.mahghuuuls.somecheesecontrol.feature.cobweb;

/**
 * The cobweb release rules as plain decisions over amounts, flags and box edges. The feature
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

    /**
     * True when a hit is actual damage: health was lost, or only absorption hearts were. A canceled
     * hit, a hit blocked by a shield and a hit fully absorbed by the invulnerability time do not get
     * this far, so the caller never asks for them; a stronger hit during the invulnerability time
     * does, and {@link #isInvulnerabilityHit} tells it apart.
     *
     * @param amount the damage left after armor, potions and absorption
     * @param absorptionBefore the absorption when the hit started
     * @param absorptionAfter the absorption after the hit's absorption part was taken
     */
    static boolean isActualDamage(float amount, float absorptionBefore, float absorptionAfter) {
        return amount > 0F || absorptionAfter < absorptionBefore;
    }

    /**
     * True when a hit arrives during the mob's invulnerability time. A new hit sets the time to its
     * maximum just before the damage is applied, so the time is exactly at its maximum for a new hit.
     * While the time is above half its maximum (an earlier hit, or a time the game set higher, as for
     * the riders of a skeleton trap), the game lets a hit through only when it is stronger than the
     * last one, and then only the difference is applied: that is such a hit. In the game, damage
     * reaches a mob only through its hit logic; damage another mod applied directly to a mob hit in
     * the last half second would also be taken for such a hit.
     */
    static boolean isInvulnerabilityHit(int hurtResistantTime, int maxHurtResistantTime) {
        return hurtResistantTime > maxHurtResistantTime / 2.0F && hurtResistantTime != maxHurtResistantTime;
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
