package com.mahghuuuls.somecheesecontrol.diagnostic;

import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** The status and inspect text: every listed item appears, in readable form. */
class ReportsTest {

    @Test
    void theStatusListsEveryItem() {
        Map<String, Boolean> features = new LinkedHashMap<>();
        features.put("unsupportedLiquidPlacement", true);
        features.put("bedExplosion", false);
        List<String> lines = StatusReport.lines(features, Arrays.asList("general.diagnosticLogging = false"),
                Arrays.asList(StatusReport.integration("Comforts", "installed, integration active", Collections.<String>emptyList())),
                StatusReport.shieldBash(false), Arrays.asList("Hold that Bucket!"),
                Arrays.asList("handOnlyCarrying.allowedHand = none allows no hand; using either"),
                Arrays.asList("handOnlyCarrying.items: no item \"nosuchmod:thing\" is registered; skipped"));
        String text = String.join("\n", lines);
        assertTrue(text.contains("Features: unsupportedLiquidPlacement on, bedExplosion off"), text);
        assertTrue(text.contains("  general.diagnosticLogging = false"), text);
        assertTrue(text.contains("  Comforts: installed, integration active"), text);
        assertTrue(text.contains("Shield bash refusal: not active (Spartan Shields not installed)"), text);
        assertTrue(text.contains("Original mods: Hold that Bucket! (Some Cheese Control replaces them; both keep running)"), text);
        assertTrue(text.contains("  error: handOnlyCarrying.allowedHand"), text);
        assertTrue(text.contains("  handOnlyCarrying.items: no item"), text);
    }

    @Test
    void anEmptyStatusSaysSo() {
        List<String> lines = StatusReport.lines(new LinkedHashMap<String, Boolean>(), Collections.<String>emptyList(),
                Collections.<String>emptyList(), StatusReport.shieldBash(true), Collections.<String>emptyList(),
                Collections.<String>emptyList(), Collections.<String>emptyList());
        assertTrue(lines.contains("Original mods: none installed"), lines.toString());
        assertTrue(lines.contains("Configuration problems: none"), lines.toString());
        assertTrue(lines.contains("Shield bash refusal: not active (not part of this version)"), lines.toString());
    }

    @Test
    void anIntegrationNoteIsShown() {
        assertEquals("Comforts: installed, integration active (inactive: bed-explosion prevention is off)",
                StatusReport.integration("Comforts", "installed, integration active", Arrays.asList("inactive: bed-explosion prevention is off")));
    }

    @Test
    void inspectShowsThePlayersState() {
        List<String> lines = InspectReport.playerLines("Steve", true, 7, "10,64,-3 minecraft:ladder", true, 12.5F);
        assertEquals(Arrays.asList(
                "Some Cheese Control: Steve",
                "  climbing state: on",
                "  release delay left: 7 ticks (0.35 s)",
                "  climbing support: 10,64,-3 minecraft:ladder",
                "  airborne: yes",
                "  pending fall: 12.5 blocks"), lines);
    }

    @Test
    void inspectOfAPlayerOnTheGround() {
        List<String> lines = InspectReport.playerLines("Alex", false, 0, null, false, 0F);
        assertTrue(lines.contains("  climbing state: off"));
        assertTrue(lines.contains("  release delay left: none"));
        assertTrue(lines.contains("  climbing support: none"));
        assertTrue(lines.contains("  pending fall: none"));
    }

    @Test
    void theLookedAtMobLine() {
        assertEquals("  looked-at mob minecraft:zombie #42: boat reboarding block 60 ticks (3.00 s) left",
                InspectReport.mobLine(true, "minecraft:zombie #42", 60));
        assertEquals("  looked-at mob minecraft:zombie #42: boat reboarding block none", InspectReport.mobLine(true, "minecraft:zombie #42", 0));
        assertEquals("  looked-at mob: none in view", InspectReport.mobLine(true, null, 0));
        assertNull(InspectReport.mobLine(false, null, 0), "the server console looks at nothing");
    }
}
