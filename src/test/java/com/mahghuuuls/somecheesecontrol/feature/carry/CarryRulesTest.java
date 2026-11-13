package com.mahghuuuls.somecheesecontrol.feature.carry;

import com.mahghuuuls.somecheesecontrol.config.AllowedHand;
import org.junit.jupiter.api.Test;

import static com.mahghuuuls.somecheesecontrol.feature.carry.CarryRules.Section.ARMOR;
import static com.mahghuuuls.somecheesecontrol.feature.carry.CarryRules.Section.MAIN;
import static com.mahghuuuls.somecheesecontrol.feature.carry.CarryRules.Section.OFFHAND;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CarryRulesTest {

    @Test
    void eitherHandAllowsTheSelectedSlotAndTheOffhandOnly() {
        assertTrue(CarryRules.isAllowedSlot(MAIN, 3, 3, AllowedHand.EITHER));
        assertTrue(CarryRules.isAllowedSlot(OFFHAND, 0, 3, AllowedHand.EITHER));
        assertFalse(CarryRules.isAllowedSlot(MAIN, 4, 3, AllowedHand.EITHER), "an unselected hotbar slot");
        assertFalse(CarryRules.isAllowedSlot(MAIN, 20, 3, AllowedHand.EITHER), "the main inventory");
        assertFalse(CarryRules.isAllowedSlot(ARMOR, 0, 3, AllowedHand.EITHER), "an armor slot");
    }

    @Test
    void pickupTakesAtMostOneFullStack() {
        assertEquals(1, CarryRules.pickupCount(3, 1, 64), "unstackable items: one per hand");
        assertEquals(16, CarryRules.pickupCount(20, 16, 64));
        assertEquals(5, CarryRules.pickupCount(5, 64, 64));
        assertEquals(10, CarryRules.pickupCount(64, 64, 10), "the inventory limit applies too");
    }

    @Test
    void mainHandOnlyRejectsTheOffhand() {
        assertTrue(CarryRules.isAllowedSlot(MAIN, 0, 0, AllowedHand.MAIN));
        assertFalse(CarryRules.isAllowedSlot(OFFHAND, 0, 0, AllowedHand.MAIN));
    }

    @Test
    void offhandOnlyRejectsTheSelectedSlot() {
        assertFalse(CarryRules.isAllowedSlot(MAIN, 0, 0, AllowedHand.OFF));
        assertTrue(CarryRules.isAllowedSlot(OFFHAND, 0, 0, AllowedHand.OFF));
    }
}
