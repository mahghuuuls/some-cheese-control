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
     * Refuses one step of a right click. The first refused step of a click is the decision: it sends
     * the message and writes the line. A later refused step of the same click only corrects the
     * client. Either way the click is recorded, so {@link ClickGuard} refuses its remaining steps.
     */
    public static void refuseClickStep(EntityPlayerMP player, Rule rule, Resync resync, ItemStack item, @Nullable BlockPos target,
                                       Object... details) {
        if (ClickRecord.get().mark(player.getUniqueID(), ServerState.tick())) {
            correctClient(player, resync);
        } else {
            refuse(player, rule, resync, item, target, details);
        }
    }

    /**
     * Records the player's click as refused without refusing this step: for a drawn item that the
     * server lets start and stops at the player's next tick, where the refusal itself comes later.
     */
    public static void markClickRefused(EntityPlayerMP player) {
        ClickRecord.get().mark(player.getUniqueID(), ServerState.tick());
    }

    /**
     * Refuses an action that is not a step of a right click (a melee attack, a forced drop, the stop
     * of a use in progress).
     *
     * @param player  the refused player, on the logical server
     * @param rule    the rule that refused
     * @param resync  what the client may have predicted
     * @param item    the item the refused action used; empty when none
     * @param target  the block the action aimed at, or null
     * @param details more key and value pairs for the diagnostic line (the bed of a refused bed use)
     */
    public static void refuse(EntityPlayerMP player, Rule rule, Resync resync, ItemStack item, @Nullable BlockPos target,
                              Object... details) {
        Settings settings = Settings.get();
        if (settings.messagesEnabled()
                && ServerState.get().messageThrottle().tryAcquire(player.getUniqueID(), System.currentTimeMillis(),
                        settings.messageIntervalMillis())) {
            player.sendStatusMessage(new TextComponentString(settings.message(rule.message())), true);
        }
        resync.sendTo(player);
        if (DiagnosticLog.enabled()) {
            FluidStack fluid = item.isEmpty() ? null : FluidUtil.getFluidContained(item);
            Object[] line = {
                    "rule", rule.key(),
                    "player", player.getName(),
                    "dim", player.dimension,
                    "item", item.isEmpty() ? "none" : item.getItem().getRegistryName(),
                    "fluid", fluid != null ? fluid.getFluid().getName() : "none",
                    "target", target != null ? target : "none"};
            Object[] all = new Object[line.length + details.length];
            System.arraycopy(line, 0, all, 0, line.length);
            System.arraycopy(details, 0, all, line.length, details.length);
            DiagnosticLog.decision("refuse", all);
        }
    }
}
