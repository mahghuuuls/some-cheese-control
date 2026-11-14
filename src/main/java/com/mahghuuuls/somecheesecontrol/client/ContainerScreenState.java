package com.mahghuuuls.somecheesecontrol.client;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.inventory.GuiContainer;
import net.minecraft.inventory.Slot;
import net.minecraft.item.ItemStack;
import net.minecraftforge.fml.relauncher.ReflectionHelper;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.Collections;
import java.util.Set;

import javax.annotation.Nullable;

/**
 * Reads, and on a refused release resets, the click state a container screen keeps between a press
 * and its release. The screen keeps that state in private and protected fields, and the screen
 * placement check is an event handler, not a subclass, so it uses reflection, by the development and
 * the obfuscated names, found once.
 *
 * <p>When a name is not found (another mod replaced the screen's code), {@link #canReadDrag} or
 * {@link #ignoreRelease} answers false, so the caller refuses more and cancels the release instead
 * of ignoring it.</p>
 */
final class ContainerScreenState {

    private static final Logger LOGGER = LogManager.getLogger("SomeCheeseControl/Client");

    private boolean looked;
    @Nullable
    private Method slotAtPosition;
    @Nullable
    private Field dragSplitting;
    @Nullable
    private Field dragSplittingSlots;
    @Nullable
    private Field doubleClick;
    @Nullable
    private Field ignoreMouseUp;
    @Nullable
    private Field shiftClickedSlot;
    @Nullable
    private Field clickedSlot;

    /** The slot under a mouse event position, found by the screen's own lookup. */
    @Nullable
    Slot slotAt(GuiContainer gui, int eventX, int eventY) {
        Minecraft mc = Minecraft.getMinecraft();
        int x = eventX * gui.width / mc.displayWidth;
        int y = gui.height - eventY * gui.height / mc.displayHeight - 1;
        look();
        if (slotAtPosition != null) {
            try {
                return (Slot) slotAtPosition.invoke(gui, x, y);
            } catch (ReflectiveOperationException | RuntimeException e) {
                // Falls through to the vanilla rule below.
            }
        }
        for (Slot slot : gui.inventorySlots.inventorySlots) {
            int sx = x - gui.getGuiLeft();
            int sy = y - gui.getGuiTop();
            if (slot.isEnabled() && sx >= slot.xPos - 1 && sx < slot.xPos + 17 && sy >= slot.yPos - 1 && sy < slot.yPos + 17) {
                return slot;
            }
        }
        return null;
    }

    /** True when the screen's drag state can be read, so {@link #draggedSlots} is reliable. */
    boolean canReadDrag() {
        look();
        return dragSplitting != null && dragSplittingSlots != null;
    }

    /** The slots the screen's current drag took; empty when no drag is running or it cannot be read. */
    Set<Slot> draggedSlots(GuiContainer gui) {
        look();
        try {
            if (dragSplitting != null && dragSplittingSlots != null && dragSplitting.getBoolean(gui)) {
                @SuppressWarnings("unchecked")
                Set<Slot> slots = (Set<Slot>) dragSplittingSlots.get(gui);
                return slots;
            }
        } catch (IllegalAccessException e) {
            // Unreachable for accessible fields.
        }
        return Collections.emptySet();
    }

    /** True when the press before this release was the second click of a double-click. */
    boolean isDoubleClick(GuiContainer gui) {
        look();
        try {
            return doubleClick != null && doubleClick.getBoolean(gui);
        } catch (IllegalAccessException e) {
            return false;
        }
    }

    /** The item of the slot a shift-click took, which a shift double-click then gathers from the same inventory. */
    ItemStack shiftClickedItem(GuiContainer gui) {
        look();
        try {
            return shiftClickedSlot != null ? (ItemStack) shiftClickedSlot.get(gui) : ItemStack.EMPTY;
        } catch (IllegalAccessException e) {
            return ItemStack.EMPTY;
        }
    }

    /** In touch screen mode, the slot the press touched; its item goes to the slot of the release. */
    @Nullable
    Slot touchedSlot(GuiContainer gui) {
        look();
        try {
            return clickedSlot != null ? (Slot) clickedSlot.get(gui) : null;
        } catch (IllegalAccessException e) {
            return null;
        }
    }

    /**
     * Makes the coming release do nothing: ends the drag, the double-click and the touch, and marks
     * the release as one to ignore, as the screen does itself after a press that already acted.
     * False when a field it needs is missing; the drag and the touch are still ended with the fields
     * found, and the caller cancels the release.
     */
    boolean ignoreRelease(GuiContainer gui) {
        look();
        try {
            if (dragSplitting != null) {
                dragSplitting.setBoolean(gui, false);
            }
            if (dragSplittingSlots != null) {
                ((Set<?>) dragSplittingSlots.get(gui)).clear();
            }
            if (clickedSlot != null) {
                clickedSlot.set(gui, null);
            }
            if (doubleClick == null || ignoreMouseUp == null) {
                return false;
            }
            doubleClick.setBoolean(gui, false);
            ignoreMouseUp.setBoolean(gui, true);
            return true;
        } catch (IllegalAccessException e) {
            return false;
        }
    }

    private void look() {
        if (looked) {
            return;
        }
        looked = true;
        slotAtPosition = method("getSlotAtPosition", "func_146975_c");
        dragSplitting = field("dragSplitting", "field_147007_t");
        dragSplittingSlots = field("dragSplittingSlots", "field_147008_s");
        doubleClick = field("doubleClick", "field_146993_M");
        ignoreMouseUp = field("ignoreMouseUp", "field_146995_H");
        shiftClickedSlot = field("shiftClickedSlot", "field_146994_N");
        clickedSlot = field("clickedSlot", "field_147005_v");
    }

    @Nullable
    private static Field field(String name, String obfuscatedName) {
        try {
            return ReflectionHelper.findField(GuiContainer.class, name, obfuscatedName);
        } catch (ReflectionHelper.UnableToFindFieldException e) {
            LOGGER.warn("Screen placement check: field {} of the container screen not found; the check is less precise", name);
            return null;
        }
    }

    @Nullable
    private static Method method(String name, String obfuscatedName) {
        try {
            return ReflectionHelper.findMethod(GuiContainer.class, name, obfuscatedName, int.class, int.class);
        } catch (ReflectionHelper.UnableToFindMethodException e) {
            LOGGER.warn("Screen placement check: method {} of the container screen not found; using the vanilla slot rule", name);
            return null;
        }
    }
}
