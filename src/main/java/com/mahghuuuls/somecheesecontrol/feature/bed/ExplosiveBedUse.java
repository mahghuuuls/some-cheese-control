package com.mahghuuuls.somecheesecontrol.feature.bed;

import net.minecraft.block.Block;
import net.minecraft.block.BlockBed;
import net.minecraft.block.BlockHorizontal;
import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.minecraft.world.WorldProvider.WorldSleepResult;
import net.minecraftforge.fml.common.eventhandler.Event.Result;

import javax.annotation.Nullable;

/**
 * The only owner of "explosive bed use": a player's use of a bed that would explode under the
 * dimension's own rule. The server handler, the client package and the Comforts integration ask it.
 *
 * <p>This class reports the world to {@link BedRules}, which decides. A bed is a vanilla bed (and
 * every block made from it: Dropt Bed, the Aether skyroot bed), or any block that says it is a bed
 * (Comforts sleeping bags and hammocks). The spot that counts is the bed's head, found through the
 * vanilla bed's part and facing, which Comforts reuses; a block without them is its own head. The
 * dimension answers through {@code WorldProvider.canSleepAt}, the call the bed itself makes, so no
 * dimension id list is needed and a modded dimension's own rule is used.</p>
 */
public final class ExplosiveBedUse implements BedRules.Probe {

    private final EntityPlayer player;
    private final World world;

    private ExplosiveBedUse(EntityPlayer player, World world) {
        this.player = player;
        this.world = world;
    }

    /**
     * True when this right click on {@code pos} would use a bed that explodes: the click reaches the
     * block's own use (sneaking and the block-use result taken into account) and the bed there would
     * explode. The block-use result can still change after a refusal listener at the highest
     * priority, so the refusing side asks again at the lowest priority (a click it let through
     * because the block use was not yet allowed).
     */
    public static boolean isRefusedClick(EntityPlayer player, World world, BlockPos pos, Result useBlock) {
        return BedRules.refusedClickHead(new ExplosiveBedUse(player, world), pos, useBlock, player.isSneaking(),
                () -> player.getHeldItemMainhand().doesSneakBypassUse(world, pos, player),
                () -> player.getHeldItemOffhand().doesSneakBypassUse(world, pos, player)) != null;
    }

    /**
     * The head of the bed at {@code pos} when using it would explode for {@code player}; null when
     * there is no bed, the bed misses its head (the game then does nothing), or it is safe.
     */
    @Nullable
    public static BlockPos explodingHead(EntityPlayer player, World world, BlockPos pos) {
        return BedRules.explodingHead(new ExplosiveBedUse(player, world), pos);
    }

    /** The bed's block id, for a diagnostic line. */
    public static String bedName(World world, BlockPos pos) {
        ResourceLocation name = world.getBlockState(pos).getBlock().getRegistryName();
        return name != null ? name.toString() : "unknown";
    }

    @Override
    public boolean isLoaded(BlockPos pos) {
        return world.isBlockLoaded(pos);
    }

    @Override
    @Nullable
    public BedRules.Part part(BlockPos pos) {
        IBlockState state = world.getBlockState(pos);
        Block block = state.getBlock();
        if (!(block instanceof BlockBed) && !block.isBed(state, world, pos, player)) {
            return null;
        }
        if (!state.getPropertyKeys().contains(BlockBed.PART) || !state.getPropertyKeys().contains(BlockHorizontal.FACING)) {
            return BedRules.Part.WHOLE;
        }
        return state.getValue(BlockBed.PART) == BlockBed.EnumPartType.HEAD ? BedRules.Part.HEAD : BedRules.Part.FOOT;
    }

    @Override
    public EnumFacing facing(BlockPos pos) {
        return world.getBlockState(pos).getValue(BlockHorizontal.FACING);
    }

    @Override
    public boolean sameBlock(BlockPos a, BlockPos b) {
        return world.getBlockState(a).getBlock() == world.getBlockState(b).getBlock();
    }

    @Override
    public WorldSleepResult sleepResult(BlockPos head) {
        return world.provider.canSleepAt(player, head);
    }
}
