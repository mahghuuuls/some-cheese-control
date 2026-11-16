package com.mahghuuuls.somecheesecontrol.feature.bed;

import net.minecraft.util.EnumFacing;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.WorldProvider.WorldSleepResult;
import net.minecraftforge.fml.common.eventhandler.Event.Result;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.function.BooleanSupplier;

import javax.annotation.Nullable;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

/**
 * The bed decision over a small world model with a stubbed dimension rule: which spot counts, when
 * the dimension makes it explode, and when a click reaches the bed at all.
 */
class BedRulesTest {

    private static final BlockPos FOOT = new BlockPos(10, 64, 10);
    private static final BlockPos HEAD = FOOT.offset(EnumFacing.NORTH);

    @Test
    void aClickOnTheFootIsJudgedAtTheHead() {
        Model nether = Model.twoBlockBed(WorldSleepResult.BED_EXPLODES);
        assertEquals(HEAD, BedRules.explodingHead(nether, FOOT), "the head is one block along the bed's facing");
        assertEquals(HEAD, nether.askedAt, "the dimension is asked at the head, not at the clicked foot");
        assertEquals(HEAD, BedRules.explodingHead(nether, HEAD));
    }

    @Test
    void theDimensionsOwnAnswerDecides() {
        assertNull(BedRules.explodingHead(Model.twoBlockBed(WorldSleepResult.ALLOW), FOOT), "the Overworld: sleeping works as before");
        assertNull(BedRules.explodingHead(Model.twoBlockBed(WorldSleepResult.DENY), FOOT), "a dimension that only denies sleep");
    }

    @Test
    void aBedMissingItsHeadDoesNothingAndIsNotRefused() {
        Model model = Model.twoBlockBed(WorldSleepResult.BED_EXPLODES);
        model.otherBlock.add(HEAD);
        assertNull(BedRules.explodingHead(model, FOOT));
    }

    @Test
    void aBedWithoutHalvesIsItsOwnHead() {
        Model model = new Model(WorldSleepResult.BED_EXPLODES);
        model.parts.put(FOOT, BedRules.Part.WHOLE);
        assertEquals(FOOT, BedRules.explodingHead(model, FOOT));
    }

    @Test
    void notABedOrNotLoadedIsNeverRefused() {
        Model model = new Model(WorldSleepResult.BED_EXPLODES);
        assertNull(BedRules.explodingHead(model, FOOT), "no bed");
        Model unloadedHead = Model.twoBlockBed(WorldSleepResult.BED_EXPLODES);
        unloadedHead.unloaded.add(HEAD);
        assertNull(BedRules.explodingHead(unloadedHead, FOOT), "nothing is read from an unloaded block");
    }

    @Test
    void aClickWithoutSneakingReachesTheBed() {
        assertEquals(HEAD, click(Result.DEFAULT, false, false, false));
    }

    @Test
    void aSneakingClickReachesTheBedOnlyWhenBothItemsLetItOrAnotherModAllowsIt() {
        assertNull(click(Result.DEFAULT, true, false, false), "sneaking to place a block against the bed is not a bed use");
        assertNull(click(Result.DEFAULT, true, true, false), "only the main hand lets a sneak through");
        assertNull(click(Result.DEFAULT, true, false, true), "only the offhand lets a sneak through");
        assertEquals(HEAD, click(Result.DEFAULT, true, true, true), "both held items let a sneak use the block");
        assertEquals(HEAD, click(Result.ALLOW, true, false, false), "another mod allowed the block use");
    }

    @Test
    void aDeniedBlockUseNeverReachesTheBed() {
        assertNull(click(Result.DENY, false, false, false));
        assertNull(click(Result.DENY, true, true, true));
    }

    @Test
    void theHeldItemsAreAskedOnlyForASneakingClickOnABed() {
        int[] asked = {0};
        BooleanSupplier counted = () -> {
            asked[0]++;
            return true;
        };
        BedRules.refusedClickHead(new Model(WorldSleepResult.BED_EXPLODES), FOOT, Result.DEFAULT, true, counted, counted);
        assertEquals(0, asked[0], "no bed: the items are not asked");
        BedRules.refusedClickHead(Model.twoBlockBed(WorldSleepResult.BED_EXPLODES), FOOT, Result.DEFAULT, false, counted, counted);
        assertEquals(0, asked[0], "a click on a bed without sneaking: the items are not asked");
        BedRules.refusedClickHead(Model.twoBlockBed(WorldSleepResult.BED_EXPLODES), FOOT, Result.DEFAULT, true, counted, counted);
        assertEquals(2, asked[0], "a sneaking click on a bed asks both items");
    }

    private static BlockPos click(Result useBlock, boolean sneaking, boolean mainBypass, boolean offBypass) {
        return BedRules.refusedClickHead(Model.twoBlockBed(WorldSleepResult.BED_EXPLODES), FOOT, useBlock, sneaking,
                () -> mainBypass, () -> offBypass);
    }

    /** A few blocks: bed parts, the bed's facing (north), unloaded spots, and a stubbed dimension rule. */
    private static final class Model implements BedRules.Probe {
        final Map<BlockPos, BedRules.Part> parts = new HashMap<>();
        final Set<BlockPos> unloaded = new HashSet<>();
        final Set<BlockPos> otherBlock = new HashSet<>();
        final WorldSleepResult dimension;
        BlockPos askedAt;

        Model(WorldSleepResult dimension) {
            this.dimension = dimension;
        }

        static Model twoBlockBed(WorldSleepResult dimension) {
            Model model = new Model(dimension);
            model.parts.put(FOOT, BedRules.Part.FOOT);
            model.parts.put(HEAD, BedRules.Part.HEAD);
            return model;
        }

        @Override
        public boolean isLoaded(BlockPos pos) {
            return !unloaded.contains(pos);
        }

        @Override
        @Nullable
        public BedRules.Part part(BlockPos pos) {
            return parts.get(pos);
        }

        @Override
        public EnumFacing facing(BlockPos pos) {
            return EnumFacing.NORTH;
        }

        @Override
        public boolean sameBlock(BlockPos a, BlockPos b) {
            return parts.containsKey(a) && parts.containsKey(b) && !otherBlock.contains(a) && !otherBlock.contains(b);
        }

        @Override
        public WorldSleepResult sleepResult(BlockPos head) {
            askedAt = head;
            return dimension;
        }
    }
}
