package com.mahghuuuls.somecheesecontrol.feature.carry;

import com.mahghuuuls.somecheesecontrol.config.AllowedHand;
import com.mahghuuuls.somecheesecontrol.config.Settings;
import com.mahghuuuls.somecheesecontrol.core.DiagnosticLog;
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

import java.util.Locale;
import java.util.Set;

/**
 * Hand-only carrying: a restricted carried item anywhere in the player's own inventory except an
 * allowed hand moves into the offhand when the offhand is an allowed hand and empty, and is dropped
 * otherwise; picking one up puts it straight into an empty allowed hand. In a screen, a client with
 * this mod does not let the player put one outside an allowed hand in the first place.
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
        boolean changed = moveOrDrop(player, inventory.mainInventory, CarryRules.Section.MAIN);
        changed |= moveOrDrop(player, inventory.offHandInventory, CarryRules.Section.OFFHAND);
        changed |= moveOrDrop(player, inventory.armorInventory, CarryRules.Section.ARMOR);
        if (changed) {
            Refusals.correctClient(player, Resync.inventory());
        }
    }

    /**
     * Moves or drops every restricted item of {@code slots} outside an allowed hand; true when one
     * was moved or dropped. A move keeps the same stack, so nothing is copied or lost, and shows no
     * message; a drop is refused like any rule.
     */
    private boolean moveOrDrop(EntityPlayerMP player, NonNullList<ItemStack> slots, CarryRules.Section section) {
        InventoryPlayer inventory = player.inventory;
        boolean changed = false;
        for (int i = 0; i < slots.size(); i++) {
            ItemStack stack = slots.get(i);
            if (stack.isEmpty() || !restricted.contains(stack.getItem())) {
                continue;
            }
            CarryRules.Outcome outcome = CarryRules.outcome(section, i, inventory.currentItem, allowedHand,
                    inventory.offHandInventory.get(0).isEmpty());
            if (outcome == CarryRules.Outcome.STAY) {
                continue;
            }
            slots.set(i, ItemStack.EMPTY);
            if (outcome == CarryRules.Outcome.MOVE_TO_OFFHAND) {
                inventory.offHandInventory.set(0, stack);
                DiagnosticLog.decision("move", "player", player.getName(), "item", stack.getItem().getRegistryName(),
                        "from", section.name().toLowerCase(Locale.ROOT) + ":" + i, "to", "offhand");
            } else {
                ItemStack forLog = stack.copy();
                player.dropItem(stack, false, false);
                Refusals.refuse(player, Rule.FORCED_DROP, Resync.none(), forLog, null);
            }
            changed = true;
        }
        return changed;
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
