package com.mahghuuuls.somecheesecontrol.config;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * The shipped defaults and their validation. Reading a real configuration file needs FML's launch
 * data, so these tests check the single option definitions that both the file reader and the
 * defaults use.
 */
class SettingsTest {

    @Test
    void shippedDefaultsMatchTheRequirements() {
        assertTrue(Options.UNSUPPORTED_LIQUID.defaultValue, "every feature is on after installation");
        assertTrue(Options.AIRBORNE_EMPTYING.defaultValue, "every feature is on after installation");
        assertTrue(Options.CARRYING.defaultValue, "every feature is on after installation");
        assertEquals(java.util.Arrays.asList("minecraft:water_bucket", "minecraft:lava_bucket"), Options.CARRIED_ITEMS.defaults(),
                "water and lava buckets are carried in a hand by default");
        assertEquals(AllowedHand.EITHER, Options.ALLOWED_HAND.defaultValue, "either hand by default");
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
        assertEquals(Options.MESSAGES_ENABLED.defaultValue, defaults.messagesEnabled());
        assertEquals(Settings.toMillis(Options.MESSAGE_INTERVAL.defaultValue), defaults.messageIntervalMillis());
        assertEquals(Options.RESTRICTIONS_IN_CREATIVE.defaultValue, defaults.restrictionsApplyInCreative());
        assertEquals(Options.DIAGNOSTIC_LOGGING.defaultValue, defaults.diagnosticLogging());
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
