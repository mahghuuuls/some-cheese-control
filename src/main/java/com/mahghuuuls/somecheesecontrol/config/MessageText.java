package com.mahghuuuls.somecheesecontrol.config;

/**
 * Every player message: its key in the configuration file and its English default text.
 *
 * <p>The text is sent as plain text built on the server, so players without the mod read it too.</p>
 */
public enum MessageText {

    UNSUPPORTED_LIQUID("unsupportedLiquid", "You cannot place liquid without support below."),
    AIRBORNE_EMPTYING("airborneEmptying", "You cannot empty a bucket while in the air."),
    FORCED_DROP("forcedDrop", "You can only carry that item in your hand."),
    CLIMBING_MELEE("climbingMelee", "You cannot attack while climbing."),
    CLIMBING_RANGED("climbingRanged", "You cannot shoot or throw while climbing.");

    private final String key;
    private final String defaultText;

    MessageText(String key, String defaultText) {
        this.key = key;
        this.defaultText = defaultText;
    }

    public String key() {
        return key;
    }

    public String defaultText() {
        return defaultText;
    }
}
