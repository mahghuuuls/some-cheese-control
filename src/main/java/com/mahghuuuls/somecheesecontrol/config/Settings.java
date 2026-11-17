package com.mahghuuuls.somecheesecontrol.config;

import io.netty.buffer.ByteBuf;
import net.minecraft.block.Block;
import net.minecraft.item.Item;
import net.minecraftforge.common.config.Configuration;
import net.minecraftforge.fml.common.network.ByteBufUtils;
import net.minecraftforge.fml.common.registry.ForgeRegistries;

import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumMap;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.annotation.Nullable;

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
 *
 * <p>There are two snapshots, one per logical side. The server's comes from the configuration file.
 * A client holds a copy of the rules of the server it is connected to, received at login and
 * dropped at logout; the client's own configuration file never decides a rule. Code that acts for a
 * world reads the snapshot of that world's side ({@link #forSide}), because in single player both
 * sides run in one game.</p>
 */
public final class Settings {

    private static Settings current = defaults();

    /** The connected server's rules on this client; null when not connected to a server with this mod. */
    @Nullable
    private static volatile Settings clientCopy;

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
    private final boolean climbingRanged;
    private final boolean climbingWizardry;
    private final boolean climbingItemBlocklist;
    private final List<String> blockedItemIds;
    private final boolean vehicleFall;
    private final boolean cobwebRelease;
    private final List<String> extraWebBlockIds;
    private final boolean cobwebFollowsMobGriefing;
    private final boolean boatEscape;
    private final int boatReboardTicks;
    private final boolean bedExplosion;
    private Set<Item> carriedItems = Collections.emptySet();
    private Set<Block> extraClimbableBlocks = Collections.emptySet();
    private Set<Item> blockedItems = Collections.emptySet();
    private Set<Block> extraWebBlocks = Collections.emptySet();
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
        this.climbingRanged = b.climbingRanged;
        this.climbingWizardry = b.climbingWizardry;
        this.climbingItemBlocklist = b.climbingItemBlocklist;
        this.blockedItemIds = b.blockedItemIds;
        this.vehicleFall = b.vehicleFall;
        this.cobwebRelease = b.cobwebRelease;
        this.extraWebBlockIds = b.extraWebBlockIds;
        this.cobwebFollowsMobGriefing = b.cobwebFollowsMobGriefing;
        this.boatEscape = b.boatEscape;
        this.boatReboardTicks = b.boatReboardTicks;
        this.bedExplosion = b.bedExplosion;
        this.messages = Collections.unmodifiableMap(new EnumMap<>(b.messages));
        this.problems = b.problems;
    }

    /** The server's snapshot, read from this game's configuration file. */
    public static Settings get() {
        return current;
    }

    /**
     * The snapshot of one logical side: the server's for the server side, and the connected server's
     * copy for the client side, which is null before the server's rules arrive and on a server
     * without this mod. Nothing is refused on the client while it is null.
     */
    @Nullable
    public static Settings forSide(boolean logicalClient) {
        return logicalClient ? clientCopy : current;
    }

    /** Installs the rules received from the server for the current connection. */
    public static void installClientCopy(Settings settings) {
        clientCopy = settings;
    }

    /** Drops the received rules when the client leaves the server. */
    public static void clearClientCopy() {
        clientCopy = null;
    }

    /**
     * Writes the values a client needs to apply the rules as the server does: the switches of the
     * rules a client checks, the item and block ids as configured, the allowed hand, the release
     * delay and the Creative setting. Message texts, intervals, diagnostics and the switches of
     * server-only features (vehicle fall damage, cobweb release, boat escape) stay on the server; a
     * client copy reads them as their defaults.
     */
    public void writeRules(ByteBuf buf) {
        buf.writeBoolean(restrictionsApplyInCreative);
        buf.writeBoolean(unsupportedLiquidPlacement);
        buf.writeBoolean(airborneEmptying);
        buf.writeBoolean(handOnlyCarrying);
        writeIds(buf, carriedItemIds);
        ByteBufUtils.writeUTF8String(buf, allowedHand.name());
        ByteBufUtils.writeVarInt(buf, climbingReleaseDelayTicks, 5);
        writeIds(buf, extraClimbableBlockIds);
        buf.writeBoolean(climbingMelee);
        buf.writeBoolean(climbingRanged);
        buf.writeBoolean(climbingWizardry);
        buf.writeBoolean(climbingItemBlocklist);
        writeIds(buf, blockedItemIds);
        buf.writeBoolean(bedExplosion);
    }

    /**
     * Reads what {@link #writeRules} wrote, as a client copy. Its registry entries are not resolved
     * yet; the receiver calls {@link #resolveRegistryEntries()}. Unwritten values keep their defaults.
     *
     * @throws IllegalArgumentException when the allowed hand is not one this version knows
     */
    public static Settings readRules(ByteBuf buf) {
        Builder b = new Builder();
        b.restrictionsApplyInCreative = buf.readBoolean();
        b.unsupportedLiquidPlacement = buf.readBoolean();
        b.airborneEmptying = buf.readBoolean();
        b.handOnlyCarrying = buf.readBoolean();
        b.carriedItemIds = readIds(buf);
        b.allowedHand = AllowedHand.valueOf(ByteBufUtils.readUTF8String(buf));
        b.climbingReleaseDelayTicks = ByteBufUtils.readVarInt(buf, 5);
        b.extraClimbableBlockIds = readIds(buf);
        b.climbingMelee = buf.readBoolean();
        b.climbingRanged = buf.readBoolean();
        b.climbingWizardry = buf.readBoolean();
        b.climbingItemBlocklist = buf.readBoolean();
        b.blockedItemIds = readIds(buf);
        b.bedExplosion = buf.readBoolean();
        b.diagnosticLogging = false;
        return b.build();
    }

    private static void writeIds(ByteBuf buf, List<String> ids) {
        ByteBufUtils.writeVarInt(buf, ids.size(), 5);
        for (String id : ids) {
            ByteBufUtils.writeUTF8String(buf, id);
        }
    }

    private static List<String> readIds(ByteBuf buf) {
        int count = ByteBufUtils.readVarInt(buf, 5);
        List<String> ids = new ArrayList<>(count);
        for (int i = 0; i < count; i++) {
            ids.add(ByteBufUtils.readUTF8String(buf));
        }
        return Collections.unmodifiableList(ids);
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
        b.climbingRanged = Options.CLIMBING_RANGED.read(config, problems);
        b.climbingWizardry = Options.CLIMBING_WIZARDRY.read(config, problems);
        b.climbingItemBlocklist = Options.CLIMBING_ITEM_BLOCKLIST.read(config, problems);
        b.blockedItemIds = Options.CLIMBING_ITEM_BLOCKLIST_ITEMS.read(config);
        b.vehicleFall = Options.VEHICLE_FALL.read(config, problems);
        b.cobwebRelease = Options.COBWEB_RELEASE.read(config, problems);
        b.extraWebBlockIds = Options.EXTRA_WEB_BLOCKS.read(config);
        b.cobwebFollowsMobGriefing = Options.COBWEB_FOLLOWS_MOB_GRIEFING.read(config, problems);
        b.boatEscape = Options.BOAT_ESCAPE.read(config, problems);
        b.boatReboardTicks = toTicks(Options.BOAT_REBOARD_DELAY.read(config, problems));
        b.bedExplosion = Options.BED_EXPLOSION.read(config, problems);
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
        blockedItems = RegistryIds.resolve(ForgeRegistries.ITEMS, blockedItemIds, Options.CLIMBING_ITEM_BLOCKLIST_ITEMS.path(), "item",
                problems);
        extraWebBlocks = RegistryIds.resolve(ForgeRegistries.BLOCKS, extraWebBlockIds, Options.EXTRA_WEB_BLOCKS.path(), "block", problems);
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

    /** The extra climbable block ids as written, before resolution. */
    public List<String> extraClimbableBlockIds() {
        return extraClimbableBlockIds;
    }

    /** Configured extra climbable blocks; empty until {@link #resolveRegistryEntries()} has run. */
    public Set<Block> extraClimbableBlocks() {
        return extraClimbableBlocks;
    }

    public boolean climbingMelee() {
        return climbingMelee;
    }

    public boolean climbingRanged() {
        return climbingRanged;
    }

    /** The climbing Wizardry refusal switch; it acts only where Electroblob's Wizardry is installed. */
    public boolean climbingWizardry() {
        return climbingWizardry;
    }

    public boolean climbingItemBlocklist() {
        return climbingItemBlocklist;
    }

    /** The climbing item blocklist as written, before resolution. */
    public List<String> blockedItemIds() {
        return blockedItemIds;
    }

    /** Items in the climbing item blocklist; empty until {@link #resolveRegistryEntries()} has run. */
    public Set<Item> blockedItems() {
        return blockedItems;
    }

    /** Fall damage for boats and minecarts, and the pending fall of a player who boards one while falling. */
    public boolean vehicleFall() {
        return vehicleFall;
    }

    /** Cobweb release: removing the cobwebs inside a mob's body when it takes actual damage. */
    public boolean cobwebRelease() {
        return cobwebRelease;
    }

    /** The extra web block ids as written, before resolution. */
    public List<String> extraWebBlockIds() {
        return extraWebBlockIds;
    }

    /** Configured extra web blocks; empty until {@link #resolveRegistryEntries()} has run. */
    public Set<Block> extraWebBlocks() {
        return extraWebBlocks;
    }

    /** True when cobweb release follows the {@code mobGriefing} game rule. */
    public boolean cobwebFollowsMobGriefing() {
        return cobwebFollowsMobGriefing;
    }

    /** Boat escape: a damaged mob leaves its vanilla boat and cannot board a boat for a while. */
    public boolean boatEscape() {
        return boatEscape;
    }

    /** How long an ejected mob cannot board a boat, in game ticks. */
    public int boatReboardTicks() {
        return boatReboardTicks;
    }

    /** Bed-explosion prevention: refusing a bed use that would explode, in every game mode. */
    public boolean bedExplosion() {
        return bedExplosion;
    }

    /**
     * Each feature's switch as it is in effect, by its configuration category (the restriction
     * messages by their option), in the order of the features in the mod's description: what the status
     * command lists as the features that are on or off.
     */
    public Map<String, Boolean> featureSwitches() {
        Map<String, Boolean> switches = new LinkedHashMap<>();
        switches.put(Options.UNSUPPORTED_LIQUID.category, unsupportedLiquidPlacement);
        switches.put(Options.AIRBORNE_EMPTYING.category, airborneEmptying);
        switches.put(Options.CARRYING.category, handOnlyCarrying);
        switches.put(Options.CLIMBING_MELEE.category, climbingMelee);
        switches.put(Options.CLIMBING_RANGED.category, climbingRanged);
        switches.put(Options.CLIMBING_WIZARDRY.category, climbingWizardry);
        switches.put(Options.CLIMBING_ITEM_BLOCKLIST.category, climbingItemBlocklist);
        switches.put(Options.COBWEB_RELEASE.category, cobwebRelease);
        switches.put(Options.BOAT_ESCAPE.category, boatEscape);
        switches.put(Options.VEHICLE_FALL.category, vehicleFall);
        switches.put(Options.BED_EXPLOSION.category, bedExplosion);
        switches.put(Options.MESSAGES_ENABLED.path(), messagesEnabled);
        return switches;
    }

    /**
     * Every option, one {@code path = value} line each, in the units of the file: what the status
     * command lists as the active configuration. Switches and numbers are as in effect after
     * validation; id lists are as written, and the ids that were skipped are named among the
     * configuration problems. Message texts are listed under {@code general.messageTexts}.
     */
    public List<String> valueLines() {
        List<String> lines = new ArrayList<>();
        line(lines, Options.RESTRICTIONS_IN_CREATIVE.path(), restrictionsApplyInCreative);
        line(lines, Options.DIAGNOSTIC_LOGGING.path(), diagnosticLogging);
        line(lines, Options.MESSAGES_ENABLED.path(), messagesEnabled);
        line(lines, Options.MESSAGE_INTERVAL.path(), seconds(messageIntervalMillis / 1000.0));
        line(lines, Options.UNSUPPORTED_LIQUID.path(), unsupportedLiquidPlacement);
        line(lines, Options.AIRBORNE_EMPTYING.path(), airborneEmptying);
        line(lines, Options.CARRYING.path(), handOnlyCarrying);
        line(lines, Options.CARRIED_ITEMS.path(), carriedItemIds);
        line(lines, Options.ALLOWED_HAND.path(), allowedHand.configValue());
        line(lines, Options.CLIMBING_RELEASE_DELAY.path(), seconds(climbingReleaseDelayTicks / 20.0));
        line(lines, Options.EXTRA_CLIMBABLE_BLOCKS.path(), extraClimbableBlockIds);
        line(lines, Options.CLIMBING_MELEE.path(), climbingMelee);
        line(lines, Options.CLIMBING_RANGED.path(), climbingRanged);
        line(lines, Options.CLIMBING_WIZARDRY.path(), climbingWizardry);
        line(lines, Options.CLIMBING_ITEM_BLOCKLIST.path(), climbingItemBlocklist);
        line(lines, Options.CLIMBING_ITEM_BLOCKLIST_ITEMS.path(), blockedItemIds);
        line(lines, Options.COBWEB_RELEASE.path(), cobwebRelease);
        line(lines, Options.EXTRA_WEB_BLOCKS.path(), extraWebBlockIds);
        line(lines, Options.COBWEB_FOLLOWS_MOB_GRIEFING.path(), cobwebFollowsMobGriefing);
        line(lines, Options.BOAT_ESCAPE.path(), boatEscape);
        line(lines, Options.BOAT_REBOARD_DELAY.path(), seconds(boatReboardTicks / 20.0));
        line(lines, Options.VEHICLE_FALL.path(), vehicleFall);
        line(lines, Options.BED_EXPLOSION.path(), bedExplosion);
        for (MessageText message : MessageText.values()) {
            line(lines, Options.MESSAGE_TEXTS + "." + message.key(), "\"" + message(message) + "\"");
        }
        return lines;
    }

    private static void line(List<String> lines, String path, Object value) {
        lines.add(path + " = " + value);
    }

    private static String seconds(double value) {
        return String.format(Locale.ROOT, "%.2f", value);
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
        boolean climbingRanged = Options.CLIMBING_RANGED.defaultValue;
        boolean climbingWizardry = Options.CLIMBING_WIZARDRY.defaultValue;
        boolean climbingItemBlocklist = Options.CLIMBING_ITEM_BLOCKLIST.defaultValue;
        List<String> blockedItemIds = Options.CLIMBING_ITEM_BLOCKLIST_ITEMS.defaults();
        boolean vehicleFall = Options.VEHICLE_FALL.defaultValue;
        boolean cobwebRelease = Options.COBWEB_RELEASE.defaultValue;
        List<String> extraWebBlockIds = Options.EXTRA_WEB_BLOCKS.defaults();
        boolean cobwebFollowsMobGriefing = Options.COBWEB_FOLLOWS_MOB_GRIEFING.defaultValue;
        boolean boatEscape = Options.BOAT_ESCAPE.defaultValue;
        int boatReboardTicks = toTicks(Options.BOAT_REBOARD_DELAY.defaultValue);
        boolean bedExplosion = Options.BED_EXPLOSION.defaultValue;
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
