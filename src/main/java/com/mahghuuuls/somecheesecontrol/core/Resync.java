package com.mahghuuuls.somecheesecontrol.core;

import io.netty.buffer.Unpooled;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLiving;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.network.PacketBuffer;
import net.minecraft.network.datasync.EntityDataManager;
import net.minecraft.network.play.server.SPacketBlockChange;
import net.minecraft.network.play.server.SPacketEntityAttach;
import net.minecraft.network.play.server.SPacketEntityMetadata;
import net.minecraft.util.math.BlockPos;

import java.io.IOException;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import javax.annotation.Nullable;

/**
 * What the client may show wrongly after the server acted, and so must be sent again.
 *
 * <p>A client runs its own copy of an action first unless it refused the action early; where the
 * two sides judged differently (near an edge, or a rule the client does not check), it can show an
 * emptied bucket or a block that does not exist on the server. While a screen such as a chest is
 * open, the normal sync covers only that screen, so offhand and armor changes need a resend too. An
 * interaction with an entity also runs on the client first, so the client can show a lead, a name
 * or a saddle the server never applied.</p>
 */
public final class Resync {

    private final boolean inventory;
    private final List<BlockPos> blocks;
    @Nullable
    private final Entity entity;

    private Resync(boolean inventory, List<BlockPos> blocks, @Nullable Entity entity) {
        this.inventory = inventory;
        this.blocks = blocks;
        this.entity = entity;
    }

    /** Nothing beyond what the normal sync already sends. */
    public static Resync none() {
        return new Resync(false, Collections.<BlockPos>emptyList(), null);
    }

    /** The player's inventory. */
    public static Resync inventory() {
        return new Resync(true, Collections.<BlockPos>emptyList(), null);
    }

    /** The player's inventory and the given block positions. */
    public static Resync inventoryAndBlocks(BlockPos... blocks) {
        return new Resync(true, Collections.unmodifiableList(Arrays.asList(blocks.clone())), null);
    }

    /** The player's inventory, and the entity's data and lead as the server has them. */
    public static Resync inventoryAndEntity(Entity entity) {
        return new Resync(true, Collections.<BlockPos>emptyList(), entity);
    }

    /** Sends the player the server's truth for everything this resync names. */
    void sendTo(EntityPlayerMP player) {
        if (inventory) {
            player.sendContainerToPlayer(player.inventoryContainer);
        }
        for (BlockPos pos : blocks) {
            player.connection.sendPacket(new SPacketBlockChange(player.world, pos));
        }
        if (entity != null) {
            SPacketEntityMetadata data = allDataOf(entity);
            if (data != null) {
                player.connection.sendPacket(data);
            }
            if (entity instanceof EntityLiving) {
                player.connection.sendPacket(new SPacketEntityAttach(entity, ((EntityLiving) entity).getLeashHolder()));
            }
        }
    }

    /**
     * A packet with every data entry of the entity. The packet's own "send all" constructor marks the
     * entity's data clean, which would drop changes not yet sent to the other players watching it, so
     * the entries are copied through a buffer instead.
     */
    @Nullable
    private static SPacketEntityMetadata allDataOf(Entity entity) {
        PacketBuffer buffer = new PacketBuffer(Unpooled.buffer());
        try {
            buffer.writeVarInt(entity.getEntityId());
            EntityDataManager.writeEntries(entity.getDataManager().getAll(), buffer);
            SPacketEntityMetadata packet = new SPacketEntityMetadata();
            packet.readPacketData(buffer);
            return packet;
        } catch (IOException e) {
            // Writing and reading an in-memory buffer does not fail; without the packet only the lead is resent.
            return null;
        } finally {
            buffer.release();
        }
    }
}
