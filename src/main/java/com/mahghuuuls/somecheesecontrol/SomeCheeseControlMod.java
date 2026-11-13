package com.mahghuuuls.somecheesecontrol;

import com.mahghuuuls.somecheesecontrol.config.Settings;
import com.mahghuuuls.somecheesecontrol.core.ServerState;
import com.mahghuuuls.somecheesecontrol.feature.liquid.LiquidPlacementFeature;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.common.config.Configuration;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.event.FMLInitializationEvent;
import net.minecraftforge.fml.common.event.FMLPreInitializationEvent;
import net.minecraftforge.fml.common.event.FMLServerStartingEvent;
import net.minecraftforge.fml.common.event.FMLServerStoppedEvent;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * Entry point of Some Cheese Control. Wires configuration and the enabled features; owns no rule.
 *
 * <p>{@code acceptableRemoteVersions = "*"} lets clients without the mod join a server that has it:
 * every rule is decided on the server.</p>
 */
@Mod(modid = Tags.MOD_ID, name = Tags.MOD_NAME, version = Tags.VERSION, acceptableRemoteVersions = "*")
public class SomeCheeseControlMod {

    public static final Logger LOGGER = LogManager.getLogger(Tags.MOD_NAME);

    @Mod.EventHandler
    public void preInit(FMLPreInitializationEvent event) {
        Settings settings = Settings.load(new Configuration(event.getSuggestedConfigurationFile()));
        for (String problem : settings.problems().all()) {
            LOGGER.warn("Configuration: {}", problem);
        }
    }

    @Mod.EventHandler
    public void init(FMLInitializationEvent event) {
        Settings settings = Settings.get();
        if (settings.unsupportedLiquidPlacement()) {
            MinecraftForge.EVENT_BUS.register(new LiquidPlacementFeature());
        }
    }

    @Mod.EventHandler
    public void serverStarting(FMLServerStartingEvent event) {
        ServerState.reset();
    }

    @Mod.EventHandler
    public void serverStopped(FMLServerStoppedEvent event) {
        ServerState.reset();
    }
}
