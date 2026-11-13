package com.mahghuuuls.somecheesecontrol.feature.carry;

import com.mahghuuuls.somecheesecontrol.config.AllowedHand;
import com.mahghuuuls.somecheesecontrol.config.Settings;
import com.mahghuuuls.somecheesecontrol.core.PlayerExemptions;
import com.mahghuuuls.somecheesecontrol.core.Refusals;
import com.mahghuuuls.somecheesecontrol.core.Resync;
import com.mahghuuuls.somecheesecontrol.core.Rule;
import net.minecraft.entity.item.EntityItem;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.entity.player.InventoryPlayer;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.stats.StatList;
import net.minecraft.util.NonNullList;
import net.minecraftforge.event.entity.player.EntityItemPickupEvent;
import net.minecraftforge.fml.common.FMLCommonHandler;
import net.minecraftforge.fml.common.eventhandler.EventPriority;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;

import java.util.Set;

/**
 * Hand-only carrying: a restricted carried item anywhere in the player's own inventory except an
 * allowed hand is dropped, and picking one up puts it straight into an empty allowed hand.
 *
 * <p>Only the player's own inventory is checked: the item held on the cursor in an open screen,
 * chests and items inside other items (backpacks) are not. Dead players are skipped, so death drops
 * and keepInventory behave as without the mod.</p>
 *
 * <p>The drop moves like a player throw but does not post the toss event: under that event's
 * contract a mod that cancels a toss takes the item over, so putting it back could copy it. The item
 * leaves the inventory and enters the world as a vanilla drop does; what another mod does with a new
 * item entity (merging it, or refusing it) is handled as for any vanilla drop.</p>
 */
public final class CarryFeature {

    private final Set<Item> restricted;
    private final AllowedHand allowedHand;

    public CarryFeature(Settings settings) {
        this.restricted = settings.carriedItems();
        this.allowedHand = settings.allowedHand();
    }

    @SubscribeEvent
    public void onPlayerTick(TickEvent.PlayerTickEvent event) {
        EntityPlayer ticking = event.player;
        if (event.phase != TickEvent.Phase.END || ticking.world.isRemote || !(ticking instanceof EntityPlayerMP)
                || restricted.isEmpty() || ticking.isDead || ticking.getHealth() <= 0) {
            return;
        }
        EntityPlayerMP player = (EntityPlayerMP) ticking;
        if (!PlayerExemptions.applies(player, Rule.FORCED_DROP)) {
            return;
        }
        InventoryPlayer inventory = player.inventory;
        boolean dropped = dropOutsideHands(player, inventory.mainInventory, CarryRules.Section.MAIN, inventory.currentItem);
        dropped |= dropOutsideHands(player, inventory.offHandInventory, CarryRules.Section.OFFHAND, inventory.currentItem);
        dropped |= dropOutsideHands(player, inventory.armorInventory, CarryRules.Section.ARMOR, inventory.currentItem);
        if (dropped) {
            Refusals.correctClient(player, Resync.inventory());
        }
    }

    /** Drops every restricted item of {@code slots} outside an allowed hand; true when one was dropped. */
    private boolean dropOutsideHands(EntityPlayerMP player, NonNullList<ItemStack> slots, CarryRules.Section section,
                                     int selected) {
        boolean dropped = false;
        for (int i = 0; i < slots.size(); i++) {
            ItemStack stack = slots.get(i);
            if (stack.isEmpty() || !restricted.contains(stack.getItem())
                    || CarryRules.isAllowedSlot(section, i, selected, allowedHand)) {
                continue;
            }
            ItemStack forLog = stack.copy();
            slots.set(i, ItemStack.EMPTY);
            player.dropItem(stack, false, false);
            Refusals.refuse(player, Rule.FORCED_DROP, Resync.none(), forLog, null);
            dropped = true;
        }
        return dropped;
    }

    /**
     * A restricted item goes straight into an empty allowed hand, at most one full stack; the rest
     * stays on the ground. With no empty allowed hand the item stays on the ground. Vanilla would
     * otherwise put it in the first free slot, from which it would be dropped again.
     */
    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public void onItemPickup(EntityItemPickupEvent event) {
        EntityPlayer player = event.getEntityPlayer();
        EntityItem entity = event.getItem();
        ItemStack ground = entity.getItem();
        if (player.world.isRemote || ground.isEmpty() || !restricted.contains(ground.getItem())
                || !PlayerExemptions.applies(player, Rule.FORCED_DROP)) {
            return;
        }
        String owner = entity.getOwner();
        if (owner != null && !owner.equals(player.getName())) {
            return;
        }
        event.setCanceled(true);
        InventoryPlayer inventory = player.inventory;
        if (allowedHand.allowsMainHand() && inventory.mainInventory.get(inventory.currentItem).isEmpty()) {
            pickUpInto(player, entity, inventory.mainInventory, inventory.currentItem);
        } else if (allowedHand.allowsOffhand() && inventory.offHandInventory.get(0).isEmpty()) {
            pickUpInto(player, entity, inventory.offHandInventory, 0);
        }
    }

    /**
     * Moves up to one full stack from the item entity into {@code slots[index]}, then reports the
     * pickup as vanilla does: the slot is filled before the pickup event fires.
     */
    private static void pickUpInto(EntityPlayer player, EntityItem entity, NonNullList<ItemStack> slots, int index) {
        ItemStack ground = entity.getItem();
        Item item = ground.getItem();
        int taken = CarryRules.pickupCount(ground.getCount(), ground.getMaxStackSize(), player.inventory.getInventoryStackLimit());
        ItemStack inHand = ground.copy();
        inHand.setCount(taken);
        slots.set(index, inHand);
        ground.shrink(taken);
        if (!ground.isEmpty()) {
            entity.setItem(ground);
        }
        FMLCommonHandler.instance().firePlayerItemPickupEvent(player, entity, inHand.copy());
        if (ground.isEmpty()) {
            // As vanilla: the collect animation removes the entity on clients, so only for a full take.
            player.onItemPickup(entity, taken);
            entity.setDead();
        }
        player.addStat(StatList.getObjectsPickedUpStats(item), taken);
        if (player instanceof EntityPlayerMP) {
            Refusals.correctClient((EntityPlayerMP) player, Resync.inventory());
        }
    }
}
