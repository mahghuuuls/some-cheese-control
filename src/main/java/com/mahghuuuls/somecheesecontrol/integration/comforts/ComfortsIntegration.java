package com.mahghuuuls.somecheesecontrol.integration.comforts;

import com.mahghuuuls.somecheesecontrol.config.Settings;
import com.mahghuuuls.somecheesecontrol.integration.IntegrationGate;
import net.minecraftforge.common.MinecraftForge;

import java.util.ArrayList;
import java.util.List;

/**
 * Comforts: the guard against a sleeping bag that Comforts uses by itself right after it is placed.
 * Loaded by {@link IntegrationGate} only when Comforts is present. A right click on a placed bag or
 * hammock needs nothing here: they are beds, and bed-explosion prevention refuses the click.
 */
public final class ComfortsIntegration implements IntegrationGate.Integration {

    private final List<String> problems = new ArrayList<>();

    @Override
    public void start() {
        if (Settings.get().bedExplosion()) {
            MinecraftForge.EVENT_BUS.register(new SleepingBagGuard());
        } else {
            problems.add("inactive: bed-explosion prevention is off");
        }
    }

    @Override
    public List<String> problems() {
        return problems;
    }
}
