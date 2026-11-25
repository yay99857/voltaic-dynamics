package com.voltaicdynamics.logistics;

import com.voltaicdynamics.core.transfer.VoltaicFluidTank;

import net.minecraft.world.level.storage.ValueInput;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.transfer.TransferPreconditions;
import net.neoforged.neoforge.transfer.fluid.FluidResource;
import net.neoforged.neoforge.transfer.transaction.TransactionContext;

/**
 * The Creative Fluid Tank's storage. The first fluid it gets sets what it holds; from then on it is always full, gives
 * that fluid without running down, swallows more of it and refuses other fluids until it is cleared.
 */
public class CreativeFluidTank extends VoltaicFluidTank {
    private final Runnable onChanged;

    public CreativeFluidTank(Runnable onChanged) {
        super(Integer.MAX_VALUE);
        this.onChanged = onChanged;
    }

    @Override
    public int insert(int index, FluidResource resource, int amount, TransactionContext transaction) {
        TransferPreconditions.checkNonEmptyNonNegative(resource, amount);
        if (isEmpty()) {
            // Taking the first fluid goes through the journal, so a simulation leaves the tank empty.
            super.insert(index, resource, capacity, transaction);
            return amount;
        }
        return resource.matches(getFluid()) ? amount : 0;
    }

    @Override
    public int extract(int index, FluidResource resource, int amount, TransactionContext transaction) {
        TransferPreconditions.checkNonEmptyNonNegative(resource, amount);
        return !isEmpty() && resource.matches(getFluid()) ? amount : 0;
    }

    @Override
    public long getCapacityAsLong(int index, FluidResource resource) {
        return Integer.MAX_VALUE;
    }

    @Override
    public void setFluid(FluidStack stack) {
        super.setFluid(stack.isEmpty() ? FluidStack.EMPTY : stack.copyWithAmount(capacity));
    }

    @Override
    public void load(ValueInput input, String key) {
        super.load(input, key);
        if (!isEmpty()) stacks.set(0, getFluid().copyWithAmount(capacity));
    }

    /** Empties it, so it can take another fluid. */
    public void clear() {
        setFluid(FluidStack.EMPTY);
    }

    @Override
    protected void onContentsChanged() {
        onChanged.run();
    }
}
