package com.mahghuuuls.somecheesecontrol.config;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.Collections;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;

/**
 * The rules a server sends its clients ({@link Settings#writeRules}, {@link Settings#readRules}) and
 * the client copy they become. Every value is set away from its default, so a value that is not
 * written, or read into the wrong field, fails.
 */
class SettingsMessageFormatTest {

    @AfterEach
    void dropClientCopy() {
        Settings.clearClientCopy();
    }

    @Test
    void everyRuleValueSurvivesTheMessage() {
        Settings.Builder b = new Settings.Builder();
        b.restrictionsApplyInCreative = !Options.RESTRICTIONS_IN_CREATIVE.defaultValue;
        b.unsupportedLiquidPlacement = !Options.UNSUPPORTED_LIQUID.defaultValue;
        b.airborneEmptying = !Options.AIRBORNE_EMPTYING.defaultValue;
        b.handOnlyCarrying = !Options.CARRYING.defaultValue;
        b.carriedItemIds = Arrays.asList("minecraft:milk_bucket", "modid:odd_bucket");
        b.allowedHand = AllowedHand.MAIN;
        b.climbingReleaseDelayTicks = 37;
        b.extraClimbableBlockIds = Collections.singletonList("minecraft:scaffold");
        b.climbingMelee = !Options.CLIMBING_MELEE.defaultValue;
        b.climbingRanged = !Options.CLIMBING_RANGED.defaultValue;
        b.climbingItemBlocklist = !Options.CLIMBING_ITEM_BLOCKLIST.defaultValue;
        b.blockedItemIds = Arrays.asList("minecraft:lead", "minecraft:name_tag", "minecraft:flint_and_steel");
        b.diagnosticLogging = true;
        Settings sent = b.build();

        Settings received = roundTrip(sent);

        assertEquals(sent.restrictionsApplyInCreative(), received.restrictionsApplyInCreative());
        assertEquals(sent.unsupportedLiquidPlacement(), received.unsupportedLiquidPlacement());
        assertEquals(sent.airborneEmptying(), received.airborneEmptying());
        assertEquals(sent.handOnlyCarrying(), received.handOnlyCarrying());
        assertEquals(sent.carriedItemIds(), received.carriedItemIds());
        assertEquals(AllowedHand.MAIN, received.allowedHand());
        assertEquals(37, received.climbingReleaseDelayTicks());
        assertEquals(sent.extraClimbableBlockIds(), received.extraClimbableBlockIds());
        assertEquals(sent.climbingMelee(), received.climbingMelee());
        assertEquals(sent.climbingRanged(), received.climbingRanged());
        assertEquals(sent.climbingItemBlocklist(), received.climbingItemBlocklist());
        assertEquals(sent.blockedItemIds(), received.blockedItemIds());
        assertFalse(received.diagnosticLogging(), "a client writes no diagnostic lines of its own");
    }

    @Test
    void theShippedDefaultsSurviveTheMessage() {
        Settings sent = Settings.defaults();
        Settings received = roundTrip(sent);
        assertEquals(sent.carriedItemIds(), received.carriedItemIds());
        assertEquals(sent.allowedHand(), received.allowedHand());
        assertEquals(sent.climbingReleaseDelayTicks(), received.climbingReleaseDelayTicks());
        assertEquals(sent.extraClimbableBlockIds(), received.extraClimbableBlockIds());
        assertEquals(sent.blockedItemIds(), received.blockedItemIds());
    }

    @Test
    void theWholeMessageIsRead() {
        ByteBuf buf = Unpooled.buffer();
        Settings.defaults().writeRules(buf);
        Settings.readRules(buf);
        assertEquals(0, buf.readableBytes(), "every written byte is read");
        buf.release();
    }

    @Test
    void aClientHasNoRulesUntilItsServerSendsThem() {
        assertNull(Settings.forSide(true), "nothing is refused on a client before the server's rules arrive");
        Settings received = Settings.defaults();
        Settings.installClientCopy(received);
        assertSame(received, Settings.forSide(true));
        assertSame(Settings.get(), Settings.forSide(false), "the server side keeps its own snapshot");
        Settings.clearClientCopy();
        assertNull(Settings.forSide(true), "leaving the server drops its rules");
    }

    private static Settings roundTrip(Settings sent) {
        ByteBuf buf = Unpooled.buffer();
        try {
            sent.writeRules(buf);
            return Settings.readRules(buf);
        } finally {
            buf.release();
        }
    }
}
