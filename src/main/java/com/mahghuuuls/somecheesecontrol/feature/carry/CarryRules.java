package com.mahghuuuls.somecheesecontrol.feature.carry;

import com.mahghuuuls.somecheesecontrol.config.AllowedHand;

/**
 * The only owner of where a restricted carried item may be, and of what happens when it is
 * elsewhere: on the server it moves into an empty allowed offhand or is dropped; in a screen, the
 * client does not let the player put it outside an allowed hand. Both sides apply these rules, so
 * they are plain decisions over slot facts.
 */
public final class CarryRules {

    /** The parts of the player's own inventory. The item on the cursor belongs to none of them. */
    enum Section {
        MAIN, OFFHAND, ARMOR
    }

    /** What a slot in an open screen means for a restricted item. */
    public enum SlotKind {
        /** Not a slot of the player's own inventory: a chest, a crafting grid, outside the screen. */
        NOT_OWN,
        /** A slot of the player's own inventory that is an allowed hand. */
        ALLOWED_HAND,
        /** A slot of the player's own inventory outside an allowed hand. */
        OUTSIDE_HANDS
    }

    /** What the carry tick does with a restricted item. */
    enum Outcome {
        STAY, MOVE_TO_OFFHAND, DROP
    }

    /** Main inventory slots, hotbar included, in the vanilla player inventory's index order. */
    static final int MAIN_SLOTS = 36;
    /** Armor slots follow the main inventory; the offhand follows the armor. */
    static final int ARMOR_SLOTS = 4;

    private CarryRules() {
    }

    /** How many items a pickup puts into one hand: never more than one full stack. */
    static int pickupCount(int groundCount, int maxStackSize, int inventoryStackLimit) {
        return Math.min(groundCount, Math.min(maxStackSize, inventoryStackLimit));
    }

    /**
     * True when a restricted item may stay in slot {@code index} of {@code section}. The main hand is
     * only the selected hotbar slot; unselected hotbar slots never count.
     */
    static boolean isAllowedSlot(Section section, int index, int selectedHotbarSlot, AllowedHand hand) {
        switch (section) {
            case MAIN:
                return hand.allowsMainHand() && index == selectedHotbarSlot;
            case OFFHAND:
                return hand.allowsOffhand();
            default:
                return false;
        }
    }

    /**
     * What happens to a restricted item in a slot: it stays in an allowed hand; elsewhere it moves
     * into the offhand when the offhand is an allowed hand and empty, and is dropped otherwise.
     */
    static Outcome outcome(Section section, int index, int selectedHotbarSlot, AllowedHand hand, boolean offhandEmpty) {
        if (isAllowedSlot(section, index, selectedHotbarSlot, hand)) {
            return Outcome.STAY;
        }
        return hand.allowsOffhand() && offhandEmpty ? Outcome.MOVE_TO_OFFHAND : Outcome.DROP;
    }

    /**
     * The kind of a slot of the player's own inventory, by its index in that inventory (main
     * inventory with the hotbar first, then armor, then the offhand).
     */
    public static SlotKind ownSlotKind(int inventoryIndex, int selectedHotbarSlot, AllowedHand hand) {
        Section section = inventoryIndex < MAIN_SLOTS ? Section.MAIN
                : inventoryIndex < MAIN_SLOTS + ARMOR_SLOTS ? Section.ARMOR : Section.OFFHAND;
        return isAllowedSlot(section, inventoryIndex, selectedHotbarSlot, hand) ? SlotKind.ALLOWED_HAND : SlotKind.OUTSIDE_HANDS;
    }

    /** True when putting the item into a slot of this kind must be refused: a click, or the end of a drag. */
    public static boolean refusesPlacing(boolean restrictedItem, SlotKind target) {
        return restrictedItem && target == SlotKind.OUTSIDE_HANDS;
    }

    /**
     * True when a shift-click on a restricted item must be refused. From a slot that is not the
     * player's own, it moves into the player's inventory. From the player's own slot it is let
     * through only where the screen's shift-click takes it out of the player's inventory; elsewhere
     * it can move between the player's own slots.
     */
    public static boolean refusesQuickMove(boolean restrictedItem, boolean fromOwnSlot, boolean screenTakesOwnItemsOut) {
        return restrictedItem && !(fromOwnSlot && screenTakesOwnItemsOut);
    }

    /**
     * True when a number key must be refused: it swaps the pointed slot with a hotbar slot. A
     * restricted item may go into the hotbar slot only when that slot is an allowed hand, and may
     * come out of the hotbar into the pointed slot only when that slot is not outside the hands.
     */
    public static boolean refusesHotbarSwap(boolean pointedRestricted, SlotKind pointed, boolean hotbarRestricted,
                                            boolean hotbarSlotIsAllowedHand) {
        boolean intoHotbar = pointedRestricted && !hotbarSlotIsAllowedHand;
        boolean outOfHotbar = hotbarRestricted && pointed == SlotKind.OUTSIDE_HANDS;
        return intoHotbar || outOfHotbar;
    }
}
