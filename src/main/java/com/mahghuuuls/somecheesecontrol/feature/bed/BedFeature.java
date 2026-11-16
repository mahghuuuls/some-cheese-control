package com.mahghuuuls.somecheesecontrol.feature.bed;

import com.mahghuuuls.somecheesecontrol.config.Settings;
import com.mahghuuuls.somecheesecontrol.core.PlayerExemptions;
import com.mahghuuuls.somecheesecontrol.core.Refusals;
import com.mahghuuuls.somecheesecontrol.core.Resync;
import com.mahghuuuls.somecheesecontrol.core.Rule;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.util.EnumActionResult;
import net.minecraft.util.math.BlockPos;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;
import net.minecraftforge.fml.common.eventhandler.EventPriority;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;

/**
 * Bed-explosion prevention: a right click that would use a bed where it explodes is refused, in
 * every game mode, Creative too.
 *
 * <p>The game posts the right-click event before the bed's own use, so the bed is still whole
 * here. The refusal runs first, as every refusal does, so no other mod acts on a refused click. A
 * click that would not reach the bed (its block use denied, or a sneak the held items do not let
 * through) is let through; another mod may still allow its block use later, so the same check runs
 * again last, without canceled events, and refuses such a click then. A refused click does nothing: no explosion, no block change, no sleep and no spawn
 * point; a held item is not used on the bed either. The client predicts nothing for a bed and the
 * client package refuses the same click early, so no resend is needed.</p>
 */
public final class BedFeature {

    /** True when bed-explosion prevention is on, so the feature must be registered. */
    public static boolean isNeeded(Settings settings) {
        return settings.bedExplosion();
    }

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public void onRightClickBlock(PlayerInteractEvent.RightClickBlock event) {
        refuse(event);
    }

    /** The same check after every other listener: a click whose block use another mod allowed after the first check. */
    @SubscribeEvent(priority = EventPriority.LOWEST)
    public void onRightClickBlockAllowedLater(PlayerInteractEvent.RightClickBlock event) {
        refuse(event);
    }

    private static void refuse(PlayerInteractEvent.RightClickBlock event) {
        EntityPlayer player = event.getEntityPlayer();
        if (player.world.isRemote || !(player instanceof EntityPlayerMP) || !PlayerExemptions.applies(player, Rule.BED_EXPLOSION)) {
            return;
        }
        BlockPos pos = event.getPos();
        if (!ExplosiveBedUse.isRefusedClick(player, event.getWorld(), pos, event.getUseBlock())) {
            return;
        }
        event.setCanceled(true);
        event.setCancellationResult(EnumActionResult.FAIL);
        Refusals.refuseClickStep((EntityPlayerMP) player, Rule.BED_EXPLOSION, Resync.none(), event.getItemStack(), pos,
                "bed", ExplosiveBedUse.bedName(event.getWorld(), pos));
    }
}
