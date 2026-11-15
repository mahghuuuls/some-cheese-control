package com.mahghuuuls.somecheesecontrol.client;

import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Which swing paths the Mixins changed. The Mixin plugin writes here while the game's classes are
 * being changed, so this class must use no game type: loading one then would load it in the middle
 * of another class's change, which fails when other mods have Mixins into it.
 */
public final class SwingPaths {

    /** The vanilla click code ({@code Minecraft}). */
    public static final String VANILLA_CLICK = "vanilla click";
    /** The vanilla attack bar reset ({@code PlayerControllerMP}). */
    public static final String VANILLA_ATTACK_BAR = "vanilla attack bar";
    /** RLCombat's client click handler. */
    public static final String RLCOMBAT_CLICK = "RLCombat click";
    /** RLCombat's hand animation. */
    public static final String RLCOMBAT_ANIMATION = "RLCombat animation";

    private static final Set<String> applied = ConcurrentHashMap.newKeySet();

    private SwingPaths() {
    }

    /** Called by the Mixin plugin when it changed a path. */
    public static void markApplied(String path) {
        applied.add(path);
    }

    public static boolean isApplied(String path) {
        return applied.contains(path);
    }

    /** The paths among {@code expected} that were not changed, in their order. */
    static Set<String> inactive(String... expected) {
        Set<String> missing = new LinkedHashSet<>();
        for (String path : expected) {
            if (!isApplied(path)) {
                missing.add(path);
            }
        }
        return Collections.unmodifiableSet(missing);
    }
}
