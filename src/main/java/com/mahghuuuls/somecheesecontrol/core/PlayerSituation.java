package com.mahghuuuls.somecheesecontrol.core;

import com.mahghuuuls.somecheesecontrol.config.Settings;
import net.minecraft.block.Block;
import net.minecraft.block.BlockRailBase;
import net.minecraft.block.material.Material;
import net.minecraft.entity.Entity;
import net.minecraft.entity.item.EntityMinecart;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.MathHelper;

import java.util.Collections;
import java.util.Set;

/**
 * The only owner of what "airborne" and "on a climbable block" mean for a player.
 *
 * <p>A player is airborne when not standing on the ground and not in water or lava; holding on to a
 * ladder above air counts as airborne. A riding player is airborne when the vehicle is neither on
 * the ground nor in water.</p>
 *
 * <p>The player's own ground state is the one the server keeps from the client's movement packets,
 * which can lag one tick. A vehicle's ground state is read from the blocks under it instead: a
 * minecart running on rails never sets its own ground flag, and a floating boat sits above the
 * water it floats on. A minecart also stands on the ground while it rides a rail, which has no
 * collision box of its own.</p>
 */
public final class PlayerSituation {

    /** How far below a vehicle's bottom the server looks for ground or water. */
    private static final double PROBE_DEPTH = 0.1;

    /** Sideways inset, so a vehicle touching a wall does not find the wall under it. */
    private static final double PROBE_INSET = 0.001;

    private PlayerSituation() {
    }

    public static boolean isAirborne(EntityPlayer player) {
        Entity vehicle = player.getRidingEntity();
        if (vehicle != null) {
            AxisAlignedBB under = underneath(vehicle.getEntityBoundingBox());
            boolean onGround = vehicle.world.collidesWithAnyBlock(under) || isOnRail(vehicle);
            boolean inWater = vehicle.isInWater() || vehicle.world.isMaterialInBB(under, Material.WATER);
            return isAirborne(onGround, inWater);
        }
        return isAirborne(player.onGround, player.isInWater() || player.isInLava());
    }

    /**
     * True when the block at the player's feet is climbable: the game's own test (ladders, vines and
     * modded blocks that declare themselves climbable), or one of the configured extra blocks at the
     * same position. The extra blocks are those of the player's side.
     */
    public static boolean isOnClimbableBlock(EntityPlayer player) {
        if (player.isOnLadder()) {
            return true;
        }
        Settings settings = Settings.forSide(player.world.isRemote);
        Set<Block> extraClimbable = settings != null ? settings.extraClimbableBlocks() : Collections.<Block>emptySet();
        if (extraClimbable.isEmpty() || player.isSpectator()) {
            return false;
        }
        BlockPos feet = new BlockPos(MathHelper.floor(player.posX), MathHelper.floor(player.getEntityBoundingBox().minY),
                MathHelper.floor(player.posZ));
        return extraClimbable.contains(player.world.getBlockState(feet).getBlock());
    }

    /**
     * A minecart on a rail stands on the rail's block. On a sloped rail the cart is raised half a block,
     * and the rail itself has no collision box, so the probe under the cart finds nothing. The rail is
     * looked for where the game's own minecart update looks for it, in the cart's block or the block
     * below, and only for a cart that can use rails.
     */
    private static boolean isOnRail(Entity vehicle) {
        if (!(vehicle instanceof EntityMinecart) || !((EntityMinecart) vehicle).canUseRail()) {
            return false;
        }
        BlockPos pos = new BlockPos(MathHelper.floor(vehicle.posX), MathHelper.floor(vehicle.posY), MathHelper.floor(vehicle.posZ));
        return BlockRailBase.isRailBlock(vehicle.world, pos) || BlockRailBase.isRailBlock(vehicle.world, pos.down());
    }

    /** Airborne means neither standing on something nor held up by a liquid. */
    static boolean isAirborne(boolean onGround, boolean inLiquid) {
        return !onGround && !inLiquid;
    }

    /** A thin box just under {@code box}, slightly narrower than it. */
    static AxisAlignedBB underneath(AxisAlignedBB box) {
        return new AxisAlignedBB(box.minX + PROBE_INSET, box.minY - PROBE_DEPTH, box.minZ + PROBE_INSET,
                box.maxX - PROBE_INSET, box.minY, box.maxZ - PROBE_INSET);
    }
}
