package com.mahghuuuls.somecheesecontrol.config;

import net.minecraftforge.common.config.Configuration;
import org.junit.jupiter.api.Test;

import java.util.Arrays;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * The shipped defaults and their validation. Reading a real configuration file needs FML's launch
 * data, so these tests check the single option definitions that both the file reader and the
 * defaults use, and read an in-memory configuration where a key must be told from another.
 */
class SettingsTest {

    @Test
    void shippedDefaultsMatchTheRequirements() {
        assertTrue(Options.UNSUPPORTED_LIQUID.defaultValue, "every feature is on after installation");
        assertTrue(Options.AIRBORNE_EMPTYING.defaultValue, "every feature is on after installation");
        assertTrue(Options.CARRYING.defaultValue, "every feature is on after installation");
        assertEquals(Arrays.asList("minecraft:water_bucket", "minecraft:lava_bucket"), Options.CARRIED_ITEMS.defaults(),
                "water and lava buckets are carried in a hand by default");
        assertEquals(AllowedHand.EITHER, Options.ALLOWED_HAND.defaultValue, "either hand by default");
        assertTrue(Options.CLIMBING_MELEE.defaultValue, "every feature is on after installation");
        assertTrue(Options.CLIMBING_RANGED.defaultValue, "every feature is on after installation");
        assertTrue(Options.CLIMBING_ITEM_BLOCKLIST.defaultValue, "every feature is on after installation");
        assertTrue(Options.VEHICLE_FALL.defaultValue, "every feature is on after installation");
        assertTrue(Options.COBWEB_RELEASE.defaultValue, "every feature is on after installation");
        assertTrue(Options.EXTRA_WEB_BLOCKS.defaults().isEmpty(), "no extra web blocks by default");
        assertFalse(Options.COBWEB_FOLLOWS_MOB_GRIEFING.defaultValue, "cobweb release ignores mobGriefing by default");
        assertTrue(Options.BOAT_ESCAPE.defaultValue, "every feature is on after installation");
        assertTrue(Options.BED_EXPLOSION.defaultValue, "every feature is on after installation");
        assertEquals(5.0, Options.BOAT_REBOARD_DELAY.defaultValue, "5 seconds before boarding again");
        assertEquals(0.0, Options.BOAT_REBOARD_DELAY.min);
        assertEquals(60.0, Options.BOAT_REBOARD_DELAY.max);
        assertTrue(Options.CLIMBING_ITEM_BLOCKLIST_ITEMS.defaults().isEmpty(), "the climbing item blocklist is empty by default");
        assertEquals(0.5, Options.CLIMBING_RELEASE_DELAY.defaultValue, "half a second after letting go");
        assertEquals(0.0, Options.CLIMBING_RELEASE_DELAY.min);
        assertEquals(5.0, Options.CLIMBING_RELEASE_DELAY.max);
        assertTrue(Options.EXTRA_CLIMBABLE_BLOCKS.defaults().isEmpty(), "no extra climbable blocks by default");
        assertTrue(Options.MESSAGES_ENABLED.defaultValue, "messages are on after installation");
        assertEquals(2.0, Options.MESSAGE_INTERVAL.defaultValue, "one message every 2 seconds at most");
        assertEquals(0.0, Options.MESSAGE_INTERVAL.min);
        assertEquals(10.0, Options.MESSAGE_INTERVAL.max);
        assertFalse(Options.RESTRICTIONS_IN_CREATIVE.defaultValue, "Creative players are exempt by default");
        assertFalse(Options.DIAGNOSTIC_LOGGING.defaultValue, "diagnostic logging is off by default");
    }

    @Test
    void defaultsSnapshotUsesTheOptionDefinitions() {
        Settings defaults = Settings.defaults();
        assertEquals(Options.UNSUPPORTED_LIQUID.defaultValue, defaults.unsupportedLiquidPlacement());
        assertEquals(Options.AIRBORNE_EMPTYING.defaultValue, defaults.airborneEmptying());
        assertEquals(Options.CARRYING.defaultValue, defaults.handOnlyCarrying());
        assertEquals(Options.CARRIED_ITEMS.defaults(), defaults.carriedItemIds());
        assertEquals(Options.ALLOWED_HAND.defaultValue, defaults.allowedHand());
        assertEquals(Options.CLIMBING_MELEE.defaultValue, defaults.climbingMelee());
        assertEquals(Options.CLIMBING_RANGED.defaultValue, defaults.climbingRanged());
        assertEquals(Options.CLIMBING_ITEM_BLOCKLIST.defaultValue, defaults.climbingItemBlocklist());
        assertEquals(10, defaults.climbingReleaseDelayTicks(), "0.5 seconds is 10 ticks");
        assertEquals(Options.MESSAGES_ENABLED.defaultValue, defaults.messagesEnabled());
        assertEquals(Settings.toMillis(Options.MESSAGE_INTERVAL.defaultValue), defaults.messageIntervalMillis());
        assertEquals(Options.RESTRICTIONS_IN_CREATIVE.defaultValue, defaults.restrictionsApplyInCreative());
        assertEquals(Options.DIAGNOSTIC_LOGGING.defaultValue, defaults.diagnosticLogging());
        assertEquals(Options.COBWEB_RELEASE.defaultValue, defaults.cobwebRelease());
        assertEquals(Options.EXTRA_WEB_BLOCKS.defaults(), defaults.extraWebBlockIds());
        assertEquals(Options.COBWEB_FOLLOWS_MOB_GRIEFING.defaultValue, defaults.cobwebFollowsMobGriefing());
        assertEquals(Options.BOAT_ESCAPE.defaultValue, defaults.boatEscape());
        assertEquals(Options.BED_EXPLOSION.defaultValue, defaults.bedExplosion());
        assertEquals(100, defaults.boatReboardTicks(), "5 seconds is 100 ticks");
        assertTrue(defaults.problems().isEmpty());
    }

    @Test
    void defaultMessageTextsAreUsedWhenNothingIsConfigured() {
        Settings defaults = Settings.defaults();
        for (MessageText message : MessageText.values()) {
            assertEquals(message.defaultText(), defaults.message(message));
        }
    }

    @Test
    void messageOptionsLiveInTheGeneralCategory() {
        assertEquals("general", Options.MESSAGES_ENABLED.category);
        assertEquals("general", Options.MESSAGE_INTERVAL.category);
        assertTrue(Options.MESSAGE_TEXTS.startsWith("general."));
    }

    @Test
    void eachFeatureHasItsOwnCategoryAndSwitch() {
        assertEquals("unsupportedLiquidPlacement.enabled", Options.UNSUPPORTED_LIQUID.path());
        assertEquals("airborneEmptying.enabled", Options.AIRBORNE_EMPTYING.path());
        assertEquals("handOnlyCarrying.enabled", Options.CARRYING.path());
        assertEquals("handOnlyCarrying.items", Options.CARRIED_ITEMS.path());
        assertEquals("handOnlyCarrying.allowedHand", Options.ALLOWED_HAND.path());
        assertEquals("climbingMelee.enabled", Options.CLIMBING_MELEE.path());
        assertEquals("climbingRanged.enabled", Options.CLIMBING_RANGED.path());
        assertEquals("climbingItemBlocklist.enabled", Options.CLIMBING_ITEM_BLOCKLIST.path());
        assertEquals("climbingItemBlocklist.items", Options.CLIMBING_ITEM_BLOCKLIST_ITEMS.path());
        assertEquals("climbing.releaseDelaySeconds", Options.CLIMBING_RELEASE_DELAY.path());
        assertEquals("climbing.extraClimbableBlocks", Options.EXTRA_CLIMBABLE_BLOCKS.path());
        assertEquals("cobwebRelease.enabled", Options.COBWEB_RELEASE.path());
        assertEquals("cobwebRelease.extraWebBlocks", Options.EXTRA_WEB_BLOCKS.path());
        assertEquals("cobwebRelease.followMobGriefing", Options.COBWEB_FOLLOWS_MOB_GRIEFING.path());
        assertEquals("boatEscape.enabled", Options.BOAT_ESCAPE.path());
        assertEquals("boatEscape.reboardSeconds", Options.BOAT_REBOARD_DELAY.path());
        assertEquals("bedExplosion.enabled", Options.BED_EXPLOSION.path());
    }

    @Test
    void theItemBlocklistSwitchAndListAreReadFromTheirOwnKeys() {
        Configuration config = new Configuration();
        config.get("climbingItemBlocklist", "enabled", true).set(false);
        config.get("climbingItemBlocklist", "items", new String[0]).set(new String[]{"minecraft:lead", "nosuchmod:thing"});
        Settings settings = Settings.read(config);
        assertFalse(settings.climbingItemBlocklist());
        assertTrue(settings.climbingRanged(), "the ranged switch is a separate key");
        assertEquals(Arrays.asList("minecraft:lead", "nosuchmod:thing"), settings.blockedItemIds());
    }

    @Test
    void vehicleFallDamageCanBeSwitchedOff() {
        Configuration config = new Configuration();
        config.get("vehicleFall", "enabled", true).set(false);
        assertFalse(Settings.read(config).vehicleFall(), "the switch is read from vehicleFall.enabled");
        assertTrue(Settings.read(new Configuration()).vehicleFall(), "on when the file does not have it yet");
        assertEquals("vehicleFall.enabled", Options.VEHICLE_FALL.path());
    }

    @Test
    void cobwebReleaseOptionsAreReadFromTheirOwnKeys() {
        Configuration config = new Configuration();
        config.get("cobwebRelease", "enabled", true).set(false);
        config.get("cobwebRelease", "followMobGriefing", false).set(true);
        config.get("cobwebRelease", "extraWebBlocks", new String[0]).set(new String[]{"somemod:thick_web"});
        Settings settings = Settings.read(config);
        assertFalse(settings.cobwebRelease());
        assertTrue(settings.cobwebFollowsMobGriefing());
        assertEquals(Arrays.asList("somemod:thick_web"), settings.extraWebBlockIds());
        assertTrue(settings.vehicleFall(), "another feature's switch is a separate key");
    }

    @Test
    void boatEscapeOptionsAreReadFromTheirOwnKeys() {
        Configuration config = new Configuration();
        config.get("boatEscape", "enabled", true).set(false);
        config.get("boatEscape", "reboardSeconds", 5.0).set(2.5);
        Settings settings = Settings.read(config);
        assertFalse(settings.boatEscape());
        assertEquals(50, settings.boatReboardTicks());
        assertTrue(settings.cobwebRelease(), "another feature's switch is a separate key");
    }

    @Test
    void intervalCommentStatesRangeAndDefault() {
        String comment = Options.MESSAGE_INTERVAL.fileComment();
        assertTrue(comment.contains("default: 2.0"), comment);
        assertTrue(comment.contains("0.0 ~ 10.0"), comment);
    }

    @Test
    void intervalThatIsNotANumberUsesTheDefault() {
        ConfigProblems problems = new ConfigProblems();
        assertEquals(2.0, Options.MESSAGE_INTERVAL.validate(Double.NaN, problems));
        assertEquals(1, problems.all().size());
    }

    @Test
    void intervalOutsideRangeIsClamped() {
        ConfigProblems problems = new ConfigProblems();
        assertEquals(10.0, Options.MESSAGE_INTERVAL.validate(30.0, problems));
        assertEquals(1, problems.all().size());
    }

    @Test
    void valueInsideRangeIsKeptWithoutProblem() {
        ConfigProblems problems = new ConfigProblems();
        assertEquals(2.0, problems.clamp("general.messageIntervalSeconds", 2.0, 0.0, 10.0));
        assertTrue(problems.isEmpty());
    }

    @Test
    void valueOutsideRangeIsClampedAndReported() {
        ConfigProblems problems = new ConfigProblems();
        assertEquals(10.0, problems.clamp("general.messageIntervalSeconds", 30.0, 0.0, 10.0));
        assertEquals(0.0, problems.clamp("general.messageIntervalSeconds", -1.0, 0.0, 10.0));
        assertEquals(2, problems.all().size());
        assertTrue(problems.all().get(0).contains("general.messageIntervalSeconds"));
    }
}
