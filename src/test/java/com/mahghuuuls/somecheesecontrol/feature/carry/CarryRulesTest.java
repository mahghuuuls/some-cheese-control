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

    @Test
    void anItemOutsideTheHandsMovesIntoAnEmptyAllowedOffhand() {
        assertEquals(CarryRules.Outcome.MOVE_TO_OFFHAND, CarryRules.outcome(MAIN, 5, 3, AllowedHand.EITHER, true), "a bucket given into slot 5");
        assertEquals(CarryRules.Outcome.MOVE_TO_OFFHAND, CarryRules.outcome(MAIN, 3, 4, AllowedHand.EITHER, true),
                "slot 3 after the selected slot changed to 4 (a hotbar switch)");
        assertEquals(CarryRules.Outcome.MOVE_TO_OFFHAND, CarryRules.outcome(ARMOR, 1, 3, AllowedHand.OFF, true));
        assertEquals(CarryRules.Outcome.MOVE_TO_OFFHAND, CarryRules.outcome(MAIN, 3, 3, AllowedHand.OFF, true),
                "offhand only: the selected slot is outside the hands");
    }

    @Test
    void anItemOutsideTheHandsIsDroppedWhenTheOffhandIsFullOrNotAllowed() {
        assertEquals(CarryRules.Outcome.DROP, CarryRules.outcome(MAIN, 5, 3, AllowedHand.EITHER, false), "a full offhand");
        assertEquals(CarryRules.Outcome.DROP, CarryRules.outcome(MAIN, 5, 3, AllowedHand.MAIN, true), "main only: never moved");
        assertEquals(CarryRules.Outcome.DROP, CarryRules.outcome(OFFHAND, 0, 3, AllowedHand.MAIN, true), "main only: the offhand drops");
    }

    @Test
    void anItemInAnAllowedHandStays() {
        assertEquals(CarryRules.Outcome.STAY, CarryRules.outcome(MAIN, 3, 3, AllowedHand.EITHER, true));
        assertEquals(CarryRules.Outcome.STAY, CarryRules.outcome(OFFHAND, 0, 3, AllowedHand.EITHER, false));
    }

    @Test
    void ownSlotsAreClassifiedByTheirInventoryIndex() {
        assertEquals(CarryRules.SlotKind.ALLOWED_HAND, CarryRules.ownSlotKind(3, 3, AllowedHand.EITHER), "the selected hotbar slot");
        assertEquals(CarryRules.SlotKind.OUTSIDE_HANDS, CarryRules.ownSlotKind(4, 3, AllowedHand.EITHER), "another hotbar slot");
        assertEquals(CarryRules.SlotKind.OUTSIDE_HANDS, CarryRules.ownSlotKind(35, 3, AllowedHand.EITHER), "the last main slot");
        assertEquals(CarryRules.SlotKind.OUTSIDE_HANDS, CarryRules.ownSlotKind(36, 3, AllowedHand.EITHER), "the first armor slot");
        assertEquals(CarryRules.SlotKind.OUTSIDE_HANDS, CarryRules.ownSlotKind(39, 3, AllowedHand.EITHER), "the last armor slot");
        assertEquals(CarryRules.SlotKind.ALLOWED_HAND, CarryRules.ownSlotKind(40, 3, AllowedHand.EITHER), "the offhand");
        assertEquals(CarryRules.SlotKind.OUTSIDE_HANDS, CarryRules.ownSlotKind(40, 3, AllowedHand.MAIN), "the offhand, main only");
        assertEquals(CarryRules.SlotKind.OUTSIDE_HANDS, CarryRules.ownSlotKind(3, 3, AllowedHand.OFF), "the selected slot, offhand only");
    }

    @Test
    void placingARestrictedItemOutsideTheHandsIsRefused() {
        assertTrue(CarryRules.refusesPlacing(true, CarryRules.SlotKind.OUTSIDE_HANDS), "a main inventory slot");
        assertFalse(CarryRules.refusesPlacing(true, CarryRules.SlotKind.ALLOWED_HAND), "the empty offhand slot");
        assertFalse(CarryRules.refusesPlacing(true, CarryRules.SlotKind.NOT_OWN), "a chest slot");
        assertFalse(CarryRules.refusesPlacing(false, CarryRules.SlotKind.OUTSIDE_HANDS), "an ordinary item");
    }

    @Test
    void aShiftClickThatBringsARestrictedItemIntoTheInventoryIsRefused() {
        assertTrue(CarryRules.refusesQuickMove(true, false, false), "from a chest into the inventory");
        assertTrue(CarryRules.refusesQuickMove(true, false, true), "from a chest even where own items would leave");
        assertTrue(CarryRules.refusesQuickMove(true, true, false), "from the hand in a crafting table, furnace or the inventory screen");
        assertFalse(CarryRules.refusesQuickMove(true, true, true), "from the hand into a chest");
        assertFalse(CarryRules.refusesQuickMove(false, false, false), "an ordinary item");
    }

    @Test
    void aNumberKeyThatPutsARestrictedItemOutsideTheHandsIsRefused() {
        assertTrue(CarryRules.refusesHotbarSwap(true, CarryRules.SlotKind.NOT_OWN, false, false),
                "a chest bucket into an unselected hotbar slot");
        assertFalse(CarryRules.refusesHotbarSwap(true, CarryRules.SlotKind.NOT_OWN, false, true),
                "a chest bucket into the selected hotbar slot");
        assertTrue(CarryRules.refusesHotbarSwap(false, CarryRules.SlotKind.OUTSIDE_HANDS, true, true),
                "a held bucket out to a main inventory slot");
        assertFalse(CarryRules.refusesHotbarSwap(false, CarryRules.SlotKind.NOT_OWN, true, true), "a held bucket out to a chest");
        assertFalse(CarryRules.refusesHotbarSwap(false, CarryRules.SlotKind.ALLOWED_HAND, true, false),
                "a bucket from an unselected hotbar slot into the offhand");
        assertFalse(CarryRules.refusesHotbarSwap(false, CarryRules.SlotKind.OUTSIDE_HANDS, false, false), "ordinary items");
    }
}
