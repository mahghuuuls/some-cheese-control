package com.mahghuuuls.somecheesecontrol.feature.vehiclefall;

import com.mahghuuuls.somecheesecontrol.core.PlayerSituation.Support;

/**
 * The vehicle fall rules as plain decisions over heights and what holds the vehicle up. The tracker
 * gathers the facts and acts on the decision.
 */
final class VehicleFallRules {

    /**
     * The smallest fall the tracker passes on or reports. Fall damage starts above 3 blocks; a fall
     * below one block is a step down, which a walking player makes all the time.
     */
    static final float MIN_FALL = 1.0F;

    /**
     * A move longer than this in one tick is a teleport, not a fall: the fastest fall covers about
     * 3.9 blocks per tick, and a few bunched movement packets stay below it.
     */
    static final double TELEPORT_DISTANCE = 24.0;

    private VehicleFallRules() {
    }

    /** What the tracker does with a fall. */
    enum Action {
        /** Keep counting; nothing happens yet. */
        KEEP,
        /** The player takes the fall now, through the game's normal fall method. */
        APPLY,
        /** The fall becomes the player's own, as for a player who is falling. */
        CARRY,
        /** The fall is gone without damage. */
        CLEAR
    }

    /**
     * The fall after one tick of a boat's move from {@code lastY} to {@code nowY}. Only descent while
     * nothing held the boat up at the start of the tick adds, so a boat stepping down a block or
     * bobbing on water counts nothing; a rise keeps the fall.
     */
    static float afterMove(float fall, double lastY, double nowY, boolean wasHeldUp) {
        double descent = lastY - nowY;
        return !wasHeldUp && descent > 0 ? fall + (float) descent : fall;
    }

    /**
     * A ridden boat at the end of a tick: on a block it gives the fall, in water it ends it, in the
     * air it keeps counting. When the game already passed a fall to the player this tick, the boat's
     * fall is not given a second time.
     */
    static Action atBoatTick(Support support, boolean passedByGame, float fall) {
        if (support == Support.NONE) {
            return Action.KEEP;
        }
        if (fall < MIN_FALL || passedByGame || support == Support.WATER) {
            return Action.CLEAR;
        }
        return Action.APPLY;
    }

    /**
     * A player leaving a vehicle: water ends the fall, and so does a rail under a minecart. A boat on
     * a block gives its fall now (it just landed). Otherwise the fall goes on as the player's own: in
     * the air, and also for a minecart just above a block, which has not passed its fall yet (a
     * minecart passes its fall in the tick it lands, before its rider can leave).
     */
    static Action atLeaving(Support support, boolean passedByGame, float fall, boolean minecart, boolean onRail) {
        if (fall < MIN_FALL || passedByGame || support == Support.WATER || (minecart && onRail)) {
            return Action.CLEAR;
        }
        return support == Support.GROUND && !minecart ? Action.APPLY : Action.CARRY;
    }

    /** True when a move of this length in one tick is a teleport. */
    static boolean isTeleport(double dx, double dy, double dz) {
        return dx * dx + dy * dy + dz * dz > TELEPORT_DISTANCE * TELEPORT_DISTANCE;
    }

    /**
     * True when a minecart's fall has been reset below the fall it carried for its rider: a rail or
     * water reset it, so the rider's pending fall is gone too.
     */
    static boolean minecartWasReset(float cartFall, float carried) {
        return cartFall < carried - 0.001F;
    }

    /** A minecart's fall without the pending fall that was added for its rider; never below 0. */
    static float withoutPending(float cartFall, float pending) {
        return Math.max(0F, cartFall - pending);
    }
}
