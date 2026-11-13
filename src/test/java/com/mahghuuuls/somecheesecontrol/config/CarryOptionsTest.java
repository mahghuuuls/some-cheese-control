package com.mahghuuuls.somecheesecontrol.config;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CarryOptionsTest {

    @Test
    void eachValidHandValueIsAccepted() {
        ConfigProblems problems = new ConfigProblems();
        assertEquals(AllowedHand.MAIN, Options.ALLOWED_HAND.validate("main", problems));
        assertEquals(AllowedHand.OFF, Options.ALLOWED_HAND.validate(" Off ", problems));
        assertEquals(AllowedHand.EITHER, Options.ALLOWED_HAND.validate("either", problems));
        assertTrue(problems.isEmpty());
    }

    @Test
    void aValueThatAllowsNoHandIsAnErrorAndUsesEither() {
        ConfigProblems problems = new ConfigProblems();
        assertEquals(AllowedHand.EITHER, Options.ALLOWED_HAND.validate("none", problems));
        assertEquals(1, problems.errors().size());
        assertTrue(problems.warnings().isEmpty());
    }

    @Test
    void handsAllowWhatTheirNamesSay() {
        assertTrue(AllowedHand.MAIN.allowsMainHand());
        assertFalse(AllowedHand.MAIN.allowsOffhand());
        assertFalse(AllowedHand.OFF.allowsMainHand());
        assertTrue(AllowedHand.OFF.allowsOffhand());
        assertTrue(AllowedHand.EITHER.allowsMainHand());
        assertTrue(AllowedHand.EITHER.allowsOffhand());
    }

    @Test
    void registryIdsMustBeLowercaseModidColonName() {
        assertTrue(RegistryIds.isWellFormed("minecraft:water_bucket"));
        assertTrue(RegistryIds.isWellFormed("some_mod:item.name/variant"));
        assertFalse(RegistryIds.isWellFormed("water_bucket"));
        assertFalse(RegistryIds.isWellFormed("Minecraft:Water_Bucket"));
        assertFalse(RegistryIds.isWellFormed("minecraft:"));
        assertFalse(RegistryIds.isWellFormed(""));
    }
}
