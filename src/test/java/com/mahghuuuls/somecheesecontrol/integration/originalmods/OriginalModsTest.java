package com.mahghuuuls.somecheesecontrol.integration.originalmods;

import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.Collections;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** The replaced mods are found by their mod ids, and the warning names the mod. */
class OriginalModsTest {

    @Test
    void eachInstalledReplacedModIsNamed() {
        assertEquals(Collections.singletonList("Hold that Bucket!"), OriginalMods.installed(id -> id.equals("hold-that-bucket")));
        assertEquals(Arrays.asList("Water Placement Nerf", "Hold that Bucket!", "No Attacking On Ladders"),
                OriginalMods.installed(id -> id.equals("waterplacementnerf") || id.equals("hold-that-bucket") || id.equals("noattackingonladders")));
        assertTrue(OriginalMods.installed(id -> false).isEmpty());
    }

    @Test
    void theWarningNamesTheModAndSaysBothRun() {
        String warning = OriginalMods.warning("Hold that Bucket!");
        assertTrue(warning.startsWith("Hold that Bucket! is installed."), warning);
        assertTrue(warning.contains("replaces it"), warning);
        assertTrue(warning.contains("both mods keep running"), warning);
    }
}
