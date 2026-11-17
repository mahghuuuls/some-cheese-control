package com.mahghuuuls.somecheesecontrol.integration.fluidlogged;

import com.mahghuuuls.somecheesecontrol.feature.liquid.LiquidPlacement;
import com.mahghuuuls.somecheesecontrol.integration.IntegrationGate;

import java.util.Collections;
import java.util.List;

/**
 * Fluidlogged API: the liquid destination of a vanilla bucket and of Forge's universal bucket. Loaded by {@link IntegrationGate}
 * only when Fluidlogged API is present, on both sides, because the early client refusal judges the
 * same destination. It registers {@link FluidloggedDestination} with the liquid placement rules.
 */
public final class FluidloggedIntegration implements IntegrationGate.Integration {

    static final String MOD_ID = "fluidlogged_api";

    @Override
    public void start() {
        try {
            LiquidPlacement.setDestinationRule(new FluidloggedDestination());
        } catch (ReflectiveOperationException e) {
            throw new IllegalStateException("this Fluidlogged API version lacks the bucket members this integration reads", e);
        }
    }

    @Override
    public List<String> problems() {
        return Collections.emptyList();
    }
}
