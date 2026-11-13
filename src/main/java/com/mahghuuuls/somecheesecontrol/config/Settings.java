package com.mahghuuuls.somecheesecontrol.config;

import net.minecraft.block.Block;
import net.minecraft.item.Item;
import net.minecraftforge.common.config.Configuration;
import net.minecraftforge.fml.common.registry.ForgeRegistries;

import java.util.Collections;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * The validated configuration, read once at startup and never changed while the game runs.
 *
 * <p>With {@link Options} and {@link MessageText}, this class is the only owner of option names,
 * defaults, ranges, message texts and validation. Everything else reads the snapshot returned by
 * {@link #get()}.</p>
 *
 * <p>The snapshot is completed in two steps: values are read at pre-initialization, and the item
 * ids are turned into items by {@link #resolveRegistryEntries()} at initialization, once every item
 * is registered. After that it never changes.</p>
 */
public final class Settings {

    private static Settings current = defaults();

    private final boolean restrictionsApplyInCreative;
    private final boolean messagesEnabled;
    private final long messageIntervalMillis;
    private final boolean diagnosticLogging;
    private final boolean unsupportedLiquidPlacement;
    private final boolean airborneEmptying;
    private final boolean handOnlyCarrying;
    private final List<String> carriedItemIds;
    private final AllowedHand allowedHand;
    private final int climbingReleaseDelayTicks;
    private final List<String> extraClimbableBlockIds;
    private final boolean climbingMelee;
    private Set<Item> carriedItems = Collections.emptySet();
    private Set<Block> extraClimbableBlocks = Collections.emptySet();
    private boolean resolved;
    private final Map<MessageText, String> messages;
    private final ConfigProblems problems;

    private Settings(Builder b) {
        this.restrictionsApplyInCreative = b.restrictionsApplyInCreative;
        this.messagesEnabled = b.messagesEnabled;
        this.messageIntervalMillis = b.messageIntervalMillis;
        this.diagnosticLogging = b.diagnosticLogging;
        this.unsupportedLiquidPlacement = b.unsupportedLiquidPlacement;
        this.airborneEmptying = b.airborneEmptying;
        this.handOnlyCarrying = b.handOnlyCarrying;
        this.carriedItemIds = b.carriedItemIds;
        this.allowedHand = b.allowedHand;
        this.climbingReleaseDelayTicks = b.climbingReleaseDelayTicks;
        this.extraClimbableBlockIds = b.extraClimbableBlockIds;
        this.climbingMelee = b.climbingMelee;
        this.messages = Collections.unmodifiableMap(new EnumMap<>(b.messages));
        this.problems = b.problems;
    }

    public static Settings get() {
        return current;
    }

    /** Reads, validates and installs the snapshot. Called once, at pre-initialization. */
    public static Settings load(Configuration config) {
        Settings settings = read(config);
        if (config.hasChanged()) {
            config.save();
        }
        current = settings;
        return settings;
    }

    /** The built-in defaults, as a fresh configuration file would give them. */
    public static Settings defaults() {
        return new Builder().build();
    }

    static Settings read(Configuration config) {
        Builder b = new Builder();
        ConfigProblems problems = b.problems;
        b.restrictionsApplyInCreative = Options.RESTRICTIONS_IN_CREATIVE.read(config, problems);
        b.diagnosticLogging = Options.DIAGNOSTIC_LOGGING.read(config, problems);
        b.messagesEnabled = Options.MESSAGES_ENABLED.read(config, problems);
        b.messageIntervalMillis = toMillis(Options.MESSAGE_INTERVAL.read(config, problems));
        for (MessageText message : MessageText.values()) {
            String text = config.getString(message.key(), Options.MESSAGE_TEXTS, message.defaultText(),
                    "Message text. Plain text, shown as written.");
            if (text.trim().isEmpty()) {
                problems.add(Options.MESSAGE_TEXTS + "." + message.key() + " is empty; using the default text");
                text = message.defaultText();
            }
            b.messages.put(message, text);
        }
        b.unsupportedLiquidPlacement = Options.UNSUPPORTED_LIQUID.read(config, problems);
        b.airborneEmptying = Options.AIRBORNE_EMPTYING.read(config, problems);
        b.handOnlyCarrying = Options.CARRYING.read(config, problems);
        b.carriedItemIds = Options.CARRIED_ITEMS.read(config);
        b.allowedHand = Options.ALLOWED_HAND.read(config, problems);
        b.climbingReleaseDelayTicks = toTicks(Options.CLIMBING_RELEASE_DELAY.read(config, problems));
        b.extraClimbableBlockIds = Options.EXTRA_CLIMBABLE_BLOCKS.read(config);
        b.climbingMelee = Options.CLIMBING_MELEE.read(config, problems);
        return b.build();
    }

    static long toMillis(double seconds) {
        return Math.round(seconds * 1000.0);
    }

    /** Seconds to game ticks (20 per second), rounded to the nearest tick. */
    static int toTicks(double seconds) {
        return (int) Math.round(seconds * 20.0);
    }

    public boolean restrictionsApplyInCreative() {
        return restrictionsApplyInCreative;
    }

    public boolean messagesEnabled() {
        return messagesEnabled;
    }

    public long messageIntervalMillis() {
        return messageIntervalMillis;
    }

    public boolean diagnosticLogging() {
        return diagnosticLogging;
    }

    public boolean unsupportedLiquidPlacement() {
        return unsupportedLiquidPlacement;
    }

    public boolean airborneEmptying() {
        return airborneEmptying;
    }

    public boolean handOnlyCarrying() {
        return handOnlyCarrying;
    }

    /** The configured ids as written. */
    public List<String> carriedItemIds() {
        return carriedItemIds;
    }

    /**
     * Turns configured item ids into items and records unknown ones as problems. Call once, at
     * initialization, before any feature reads the items and before the problems are logged.
     */
    public void resolveRegistryEntries() {
        if (resolved) {
            return;
        }
        carriedItems = RegistryIds.resolve(ForgeRegistries.ITEMS, carriedItemIds, Options.CARRIED_ITEMS.path(), "item", problems);
        extraClimbableBlocks = RegistryIds.resolve(ForgeRegistries.BLOCKS, extraClimbableBlockIds,
                Options.EXTRA_CLIMBABLE_BLOCKS.path(), "block", problems);
        resolved = true;
    }

    /** The restricted carried items; empty until {@link #resolveRegistryEntries()} has run. */
    public Set<Item> carriedItems() {
        return carriedItems;
    }

    public AllowedHand allowedHand() {
        return allowedHand;
    }

    /** The climbing release delay in game ticks. */
    public int climbingReleaseDelayTicks() {
        return climbingReleaseDelayTicks;
    }

    /** Configured extra climbable blocks; empty until {@link #resolveRegistryEntries()} has run. */
    public Set<Block> extraClimbableBlocks() {
        return extraClimbableBlocks;
    }

    public boolean climbingMelee() {
        return climbingMelee;
    }

    public String message(MessageText message) {
        String text = messages.get(message);
        return text != null ? text : message.defaultText();
    }

    public ConfigProblems problems() {
        return problems;
    }

    /** Starts from every option's single default in {@link Options}. */
    static final class Builder {
        boolean restrictionsApplyInCreative = Options.RESTRICTIONS_IN_CREATIVE.defaultValue;
        boolean messagesEnabled = Options.MESSAGES_ENABLED.defaultValue;
        long messageIntervalMillis = toMillis(Options.MESSAGE_INTERVAL.defaultValue);
        boolean diagnosticLogging = Options.DIAGNOSTIC_LOGGING.defaultValue;
        boolean unsupportedLiquidPlacement = Options.UNSUPPORTED_LIQUID.defaultValue;
        boolean airborneEmptying = Options.AIRBORNE_EMPTYING.defaultValue;
        boolean handOnlyCarrying = Options.CARRYING.defaultValue;
        List<String> carriedItemIds = Options.CARRIED_ITEMS.defaults();
        AllowedHand allowedHand = Options.ALLOWED_HAND.defaultValue;
        int climbingReleaseDelayTicks = toTicks(Options.CLIMBING_RELEASE_DELAY.defaultValue);
        List<String> extraClimbableBlockIds = Options.EXTRA_CLIMBABLE_BLOCKS.defaults();
        boolean climbingMelee = Options.CLIMBING_MELEE.defaultValue;
        final Map<MessageText, String> messages = new EnumMap<>(MessageText.class);
        final ConfigProblems problems = new ConfigProblems();

        Builder() {
            for (MessageText message : MessageText.values()) {
                messages.put(message, message.defaultText());
            }
        }

        Settings build() {
            return new Settings(this);
        }
    }
}
