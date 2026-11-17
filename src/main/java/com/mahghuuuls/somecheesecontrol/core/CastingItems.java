package com.mahghuuuls.somecheesecontrol.core;

import net.minecraft.item.ItemStack;

import java.util.function.Predicate;

/**
 * Which items are spell-casting items, as an integration recognizes them, for code that may not
 * name the integrated mod's classes (the early client refusal). Without such an integration no
 * item is one.
 */
public final class CastingItems {

    private static volatile Predicate<ItemStack> recognizer = stack -> false;

    private CastingItems() {
    }

    /** Sets the recognizer. Called by the integration at its start, on both sides. */
    public static void recognize(Predicate<ItemStack> isCastingItem) {
        recognizer = isCastingItem;
    }

    /** True when the stack is a spell-casting item. */
    public static boolean is(ItemStack stack) {
        return !stack.isEmpty() && recognizer.test(stack);
    }
}
