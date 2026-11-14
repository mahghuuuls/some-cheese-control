package com.mahghuuuls.somecheesecontrol.feature.liquid;

import com.mahghuuuls.somecheesecontrol.config.Settings;
import com.mahghuuuls.somecheesecontrol.core.Refusals;
import com.mahghuuuls.somecheesecontrol.core.Resync;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraftforge.event.entity.player.FillBucketEvent;
import net.minecraftforge.fml.common.eventhandler.EventPriority;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;

/**
 * Refuses emptying a player bucket while the player is airborne, or where the block below the liquid
 * gives no support. The two rules have their own switches.
 *
 * <p>{@link FillBucketEvent} fires before a bucket changes anything, for filling and emptying, and only
 * for player bucket use: dispensers never fire it. Cancelling it leaves the bucket and the world as
 * they were.</p>
 *
 * <p>The event also fires on the client, which places the liquid and plays its sound on its own;
 * a client with this mod refuses the use there too ({@link LiquidPlacement#check}), so it shows nothing.</p>
 */
public final class LiquidPlacementFeature {

    /** True when either liquid placement rule is switched on, so the handler must be registered. */
    public static boolean isNeeded(Settings settings) {
        return settings.unsupportedLiquidPlacement() || settings.airborneEmptying();
    }

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public void onFillBucket(FillBucketEvent event) {
        if (event.getWorld().isRemote || !(event.getEntityPlayer() instanceof EntityPlayerMP)) {
            return;
        }
        EntityPlayerMP player = (EntityPlayerMP) event.getEntityPlayer();
        LiquidPlacement.Refused refused = LiquidPlacement.check(Settings.get(), player, event.getWorld(), event.getEmptyBucket(),
                event.getTarget());
        if (refused != null) {
            event.setCanceled(true);
            Refusals.refuseClickStep(player, refused.rule, Resync.inventoryAndBlocks(refused.destination), event.getEmptyBucket(),
                    refused.destination);
        }
    }
}
