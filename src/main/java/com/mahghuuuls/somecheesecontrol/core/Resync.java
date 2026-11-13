package com.mahghuuuls.somecheesecontrol.core;

import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.network.play.server.SPacketBlockChange;
import net.minecraft.util.math.BlockPos;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

/**
 * What the client may show wrongly after the server acted, and so must be sent again.
 *
 * <p>A client without this mod runs its own copy of an action first; after a refusal it can show an
 * emptied bucket or a block that does not exist on the server. While a screen such as a chest is
 * open, the normal sync covers only that screen, so offhand and armor changes need a resend too.</p>
 */
public final class Resync {

    private final boolean inventory;
    private final List<BlockPos> blocks;

    private Resync(boolean inventory, List<BlockPos> blocks) {
        this.inventory = inventory;
        this.blocks = blocks;
    }

    /** Nothing beyond what the normal sync already sends. */
    public static Resync none() {
        return new Resync(false, Collections.<BlockPos>emptyList());
    }

    /** The player's inventory. */
    public static Resync inventory() {
        return new Resync(true, Collections.<BlockPos>emptyList());
    }

    /** The player's inventory and the given block positions. */
    public static Resync inventoryAndBlocks(BlockPos... blocks) {
        return new Resync(true, Collections.unmodifiableList(Arrays.asList(blocks.clone())));
    }

    /** Sends the player the server's truth for everything this resync names. */
    void sendTo(EntityPlayerMP player) {
        if (inventory) {
            player.sendContainerToPlayer(player.inventoryContainer);
        }
        for (BlockPos pos : blocks) {
            player.connection.sendPacket(new SPacketBlockChange(player.world, pos));
        }
    }
}
