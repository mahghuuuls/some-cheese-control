package com.mahghuuuls.somecheesecontrol.feature.bed;

import net.minecraft.util.EnumFacing;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.WorldProvider.WorldSleepResult;
import net.minecraftforge.fml.common.eventhandler.Event.Result;

import java.util.function.BooleanSupplier;

import javax.annotation.Nullable;

/**
 * The bed decision over what a {@link Probe} reports about the world, so it can be checked against
 * a small model with a stubbed dimension rule. {@link ExplosiveBedUse} is the probe over a real world.
 */
final class BedRules {

    private BedRules() {
    }

    /** Which part of a bed a block is. */
    enum Part {
        /** The head half of a two-block bed: the spot that counts. */
        HEAD,
        /** The foot half; its head is one block along its facing. */
        FOOT,
        /** A bed without head and foot halves: it is its own head. */
        WHOLE
    }

    /** What the decision needs to know about the world. */
    interface Probe {
        /** True when the block at {@code pos} is loaded; nothing is read from an unloaded block. */
        boolean isLoaded(BlockPos pos);

        /** The bed part at {@code pos}, or null when no bed is there. */
        @Nullable
        Part part(BlockPos pos);

        /** The facing of the bed at {@code pos}: from its foot toward its head. */
        EnumFacing facing(BlockPos pos);

        /** True when the blocks at {@code a} and {@code b} are the same block. */
        boolean sameBlock(BlockPos a, BlockPos b);

        /** The dimension's own answer for sleeping at {@code head}. */
        WorldSleepResult sleepResult(BlockPos head);
    }

    /**
     * The head of the bed a right click on {@code pos} would use, when that use would explode; null
     * when the click does not reach a bed, the bed misses its head (the game then does nothing), or
     * the dimension allows or only denies sleep there. A sneaking click reaches the block only when
     * both held items let a sneak use it (asked only for a sneaking click on a bed), or another mod
     * allowed the block use.
     */
    @Nullable
    static BlockPos refusedClickHead(Probe probe, BlockPos pos, Result useBlock, boolean sneaking,
                                     BooleanSupplier mainHandBypassesSneak, BooleanSupplier offHandBypassesSneak) {
        if (useBlock == Result.DENY || !probe.isLoaded(pos) || probe.part(pos) == null) {
            return null;
        }
        if (sneaking && useBlock != Result.ALLOW && !(mainHandBypassesSneak.getAsBoolean() && offHandBypassesSneak.getAsBoolean())) {
            return null;
        }
        return explodingHead(probe, pos);
    }

    /**
     * The head of the bed at {@code pos} when using it would explode; null when there is no loaded
     * bed, the bed misses its head, or the dimension does not make it explode.
     */
    @Nullable
    static BlockPos explodingHead(Probe probe, BlockPos pos) {
        if (!probe.isLoaded(pos)) {
            return null;
        }
        Part part = probe.part(pos);
        if (part == null) {
            return null;
        }
        BlockPos head = pos;
        if (part == Part.FOOT) {
            head = pos.offset(probe.facing(pos));
            if (!probe.isLoaded(head) || !probe.sameBlock(pos, head)) {
                return null;
            }
        }
        return probe.sleepResult(head) == WorldSleepResult.BED_EXPLODES ? head : null;
    }
}
