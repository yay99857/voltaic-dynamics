package com.voltaicdynamics.core.transfer;

import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.transfer.access.ItemAccess;
import net.neoforged.neoforge.transfer.ResourceHandler;
import net.neoforged.neoforge.transfer.ResourceHandlerUtil;
import net.neoforged.neoforge.transfer.energy.EnergyHandler;
import net.neoforged.neoforge.transfer.fluid.FluidResource;
import net.neoforged.neoforge.transfer.item.ItemResource;
import net.neoforged.neoforge.transfer.item.ItemStacksResourceHandler;
import net.neoforged.neoforge.transfer.transaction.Transaction;
import org.jetbrains.annotations.Nullable;

/**
 * One-shot transfers on NeoForge's transactional handlers, for code that runs outside any transaction (ticks,
 * menu clicks). Each call opens its own transaction and commits it unless {@code simulate} is set, which is how
 * the old {@code simulate} / {@code FluidAction.SIMULATE} flags map onto the transfer API.
 */
public final class Transfers {
    private Transfers() {
    }

    /**
     * A transaction nested in the one open on this thread, or a root one. Simulations can then run from inside
     * another handler's insert (a cable asking a neighbour whether it takes energy).
     */
    @SuppressWarnings("deprecation")
    private static Transaction open() {
        return Transaction.open(Transaction.getCurrentOpenedTransaction());
    }

    // ---- energy ----

    public static int insertEnergy(@Nullable EnergyHandler handler, int amount, boolean simulate) {
        if (handler == null || amount <= 0) return 0;
        try (Transaction tx = open()) {
            int inserted = handler.insert(amount, tx);
            if (!simulate) tx.commit();
            return inserted;
        }
    }

    public static int extractEnergy(@Nullable EnergyHandler handler, int amount, boolean simulate) {
        if (handler == null || amount <= 0) return 0;
        try (Transaction tx = open()) {
            int extracted = handler.extract(amount, tx);
            if (!simulate) tx.commit();
            return extracted;
        }
    }

    /** Whether the handler would take any energy right now. */
    public static boolean canInsertEnergy(@Nullable EnergyHandler handler) {
        return insertEnergy(handler, 1, true) > 0;
    }

    /** Whether the handler would give any energy right now. */
    public static boolean canExtractEnergy(@Nullable EnergyHandler handler) {
        return extractEnergy(handler, 1, true) > 0;
    }

    public static int energyStored(@Nullable EnergyHandler handler) {
        return handler == null ? 0 : handler.getAmountAsInt();
    }

    public static int energyCapacity(@Nullable EnergyHandler handler) {
        return handler == null ? 0 : handler.getCapacityAsInt();
    }

    /** The energy handler of the item in a slot; changes it makes are written back into the slot. */
    @Nullable
    public static EnergyHandler itemEnergy(ResourceHandler<ItemResource> handler, int slot) {
        if (handler.getResource(slot).isEmpty()) return null;
        return ItemAccess.forHandlerIndex(handler, slot).getCapability(Capabilities.Energy.ITEM);
    }

    /** The energy handler of a lone stack, for reading it or checking it; changes go to that stack only. */
    @Nullable
    public static EnergyHandler itemEnergy(ItemStack stack) {
        if (stack.isEmpty()) return null;
        return ItemAccess.forStack(stack).getCapability(Capabilities.Energy.ITEM);
    }

    // ---- items ----

    /** Inserts into one slot; returns what did not fit. */
    public static ItemStack insertItem(ResourceHandler<ItemResource> handler, int slot, ItemStack stack, boolean simulate) {
        if (stack.isEmpty()) return ItemStack.EMPTY;
        try (Transaction tx = open()) {
            int inserted = handler.insert(slot, ItemResource.of(stack), stack.getCount(), tx);
            if (!simulate) tx.commit();
            return stack.copyWithCount(stack.getCount() - inserted);
        }
    }

    /** Inserts anywhere, filling matching stacks first; returns what did not fit. */
    public static ItemStack insertItemStacked(@Nullable ResourceHandler<ItemResource> handler, ItemStack stack, boolean simulate) {
        if (handler == null || stack.isEmpty()) return stack;
        try (Transaction tx = open()) {
            int inserted = ResourceHandlerUtil.insertStacking(handler, ItemResource.of(stack), stack.getCount(), tx);
            if (!simulate) tx.commit();
            return stack.copyWithCount(stack.getCount() - inserted);
        }
    }

    /** Takes up to {@code amount} of whatever is in the slot. */
    public static ItemStack extractItem(ResourceHandler<ItemResource> handler, int slot, int amount, boolean simulate) {
        ItemResource resource = handler.getResource(slot);
        if (resource.isEmpty() || amount <= 0) return ItemStack.EMPTY;
        try (Transaction tx = open()) {
            int extracted = handler.extract(slot, resource, amount, tx);
            if (!simulate) tx.commit();
            return extracted <= 0 ? ItemStack.EMPTY : resource.toStack(extracted);
        }
    }

    public static ItemStack getStack(ResourceHandler<ItemResource> handler, int slot) {
        ItemResource resource = handler.getResource(slot);
        return resource.isEmpty() ? ItemStack.EMPTY : resource.toStack(handler.getAmountAsInt(slot));
    }

    // ---- fluids ----

    /** Whether the stack holds or can hold fluid (a bucket, a tank item). */
    public static boolean isFluidContainer(ItemStack stack) {
        return !stack.isEmpty() && ItemAccess.forStack(stack).getCapability(Capabilities.Fluid.ITEM) != null;
    }

    /**
     * Empties the container in {@code slot} into {@code tank}, or fills it from the tank when that moves nothing, and
     * then moves the container to {@code outSlot}. Nothing happens unless the out slot can take the container
     * afterwards. The slots are set directly, so the out slot may refuse items to pipes. Returns the mB moved.
     */
    public static int processContainer(VoltaicItemHandler items, int slot, int outSlot, ResourceHandler<FluidResource> tank,
                                       boolean allowFill) {
        ItemStack input = items.getStackInSlot(slot);
        if (input.isEmpty()) return 0;
        // The container works on a scratch slot: the in slot only takes full containers, so it would refuse the
        // empty bucket a lava bucket turns into.
        ItemStacksResourceHandler scratch = new ItemStacksResourceHandler(1);
        scratch.set(0, ItemResource.of(input), input.getCount());
        ResourceHandler<FluidResource> container =
                ItemAccess.forHandlerIndex(scratch, 0).getCapability(Capabilities.Fluid.ITEM);
        if (container == null) return 0;
        ItemStack after;
        int moved;
        try (Transaction tx = open()) {
            moved = ResourceHandlerUtil.move(container, tank, r -> true, Integer.MAX_VALUE, tx);
            if (moved <= 0 && allowFill) moved = ResourceHandlerUtil.move(tank, container, r -> true, Integer.MAX_VALUE, tx);
            if (moved <= 0) return 0;
            after = getStack(scratch, 0);
            ItemStack output = items.getStackInSlot(outSlot);
            if (!after.isEmpty() && !output.isEmpty() && (!ItemStack.isSameItemSameComponents(output, after)
                    || output.getCount() + after.getCount() > output.getMaxStackSize())) {
                return 0; // aborted: the out slot cannot take it
            }
            tx.commit();
        }
        items.setStackInSlot(slot, ItemStack.EMPTY);
        if (!after.isEmpty()) {
            ItemStack output = items.getStackInSlot(outSlot);
            items.setStackInSlot(outSlot, output.isEmpty() ? after : output.copyWithCount(output.getCount() + after.getCount()));
        }
        return moved;
    }

    /** Fills the handler; returns the amount accepted. */
    public static int fill(@Nullable ResourceHandler<FluidResource> handler, FluidStack stack, boolean simulate) {
        if (handler == null || stack.isEmpty()) return 0;
        try (Transaction tx = open()) {
            int inserted = handler.insert(FluidResource.of(stack), stack.getAmount(), tx);
            if (!simulate) tx.commit();
            return inserted;
        }
    }

    /** Drains up to the stack's amount of this exact fluid. */
    public static FluidStack drain(@Nullable ResourceHandler<FluidResource> handler, FluidStack stack, boolean simulate) {
        if (handler == null || stack.isEmpty()) return FluidStack.EMPTY;
        FluidResource resource = FluidResource.of(stack);
        try (Transaction tx = open()) {
            int extracted = handler.extract(resource, stack.getAmount(), tx);
            if (!simulate) tx.commit();
            return extracted <= 0 ? FluidStack.EMPTY : resource.toStack(extracted);
        }
    }

    /** Drains up to {@code amount} of the first fluid the handler holds. */
    public static FluidStack drain(@Nullable ResourceHandler<FluidResource> handler, int amount, boolean simulate) {
        if (handler == null || amount <= 0) return FluidStack.EMPTY;
        for (int i = 0; i < handler.size(); i++) {
            FluidResource resource = handler.getResource(i);
            if (resource.isEmpty() || handler.getAmountAsLong(i) <= 0) continue;
            try (Transaction tx = open()) {
                int extracted = handler.extract(resource, amount, tx);
                if (extracted <= 0) continue;
                if (!simulate) tx.commit();
                return resource.toStack(extracted);
            }
        }
        return FluidStack.EMPTY;
    }

    public static FluidStack getFluid(ResourceHandler<FluidResource> handler, int tank) {
        FluidResource resource = handler.getResource(tank);
        return resource.isEmpty() ? FluidStack.EMPTY : resource.toStack(handler.getAmountAsInt(tank));
    }
}
