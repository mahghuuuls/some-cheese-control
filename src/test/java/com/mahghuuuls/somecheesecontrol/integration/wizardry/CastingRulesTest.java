package com.mahghuuuls.somecheesecontrol.integration.wizardry;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CastingRulesTest {

    @Test
    void heldItemSourcesAreOpenEnded() {
        assertTrue(CastingRules.mayComeFromHeldItem("WAND"));
        assertTrue(CastingRules.mayComeFromHeldItem("SCROLL"));
        assertTrue(CastingRules.mayComeFromHeldItem("OTHER"));
        assertTrue(CastingRules.mayComeFromHeldItem("BATTLEMAGE_ITEM"), "an addon's own source added at runtime");
        assertTrue(CastingRules.mayComeFromHeldItem("SAGE_ITEM"));
        assertTrue(CastingRules.mayComeFromHeldItem(null), "an addon cast with no source");
    }

    @Test
    void commandsEntitiesAndDispensersAreNeverHeldItems() {
        assertFalse(CastingRules.mayComeFromHeldItem("COMMAND"), "/cast passes");
        assertFalse(CastingRules.mayComeFromHeldItem("NPC"), "a spell-casting entity passes");
        assertFalse(CastingRules.mayComeFromHeldItem("DISPENSER"));
    }

    @Test
    void chargeupIsTheSpellsTimesTheModifierRoundedDown() {
        assertEquals(20, CastingRules.chargeupTicks(20, 1.0F, false));
        assertEquals(14, CastingRules.chargeupTicks(20, 0.7F, false), "a charge-up upgrade shortens it");
        assertEquals(15, CastingRules.chargeupTicks(20, 0.79F, false), "15.8 is rounded down, as Wizardry does");
        assertEquals(0, CastingRules.chargeupTicks(20, 0.0F, false));
        assertEquals(0, CastingRules.chargeupTicks(0, 1.0F, false));
    }

    @Test
    void aScrollHasNoChargeup() {
        assertEquals(0, CastingRules.chargeupTicks(30, 1.0F, true));
    }

    @Test
    void aChargedUseStopsUpToAndIncludingTheTickThatWouldCast() {
        assertTrue(CastingRules.isBeforeFirstCast(0, 20, false));
        assertTrue(CastingRules.isBeforeFirstCast(19, 20, false));
        assertTrue(CastingRules.isBeforeFirstCast(20, 20, false), "the charged spell would fire in this tick's use update");
        assertFalse(CastingRules.isBeforeFirstCast(21, 20, false), "fired already: the button is only held down");
        assertTrue(CastingRules.isBeforeFirstCast(20, 20, true), "a continuous spell's first cast, with no event");
        assertFalse(CastingRules.isBeforeFirstCast(21, 20, true), "channeling: the channel event stops it");
    }

    @Test
    void aChannelWithoutChargeupStopsOnlyAtItsFirstTick() {
        assertTrue(CastingRules.isBeforeFirstCast(0, 0, true), "the first channel tick casts with no event");
        assertFalse(CastingRules.isBeforeFirstCast(1, 0, true), "later ticks are stopped by the channel event");
    }

    @Test
    void anInstantSpellHasNoUseToStop() {
        assertFalse(CastingRules.isBeforeFirstCast(0, 0, false), "it cast at the click; its use is only the button held down");
    }
}
