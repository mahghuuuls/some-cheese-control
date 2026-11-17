package com.mahghuuuls.somecheesecontrol.diagnostic;

import com.mahghuuuls.somecheesecontrol.integration.IntegrationGate;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * The status command's text, built from plain values so it can be checked without a game: the
 * features that are on, the active configuration, the integrated mods, the shield-bash refusal, the
 * installed original mods and the configuration problems.
 */
final class StatusReport {

    private StatusReport() {
    }

    static List<String> lines(Map<String, Boolean> features, List<String> values, List<String> integrations, String shieldBash,
                              List<String> originalMods, List<String> errors, List<String> warnings) {
        List<String> lines = new ArrayList<>();
        lines.add("Some Cheese Control status");
        StringBuilder on = new StringBuilder("Features:");
        String separator = " ";
        for (Map.Entry<String, Boolean> feature : features.entrySet()) {
            on.append(separator).append(feature.getKey()).append(feature.getValue() ? " on" : " off");
            separator = ", ";
        }
        lines.add(on.toString());
        lines.add("Configuration:");
        for (String value : values) {
            lines.add("  " + value);
        }
        lines.add("Integrations:");
        for (String integration : integrations) {
            lines.add("  " + integration);
        }
        lines.add("Shield bash refusal: " + shieldBash);
        lines.add("Original mods: " + (originalMods.isEmpty() ? "none installed" : String.join(", ", originalMods)
                + " (Some Cheese Control replaces them; both keep running)"));
        if (errors.isEmpty() && warnings.isEmpty()) {
            lines.add("Configuration problems: none");
        } else {
            lines.add("Configuration problems:");
            for (String error : errors) {
                lines.add("  error: " + error);
            }
            for (String warning : warnings) {
                lines.add("  " + warning);
            }
        }
        return lines;
    }

    /** One integrated mod: its name, where it stands, and any notes. */
    static String integration(String name, String state, List<String> notes) {
        return name + ": " + state + (notes.isEmpty() ? "" : " (" + String.join("; ", notes) + ")");
    }

    /**
     * The shield-bash refusal: active when the Spartan Shields integration started with no notes;
     * otherwise not active, with the integration's notes (the switch is off, the Mixin is missing,
     * or the integration failed).
     */
    static String shieldBash(boolean spartanShieldsInstalled, boolean active, List<String> notes) {
        if (!spartanShieldsInstalled) {
            return "not active (Spartan Shields not installed)";
        }
        if (active) {
            return "active";
        }
        if (notes.isEmpty()) {
            return "not active";
        }
        List<String> reasons = new ArrayList<>();
        for (String note : notes) {
            reasons.add(note.startsWith(IntegrationGate.INACTIVE) ? note.substring(IntegrationGate.INACTIVE.length()) : note);
        }
        return "not active (" + String.join("; ", reasons) + ")";
    }
}
