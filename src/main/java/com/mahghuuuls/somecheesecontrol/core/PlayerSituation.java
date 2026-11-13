package com.mahghuuuls.somecheesecontrol.core;

import net.minecraft.block.material.Material;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.util.math.AxisAlignedBB;

/**
 * The only owner of what "airborne" means for a player.
 *
 * <p>A player is airborne when not standing on the ground and not in water or lava; holding on to a
 * ladder above air counts as airborne. A riding player is airborne when the vehicle is neither on
 * the ground nor in water.</p>
 *
 * <p>The player's own ground state is the one the server keeps from the client's movement packets,
 * which can lag one tick. A vehicle's ground state is read from the blocks under it instead: a
 * minecart running on rails never sets its own ground flag, and a floating boat sits above the
 * water it floats on.</p>
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
            boolean onGround = vehicle.world.collidesWithAnyBlock(under);
            boolean inWater = vehicle.isInWater() || vehicle.world.isMaterialInBB(under, Material.WATER);
            return isAirborne(onGround, inWater);
        }
        return isAirborne(player.onGround, player.isInWater() || player.isInLava());
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
