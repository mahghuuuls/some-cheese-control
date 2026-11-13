package com.mahghuuuls.somecheesecontrol.feature.liquid;

import com.mahghuuuls.somecheesecontrol.core.PlayerExemptions;
import com.mahghuuuls.somecheesecontrol.core.Refusals;
import com.mahghuuuls.somecheesecontrol.core.Resync;
import com.mahghuuuls.somecheesecontrol.core.Rule;
import net.minecraft.block.Block;
import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.item.ItemStack;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.RayTraceResult;
import net.minecraft.world.World;
import net.minecraftforge.event.entity.player.FillBucketEvent;
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.fluids.FluidUtil;
import net.minecraftforge.fluids.UniversalBucket;
import net.minecraftforge.fml.common.eventhandler.EventPriority;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;

/**
 * Refuses emptying a player bucket where the block below the liquid gives no support.
 *
 * <p>{@link FillBucketEvent} fires before a bucket changes anything, for filling and emptying, and only
 * for player bucket use: dispensers never fire it. Cancelling it leaves the bucket and the world as
 * they were.</p>
 */
public final class LiquidPlacementFeature {

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public void onFillBucket(FillBucketEvent event) {
        World world = event.getWorld();
        if (world.isRemote || !(event.getEntityPlayer() instanceof EntityPlayerMP)) {
            return;
        }
        EntityPlayerMP player = (EntityPlayerMP) event.getEntityPlayer();
        ItemStack bucket = event.getEmptyBucket();
        if (!isEmptying(bucket) || !PlayerExemptions.applies(player, Rule.UNSUPPORTED_LIQUID)) {
            return;
        }
        RayTraceResult target = event.getTarget();
        if (target == null || target.typeOfHit != RayTraceResult.Type.BLOCK) {
            return;
        }
        BlockPos destination = destination(world, target, bucket);
        BlockPos below = destination.down();
        IBlockState belowState = world.getBlockState(below);
        boolean supported = LiquidRules.isSupported(belowState.getMaterial().isLiquid(),
                belowState.getCollisionBoundingBox(world, below) != Block.NULL_AABB);
        if (supported) {
            return;
        }
        event.setCanceled(true);
        Refusals.refuse(player, Rule.UNSUPPORTED_LIQUID, Resync.inventoryAndBlocks(destination), bucket, destination);
    }

    /** True when the bucket holds a fluid, so the use empties it; filling is never refused here. */
    static boolean isEmptying(ItemStack bucket) {
        if (bucket.isEmpty()) {
            return false;
        }
        FluidStack contained = FluidUtil.getFluidContained(bucket);
        return contained != null && contained.amount > 0;
    }

    private static BlockPos destination(World world, RayTraceResult target, ItemStack bucket) {
        BlockPos clicked = target.getBlockPos();
        boolean replaceable = world.getBlockState(clicked).getBlock().isReplaceable(world, clicked);
        boolean intoClicked = LiquidRules.placesIntoClickedBlock(bucket.getItem() instanceof UniversalBucket,
                replaceable, target.sideHit == EnumFacing.UP);
        return intoClicked ? clicked : clicked.offset(target.sideHit);
    }
}
