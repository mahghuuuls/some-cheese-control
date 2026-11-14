package com.mahghuuuls.somecheesecontrol.feature.climbing;

import com.mahghuuuls.somecheesecontrol.feature.climbing.RangedItems.SpartanKind;
import net.minecraft.item.ItemBow;
import net.minecraft.item.ItemLingeringPotion;
import net.minecraft.item.ItemPotion;
import net.minecraft.item.ItemSplashPotion;
import org.junit.jupiter.api.Test;

import java.util.AbstractList;
import java.util.ArrayList;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class RangedItemsTest {

    private static final int NONE = 0;
    private static final int LONG = 72000;

    @Test
    void vanillaRangedTypesAreRangedWhateverTheirUse() {
        assertTrue(RangedItems.decide(true, SpartanKind.NOT_SPARTAN, LONG, false), "bow");
        assertTrue(RangedItems.decide(true, SpartanKind.NOT_SPARTAN, NONE, false), "snowball, pearl, potion, fishing rod");
    }

    @Test
    void spartanItemsWithAUseDurationAreRanged() {
        assertTrue(RangedItems.decide(false, SpartanKind.OTHER, LONG, false), "throwing weapon, throwable dagger, loaded crossbow");
        assertTrue(RangedItems.decide(false, SpartanKind.OTHER, 25, false), "crossbow while loading");
    }

    @Test
    void spartanDynamiteIsRangedWithoutAUseDuration() {
        assertTrue(RangedItems.decide(false, SpartanKind.DYNAMITE, NONE, false));
    }

    @Test
    void spartanMeleeAndBlockingItemsAreNotRanged() {
        assertFalse(RangedItems.decide(false, SpartanKind.OTHER, NONE, false), "sword, spear, quiver, arrows");
        assertFalse(RangedItems.decide(false, SpartanKind.OTHER, LONG, true), "parrying dagger blocks");
    }

    @Test
    void otherModsItemsWithAUseDurationAreNotRanged() {
        assertFalse(RangedItems.decide(false, SpartanKind.NOT_SPARTAN, LONG, false), "a modded food or shield");
    }

    @Test
    void splashAndLingeringPotionsAreThrownAtTheClick() {
        assertTrue(RangedItems.throwsAtClick(ItemSplashPotion.class));
        assertTrue(RangedItems.throwsAtClick(ItemLingeringPotion.class));
        assertFalse(RangedItems.throwsAtClick(ItemPotion.class), "a drinkable potion is used over time");
        assertFalse(RangedItems.throwsAtClick(ItemBow.class));
    }

    @Test
    void theSpartanNamesMatchSpartanWeaponry161() {
        assertEquals("spartanweaponry", RangedItems.SPARTAN_NAMESPACE);
        assertEquals("com.oblivioussp.spartanweaponry.item.ItemDynamite", RangedItems.SPARTAN_DYNAMITE_CLASS,
                "class name read from SpartanWeaponry-1.12.2-1.6.1.jar");
        assertEquals("com.oblivioussp.spartanweaponry.", RangedItems.SPARTAN_PACKAGE);
    }

    @Test
    void classFamilyIncludesSuperclasses() {
        assertTrue(RangedItems.isInPackage(ArrayList.class, "java.util."));
        assertTrue(RangedItems.isInPackage(Sub.class, "java.util."), "a subclass in another package counts by its superclass");
        assertFalse(RangedItems.isInPackage(String.class, "java.util."));
        assertTrue(RangedItems.isOrExtends(Sub.class, "java.util.AbstractList"));
        assertFalse(RangedItems.isOrExtends(String.class, "java.util.AbstractList"));
    }

    private static final class Sub extends AbstractList<Object> {
        @Override
        public Object get(int index) {
            throw new IndexOutOfBoundsException();
        }

        @Override
        public int size() {
            return 0;
        }
    }
}
