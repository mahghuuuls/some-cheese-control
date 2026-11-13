package com.mahghuuuls.somecheesecontrol.config;

import net.minecraftforge.common.config.Configuration;
import net.minecraftforge.common.config.Property;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

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
    static final String CARRYING_CATEGORY = "handOnlyCarrying";
    static final String CLIMBING_STATE_CATEGORY = "climbing";
    static final String CLIMBING_MELEE_CATEGORY = "climbingMelee";
    static final String CLIMBING_RANGED_CATEGORY = "climbingRanged";
    static final String CLIMBING_ITEM_BLOCKLIST_CATEGORY = "climbingItemBlocklist";

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

    static final BooleanOption CARRYING = new BooleanOption(CARRYING_CATEGORY, "enabled", true,
            "Drop the listed items when they are anywhere in the player's inventory except an allowed hand. Backpacks and chests are not checked. Restart required.");
    static final StringListOption CARRIED_ITEMS = new StringListOption(CARRYING_CATEGORY, "items",
            new String[] {"minecraft:water_bucket", "minecraft:lava_bucket"},
            "Items that may only be carried in an allowed hand, one modid:name per line. Example: minecraft:water_bucket. Restart required.");
    static final AllowedHandOption ALLOWED_HAND = new AllowedHandOption(CARRYING_CATEGORY, "allowedHand", AllowedHand.EITHER,
            "Where the listed items may be carried: main (the selected hotbar slot), off (the offhand) or either. Restart required.");

    static final DoubleOption CLIMBING_RELEASE_DELAY = new DoubleOption(CLIMBING_STATE_CATEGORY, "releaseDelaySeconds", 0.5, 0.0, 5.0,
            "How long the climbing restrictions still apply after letting go of a ladder or vine, in seconds. Standing on the ground ends them at once. Restart required.");
    static final StringListOption EXTRA_CLIMBABLE_BLOCKS = new StringListOption(CLIMBING_STATE_CATEGORY, "extraClimbableBlocks",
            new String[0],
            "Blocks that also count as climbable, one modid:name per line. Ladders, vines and modded blocks that declare themselves climbable count already. Example: minecraft:iron_bars. Restart required.");
    static final StringListOption CLIMBING_ITEM_BLOCKLIST_ITEMS = new StringListOption(CLIMBING_ITEM_BLOCKLIST_CATEGORY, "items",
            new String[0],
            "Items that cannot be used in any way while on a ladder or vine above the ground and for the release delay after letting go: right click in the air, on a block or on an entity, or holding use. One modid:name per line; empty by default. Example: minecraft:lead. Used only when enabled is true. Restart required.");
    static final BooleanOption CLIMBING_MELEE = new BooleanOption(CLIMBING_MELEE_CATEGORY, "enabled", true,
            "Refuse melee attacks, with either hand, while on a ladder or vine above the ground and for the release delay after letting go. Restart required.");
    static final BooleanOption CLIMBING_ITEM_BLOCKLIST = new BooleanOption(CLIMBING_ITEM_BLOCKLIST_CATEGORY, "enabled", true,
            "Refuse every use of the items in the list below while climbing. With an empty list this adds nothing. Restart required.");
    static final BooleanOption CLIMBING_RANGED = new BooleanOption(CLIMBING_RANGED_CATEGORY, "enabled", true,
            "Refuse bows, crossbows, throwing weapons, thrown items (snowballs, eggs, ender pearls, splash and lingering potions, experience bottles, dynamite) and fishing rods while on a ladder or vine above the ground and for the release delay after letting go. A draw, load or charge in progress stops without firing and without cost. Restart required.");

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

    static final class StringListOption {
        final String category;
        final String key;
        final String[] defaultValue;
        final String comment;

        StringListOption(String category, String key, String[] defaultValue, String comment) {
            this.category = category;
            this.key = key;
            this.defaultValue = defaultValue.clone();
            this.comment = comment;
        }

        String path() {
            return category + "." + key;
        }

        List<String> defaults() {
            return Collections.unmodifiableList(Arrays.asList(defaultValue.clone()));
        }

        List<String> read(Configuration config) {
            String[] values = config.getStringList(key, category, defaultValue, comment);
            return Collections.unmodifiableList(Arrays.asList(values));
        }
    }

    static final class AllowedHandOption {
        final String category;
        final String key;
        final AllowedHand defaultValue;
        final String comment;

        AllowedHandOption(String category, String key, AllowedHand defaultValue, String comment) {
            this.category = category;
            this.key = key;
            this.defaultValue = defaultValue;
            this.comment = comment;
        }

        String path() {
            return category + "." + key;
        }

        AllowedHand read(Configuration config, ConfigProblems problems) {
            String value = config.getString(key, category, defaultValue.configValue(), comment, AllowedHand.configValues());
            return validate(value, problems);
        }

        /**
         * The hand for {@code value}. A value that allows no hand would make every listed item drop at
         * once and forever, so it is an error and the default is used instead.
         */
        AllowedHand validate(String value, ConfigProblems problems) {
            AllowedHand hand = AllowedHand.parse(value);
            if (hand == null) {
                problems.addError(path() + " = " + value + " allows no hand; using " + defaultValue.configValue());
                return defaultValue;
            }
            return hand;
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
