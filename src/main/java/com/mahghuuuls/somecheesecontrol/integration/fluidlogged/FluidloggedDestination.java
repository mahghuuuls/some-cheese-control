package com.mahghuuuls.somecheesecontrol.integration.fluidlogged;

import com.mahghuuuls.somecheesecontrol.feature.liquid.LiquidDestination;
import com.mahghuuuls.somecheesecontrol.integration.IntegrationGate;
import net.minecraft.block.Block;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.init.Blocks;
import net.minecraft.item.ItemBucket;
import net.minecraft.item.ItemStack;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.RayTraceResult;
import net.minecraft.world.World;
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.fluids.FluidUtil;
import net.minecraftforge.fluids.UniversalBucket;
import net.minecraftforge.fml.relauncher.ReflectionHelper;

import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.function.Predicate;

import javax.annotation.Nullable;

/**
 * Fluidlogged API's destination for the two buckets it changes.
 *
 * <ul>
 * <li>A vanilla bucket ({@code ItemBucket}, whose right click it replaces): the clicked block itself
 * when its bucket-fluidlogging setting allows this player and the clicked block can hold the
 * bucket's liquid. Otherwise Fluidlogged API uses the vanilla destination, so this rule returns null
 * and the default answers. A subclass that replaces the right click without calling the vanilla one
 * would not get Fluidlogged API's placement but is still judged by this rule.</li>
 * <li>Forge's universal bucket, whose target Fluidlogged API computes in one public method: that
 * method's answer, the clicked block on the same two conditions (or the top face of a replaceable
 * block), otherwise the block on the clicked side. Fluidlogged API patches exactly Forge's class; a
 * subclass that replaces the right click is still judged by this rule.</li>
 * </ul>
 * Other buckets are not decided here.
 *
 * <p>Fluidlogged API is reached by name: compiling against its jar would also apply its access
 * transformer to the development game, so code could compile there against members that stay
 * closed in a game without Fluidlogged API. The names, kinds and types are checked when the rule is
 * made, at the integration's start, so a Fluidlogged API version without them fails the start with
 * a clear error. The setting is read at each use, because Fluidlogged API can reload its
 * configuration.</p>
 *
 * <p>An error during a use is reported to {@link IntegrationGate} once; the rule then returns null
 * for the rest of the session, so the default destination applies. Fluidlogged API still fills
 * fluidloggable blocks then, so such placements are judged at the game's own destination (vanilla's
 * rule, or Forge's for the universal bucket).</p>
 */
final class FluidloggedDestination implements LiquidDestination {

    private static final String CONFIG_CLASS = "git.jbredwards.fluidlogged_api.mod.common.config.FluidloggedAPIConfig";
    private static final String HOOKS_CLASS = "git.jbredwards.fluidlogged_api.mod.asm.plugins.vanilla.item.PluginItemBucket$Hooks";
    private static final String UNIVERSAL_HOOKS_CLASS = "git.jbredwards.fluidlogged_api.mod.asm.plugins.forge.PluginUniversalBucket$Hooks";

    /** The bucket's liquid block, which Fluidlogged API passes to its own check; not public in vanilla. */
    private final Field containedBlock;
    /** {@code FluidloggedAPIConfig.bucketFluidlogging}: a mode that is a {@code Predicate<Entity>} (always, sneaking, not sneaking). */
    private final Field bucketFluidlogging;
    /** {@code PluginItemBucket.Hooks.isFluidloggable(World, BlockPos, Block)}: whether the block can hold that liquid. */
    private final Method isFluidloggable;
    /** {@code PluginUniversalBucket.Hooks.getTargetPos(BlockPos, EnumFacing, World, EntityPlayer, FluidStack)}: the universal bucket's destination. */
    private final Method universalTarget;

    private volatile boolean failed;

    /** Looks up Fluidlogged API's members; throws when this version does not have them. */
    FluidloggedDestination() throws ReflectiveOperationException {
        ClassLoader loader = FluidloggedDestination.class.getClassLoader();
        this.containedBlock = ReflectionHelper.findField(ItemBucket.class, "containedBlock", "field_77876_a");
        this.bucketFluidlogging = Class.forName(CONFIG_CLASS, false, loader).getField("bucketFluidlogging");
        this.isFluidloggable = Class.forName(HOOKS_CLASS, false, loader).getMethod("isFluidloggable", World.class, BlockPos.class, Block.class);
        if (!Modifier.isStatic(bucketFluidlogging.getModifiers()) || !Predicate.class.isAssignableFrom(bucketFluidlogging.getType())) {
            throw new NoSuchFieldException("bucketFluidlogging is not a static player predicate");
        }
        if (!Modifier.isStatic(isFluidloggable.getModifiers()) || isFluidloggable.getReturnType() != boolean.class) {
            throw new NoSuchMethodException("isFluidloggable is not a static boolean method");
        }
        this.universalTarget = Class.forName(UNIVERSAL_HOOKS_CLASS, false, loader)
                .getMethod("getTargetPos", BlockPos.class, EnumFacing.class, World.class, EntityPlayer.class, FluidStack.class);
        if (!Modifier.isStatic(universalTarget.getModifiers()) || universalTarget.getReturnType() != BlockPos.class) {
            throw new NoSuchMethodException("getTargetPos is not a static method returning a block position");
        }
    }

    @Nullable
    @Override
    public BlockPos destination(World world, EntityPlayer player, ItemStack bucket, RayTraceResult target) {
        if (failed) {
            return null;
        }
        try {
            if (bucket.getItem() instanceof UniversalBucket) {
                FluidStack fluid = FluidUtil.getFluidContained(bucket);
                return fluid != null ? (BlockPos) universalTarget.invoke(null, target.getBlockPos(), target.sideHit, world, player, fluid) : null;
            }
            if (!(bucket.getItem() instanceof ItemBucket)) {
                return null;
            }
            Block liquid = (Block) containedBlock.get(bucket.getItem());
            BlockPos clicked = target.getBlockPos();
            boolean intoClicked = liquid != null && liquid != Blocks.AIR && allows(player)
                    && (Boolean) isFluidloggable.invoke(null, world, clicked, liquid);
            return intoClicked ? clicked : null;
        } catch (ReflectiveOperationException | RuntimeException | LinkageError e) {
            failed = true;
            IntegrationGate.fail(FluidloggedIntegration.MOD_ID, e instanceof InvocationTargetException ? e.getCause() : e);
            return null;
        }
    }

    @SuppressWarnings("unchecked")
    private boolean allows(EntityPlayer player) throws IllegalAccessException {
        Object mode = bucketFluidlogging.get(null);
        if (!(mode instanceof Predicate)) {
            throw new IllegalStateException("bucketFluidlogging holds no player predicate: " + mode);
        }
        return ((Predicate<Entity>) mode).test(player);
    }
}
