package com.mahghuuuls.somecheesecontrol;

import com.mahghuuuls.somecheesecontrol.client.EarlyClientRefusal;
import com.mahghuuuls.somecheesecontrol.client.ScreenPlacementCheck;
import com.mahghuuuls.somecheesecontrol.client.SwingHiding;
import com.mahghuuuls.somecheesecontrol.config.Settings;
import com.mahghuuuls.somecheesecontrol.core.ActualDamage;
import com.mahghuuuls.somecheesecontrol.core.ClickGuard;
import com.mahghuuuls.somecheesecontrol.core.ServerState;
import com.mahghuuuls.somecheesecontrol.feature.boatescape.BoatEscape;
import com.mahghuuuls.somecheesecontrol.feature.carry.CarryFeature;
import com.mahghuuuls.somecheesecontrol.feature.climbing.ClimbingFeature;
import com.mahghuuuls.somecheesecontrol.feature.cobweb.CobwebRelease;
import com.mahghuuuls.somecheesecontrol.feature.liquid.LiquidPlacementFeature;
import com.mahghuuuls.somecheesecontrol.feature.vehiclefall.VehicleFallTracker;
import com.mahghuuuls.somecheesecontrol.network.Network;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.common.config.Configuration;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.event.FMLInitializationEvent;
import net.minecraftforge.fml.common.event.FMLLoadCompleteEvent;
import net.minecraftforge.fml.common.event.FMLPreInitializationEvent;
import net.minecraftforge.fml.common.event.FMLServerStartingEvent;
import net.minecraftforge.fml.common.event.FMLServerStoppedEvent;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * Entry point of Some Cheese Control. Wires configuration and the enabled features; owns no rule.
 *
 * <p>The mod is required on both sides, in the same version: the server decides every rule, and
 * the client refuses its own player's actions early with the server's rules, so a refused action
 * shows only the message. Without {@code acceptableRemoteVersions}, Forge refuses a client without
 * the mod at join.</p>
 */
@Mod(modid = Tags.MOD_ID, name = Tags.MOD_NAME, version = Tags.VERSION)
public class SomeCheeseControlMod {

    public static final Logger LOGGER = LogManager.getLogger(Tags.MOD_NAME);

    @Mod.EventHandler
    public void preInit(FMLPreInitializationEvent event) {
        Settings.load(new Configuration(event.getSuggestedConfigurationFile()));
        Network.register(Tags.MOD_ID);
    }

    @Mod.EventHandler
    public void init(FMLInitializationEvent event) {
        Settings settings = Settings.get();
        settings.resolveRegistryEntries();
        for (String error : settings.problems().errors()) {
            LOGGER.error("Configuration: {}", error);
        }
        for (String warning : settings.problems().warnings()) {
            LOGGER.warn("Configuration: {}", warning);
        }
        // Registered first and always: any feature's refused click step makes the rest of the click refused.
        MinecraftForge.EVENT_BUS.register(new ClickGuard());
        if (LiquidPlacementFeature.isNeeded(settings)) {
            MinecraftForge.EVENT_BUS.register(new LiquidPlacementFeature());
        }
        if (settings.handOnlyCarrying()) {
            MinecraftForge.EVENT_BUS.register(new CarryFeature(settings));
        }
        if (ClimbingFeature.isNeeded(settings)) {
            MinecraftForge.EVENT_BUS.register(new ClimbingFeature(settings));
        }
        if (VehicleFallTracker.isNeeded(settings)) {
            MinecraftForge.EVENT_BUS.register(new VehicleFallTracker());
        }
        if (ActualDamage.isNeeded(settings)) {
            MinecraftForge.EVENT_BUS.register(new ActualDamage());
        }
        // Cobweb release and boat escape both listen last to the damage event, in this order: the webs
        // around the mob are released before boat escape moves it away from them.
        if (CobwebRelease.isNeeded(settings)) {
            MinecraftForge.EVENT_BUS.register(new CobwebRelease(settings));
        }
        if (BoatEscape.isNeeded(settings)) {
            MinecraftForge.EVENT_BUS.register(new BoatEscape(settings));
        }
        if (event.getSide().isClient()) {
            EarlyClientRefusal.register();
            ScreenPlacementCheck.register();
        }
    }

    @Mod.EventHandler
    public void loadComplete(FMLLoadCompleteEvent event) {
        if (event.getSide().isClient()) {
            SwingHiding.reportInactivePaths();
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
