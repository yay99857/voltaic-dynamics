package com.voltaicdynamics.core.menu;

import com.voltaicdynamics.core.transfer.Transfers;
import com.voltaicdynamics.core.transfer.VoltaicItemHandler;

import net.minecraft.world.Container;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.transfer.ResourceHandler;
import net.neoforged.neoforge.transfer.StacksResourceHandler;
import net.neoforged.neoforge.transfer.item.ItemResource;

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
    @Nullable
    private final ResourceHandler<ItemResource> handler;
    @Nullable
    private final Container backing;

    public FixedSlot(ResourceHandler<ItemResource> handler, int index, int x, int y) {
        super(new SimpleContainer(0), index, x, y);
        this.handler = handler;
        this.backing = null;
    }

    public FixedSlot(Container container, int index, int x, int y) {
        super(new SimpleContainer(0), index, x, y);
        this.handler = null;
        this.backing = container;
    }

    @Override
    public ItemStack getItem() {
        int slot = getContainerSlot();
        if (backing != null) return backing.getItem(slot);
        // Menus change the slot's stack in place (shift-click merges into it), so ours must be the live one; a copy
        // would take the items from the player and drop them on the floor of nowhere.
        if (handler instanceof VoltaicItemHandler own) return own.getStackInSlot(slot);
        return Transfers.getStack(handler, slot);
    }

    @Override
    public void set(ItemStack stack) {
        int slot = getContainerSlot();
        if (backing != null) {
            backing.setItem(slot, stack);
        } else if (handler instanceof StacksResourceHandler<?, ItemResource> stacks) {
            stacks.set(slot, ItemResource.of(stack), stack.getCount());
        } else {
            Transfers.extractItem(handler, slot, Integer.MAX_VALUE, false);
            if (!stack.isEmpty()) Transfers.insertItem(handler, slot, stack, false);
        }
        setChanged();
    }

    @Override
    public ItemStack remove(int amount) {
        int slot = getContainerSlot();
        if (backing != null) return backing.removeItem(slot, amount);
        return Transfers.extractItem(handler, slot, amount, false);
    }

    @Override
    public boolean mayPlace(ItemStack stack) {
        int slot = getContainerSlot();
        if (backing != null) return backing.canPlaceItem(slot, stack);
        return !stack.isEmpty() && handler.isValid(slot, ItemResource.of(stack));
    }

    @Override
    public boolean mayPickup(Player player) {
        int slot = getContainerSlot();
        if (backing != null) return !backing.getItem(slot).isEmpty();
        return !Transfers.extractItem(handler, slot, 1, true).isEmpty();
    }

    @Override
    public int getMaxStackSize() {
        if (backing != null) return backing.getMaxStackSize();
        return handler.getCapacityAsInt(getContainerSlot(), ItemResource.EMPTY);
    }

    @Override
    public int getMaxStackSize(ItemStack stack) {
        if (backing != null) return Math.min(backing.getMaxStackSize(), stack.getMaxStackSize());
        return stack.isEmpty() ? getMaxStackSize() : handler.getCapacityAsInt(getContainerSlot(), ItemResource.of(stack));
    }

    @Override
    public void setChanged() {
        if (backing != null) backing.setChanged();
        else if (handler instanceof VoltaicItemHandler own) own.markChanged(getContainerSlot());
    }
}
