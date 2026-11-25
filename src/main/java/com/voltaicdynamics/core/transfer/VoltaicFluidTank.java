package com.voltaicdynamics.core.transfer;

import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.transfer.fluid.FluidResource;
import net.neoforged.neoforge.transfer.fluid.FluidStacksResourceHandler;

import java.util.function.Predicate;

/**
 * A single tank on NeoForge's transactional fluid handler, with the helpers the old {@code FluidTank} had.
 * {@link #onContentsChanged()} runs after every committed change.
 */
public class VoltaicFluidTank extends FluidStacksResourceHandler {
    private final Predicate<FluidStack> validator;

    public VoltaicFluidTank(int capacity) {
        this(capacity, stack -> true);
    }

    public VoltaicFluidTank(int capacity, Predicate<FluidStack> validator) {
        super(1, capacity);
        this.validator = validator;
    }

    public FluidStack getFluid() {
        return stacks.get(0);
    }

    public int getFluidAmount() {
        return stacks.get(0).getAmount();
    }

    public int getCapacity() {
        return capacity;
    }

    public void setCapacity(int capacity) {
        this.capacity = capacity;
    }

    public boolean isEmpty() {
        return stacks.get(0).isEmpty();
    }

    public int getSpace() {
        return Math.max(0, capacity - getFluidAmount());
    }

    public boolean isFluidValid(FluidStack stack) {
        return validator.test(stack);
    }

    public void setFluid(FluidStack stack) {
        set(0, stack.isEmpty() ? FluidResource.EMPTY : FluidResource.of(stack), stack.isEmpty() ? 0 : stack.getAmount());
    }

    @Override
    public boolean isValid(int index, FluidResource resource) {
        return validator.test(resource.toStack(1));
    }

    @Override
    protected void onContentsChanged(int index, FluidStack previousContents) {
        onContentsChanged();
    }

    protected void onContentsChanged() {
    }

    /** Saved as a "Fluid" child, so a missing tank loads empty. */
    public void save(ValueOutput output, String key) {
        if (!isEmpty()) output.store(key, FluidStack.CODEC, getFluid());
    }

    public void load(ValueInput input, String key) {
        FluidStack loaded = input.read(key, FluidStack.CODEC).orElse(FluidStack.EMPTY);
        stacks.set(0, loaded.isEmpty() ? FluidStack.EMPTY : loaded.copyWithAmount(Math.min(loaded.getAmount(), capacity)));
    }
}
