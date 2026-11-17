package com.mahghuuuls.somecheesecontrol.mixin;

import net.minecraftforge.fml.common.Loader;
import net.minecraftforge.fml.relauncher.FMLLaunchHandler;
import zone.rong.mixinbooter.ILateMixinLoader;

import java.util.Arrays;
import java.util.List;

/**
 * Queues the Mixins into other mods' classes, each only when its target mod is loaded (and, for the
 * client-only RLCombat Mixins, only on the physical client). MixinBooter
 * finds this class by its interface and loads it; without MixinBooter nothing loads it, so nothing
 * else in the mod may refer to it.
 */
public final class SccLateMixins implements ILateMixinLoader {

    private static final String RLCOMBAT_CONFIG = "mixins.somecheesecontrol.rlcombat.json";
    private static final String SPARTAN_SHIELDS_CONFIG = "mixins.somecheesecontrol.spartanshields.json";

    @Override
    public List<String> getMixinConfigs() {
        return Arrays.asList(RLCOMBAT_CONFIG, SPARTAN_SHIELDS_CONFIG);
    }

    @Override
    public boolean shouldMixinConfigQueue(String mixinConfig) {
        if (RLCOMBAT_CONFIG.equals(mixinConfig)) {
            return FMLLaunchHandler.side().isClient() && Loader.isModLoaded("bettercombatmod");
        }
        if (SPARTAN_SHIELDS_CONFIG.equals(mixinConfig)) {
            return Loader.isModLoaded("spartanshields");
        }
        return true;
    }
}
