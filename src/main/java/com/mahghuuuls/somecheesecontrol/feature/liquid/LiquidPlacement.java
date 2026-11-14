package com.mahghuuuls.somecheesecontrol.feature.liquid;

import com.mahghuuuls.somecheesecontrol.config.Settings;
import com.mahghuuuls.somecheesecontrol.core.PlayerExemptions;
import com.mahghuuuls.somecheesecontrol.core.PlayerSituation;
import com.mahghuuuls.somecheesecontrol.core.Rule;
import net.minecraft.block.Block;
import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.RayTraceResult;
import net.minecraft.world.World;
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.fluids.FluidUtil;
import net.minecraftforge.fluids.UniversalBucket;

import javax.annotation.Nullable;

/**
 * The only owner of whether a player's bucket use breaks a liquid placement rule, for either side:
 * the server decides with its settings, and a client with its copy of its server's settings.
 */
public final class LiquidPlacement {

    private LiquidPlacement() {
    }

    /**
     * Decides whether this bucket use breaks a liquid placement rule under the given side's settings.
     * The server decides with its own settings; a client decides the same way with its copy of its
     * server's settings before it shows the liquid.
     *
     * @return the broken rule and the liquid's destination, or null when the use is allowed
     */
    @Nullable
    public static Refused check(Settings settings, EntityPlayer player, World world, ItemStack bucket, @Nullable RayTraceResult target) {
        if (!isEmptying(bucket) || target == null || target.typeOfHit != RayTraceResult.Type.BLOCK) {
            return null;
        }
        BlockPos destination = destination(world, target, bucket);
        if (settings.airborneEmptying() && PlayerExemptions.applies(player, Rule.AIRBORNE_EMPTYING)
                && PlayerSituation.isAirborne(player)) {
            return new Refused(Rule.AIRBORNE_EMPTYING, destination);
        }
        if (!settings.unsupportedLiquidPlacement() || !PlayerExemptions.applies(player, Rule.UNSUPPORTED_LIQUID)) {
            return null;
        }
        BlockPos below = destination.down();
        IBlockState belowState = world.getBlockState(below);
        boolean supported = LiquidRules.isSupported(belowState.getMaterial().isLiquid(),
                belowState.getCollisionBoundingBox(world, below) != Block.NULL_AABB);
        return supported ? null : new Refused(Rule.UNSUPPORTED_LIQUID, destination);
    }

    /** A refused bucket use: the broken rule and where the liquid would have gone. */
    public static final class Refused {
        public final Rule rule;
        public final BlockPos destination;

        Refused(Rule rule, BlockPos destination) {
            this.rule = rule;
            this.destination = destination;
        }
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
