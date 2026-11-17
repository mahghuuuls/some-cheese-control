package com.mahghuuuls.somecheesecontrol.integration.wizardry;

import com.mahghuuuls.somecheesecontrol.config.Settings;
import com.mahghuuuls.somecheesecontrol.core.CastingItems;
import com.mahghuuuls.somecheesecontrol.integration.IntegrationGate;
import electroblob.wizardry.item.ISpellCastingItem;
import net.minecraft.item.ItemStack;
import net.minecraftforge.common.MinecraftForge;

import java.util.ArrayList;
import java.util.List;

/**
 * Electroblob's Wizardry: the climbing Wizardry refusal. Loaded by {@link IntegrationGate} only when
 * Wizardry is present, on both sides.
 *
 * <p>It always tells {@link CastingItems} which items cast spells, because the early client refusal
 * applies the rules of the server it joins, whatever this side's own file says. The refusal handlers
 * act on the logical server, and are registered only when this side's switch is on.</p>
 *
 * <p>One failure state for the whole integration in this game instance: after an error, here or in
 * the handlers, it is reported to {@link IntegrationGate} once, no item counts as a casting item and
 * the handlers stop acting; every other feature keeps working.</p>
 */
public final class WizardryIntegration implements IntegrationGate.Integration {

    static final String MOD_ID = "ebwizardry";

    private static volatile boolean failed;

    private final List<String> problems = new ArrayList<>();

    @Override
    public void start() {
        CastingItems.recognize(WizardryIntegration::isCastingItem);
        if (Settings.get().climbingWizardry()) {
            MinecraftForge.EVENT_BUS.register(new CastingRefusal());
        } else {
            problems.add("inactive: climbing Wizardry refusal is off");
        }
    }

    @Override
    public List<String> problems() {
        return problems;
    }

    /** True after an error disabled the integration. */
    static boolean failed() {
        return failed;
    }

    /** Disables the integration after an error and reports it once. */
    static void fail(Throwable error) {
        if (!failed) {
            failed = true;
            IntegrationGate.fail(MOD_ID, error);
        }
    }

    private static boolean isCastingItem(ItemStack stack) {
        if (failed) {
            return false;
        }
        try {
            return stack.getItem() instanceof ISpellCastingItem;
        } catch (RuntimeException | LinkageError e) {
            fail(e);
            return false;
        }
    }
}
