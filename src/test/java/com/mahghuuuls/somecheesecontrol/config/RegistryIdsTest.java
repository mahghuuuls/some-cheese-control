package com.mahghuuuls.somecheesecontrol.config;

import net.minecraft.util.ResourceLocation;
import net.minecraftforge.registries.IForgeRegistry;
import net.minecraftforge.registries.IForgeRegistryEntry;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Proxy;
import java.util.Arrays;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class RegistryIdsTest {

    private static final Entry LADDER = new Entry();
    private static final Entry DEFAULT = new Entry();

    @Test
    void anUnknownIdIsSkippedEvenWhenTheRegistryHasADefaultEntry() {
        ConfigProblems problems = new ConfigProblems();
        Set<Entry> resolved = RegistryIds.resolve(registryWith("minecraft:ladder"),
                Arrays.asList("minecraft:ladder", "minecraft:no_such_block", "Not An Id"), "climbing.extraClimbableBlocks", "block",
                problems);
        assertEquals(1, resolved.size());
        assertTrue(resolved.contains(LADDER));
        assertEquals(2, problems.warnings().size(), "one warning for the unknown id, one for the malformed one");
        assertTrue(problems.warnings().get(0).contains("no block \"minecraft:no_such_block\" is registered"));
        assertTrue(problems.warnings().get(1).contains("\"Not An Id\" is not a block id"),
                "a malformed id is rejected by the format check, not looked up");
    }

    @SuppressWarnings("unchecked")
    private static IForgeRegistry<Entry> registryWith(String knownId) {
        ResourceLocation known = new ResourceLocation(knownId);
        return (IForgeRegistry<Entry>) Proxy.newProxyInstance(RegistryIdsTest.class.getClassLoader(),
                new Class<?>[]{IForgeRegistry.class}, (proxy, method, args) -> {
                    switch (method.getName()) {
                        case "containsKey":
                            return known.equals(args[0]);
                        case "getValue":
                            return known.equals(args[0]) ? LADDER : DEFAULT;
                        default:
                            throw new UnsupportedOperationException(method.getName());
                    }
                });
    }

    private static final class Entry implements IForgeRegistryEntry<Entry> {
        @Override
        public Entry setRegistryName(ResourceLocation name) {
            return this;
        }

        @Override
        public ResourceLocation getRegistryName() {
            return null;
        }

        @Override
        public Class<Entry> getRegistryType() {
            return Entry.class;
        }
    }
}
