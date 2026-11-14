package com.mahghuuuls.somecheesecontrol.core;

import com.mahghuuuls.somecheesecontrol.config.Settings;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraftforge.common.util.FakePlayer;

import javax.annotation.Nullable;

/**
 * The only owner of "does this player restriction apply to this player".
 *
 * <p>Fake players (machines acting as players) and spectators are never restricted. Creative players
 * are exempt unless the Creative setting is on or the rule ignores the exemption. Operator status
 * gives no exemption.</p>
 *
 * <p>The Creative setting is read from the snapshot of the player's side. On a client that has no
 * rules from its server yet, no restriction applies.</p>
 */
public final class PlayerExemptions {

    private PlayerExemptions() {
    }

    public static boolean applies(EntityPlayer player, Rule rule) {
        return appliesUnder(Settings.forSide(player.world.isRemote), player instanceof FakePlayer, player.isSpectator(),
                player.capabilities.isCreativeMode, rule);
    }

    /** The decision under one side's settings; without settings (a client before its server's rules arrive) nothing applies. */
    static boolean appliesUnder(@Nullable Settings settings, boolean fakePlayer, boolean spectator, boolean creative, Rule rule) {
        return settings != null
                && applies(fakePlayer, spectator, creative, settings.restrictionsApplyInCreative(), rule.appliesInCreative());
    }

    static boolean applies(boolean fakePlayer, boolean spectator, boolean creative,
                           boolean restrictionsApplyInCreative, boolean ruleAppliesInCreative) {
        if (fakePlayer || spectator) {
            return false;
        }
        return !creative || restrictionsApplyInCreative || ruleAppliesInCreative;
    }
}
