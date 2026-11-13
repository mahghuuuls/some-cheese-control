package com.mahghuuuls.somecheesecontrol.config;

import net.minecraftforge.common.config.Configuration;
import net.minecraftforge.common.config.Property;

/**
 * Every option of the configuration file: its category, key, default, range and comment.
 *
 * <p>Each value is defined once here. Reading the file and building the defaults both use these
 * definitions, so the shipped default and the documented default cannot drift apart.</p>
 */
final class Options {

    static final String GENERAL = "general";
    static final String MESSAGE_TEXTS = "general.messageTexts";
    static final String UNSUPPORTED_LIQUID_CATEGORY = "unsupportedLiquidPlacement";
    static final String AIRBORNE_EMPTYING_CATEGORY = "airborneEmptying";

    static final BooleanOption RESTRICTIONS_IN_CREATIVE = new BooleanOption(GENERAL, "restrictionsApplyInCreative", false,
            "Apply the player restrictions to players in Creative mode too. Bed-explosion prevention always applies. Restart required.");
    static final BooleanOption DIAGNOSTIC_LOGGING = new BooleanOption(GENERAL, "diagnosticLogging", false,
            "Write one log line for each decision the mod makes (refusals and countermeasures). For server owners checking the mod. Restart required.");
    static final BooleanOption MESSAGES_ENABLED = new BooleanOption(GENERAL, "messagesEnabled", true,
            "Show a short action-bar message when an action is refused or an item is dropped. Restart required.");
    static final DoubleOption MESSAGE_INTERVAL = new DoubleOption(GENERAL, "messageIntervalSeconds", 2.0, 0.0, 10.0,
            "Shortest time between two messages to the same player, in seconds. Restart required.");
    static final BooleanOption UNSUPPORTED_LIQUID = new BooleanOption(UNSUPPORTED_LIQUID_CATEGORY, "enabled", true,
            "Refuse emptying a bucket where the block below the liquid gives no support (air, or a block you can walk through such as grass or a torch). Liquid below counts as support. Restart required.");
    static final BooleanOption AIRBORNE_EMPTYING = new BooleanOption(AIRBORNE_EMPTYING_CATEGORY, "enabled", true,
            "Refuse emptying a bucket while in the air: jumping, falling, holding on to a ladder above air, or riding a vehicle that is in the air. Swimming and standing on the ground are fine. Filling a bucket is never refused. Restart required.");

    private Options() {
    }

    static final class BooleanOption {
        final String category;
        final String key;
        final boolean defaultValue;
        final String comment;

        BooleanOption(String category, String key, boolean defaultValue, String comment) {
            this.category = category;
            this.key = key;
            this.defaultValue = defaultValue;
            this.comment = comment;
        }

        String path() {
            return category + "." + key;
        }

        boolean read(Configuration config, ConfigProblems problems) {
            Property existing = existing(config, category, key);
            if (existing != null && !existing.isBooleanValue()) {
                problems.add(path() + " = " + existing.getString() + " is not true or false; using " + defaultValue);
            }
            return config.getBoolean(key, category, defaultValue, comment);
        }
    }

    static final class DoubleOption {
        final String category;
        final String key;
        final double defaultValue;
        final double min;
        final double max;
        final String comment;

        DoubleOption(String category, String key, double defaultValue, double min, double max, String comment) {
            this.category = category;
            this.key = key;
            this.defaultValue = defaultValue;
            this.min = min;
            this.max = max;
            this.comment = comment;
        }

        String path() {
            return category + "." + key;
        }

        /** The comment written to the file: the description, then the range and the default. */
        String fileComment() {
            return comment + " [range: " + min + " ~ " + max + ", default: " + defaultValue + "]";
        }

        double read(Configuration config, ConfigProblems problems) {
            Property existing = existing(config, category, key);
            if (existing != null && !existing.isDoubleValue()) {
                problems.add(path() + " = " + existing.getString() + " is not a number; using " + defaultValue);
            }
            Property property = config.get(category, key, defaultValue, fileComment(), min, max);
            return validate(property.getDouble(defaultValue), problems);
        }

        /** A value that is not a number becomes the default; one outside the range is clamped. */
        double validate(double value, ConfigProblems problems) {
            if (Double.isNaN(value)) {
                problems.add(path() + " is not a number; using " + defaultValue);
                return defaultValue;
            }
            return problems.clamp(path(), value, min, max);
        }
    }

    /**
     * The property as read from the file, before Forge's getters replace an invalid value with the
     * default without saying so; null when the file does not have it yet.
     */
    private static Property existing(Configuration config, String category, String key) {
        if (!config.hasKey(category, key)) {
            return null;
        }
        return config.getCategory(category).get(key);
    }
}
