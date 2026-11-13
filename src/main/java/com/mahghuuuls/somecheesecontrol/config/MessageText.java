package com.mahghuuuls.somecheesecontrol.config;

/**
 * Every player message: its key in the configuration file and its English default text.
 *
 * <p>The text is sent as plain text built on the server, so players without the mod read it too.</p>
 */
public enum MessageText {

    UNSUPPORTED_LIQUID("unsupportedLiquid", "You cannot place liquid without support below.");

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
