package com.mahghuuuls.somecheesecontrol.core;

import com.mahghuuuls.somecheesecontrol.config.Settings;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.item.ItemStack;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.text.TextComponentString;
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.fluids.FluidUtil;

import javax.annotation.Nullable;

/**
 * The single path every player restriction takes after it refuses an action.
 *
 * <p>One call sends the throttled action-bar message, sends the client the true inventory and blocks
 * it may have predicted wrongly, and writes the diagnostic line. Features never send messages or
 * packets themselves, so the message rules and the resync stay the same everywhere.</p>
 */
public final class Refusals {

    private Refusals() {
    }

    /**
     * Sends the client the server's truth after the mod changed something the client shows, when no
     * refusal message belongs to it (a pickup into a hand, several drops at once), or when a refusal
     * already decided repeats (the later steps of one refused right click).
     */
    public static void correctClient(EntityPlayerMP player, Resync resync) {
        resync.sendTo(player);
    }

    /**
     * @param player  the refused player, on the logical server
     * @param rule    the rule that refused
     * @param resync  what the client may have predicted
     * @param item    the item the refused action used; empty when none
     * @param target  the block the action aimed at, or null
     */
    public static void refuse(EntityPlayerMP player, Rule rule, Resync resync, ItemStack item, @Nullable BlockPos target) {
        Settings settings = Settings.get();
        if (settings.messagesEnabled()
                && ServerState.get().messageThrottle().tryAcquire(player.getUniqueID(), System.currentTimeMillis(),
                        settings.messageIntervalMillis())) {
            player.sendStatusMessage(new TextComponentString(settings.message(rule.message())), true);
        }
        resync.sendTo(player);
        if (DiagnosticLog.enabled()) {
            FluidStack fluid = item.isEmpty() ? null : FluidUtil.getFluidContained(item);
            DiagnosticLog.decision("refuse",
                    "rule", rule.key(),
                    "player", player.getName(),
                    "dim", player.dimension,
                    "item", item.isEmpty() ? "none" : item.getItem().getRegistryName(),
                    "fluid", fluid != null ? fluid.getFluid().getName() : "none",
                    "target", target != null ? target : "none");
        }
    }
}
