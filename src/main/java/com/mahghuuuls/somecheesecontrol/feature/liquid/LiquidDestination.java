package com.mahghuuuls.somecheesecontrol.feature.liquid;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.RayTraceResult;
import net.minecraft.world.World;

import javax.annotation.Nullable;

/**
 * Where a bucket use puts its liquid. The liquid placement rules judge the block below that place.
 *
 * <p>This package has the default rule (vanilla and Forge universal buckets). An integration whose
 * mod moves the liquid elsewhere registers its own rule with {@link LiquidPlacement#setDestinationRule};
 * that rule answers first, and the default answers when it returns null.</p>
 */
public interface LiquidDestination {

    /**
     * The block the liquid goes into, or null when this rule does not decide this use.
     *
     * @param target the bucket's block hit, from the bucket event
     */
    @Nullable
    BlockPos destination(World world, EntityPlayer player, ItemStack bucket, RayTraceResult target);
}
