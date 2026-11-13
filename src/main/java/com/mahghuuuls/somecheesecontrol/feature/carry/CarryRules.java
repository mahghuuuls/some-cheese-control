package com.mahghuuuls.somecheesecontrol.feature.carry;

import com.mahghuuuls.somecheesecontrol.config.AllowedHand;

/** Which inventory places count as an allowed hand. */
final class CarryRules {

    /** The parts of the player's own inventory. The item on the cursor belongs to none of them. */
    enum Section {
        MAIN, OFFHAND, ARMOR
    }

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
}
