package com.mahghuuuls.somecheesecontrol.config;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/** Configuration entries that were invalid and how they were handled, collected while reading. */
public final class ConfigProblems {

    private final List<String> problems = new ArrayList<>();

    public void add(String problem) {
        problems.add(problem);
    }

    public List<String> all() {
        return Collections.unmodifiableList(problems);
    }

    public boolean isEmpty() {
        return problems.isEmpty();
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
