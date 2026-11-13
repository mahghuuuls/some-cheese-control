package com.mahghuuuls.somecheesecontrol.config;

import net.minecraft.item.Item;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.fml.common.registry.ForgeRegistries;

import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.regex.Pattern;

/**
 * Turns configured item ids into items once the item registry is complete.
 *
 * <p>An entry that is malformed or names no registered item is skipped and reported, so the rest of
 * the list still works.</p>
 */
public final class ItemIds {

    private static final Pattern WELL_FORMED = Pattern.compile("[a-z0-9_.-]+:[a-z0-9_./-]+");

    private ItemIds() {
    }

    /** Resolves {@code ids}; call during or after initialization, when every item is registered. */
    public static Set<Item> resolve(List<String> ids, String path, ConfigProblems problems) {
        Set<Item> items = new LinkedHashSet<>();
        for (String raw : ids) {
            String id = raw.trim();
            if (!isWellFormed(id)) {
                problems.add(path + ": \"" + raw + "\" is not an item id (lowercase modid:name); skipped");
                continue;
            }
            Item item = ForgeRegistries.ITEMS.getValue(new ResourceLocation(id));
            if (item == null) {
                problems.add(path + ": no item \"" + id + "\" is registered; skipped");
                continue;
            }
            items.add(item);
        }
        return Collections.unmodifiableSet(items);
    }

    /** True for a lowercase {@code modid:name} id. */
    static boolean isWellFormed(String id) {
        return WELL_FORMED.matcher(id).matches();
    }
}
