package com.voltaicdynamics.core.menu;

import net.minecraft.world.Container;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.items.IItemHandler;
import net.neoforged.neoforge.items.IItemHandlerModifiable;
import net.neoforged.neoforge.items.wrapper.InvWrapper;

import javax.annotation.Nullable;

/**
 * A slot that inventory-sorting mods leave alone: machine inputs, outputs, upgrades and battery slots.
 * <p>
 * Inventory Tweaks Refoxed sorts every slot of an open menu that is not the player's, as one chest, unless the slot is
 * a plain slot over an empty container (and, from its newer versions, an item-handler slot that cannot be written to).
 * So this slot sits on an empty container of its own and reads and writes its real inventory itself: clicking,
 * shift-clicking, hoppers and the screen see the items as before, while sorters skip it.
 */
public class FixedSlot extends Slot {
    private final IItemHandler handler;
    @Nullable
    private final Container backing;

    public FixedSlot(IItemHandler handler, int index, int x, int y) {
        this(handler, null, index, x, y);
    }

    public FixedSlot(Container container, int index, int x, int y) {
        this(new InvWrapper(container), container, index, x, y);
    }

    private FixedSlot(IItemHandler handler, @Nullable Container backing, int index, int x, int y) {
        super(new SimpleContainer(0), index, x, y);
        this.handler = handler;
        this.backing = backing;
    }

    @Override
    public ItemStack getItem() {
        return handler.getStackInSlot(getContainerSlot());
    }

    @Override
    public void set(ItemStack stack) {
        int slot = getContainerSlot();
        if (handler instanceof IItemHandlerModifiable modifiable) {
            modifiable.setStackInSlot(slot, stack);
        } else {
            handler.extractItem(slot, Integer.MAX_VALUE, false);
            if (!stack.isEmpty()) handler.insertItem(slot, stack, false);
        }
        setChanged();
    }

    @Override
    public ItemStack remove(int amount) {
        return handler.extractItem(getContainerSlot(), amount, false);
    }

    @Override
    public boolean mayPlace(ItemStack stack) {
        return handler.isItemValid(getContainerSlot(), stack);
    }

    @Override
    public boolean mayPickup(Player player) {
        return !handler.extractItem(getContainerSlot(), 1, true).isEmpty();
    }

    @Override
    public int getMaxStackSize() {
        return handler.getSlotLimit(getContainerSlot());
    }

    @Override
    public void setChanged() {
        if (backing != null) backing.setChanged();
    }
}
