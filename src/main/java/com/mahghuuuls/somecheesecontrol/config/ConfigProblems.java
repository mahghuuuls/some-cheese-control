package com.mahghuuuls.somecheesecontrol.config;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Configuration entries that were invalid and how they were handled, collected while reading.
 *
 * <p>A warning is a skipped or adjusted entry. An error is a setting that cannot work as written and
 * was replaced, such as an allowed-hand value that allows no hand.</p>
 */
public final class ConfigProblems {

    private final List<String> warnings = new ArrayList<>();
    private final List<String> errors = new ArrayList<>();

    public void add(String warning) {
        warnings.add(warning);
    }

    public void addError(String error) {
        errors.add(error);
    }

    public List<String> warnings() {
        return Collections.unmodifiableList(warnings);
    }

    public List<String> errors() {
        return Collections.unmodifiableList(errors);
    }

    /** Errors first, then warnings. */
    public List<String> all() {
        List<String> all = new ArrayList<>(errors);
        all.addAll(warnings);
        return Collections.unmodifiableList(all);
    }

    public boolean isEmpty() {
        return warnings.isEmpty() && errors.isEmpty();
    }

    /**
     * Returns {@code value} inside {@code [min, max]}; a value outside is set to the nearest bound and
     * reported, so a wrong entry is never silently repaired.
     */
    public double clamp(String name, double value, double min, double max) {
        if (value < min) {
            add(name + " = " + value + " is below " + min + "; using " + min);
            return min;
        }
        if (value > max) {
            add(name + " = " + value + " is above " + max + "; using " + max);
            return max;
        }
        return value;
    }
}
