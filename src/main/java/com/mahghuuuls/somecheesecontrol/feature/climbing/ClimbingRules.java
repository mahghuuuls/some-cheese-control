package com.mahghuuuls.somecheesecontrol.feature.climbing;

/**
 * The restricted climbing state as a plain decision.
 *
 * <p>A player holds on while on a climbable block and not supported. Supported means standing on the
 * ground or riding: a rider is held up by the mount, not by the ladder. The state lasts while holding
 * on and for the release delay counted from the last tick the player held on. Being supported ends
 * it at once.</p>
 */
final class ClimbingRules {

    /** No holding tick recorded. */
    static final long NEVER = Long.MIN_VALUE;

    private ClimbingRules() {
    }

    /**
     * The last tick the player held on, after this tick's facts: cleared when supported (even on a
     * climbable block), this tick when holding on, otherwise unchanged.
     */
    static long update(long lastHoldingTick, boolean onClimbable, boolean supported, long nowTick) {
        if (supported) {
            return NEVER;
        }
        return onClimbable ? nowTick : lastHoldingTick;
    }

    /**
     * True while holding on now, or within {@code delayTicks} of the last holding tick and not
     * supported. The live facts count even when no tick has been recorded yet.
     */
    static boolean isRestricted(boolean onClimbable, boolean supported, long lastHoldingTick, long nowTick, int delayTicks) {
        if (supported) {
            return false;
        }
        if (onClimbable) {
            return true;
        }
        if (lastHoldingTick == NEVER) {
            return false;
        }
        return nowTick - lastHoldingTick <= delayTicks;
    }
}
