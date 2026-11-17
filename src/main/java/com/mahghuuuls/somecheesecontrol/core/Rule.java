package com.mahghuuuls.somecheesecontrol.core;

import com.mahghuuuls.somecheesecontrol.config.MessageText;

/**
 * A player-facing rule whose refusal the player is told about.
 *
 * <p>Each rule names its message and whether it still applies to players in Creative mode when the
 * Creative exemption is on. Message keys and texts belong to {@link MessageText}.</p>
 */
public enum Rule {

    UNSUPPORTED_LIQUID(MessageText.UNSUPPORTED_LIQUID, false),
    AIRBORNE_EMPTYING(MessageText.AIRBORNE_EMPTYING, false),
    FORCED_DROP(MessageText.FORCED_DROP, false),
    CLIMBING_MELEE(MessageText.CLIMBING_MELEE, false),
    CLIMBING_RANGED(MessageText.CLIMBING_RANGED, false),
    CLIMBING_WIZARDRY(MessageText.CLIMBING_WIZARDRY, false),
    CLIMBING_ITEM(MessageText.CLIMBING_ITEM, false),
    BED_EXPLOSION(MessageText.BED_EXPLOSION, true);

    private final MessageText message;
    private final boolean appliesInCreative;

    Rule(MessageText message, boolean appliesInCreative) {
        this.message = message;
        this.appliesInCreative = appliesInCreative;
    }

    public MessageText message() {
        return message;
    }

    /** The rule's name in diagnostic lines: its message key. */
    public String key() {
        return message.key();
    }

    /** True for a rule that ignores the Creative exemption (bed-explosion prevention). */
    public boolean appliesInCreative() {
        return appliesInCreative;
    }
}
