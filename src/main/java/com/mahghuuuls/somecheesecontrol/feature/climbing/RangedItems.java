package com.mahghuuuls.somecheesecontrol.feature.climbing;

import net.minecraft.item.EnumAction;
import net.minecraft.item.Item;
import net.minecraft.item.ItemBow;
import net.minecraft.item.ItemEgg;
import net.minecraft.item.ItemEnderPearl;
import net.minecraft.item.ItemExpBottle;
import net.minecraft.item.ItemFishingRod;
import net.minecraft.item.ItemLingeringPotion;
import net.minecraft.item.ItemSnowball;
import net.minecraft.item.ItemSplashPotion;
import net.minecraft.item.ItemStack;
import net.minecraft.util.ResourceLocation;

/**
 * The one rule for which items fire or throw something.
 *
 * <p>Vanilla types (and their subclasses, so modded bows count): bows, snowballs, eggs, ender
 * pearls, splash and lingering potions, experience bottles, fishing rods. Spartan Weaponry items,
 * including those its add-ons register under their own names, are recognized without its classes on
 * the compile path: by the {@code spartanweaponry} namespace or by class family. Among them, an item
 * with a use duration that is not a block (crossbows, throwing weapons, throwable daggers) is
 * ranged, and so is dynamite, which throws at once. Spartan longbows are vanilla bows.</p>
 */
final class RangedItems {

    static final String SPARTAN_NAMESPACE = "spartanweaponry";
    static final String SPARTAN_PACKAGE = "com.oblivioussp.spartanweaponry.";
    static final String SPARTAN_DYNAMITE_CLASS = SPARTAN_PACKAGE + "item.ItemDynamite";

    private RangedItems() {
    }

    /** Which Spartan Weaponry family an item belongs to, if any. */
    enum SpartanKind {
        NOT_SPARTAN,
        DYNAMITE,
        OTHER
    }

    static boolean isRanged(ItemStack stack) {
        if (stack.isEmpty()) {
            return false;
        }
        Item item = stack.getItem();
        return decide(isVanillaRanged(item), spartanKind(item), stack.getMaxItemUseDuration(),
                stack.getItemUseAction() == EnumAction.BLOCK);
    }

    /**
     * True for an item that acts at the right click itself, with nothing to draw or charge first: any
     * item without a use duration, and splash and lingering potions, which inherit the drinking duration
     * of potions but are thrown at the click. A ranged item for which this is true is refused at the
     * click; callers also ask {@link #isRanged}.
     */
    static boolean actsAtOnce(ItemStack stack) {
        return throwsAtClick(stack.getItem().getClass()) || stack.getMaxItemUseDuration() <= 0;
    }

    /** True for the potion types thrown at the click although they keep the drinking duration. */
    static boolean throwsAtClick(Class<?> itemType) {
        return ItemSplashPotion.class.isAssignableFrom(itemType) || ItemLingeringPotion.class.isAssignableFrom(itemType);
    }

    /** The decision on plain facts about an item. */
    static boolean decide(boolean vanillaRanged, SpartanKind spartan, int useDuration, boolean blocks) {
        if (vanillaRanged) {
            return true;
        }
        switch (spartan) {
            case DYNAMITE:
                return true;
            case OTHER:
                return useDuration > 0 && !blocks;
            default:
                return false;
        }
    }

    static SpartanKind spartanKind(Item item) {
        if (isOrExtends(item.getClass(), SPARTAN_DYNAMITE_CLASS)) {
            return SpartanKind.DYNAMITE;
        }
        ResourceLocation id = item.getRegistryName();
        if ((id != null && SPARTAN_NAMESPACE.equals(id.getNamespace())) || isInPackage(item.getClass(), SPARTAN_PACKAGE)) {
            return SpartanKind.OTHER;
        }
        return SpartanKind.NOT_SPARTAN;
    }

    private static boolean isVanillaRanged(Item item) {
        return item instanceof ItemBow || item instanceof ItemSnowball || item instanceof ItemEgg || item instanceof ItemEnderPearl
                || item instanceof ItemSplashPotion || item instanceof ItemLingeringPotion || item instanceof ItemExpBottle
                || item instanceof ItemFishingRod;
    }

    /** True when the class or one of its superclasses is in {@code packagePrefix}. */
    static boolean isInPackage(Class<?> type, String packagePrefix) {
        for (Class<?> c = type; c != null; c = c.getSuperclass()) {
            if (c.getName().startsWith(packagePrefix)) {
                return true;
            }
        }
        return false;
    }

    /** True when the class is, or extends, the class named {@code className}. */
    static boolean isOrExtends(Class<?> type, String className) {
        for (Class<?> c = type; c != null; c = c.getSuperclass()) {
            if (c.getName().equals(className)) {
                return true;
            }
        }
        return false;
    }
}
