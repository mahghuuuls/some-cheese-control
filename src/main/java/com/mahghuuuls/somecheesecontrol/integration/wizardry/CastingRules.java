package com.mahghuuuls.somecheesecontrol.integration.wizardry;

import javax.annotation.Nullable;

/**
 * The plain decisions of the climbing Wizardry refusal, with no Wizardry types, so they can be
 * tested on their own.
 */
final class CastingRules {

    private CastingRules() {
    }

    /**
     * True when a cast with this source can come from an item in the caster's hand. The source list
     * is open: addons add their own values at runtime (Ancient Spellcraft's item sources), and an
     * unknown or missing source may be one. Only the sources that never come from a held item are
     * named here: the {@code /cast} command (and continuous casts that Wizardry keeps for a command),
     * a spell-casting entity, and a dispenser.
     */
    static boolean mayComeFromHeldItem(@Nullable String sourceName) {
        return !"COMMAND".equals(sourceName) && !"NPC".equals(sourceName) && !"DISPENSER".equals(sourceName);
    }

    /**
     * The charge-up time of a held use, in ticks, as Wizardry computes it for a wand: the spell's
     * charge-up times the charge-up modifier, rounded down. A scroll has none.
     */
    static int chargeupTicks(int spellChargeup, float modifier, boolean scroll) {
        return scroll ? 0 : (int) (spellChargeup * modifier);
    }

    /**
     * True when the held use has not cast yet. A use waits for its first cast only when the spell
     * is continuous or has a charge-up; an instant spell cast at the click, and its use afterwards
     * is only the button held down. At the end of a charge-up Wizardry casts with no event (an
     * instant spell fires, a continuous spell makes its first cast), so the use must stop at or
     * before that tick's use update. Past it, a continuous spell is channeling, which its own tick
     * event stops.
     */
    static boolean isBeforeFirstCast(int useTick, int chargeupTicks, boolean continuous) {
        return (continuous || chargeupTicks > 0) && useTick <= chargeupTicks;
    }
}
