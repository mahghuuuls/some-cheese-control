package com.mahghuuuls.somecheesecontrol.core;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.util.EnumActionResult;
import net.minecraft.util.math.BlockPos;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;
import net.minecraftforge.fml.common.eventhandler.EventPriority;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.PlayerEvent;

/**
 * Refuses the remaining steps of a right click after one of its steps was refused on the server, so
 * one right click is one action: after a refused main-hand step, the offhand does nothing either.
 *
 * <p>Registered always, and before the features, because any feature's refused step counts and a
 * later step must be refused here before a feature decides it again. Registering it after them would
 * still refuse the step, only with a second resend. The later steps only correct the client: no
 * message and no diagnostic line. Without a refused step it costs one map lookup per step.</p>
 */
public final class ClickGuard {

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public void onRightClickItem(PlayerInteractEvent.RightClickItem event) {
        if (refuseLaterStep(event.getEntityPlayer(), Resync.inventory())) {
            cancel(event);
        }
    }

    /** The server resends the clicked block and its neighbour itself; a door's other half needs the blocks above and below. */
    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public void onRightClickBlock(PlayerInteractEvent.RightClickBlock event) {
        BlockPos pos = event.getPos();
        if (refuseLaterStep(event.getEntityPlayer(), Resync.inventoryAndBlocks(pos.up(), pos.down()))) {
            cancel(event);
        }
    }

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public void onEntityInteract(PlayerInteractEvent.EntityInteract event) {
        if (refuseLaterStep(event.getEntityPlayer(), Resync.inventoryAndEntity(event.getTarget()))) {
            cancel(event);
        }
    }

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public void onEntityInteractSpecific(PlayerInteractEvent.EntityInteractSpecific event) {
        if (refuseLaterStep(event.getEntityPlayer(), Resync.inventoryAndEntity(event.getTarget()))) {
            cancel(event);
        }
    }

    @SubscribeEvent
    public void onLogout(PlayerEvent.PlayerLoggedOutEvent event) {
        ClickRecord.get().forget(event.player.getUniqueID());
    }

    private static boolean refuseLaterStep(EntityPlayer player, Resync resync) {
        if (player.world.isRemote || !(player instanceof EntityPlayerMP)
                || !ClickRecord.get().isRefused(player.getUniqueID(), ServerState.tick())) {
            return false;
        }
        Refusals.correctClient((EntityPlayerMP) player, resync);
        return true;
    }

    private static void cancel(PlayerInteractEvent event) {
        event.setCanceled(true);
        event.setCancellationResult(EnumActionResult.FAIL);
    }
}
