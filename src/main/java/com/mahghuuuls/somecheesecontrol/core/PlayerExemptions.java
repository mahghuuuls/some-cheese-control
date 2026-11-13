package com.mahghuuuls.somecheesecontrol.core;

import com.mahghuuuls.somecheesecontrol.config.Settings;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraftforge.common.util.FakePlayer;

/**
 * The only owner of "does this player restriction apply to this player".
 *
 * <p>Fake players (machines acting as players) and spectators are never restricted. Creative players
 * are exempt unless the Creative setting is on or the rule ignores the exemption. Operator status
 * gives no exemption.</p>
 */
public final class PlayerExemptions {

    private PlayerExemptions() {
    }

    public static boolean applies(EntityPlayer player, Rule rule) {
        return applies(player instanceof FakePlayer, player.isSpectator(), player.capabilities.isCreativeMode,
                Settings.get().restrictionsApplyInCreative(), rule.appliesInCreative());
    }

    static boolean applies(boolean fakePlayer, boolean spectator, boolean creative,
                           boolean restrictionsApplyInCreative, boolean ruleAppliesInCreative) {
        if (fakePlayer || spectator) {
            return false;
        }
        return !creative || restrictionsApplyInCreative || ruleAppliesInCreative;
    }
}
