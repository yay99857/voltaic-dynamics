package com.voltaicdynamics.core.transfer;

import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.transfer.item.ItemResource;
import net.neoforged.neoforge.transfer.item.ItemStacksResourceHandler;

/**
 * Slot inventory of a Voltaic block entity on NeoForge's transactional item handler, with the slot-style helpers
 * the machines use for their own logic ({@link #getStackInSlot}, {@link #setStackInSlot}). Override
 * {@link #isItemValid} and {@link #getSlotLimit} as with the old {@code ItemStackHandler};
 * {@link #onContentsChanged(int)} runs after every committed change.
 */
public class VoltaicItemHandler extends ItemStacksResourceHandler {
    public VoltaicItemHandler(int size) {
        super(size);
    }

    public int getSlots() {
        return size();
    }

    /**
     * The live stack in the slot. Menus change it in place and then call {@link #markChanged}; anything else should
     * use {@link #setStackInSlot}.
     */
    public ItemStack getStackInSlot(int slot) {
        return stacks.get(slot);
    }

    public void setStackInSlot(int slot, ItemStack stack) {
        set(slot, stack.isEmpty() ? ItemResource.EMPTY : ItemResource.of(stack), stack.isEmpty() ? 0 : stack.getCount());
    }

    /** Inserts into one slot in its own transaction; returns what did not fit. */
    public ItemStack insertItem(int slot, ItemStack stack, boolean simulate) {
        return Transfers.insertItem(this, slot, stack, simulate);
    }

    /** Takes up to {@code amount} from one slot in its own transaction. */
    public ItemStack extractItem(int slot, int amount, boolean simulate) {
        return Transfers.extractItem(this, slot, amount, simulate);
    }

    /** Most items a slot holds, before the item's own stack size. */
    public int getSlotLimit(int slot) {
        return Item.MAX_STACK;
    }

    public boolean isItemValid(int slot, ItemStack stack) {
        return true;
    }

    @Override
    public boolean isValid(int index, ItemResource resource) {
        return isItemValid(index, resource.toStack());
    }

    @Override
    protected int getCapacity(int index, ItemResource resource) {
        int limit = getSlotLimit(index);
        return resource.isEmpty() ? limit : Math.min(limit, resource.getMaxStackSize());
    }

    @Override
    protected void onContentsChanged(int index, ItemStack previousContents) {
        onContentsChanged(index);
    }

    protected void onContentsChanged(int slot) {
    }

    /** After a menu changed the live stack of a slot in place. */
    public void markChanged(int slot) {
        onContentsChanged(slot);
    }

    /** Constants that used to come from {@code Item}. */
    private static final class Item {
        static final int MAX_STACK = 64;
    }
}
