package com.mahghuuuls.somecheesecontrol.feature.liquid;

import net.minecraft.item.ItemStack;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.RayTraceResult;
import net.minecraft.util.math.Vec3d;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class LiquidDestinationTest {

    private static final BlockPos CLICKED = new BlockPos(10, 64, 10);
    private static final RayTraceResult SIDE_CLICK = new RayTraceResult(new Vec3d(10.0, 64.5, 10.5), EnumFacing.WEST, CLICKED);
    private static final LiquidDestination SIDE_BLOCK = (world, player, bucket, target) -> target.getBlockPos().offset(target.sideHit);

    @Test
    void withoutAnIntegrationRuleTheDefaultDecides() {
        assertEquals(CLICKED.west(), LiquidPlacement.destination(null, SIDE_BLOCK, null, null, ItemStack.EMPTY, SIDE_CLICK));
    }

    @Test
    void anIntegrationRuleThatDecidesWins() {
        AtomicInteger defaultCalls = new AtomicInteger();
        LiquidDestination counting = (world, player, bucket, target) -> {
            defaultCalls.incrementAndGet();
            return SIDE_BLOCK.destination(world, player, bucket, target);
        };
        LiquidDestination intoClicked = (world, player, bucket, target) -> target.getBlockPos();
        assertEquals(CLICKED, LiquidPlacement.destination(intoClicked, counting, null, null, ItemStack.EMPTY, SIDE_CLICK));
        assertEquals(0, defaultCalls.get(), "the default is not asked");
    }

    @Test
    void anIntegrationRuleThatDoesNotDecideFallsBackToTheDefault() {
        LiquidDestination undecided = (world, player, bucket, target) -> null;
        assertEquals(CLICKED.west(), LiquidPlacement.destination(undecided, SIDE_BLOCK, null, null, ItemStack.EMPTY, SIDE_CLICK));
    }

    @AfterEach
    void forgetTheRegisteredRule() {
        LiquidPlacement.clearDestinationRule();
    }

    @Test
    void theRegisteredRuleIsTheOneAsked() {
        LiquidPlacement.setDestinationRule((world, player, bucket, target) -> target.getBlockPos());
        assertEquals(CLICKED, LiquidPlacement.destination(null, null, ItemStack.EMPTY, SIDE_CLICK),
                "the registered rule decides, so the default (which needs a world) is not asked");
    }

    @Test
    void aSecondRuleIsRefused() {
        LiquidPlacement.setDestinationRule((world, player, bucket, target) -> null);
        assertThrows(IllegalStateException.class, () -> LiquidPlacement.setDestinationRule((world, player, bucket, target) -> null));
    }
}
