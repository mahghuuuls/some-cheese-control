package com.mahghuuuls.somecheesecontrol.feature.cobweb;

import com.mahghuuuls.somecheesecontrol.config.Settings;
import com.mahghuuuls.somecheesecontrol.core.DiagnosticLog;
import com.mahghuuuls.somecheesecontrol.core.ServerState;
import net.minecraft.block.Block;
import net.minecraft.block.BlockWeb;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityList;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.minecraftforge.event.entity.living.LivingDamageEvent;
import net.minecraftforge.event.entity.living.LivingHurtEvent;
import net.minecraftforge.fml.common.eventhandler.EventPriority;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.relauncher.ReflectionHelper;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.lang.reflect.Field;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.WeakHashMap;

import javax.annotation.Nullable;

/**
 * Cobweb release: when a mob (any living entity except a player) takes actual damage, the cobwebs
 * inside its body are removed, with the normal break particles and sound and no item drops.
 *
 * <p>The game takes a hit's absorption part before it posts the damage event, so a hit that only
 * removes absorption hearts arrives there with no damage left. The absorption is therefore noted
 * when the hit starts (the hurt event) and compared at the damage event. Both listen last, without
 * canceled events, so a hit another mod cancels never counts; a hit blocked by a shield, or fully
 * absorbed by the invulnerability time, never reaches the damage event. A stronger hit during the
 * invulnerability time does reach it, with the difference as damage, and is not counted: a hit
 * during that time is not actual damage.</p>
 *
 * <p>Every living entity except a player counts as a mob. An armor stand handles its own damage
 * without the damage events, so it never releases webs.</p>
 *
 * <p>Only the block positions inside the mob's body box are checked, with the same margin the game
 * uses to decide that an entity is inside a web, and only when they are all loaded, so no chunk is
 * loaded and no wider area is scanned.</p>
 *
 * <p>A web sets the entity's protected "in web" flag, which the game uses at the start of the
 * entity's next move: it slows that move and stops the entity. The hit's knockback is given after
 * the damage event, so the flag is cleared when webs are removed, and the knockback applies.</p>
 */
public final class CobwebRelease {

    private static final Logger LOGGER = LogManager.getLogger("SomeCheeseControl/Cobweb");

    /** {@code Entity.isInWeb}; null when it cannot be found, and then the flag is left alone. */
    @Nullable
    private static final Field IN_WEB = findInWeb();

    private static final ServerState.Slot<AbsorptionNotes> NOTES = new ServerState.Slot<>(AbsorptionNotes.class, AbsorptionNotes::new);

    private final Set<Block> extraWebBlocks;
    private final boolean followMobGriefing;

    public CobwebRelease(Settings settings) {
        this.extraWebBlocks = settings.extraWebBlocks();
        this.followMobGriefing = settings.cobwebFollowsMobGriefing();
    }

    /** True when cobweb release is on, so the feature must be registered. */
    public static boolean isNeeded(Settings settings) {
        return settings.cobwebRelease();
    }

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public void onHurt(LivingHurtEvent event) {
        EntityLivingBase entity = event.getEntityLiving();
        if (isMobOnServer(entity)) {
            ServerState.get().get(NOTES).byMob.put(entity, entity.getAbsorptionAmount());
        }
    }

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public void onDamage(LivingDamageEvent event) {
        EntityLivingBase entity = event.getEntityLiving();
        if (!isMobOnServer(entity)) {
            return;
        }
        Float before = ServerState.get().get(NOTES).byMob.remove(entity);
        if (CobwebRules.isInvulnerabilityHit(entity.hurtResistantTime, entity.maxHurtResistantTime)) {
            return;
        }
        float after = entity.getAbsorptionAmount();
        if (!CobwebRules.isActualDamage(event.getAmount(), before != null ? before : after, after)) {
            return;
        }
        World world = entity.world;
        if (!CobwebRules.mayRemove(followMobGriefing, world.getGameRules().getBoolean("mobGriefing"))) {
            return;
        }
        int removed = removeWebsInside(world, entity.getEntityBoundingBox());
        if (removed > 0) {
            clearInWeb(entity);
        }
        if (removed > 0 && DiagnosticLog.enabled()) {
            DiagnosticLog.decision("cobweb-release", "mob", mobName(entity), "entity", entity.getEntityId(),
                    "dim", entity.dimension, "count", removed,
                    "pos", String.format(Locale.ROOT, "%.1f,%.1f,%.1f", entity.posX, entity.posY, entity.posZ));
        }
    }

    /**
     * The absorption of each mob at the start of its latest hit, per server. Every hit posts the hurt
     * event before the damage event, so the damage event reads the note of its own hit, and the next
     * hurt event replaces a note whose hit stopped in between (a shield block, or another mod setting
     * the damage to 0). Weak keys, so a mob that is gone is not kept.
     */
    private static final class AbsorptionNotes {
        final Map<EntityLivingBase, Float> byMob = new WeakHashMap<>();
    }

    /** Removes every web inside {@code body}; returns how many were removed. */
    private int removeWebsInside(World world, AxisAlignedBB body) {
        CobwebRules.Range range = CobwebRules.inside(body.minX, body.minY, body.minZ, body.maxX, body.maxY, body.maxZ);
        if (range.count() == 0) {
            return 0;
        }
        BlockPos from = new BlockPos(range.fromX, range.fromY, range.fromZ);
        BlockPos to = new BlockPos(range.toX, range.toY, range.toZ);
        if (!world.isAreaLoaded(from, to)) {
            return 0;
        }
        int removed = 0;
        BlockPos.MutableBlockPos probe = new BlockPos.MutableBlockPos();
        for (int x = range.fromX; x <= range.toX; x++) {
            for (int y = range.fromY; y <= range.toY; y++) {
                for (int z = range.fromZ; z <= range.toZ; z++) {
                    probe.setPos(x, y, z);
                    if (isWeb(world.getBlockState(probe).getBlock())
                            // A fresh position: the break effect packet keeps the position it is given.
                            && world.destroyBlock(new BlockPos(x, y, z), false)) {
                        removed++;
                    }
                }
            }
        }
        return removed;
    }

    /** The vanilla cobweb, modded webs made from it, and the configured extra web blocks. */
    private boolean isWeb(Block block) {
        return block instanceof BlockWeb || extraWebBlocks.contains(block);
    }

    private static boolean isMobOnServer(@Nullable EntityLivingBase entity) {
        return entity != null && !entity.world.isRemote && !(entity instanceof EntityPlayer);
    }

    private static void clearInWeb(Entity entity) {
        if (IN_WEB == null) {
            return;
        }
        try {
            IN_WEB.setBoolean(entity, false);
        } catch (IllegalAccessException e) {
            // findField made it accessible; nothing else to do here
        }
    }

    private static String mobName(Entity entity) {
        ResourceLocation key = EntityList.getKey(entity);
        return key != null ? key.toString() : entity.getClass().getSimpleName();
    }

    @Nullable
    private static Field findInWeb() {
        try {
            return ReflectionHelper.findField(Entity.class, "isInWeb", "field_70134_J");
        } catch (ReflectionHelper.UnableToFindFieldException e) {
            LOGGER.warn("Cannot find the entity web flag; a released mob may not be knocked back by the hit that released it");
            return null;
        }
    }
}
