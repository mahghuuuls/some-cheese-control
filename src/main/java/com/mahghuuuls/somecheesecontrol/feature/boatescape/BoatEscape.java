package com.mahghuuuls.somecheesecontrol.feature.boatescape;

import com.mahghuuuls.somecheesecontrol.config.Settings;
import com.mahghuuuls.somecheesecontrol.core.ActualDamage;
import com.mahghuuuls.somecheesecontrol.core.DiagnosticLog;
import com.mahghuuuls.somecheesecontrol.core.ServerState;
import net.minecraft.block.material.Material;
import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.item.EntityBoat;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.MathHelper;
import net.minecraft.world.World;
import net.minecraftforge.event.entity.EntityMountEvent;
import net.minecraftforge.event.entity.living.LivingDamageEvent;
import net.minecraftforge.fml.common.eventhandler.EventPriority;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;

/**
 * Boat escape: when a mob (any living entity except a player) riding a vanilla boat takes actual
 * damage, it leaves the boat, is placed next to it where it cannot suffocate, and cannot board any
 * boat for the configured time. Other passengers and the boat are not touched.
 *
 * <p>Whether a hit is actual damage is decided by {@link ActualDamage}; this listener runs last,
 * without canceled events, as that class asks. The hit's knockback is given after the damage event,
 * so it applies to the mob at its new place.</p>
 *
 * <p>The exit spot is searched only around the boat's block ({@link BoatEscapeRules#SPOTS}), and only
 * when that area is loaded, so no chunk is loaded. With no free spot, or an area not loaded, the mob
 * goes to the boat's own position.</p>
 *
 * <p>A vanilla boat takes in a mob that touches it, every tick. The reboarding block cancels that
 * mount (and any other boat mount by the mob) until it expires. The game itself keeps any entity
 * that leaves a vehicle from boarding again for {@link BoatEscapeRules#VANILLA_RIDE_COOLDOWN} ticks,
 * but posts the mount event before it checks that wait; so the refusal line is written for the first
 * refusal after that wait, the first one this mod alone causes. The blocks are kept per server and
 * per mob, by the mob's UUID, and are never saved.</p>
 */
public final class BoatEscape {

    private static final ServerState.Slot<Blocks> BLOCKS = new ServerState.Slot<>(Blocks.class, Blocks::new);

    private final int reboardTicks;

    public BoatEscape(Settings settings) {
        this.reboardTicks = settings.boatReboardTicks();
    }

    /** True when boat escape is on, so the feature must be registered. */
    public static boolean isNeeded(Settings settings) {
        return settings.boatEscape();
    }

    /** Game ticks left before this mob can board a boat again; 0 when it is not blocked. */
    public static long reboardTicksLeft(Entity mob) {
        Block block = ServerState.get().get(BLOCKS).byMob.get(mob.getUniqueID());
        long now = ServerState.tick();
        return block != null && BoatEscapeRules.isBlocked(now, block.until) ? block.until - now : 0;
    }

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public void onDamage(LivingDamageEvent event) {
        EntityLivingBase mob = event.getEntityLiving();
        if (mob.world.isRemote || mob instanceof EntityPlayer) {
            return;
        }
        Entity boat = mob.getRidingEntity();
        if (boat == null || !BoatEscapeRules.isCoveredBoat(boat.getClass()) || !ActualDamage.isActual(event)) {
            return;
        }
        double fromX = mob.posX;
        double fromY = mob.posY;
        double fromZ = mob.posZ;
        mob.dismountRidingEntity();
        if (mob.getRidingEntity() == boat) {
            return; // another mod refused the dismount
        }
        BoatEscapeRules.Offset spot = findSpot(mob, boat);
        if (spot != null) {
            BlockPos base = baseOf(boat);
            mob.setPositionAndUpdate(base.getX() + spot.dx + 0.5, base.getY() + spot.dy, base.getZ() + spot.dz + 0.5);
        } else {
            mob.setPositionAndUpdate(boat.posX, boat.posY, boat.posZ);
        }
        if (reboardTicks > 0) {
            block(mob.getUniqueID());
        }
        if (DiagnosticLog.enabled()) {
            DiagnosticLog.decision("boat-escape", "mob", DiagnosticLog.entityName(mob), "entity", mob.getEntityId(),
                    "dim", mob.dimension, "from", position(fromX, fromY, fromZ), "to", position(mob.posX, mob.posY, mob.posZ),
                    "spot", spot != null ? "side" : "boat", "reboardTicks", reboardTicks);
        }
    }

    @SubscribeEvent
    public void onMount(EntityMountEvent event) {
        Entity mob = event.getEntityMounting();
        if (!event.isMounting() || mob.world.isRemote || !(event.getEntityBeingMounted() instanceof EntityBoat)) {
            return;
        }
        Map<UUID, Block> byMob = ServerState.get().get(BLOCKS).byMob;
        Block block = byMob.get(mob.getUniqueID());
        if (block == null) {
            return;
        }
        long now = ServerState.tick();
        if (!BoatEscapeRules.isBlocked(now, block.until)) {
            byMob.remove(mob.getUniqueID());
            return;
        }
        event.setCanceled(true);
        if (block.refusalLogged || !BoatEscapeRules.isPastVanillaCooldown(now, block.ejectedAt)) {
            return;
        }
        block.refusalLogged = true;
        if (DiagnosticLog.enabled()) {
            DiagnosticLog.decision("boat-reboard-refused", "mob", DiagnosticLog.entityName(mob), "entity", mob.getEntityId(),
                    "dim", mob.dimension, "sinceEjection", now - block.ejectedAt, "ticksLeft", block.until - now);
        }
    }

    private void block(UUID mob) {
        long now = ServerState.tick();
        Map<UUID, Block> byMob = ServerState.get().get(BLOCKS).byMob;
        for (Iterator<Block> it = byMob.values().iterator(); it.hasNext(); ) {
            if (!BoatEscapeRules.isBlocked(now, it.next().until)) {
                it.remove();
            }
        }
        byMob.put(mob, new Block(now, BoatEscapeRules.blockedUntil(now, reboardTicks)));
    }

    /** The exit spot for {@code mob} around {@code boat}, or null when none fits or the area is not loaded. */
    private static BoatEscapeRules.Offset findSpot(EntityLivingBase mob, Entity boat) {
        World world = mob.world;
        BlockPos base = baseOf(boat);
        int reach = BoatEscapeRules.REACH;
        int top = MathHelper.ceil(mob.height) + 1;
        if (!world.isAreaLoaded(base.add(-reach, -1, -reach), base.add(reach, top, reach))) {
            return null;
        }
        double halfWidth = mob.width / 2.0;
        double height = mob.height;
        return BoatEscapeRules.choose(new BoatEscapeRules.Probe() {
            @Override
            public boolean isFree(BoatEscapeRules.Offset spot) {
                double x = base.getX() + spot.dx + 0.5;
                double y = base.getY() + spot.dy;
                double z = base.getZ() + spot.dz + 0.5;
                AxisAlignedBB body = new AxisAlignedBB(x - halfWidth, y, z - halfWidth, x + halfWidth, y + height, z + halfWidth);
                return !world.collidesWithAnyBlock(body) && !world.isFlammableWithin(body);
            }

            @Override
            public boolean isSupported(BoatEscapeRules.Offset spot) {
                BlockPos below = base.add(spot.dx, spot.dy - 1, spot.dz);
                IBlockState state = world.getBlockState(below);
                return state.isSideSolid(world, below, EnumFacing.UP) || state.getMaterial() == Material.WATER;
            }
        });
    }

    /** The boat's block: the one its bottom is in, with a small margin for a boat resting on a block. */
    private static BlockPos baseOf(Entity boat) {
        return new BlockPos(MathHelper.floor(boat.posX), MathHelper.floor(boat.posY + 0.001), MathHelper.floor(boat.posZ));
    }

    private static String position(double x, double y, double z) {
        return String.format(Locale.ROOT, "%.1f,%.1f,%.1f", x, y, z);
    }

    /** One mob's reboarding block. */
    private static final class Block {
        final long ejectedAt;
        final long until;
        boolean refusalLogged;

        Block(long ejectedAt, long until) {
            this.ejectedAt = ejectedAt;
            this.until = until;
        }
    }

    /** The reboarding blocks of one server, by mob UUID. */
    private static final class Blocks {
        final Map<UUID, Block> byMob = new HashMap<>();
    }
}
