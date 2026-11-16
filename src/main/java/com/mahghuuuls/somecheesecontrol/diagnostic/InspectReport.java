package com.mahghuuuls.somecheesecontrol.diagnostic;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import javax.annotation.Nullable;

/**
 * The inspect command's text, built from plain values so it can be checked without a game: for a
 * player, the climbing state, the release delay left, the block giving climbing support, the
 * airborne state and the pending fall; for the mob the operator looks at, the boat reboarding block.
 */
final class InspectReport {

    private InspectReport() {
    }

    static List<String> playerLines(String player, boolean climbing, long releaseTicksLeft, @Nullable String climbingBlock,
                                    boolean airborne, float pendingFall) {
        List<String> lines = new ArrayList<>();
        lines.add("Some Cheese Control: " + player);
        lines.add("  climbing state: " + (climbing ? "on" : "off"));
        lines.add("  release delay left: " + ticks(releaseTicksLeft));
        lines.add("  climbing support: " + (climbingBlock != null ? climbingBlock : "none"));
        lines.add("  airborne: " + (airborne ? "yes" : "no"));
        lines.add("  pending fall: " + (pendingFall > 0F ? String.format(Locale.ROOT, "%.1f blocks", pendingFall) : "none"));
        return lines;
    }

    /**
     * The looked-at mob's line: its reboarding block, or that no mob is in view, or nothing when the
     * command does not come from a player (the server console looks at nothing).
     */
    @Nullable
    static String mobLine(boolean senderIsPlayer, @Nullable String mob, long reboardTicksLeft) {
        if (!senderIsPlayer) {
            return null;
        }
        if (mob == null) {
            return "  looked-at mob: none in view";
        }
        return "  looked-at mob " + mob + ": boat reboarding block " + (reboardTicksLeft > 0 ? ticks(reboardTicksLeft) + " left" : "none");
    }

    static String ticks(long ticks) {
        return ticks <= 0 ? "none" : String.format(Locale.ROOT, "%d ticks (%.2f s)", ticks, ticks / 20.0);
    }
}
