package com.mahghuuuls.somecheesecontrol.config;

import net.minecraft.util.ResourceLocation;
import net.minecraftforge.registries.IForgeRegistry;
import net.minecraftforge.registries.IForgeRegistryEntry;

import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.regex.Pattern;

/**
 * Turns configured registry ids (items, blocks) into registry entries once registration is complete.
 *
 * <p>An entry that is malformed or names nothing registered is skipped and reported, so the rest of
 * the list still works.</p>
 */
final class RegistryIds {

    private static final Pattern WELL_FORMED = Pattern.compile("[a-z0-9_.-]+:[a-z0-9_./-]+");

    private RegistryIds() {
    }

    /**
     * Resolves {@code ids} against {@code registry}; call during or after initialization.
     *
     * @param kind "item" or "block", for problem reports
     */
    static <T extends IForgeRegistryEntry<T>> Set<T> resolve(IForgeRegistry<T> registry, List<String> ids, String path,
                                                            String kind, ConfigProblems problems) {
        Set<T> entries = new LinkedHashSet<>();
        for (String raw : ids) {
            String id = raw.trim();
            if (!isWellFormed(id)) {
                problems.add(path + ": \"" + raw + "\" is not a " + kind + " id (lowercase modid:name); skipped");
                continue;
            }
            ResourceLocation key = new ResourceLocation(id);
            T entry = registry.containsKey(key) ? registry.getValue(key) : null;
            if (entry == null) {
                problems.add(path + ": no " + kind + " \"" + id + "\" is registered; skipped");
                continue;
            }
            entries.add(entry);
        }
        return Collections.unmodifiableSet(entries);
    }

    /** True for a lowercase {@code modid:name} id. */
    static boolean isWellFormed(String id) {
        return WELL_FORMED.matcher(id).matches();
    }
}
