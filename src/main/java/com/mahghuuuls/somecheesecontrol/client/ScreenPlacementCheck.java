package com.mahghuuuls.somecheesecontrol.client;

import com.mahghuuuls.somecheesecontrol.config.Settings;
import com.mahghuuuls.somecheesecontrol.core.PlayerExemptions;
import com.mahghuuuls.somecheesecontrol.core.Rule;
import com.mahghuuuls.somecheesecontrol.feature.carry.CarryRules;
import com.mahghuuuls.somecheesecontrol.feature.carry.CarryRules.SlotKind;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.client.gui.inventory.GuiContainer;
import net.minecraft.client.gui.inventory.GuiContainerCreative;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.inventory.Container;
import net.minecraft.inventory.ContainerChest;
import net.minecraft.inventory.ContainerDispenser;
import net.minecraft.inventory.ContainerHopper;
import net.minecraft.inventory.ContainerShulkerBox;
import net.minecraft.inventory.Slot;
import net.minecraft.item.ItemStack;
import net.minecraftforge.client.event.GuiOpenEvent;
import net.minecraftforge.client.event.GuiScreenEvent;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import org.lwjgl.input.Keyboard;
import org.lwjgl.input.Mouse;

import java.util.Arrays;
import java.util.Set;

import javax.annotation.Nullable;

/**
 * Stops a screen action that would put a restricted carried item into a slot of the client's own
 * player's inventory outside an allowed hand: a click, the end of a drag, a shift-click (also a shift
 * double-click) and a number key. The action does nothing and shows no message, as when an item does
 * not fit a slot.
 *
 * <p>Forge has no event for a slot click, so the check reads the screen's mouse and keyboard input
 * just before the screen turns it into clicks it sends to the server, and decides from the screen's
 * own state at that moment. With an item on the cursor, a press only starts a drag and the release
 * places the item: into the slots the drag took, or else into the slot under the pointer. A refused
 * release is let through with the screen's drag ended and the release marked as one to ignore, the
 * way the screen ends a drag itself, so the press and release stay paired. Without an item on the
 * cursor, a shift-click acts at the press; that press is refused, and its release is ignored.</p>
 *
 * <p>A shift-click from the player's own slot is let through only in the vanilla storage screens,
 * whose shift-click always takes such an item out of the player's inventory; other screens (a
 * crafting table, a furnace, a trader) can move it between the hotbar and the main inventory.</p>
 *
 * <p>The Creative inventory screen keeps its own slots and is not checked; the server's carry rule
 * still applies there, as it does when another mod's screen action gets past this check.</p>
 */
public final class ScreenPlacementCheck {

    /** Per mouse button: its press was refused, so its release must do nothing. */
    private final boolean[] refusedPress = new boolean[16];

    private final ContainerScreenState screen = new ContainerScreenState();

    private ScreenPlacementCheck() {
    }

    /** Registers the check. Call once, at initialization, on the physical client only. */
    public static void register() {
        MinecraftForge.EVENT_BUS.register(new ScreenPlacementCheck());
    }

    @SubscribeEvent
    public void onScreenChange(GuiOpenEvent event) {
        Arrays.fill(refusedPress, false);
    }

    @SubscribeEvent
    public void onMouseInput(GuiScreenEvent.MouseInputEvent.Pre event) {
        GuiContainer gui = checkedScreen(event.getGui());
        EntityPlayer player = Minecraft.getMinecraft().player;
        Settings rules = gui != null && player != null ? carryRules(player) : null;
        int button = Mouse.getEventButton();
        if (rules == null || button >= refusedPress.length) {
            return;
        }
        if (button < 0) {
            // A pointer move. Only touch screen mode acts on it: holding a touched stack over a slot
            // splits one item into that slot. Pointer moves are frequent, so the slot is looked up
            // only then.
            Slot touched = Minecraft.getMinecraft().gameSettings.touchscreen ? screen.touchedSlot(gui) : null;
            if (touched != null && isRestricted(rules, touched.getStack())) {
                Slot slot = screen.slotAt(gui, Mouse.getEventX(), Mouse.getEventY());
                if (touched != slot && refusesPlacing(rules, player, touched.getStack(), slot)) {
                    event.setCanceled(true);
                }
            }
            return;
        }
        Slot slot = screen.slotAt(gui, Mouse.getEventX(), Mouse.getEventY());
        if (Mouse.getEventButtonState()) {
            refusedPress[button] = false;
            if (player.inventory.getItemStack().isEmpty() && GuiScreen.isShiftKeyDown()
                    && refusesQuickMove(rules, player, gui, slot, slot != null ? slot.getStack() : ItemStack.EMPTY)) {
                event.setCanceled(true);
                refusedPress[button] = true;
            }
            return;
        }
        boolean refused = refusedPress[button] || refusesRelease(rules, player, gui, slot, button);
        refusedPress[button] = false;
        if (refused && !screen.ignoreRelease(gui)) {
            event.setCanceled(true);
        }
    }

    /** A number key swaps the pointed slot with a hotbar slot. */
    @SubscribeEvent
    public void onKeyboardInput(GuiScreenEvent.KeyboardInputEvent.Pre event) {
        GuiContainer gui = checkedScreen(event.getGui());
        EntityPlayer player = Minecraft.getMinecraft().player;
        Settings rules = gui != null && player != null ? carryRules(player) : null;
        if (rules == null || !Keyboard.getEventKeyState() || !player.inventory.getItemStack().isEmpty()) {
            return;
        }
        Slot pointed = gui.getSlotUnderMouse();
        if (pointed == null) {
            return;
        }
        int key = Keyboard.getEventKey();
        for (int hotbar = 0; hotbar < 9; hotbar++) {
            if (!Minecraft.getMinecraft().gameSettings.keyBindsHotbar[hotbar].isActiveAndMatches(key)) {
                continue;
            }
            boolean hotbarIsHand = CarryRules.ownSlotKind(hotbar, player.inventory.currentItem, rules.allowedHand())
                    == SlotKind.ALLOWED_HAND;
            if (CarryRules.refusesHotbarSwap(isRestricted(rules, pointed.getStack()), kindOf(rules, player, pointed),
                    isRestricted(rules, player.inventory.mainInventory.get(hotbar)), hotbarIsHand)) {
                event.setCanceled(true);
            }
            return;
        }
    }

    /**
     * True when this release could put a restricted item outside the hands by any of the paths the
     * screen may take for it: a shift double-click (which moves every matching item of the pointed
     * slot's inventory), in touch screen mode the move of the touched slot's item to the pointed slot,
     * and the placing of the cursor item (into the drag's slots, or else into the slot under the
     * pointer, by a click or a shift-click). Every path is judged, not only the one the screen will
     * take, so the order of the screen's own branches does not matter; refusing a release whose other
     * path was harmless only makes that release do nothing.
     */
    private boolean refusesRelease(Settings rules, EntityPlayer player, GuiContainer gui, @Nullable Slot slot, int button) {
        boolean shift = GuiScreen.isShiftKeyDown();
        if (button == 0 && shift && screen.isDoubleClick(gui)
                && refusesQuickMove(rules, player, gui, slot, screen.shiftClickedItem(gui))) {
            return true;
        }
        Slot touched = Minecraft.getMinecraft().gameSettings.touchscreen ? screen.touchedSlot(gui) : null;
        if (touched != null && touched != slot && refusesPlacing(rules, player, touched.getStack(), slot)) {
            return true;
        }
        ItemStack cursor = player.inventory.getItemStack();
        if (cursor.isEmpty()) {
            return false;
        }
        if (!screen.canReadDrag()) {
            // Without the drag's slots, any release with a restricted item on the cursor may place it.
            return isRestricted(rules, cursor);
        }
        // A drag that took slots places only into them; the slot under the pointer counts only without one.
        Set<Slot> dragged = screen.draggedSlots(gui);
        if (!dragged.isEmpty()) {
            for (Slot target : dragged) {
                if (refusesPlacing(rules, player, cursor, target)) {
                    return true;
                }
            }
            return false;
        }
        return shift ? refusesQuickMove(rules, player, gui, slot, slot != null ? slot.getStack() : ItemStack.EMPTY)
                : refusesPlacing(rules, player, cursor, slot);
    }

    /** The screen when it is a container screen this check covers; otherwise null. */
    @Nullable
    private static GuiContainer checkedScreen(GuiScreen screen) {
        return screen instanceof GuiContainer && !(screen instanceof GuiContainerCreative) ? (GuiContainer) screen : null;
    }

    /** The server's rules when hand-only carrying applies to this player; otherwise null. */
    @Nullable
    private static Settings carryRules(EntityPlayer player) {
        Settings rules = Settings.forSide(true);
        if (rules == null || !rules.handOnlyCarrying() || rules.carriedItems().isEmpty()
                || !PlayerExemptions.applies(player, Rule.FORCED_DROP)) {
            return null;
        }
        return rules;
    }

    private static boolean isRestricted(Settings rules, ItemStack stack) {
        return !stack.isEmpty() && rules.carriedItems().contains(stack.getItem());
    }

    private static SlotKind kindOf(Settings rules, EntityPlayer player, @Nullable Slot slot) {
        if (slot == null || slot.inventory != player.inventory) {
            return SlotKind.NOT_OWN;
        }
        return CarryRules.ownSlotKind(slot.getSlotIndex(), player.inventory.currentItem, rules.allowedHand());
    }

    private static boolean refusesPlacing(Settings rules, EntityPlayer player, ItemStack item, @Nullable Slot slot) {
        return CarryRules.refusesPlacing(isRestricted(rules, item), kindOf(rules, player, slot));
    }

    /** A shift-click on {@code slot} moving {@code item}: the slot's item or, for a double-click, the remembered one. */
    private static boolean refusesQuickMove(Settings rules, EntityPlayer player, GuiContainer gui, @Nullable Slot slot,
                                            ItemStack item) {
        if (slot == null) {
            return false;
        }
        boolean fromOwnSlot = slot.inventory == player.inventory;
        return CarryRules.refusesQuickMove(isRestricted(rules, item), fromOwnSlot,
                fromOwnSlot && isStorageScreen(gui.inventorySlots));
    }

    /**
     * The vanilla storage containers, whose shift-click moves an item from the player's inventory
     * only into the container. Exact classes: a subclass may move items differently.
     */
    private static boolean isStorageScreen(Container container) {
        Class<?> type = container.getClass();
        return type == ContainerChest.class || type == ContainerShulkerBox.class || type == ContainerHopper.class
                || type == ContainerDispenser.class;
    }
}
