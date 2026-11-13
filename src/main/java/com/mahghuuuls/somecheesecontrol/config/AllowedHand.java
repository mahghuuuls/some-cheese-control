package com.mahghuuuls.somecheesecontrol.config;

import java.util.Locale;

/** Where a restricted carried item may be: the selected hotbar slot, the offhand, or either. */
public enum AllowedHand {

    MAIN("main"),
    OFF("off"),
    EITHER("either");

    private final String configValue;

    AllowedHand(String configValue) {
        this.configValue = configValue;
    }

    public String configValue() {
        return configValue;
    }

    public boolean allowsMainHand() {
        return this != OFF;
    }

    public boolean allowsOffhand() {
        return this != MAIN;
    }

    /** The hand for a configuration value, ignoring case and spaces at either end; null for any other value. */
    static AllowedHand parse(String value) {
        String normalized = value.trim().toLowerCase(Locale.ROOT);
        for (AllowedHand hand : values()) {
            if (hand.configValue.equals(normalized)) {
                return hand;
            }
        }
        return null;
    }

    static String[] configValues() {
        AllowedHand[] hands = values();
        String[] result = new String[hands.length];
        for (int i = 0; i < hands.length; i++) {
            result[i] = hands[i].configValue;
        }
        return result;
    }
}
