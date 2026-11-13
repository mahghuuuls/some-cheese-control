package com.mahghuuuls.somecheesecontrol.config;

import net.minecraftforge.common.config.Configuration;

import java.util.Collections;
import java.util.EnumMap;
import java.util.Map;

/**
 * The validated configuration, read once at startup and never changed while the game runs.
 *
 * <p>With {@link Options} and {@link MessageText}, this class is the only owner of option names,
 * defaults, ranges, message texts and validation. Everything else reads the snapshot returned by
 * {@link #get()}.</p>
 */
public final class Settings {

    private static Settings current = defaults();

    private final boolean restrictionsApplyInCreative;
    private final boolean messagesEnabled;
    private final long messageIntervalMillis;
    private final boolean diagnosticLogging;
    private final boolean unsupportedLiquidPlacement;
    private final boolean airborneEmptying;
    private final Map<MessageText, String> messages;
    private final ConfigProblems problems;

    private Settings(Builder b) {
        this.restrictionsApplyInCreative = b.restrictionsApplyInCreative;
        this.messagesEnabled = b.messagesEnabled;
        this.messageIntervalMillis = b.messageIntervalMillis;
        this.diagnosticLogging = b.diagnosticLogging;
        this.unsupportedLiquidPlacement = b.unsupportedLiquidPlacement;
        this.airborneEmptying = b.airborneEmptying;
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
        return b.build();
    }

    static long toMillis(double seconds) {
        return Math.round(seconds * 1000.0);
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
