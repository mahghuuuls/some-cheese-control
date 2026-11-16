package com.mahghuuuls.somecheesecontrol.integration.originalmods;

import net.minecraftforge.fml.common.Loader;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.function.Predicate;

/**
 * The three mods this mod replaces: Water Placement Nerf, Hold that Bucket! and No Attacking On
 * Ladders. When one is installed, one warning names it at startup; both keep running. Only their mod
 * ids are used; none of their classes is loaded.
 */
public final class OriginalMods {

    private static final Logger LOGGER = LogManager.getLogger("SomeCheeseControl/Integrations");

    /** Each replaced mod. */
    static final List<Mod> MODS = Collections.unmodifiableList(Arrays.asList(
            new Mod("waterplacementnerf", "Water Placement Nerf"),
            new Mod("hold-that-bucket", "Hold that Bucket!"),
            new Mod("noattackingonladders", "No Attacking On Ladders")));

    private OriginalMods() {
    }

    /** Writes one warning per installed replaced mod. Call once, at initialization. */
    public static void warnInstalled() {
        for (String name : installed()) {
            LOGGER.warn(warning(name));
        }
    }

    /** The display names of the replaced mods that are installed. */
    public static List<String> installed() {
        return installed(Loader::isModLoaded);
    }

    static List<String> installed(Predicate<String> modLoaded) {
        List<String> names = new ArrayList<>();
        for (Mod mod : MODS) {
            if (modLoaded.test(mod.id)) {
                names.add(mod.name);
            }
        }
        return names;
    }

    static String warning(String name) {
        return name + " is installed. Some Cheese Control replaces it; both mods keep running.";
    }

    /** A replaced mod: its mod id and display name. */
    static final class Mod {
        final String id;
        final String name;

        Mod(String id, String name) {
            this.id = id;
            this.name = name;
        }
    }
}
