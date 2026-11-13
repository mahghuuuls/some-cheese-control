package com.mahghuuuls.somecheesecontrol.feature.liquid;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class LiquidRulesTest {

    @Test
    void noCollisionAndNoLiquidBelowGivesNoSupport() {
        // Air, and walk-through blocks such as tall grass, torches and rails. Which blocks have no
        // collision box is the game's own knowledge and is checked in a running game, not here.
        assertFalse(LiquidRules.isSupported(false, false));
    }

    @Test
    void solidOrPartialBlockBelowGivesSupport() {
        // Full blocks, slabs, stairs, fences: they have a collision box.
        assertTrue(LiquidRules.isSupported(false, true));
    }

    @Test
    void liquidBelowGivesSupportSoPoolsCanBeFilled() {
        assertTrue(LiquidRules.isSupported(true, false));
    }

    @Test
    void vanillaBucketFillsReplaceableClickedBlockOnlyFromTheTop() {
        assertTrue(LiquidRules.placesIntoClickedBlock(false, true, true));
        assertFalse(LiquidRules.placesIntoClickedBlock(false, true, false));
        assertFalse(LiquidRules.placesIntoClickedBlock(false, false, true));
    }

    @Test
    void universalBucketAlwaysUsesTheBlockOnTheClickedSide() {
        assertFalse(LiquidRules.placesIntoClickedBlock(true, true, true));
    }
}
