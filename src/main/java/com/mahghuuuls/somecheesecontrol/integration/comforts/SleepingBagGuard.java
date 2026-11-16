package com.mahghuuuls.somecheesecontrol.integration.comforts;

import c4.comforts.common.capability.CapabilitySleeping;
import com.mahghuuuls.somecheesecontrol.core.PlayerExemptions;
import com.mahghuuuls.somecheesecontrol.core.Refusals;
import com.mahghuuuls.somecheesecontrol.core.Resync;
import com.mahghuuuls.somecheesecontrol.core.Rule;
import com.mahghuuuls.somecheesecontrol.feature.bed.ExplosiveBedUse;
import com.mahghuuuls.somecheesecontrol.integration.IntegrationGate;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.item.ItemStack;
import net.minecraft.util.math.BlockPos;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;
import net.minecraftforge.fml.relauncher.Side;

/**
 * With Comforts' "Auto Use Sleeping Bags" on, placing a sleeping bag marks the player as about to
 * sleep in it (Comforts' sleeping capability, with the bag's head), and Comforts uses the bag at the
 * end of the player's server tick, with no click; where beds explode, the bag explodes. At the start
 * of that tick, which always runs before Comforts' end-of-tick handler, this guard clears the mark
 * when the bag's use would explode. The bag stays placed and nothing explodes; the player gets the
 * bed message.
 *
 * <p>An error here is reported to {@link IntegrationGate} once, and the guard stops acting; every
 * other feature keeps working.</p>
 */
public final class SleepingBagGuard {

    private boolean failed;

    @SubscribeEvent
    public void onPlayerTick(TickEvent.PlayerTickEvent event) {
        if (failed || event.phase != TickEvent.Phase.START || event.side != Side.SERVER) {
            return;
        }
        try {
            guard(event.player);
        } catch (RuntimeException | LinkageError e) {
            failed = true;
            IntegrationGate.fail("comforts", e);
        }
    }

    private static void guard(EntityPlayer player) {
        CapabilitySleeping.ISleeping sleeping = CapabilitySleeping.getSleeping(player);
        if (sleeping == null || !sleeping.isSleeping() || !(player instanceof EntityPlayerMP)
                || !PlayerExemptions.applies(player, Rule.BED_EXPLOSION)) {
            return;
        }
        BlockPos pos = sleeping.getPos();
        if (pos == null || ExplosiveBedUse.explodingHead(player, player.world, pos) == null) {
            return;
        }
        String bed = ExplosiveBedUse.bedName(player.world, pos);
        sleeping.setSleeping(false);
        sleeping.setPos(BlockPos.ORIGIN);
        Refusals.refuse((EntityPlayerMP) player, Rule.BED_EXPLOSION, Resync.none(), ItemStack.EMPTY, pos, "bed", bed, "use", "auto");
    }
}
