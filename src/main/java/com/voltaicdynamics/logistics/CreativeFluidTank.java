package com.voltaicdynamics.logistics;

import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.capability.templates.FluidTank;

/**
 * The Creative Fluid Tank's storage. The first fluid it gets sets what it holds; from then on it is always full, gives
 * that fluid without running down, swallows more of it and refuses other fluids until it is cleared.
 */
public class CreativeFluidTank extends FluidTank {
    private final Runnable onChanged;

    public CreativeFluidTank(Runnable onChanged) {
        super(Integer.MAX_VALUE);
        this.onChanged = onChanged;
    }

    @Override
    public int fill(FluidStack resource, FluidAction action) {
        if (resource.isEmpty() || !isFluidValid(resource)) return 0;
        if (fluid.isEmpty()) {
            if (action.execute()) {
                fluid = resource.copyWithAmount(capacity);
                onContentsChanged();
            }
            return resource.getAmount();
        }
        return FluidStack.isSameFluidSameComponents(fluid, resource) ? resource.getAmount() : 0;
    }

    @Override
    public FluidStack drain(FluidStack resource, FluidAction action) {
        if (resource.isEmpty() || !FluidStack.isSameFluidSameComponents(fluid, resource)) return FluidStack.EMPTY;
        return drain(resource.getAmount(), action);
    }

    @Override
    public FluidStack drain(int maxDrain, FluidAction action) {
        if (fluid.isEmpty() || maxDrain <= 0) return FluidStack.EMPTY;
        return fluid.copyWithAmount(maxDrain);
    }

    @Override
    public void setFluid(FluidStack stack) {
        super.setFluid(stack.isEmpty() ? FluidStack.EMPTY : stack.copyWithAmount(capacity));
        onContentsChanged();
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
