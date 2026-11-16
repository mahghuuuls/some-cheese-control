package com.mahghuuuls.somecheesecontrol.config;

/**
 * Every player message: its key in the configuration file and its English default text.
 *
 * <p>The text is sent as plain text built on the server, so it reads the same whatever language or
 * resource pack the client uses.</p>
 */
public enum MessageText {

    UNSUPPORTED_LIQUID("unsupportedLiquid", "You cannot place liquid without support below."),
    AIRBORNE_EMPTYING("airborneEmptying", "You cannot empty a bucket while in the air."),
    FORCED_DROP("forcedDrop", "You can only carry that item in your hand."),
    CLIMBING_MELEE("climbingMelee", "You cannot attack while climbing."),
    CLIMBING_RANGED("climbingRanged", "You cannot shoot or throw while climbing."),
    CLIMBING_ITEM("climbingItem", "You cannot use that item while climbing."),
    BED_EXPLOSION("bedExplosion", "You cannot use a bed here; it would explode.");

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
