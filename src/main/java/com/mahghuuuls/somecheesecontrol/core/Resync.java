package com.mahghuuuls.somecheesecontrol.core;

import net.minecraft.util.math.BlockPos;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

/**
 * What the client may have predicted before the server refused, and so must be sent again.
 *
 * <p>A client without this mod runs its own copy of the action first; after a refusal it can show an
 * emptied bucket or a block that does not exist on the server until the server sends the truth.</p>
 */
public final class Resync {

    private final boolean inventory;
    private final List<BlockPos> blocks;

    private Resync(boolean inventory, List<BlockPos> blocks) {
        this.inventory = inventory;
        this.blocks = blocks;
    }

    /** The player's inventory and the given block positions. */
    public static Resync inventoryAndBlocks(BlockPos... blocks) {
        return new Resync(true, Collections.unmodifiableList(Arrays.asList(blocks.clone())));
    }

    boolean inventoryNeeded() {
        return inventory;
    }

    List<BlockPos> blocks() {
        return blocks;
    }
}
