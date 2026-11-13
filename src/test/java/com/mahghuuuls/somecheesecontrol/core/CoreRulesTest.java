package com.mahghuuuls.somecheesecontrol.core;

import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CoreRulesTest {

    @Test
    void survivalPlayerIsRestricted() {
        assertTrue(PlayerExemptions.applies(false, false, false, false, false));
    }

    @Test
    void creativePlayerIsExemptByDefault() {
        assertFalse(PlayerExemptions.applies(false, false, true, false, false));
    }

    @Test
    void creativeSettingRestrictsCreativePlayers() {
        assertTrue(PlayerExemptions.applies(false, false, true, true, false));
    }

    @Test
    void ruleThatIgnoresTheExemptionAppliesInCreative() {
        assertTrue(PlayerExemptions.applies(false, false, true, false, true));
    }

    @Test
    void fakePlayersAndSpectatorsAreNeverRestricted() {
        assertFalse(PlayerExemptions.applies(true, false, false, true, true));
        assertFalse(PlayerExemptions.applies(false, true, false, true, true));
    }

    @Test
    void throttleAllowsOneMessagePerInterval() {
        MessageThrottle throttle = new MessageThrottle();
        UUID player = UUID.randomUUID();
        int sent = 0;
        for (long t = 0; t < 1000; t += 200) {
            if (throttle.tryAcquire(player, t, 2000)) {
                sent++;
            }
        }
        assertEquals(1, sent);
        assertTrue(throttle.tryAcquire(player, 2000, 2000));
    }

    @Test
    void throttleIsPerPlayer() {
        MessageThrottle throttle = new MessageThrottle();
        assertTrue(throttle.tryAcquire(UUID.randomUUID(), 0, 2000));
        assertTrue(throttle.tryAcquire(UUID.randomUUID(), 0, 2000));
    }

    @Test
    void zeroIntervalNeverThrottles() {
        MessageThrottle throttle = new MessageThrottle();
        UUID player = UUID.randomUUID();
        assertTrue(throttle.tryAcquire(player, 0, 0));
        assertTrue(throttle.tryAcquire(player, 0, 0));
    }

    @Test
    void diagnosticLineKeepsATrailingKeyWithoutValue() {
        assertEquals("[SCC] refuse rule=?", DiagnosticLog.format("refuse", "rule"));
    }

    @Test
    void diagnosticLineHasEventThenKeyValuePairs() {
        assertEquals("[SCC] refuse rule=unsupportedLiquid player=Steve",
                DiagnosticLog.format("refuse", "rule", "unsupportedLiquid", "player", "Steve"));
    }

    @Test
    void everyRuleHasAMessageWithKeyAndText() {
        for (Rule rule : Rule.values()) {
            assertFalse(rule.key().isEmpty());
            assertFalse(rule.message().defaultText().isEmpty());
        }
    }
}
