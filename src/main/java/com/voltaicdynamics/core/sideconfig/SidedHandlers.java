package com.voltaicdynamics.core.sideconfig;

import net.minecraft.core.Direction;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.energy.IEnergyStorage;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;
import net.neoforged.neoforge.items.IItemHandler;
import org.jetbrains.annotations.Nullable;

/**
 * Capability views of one machine filtered by its {@link SideConfig}: an OFF face exposes nothing (so cables
 * and pipes do not even connect), an INPUT face only accepts, an OUTPUT face only gives. Views are cached per
 * face and rebuilt after the configuration changes ({@link #invalidate()}).
 */
public final class SidedHandlers {
    private final SideConfigurable owner;
    @Nullable
    private final IItemHandler items;
    @Nullable
    private final IFluidHandler fluids;
    @Nullable
    private final IEnergyStorage energy;
    private final Object[][] cache = new Object[3][6];

    public SidedHandlers(SideConfigurable owner, @Nullable IItemHandler items, @Nullable IFluidHandler fluids,
                         @Nullable IEnergyStorage energy) {
        this.owner = owner;
        this.items = items;
        this.fluids = fluids;
        this.energy = energy;
    }

    public void invalidate() {
        for (Object[] row : cache) java.util.Arrays.fill(row, null);
    }

    private IoMode mode(TransferType type, Direction side) {
        return owner.getSideConfig().mode(type, side, owner.getFacing());
    }

    @Nullable
    public IItemHandler items(@Nullable Direction side) {
        if (items == null) return null;
        if (side == null) return items;
        IoMode mode = mode(TransferType.ITEM, side);
        if (mode == IoMode.NONE) return null;
        if (mode == IoMode.BOTH) return items;
        Object cached = cache[0][side.ordinal()];
        if (cached instanceof ItemView view && view.mode == mode) return view;
        ItemView view = new ItemView(items, mode);
        cache[0][side.ordinal()] = view;
        return view;
    }

    @Nullable
    public IFluidHandler fluids(@Nullable Direction side) {
        if (fluids == null) return null;
        if (side == null) return fluids;
        IoMode mode = mode(TransferType.FLUID, side);
        if (mode == IoMode.NONE) return null;
        if (mode == IoMode.BOTH) return fluids;
        Object cached = cache[1][side.ordinal()];
        if (cached instanceof FluidView view && view.mode == mode) return view;
        FluidView view = new FluidView(fluids, mode);
        cache[1][side.ordinal()] = view;
        return view;
    }

    @Nullable
    public IEnergyStorage energy(@Nullable Direction side) {
        if (energy == null) return null;
        if (side == null) return energy;
        IoMode mode = mode(TransferType.ENERGY, side);
        if (mode == IoMode.NONE) return null;
        if (mode == IoMode.BOTH) return energy;
        Object cached = cache[2][side.ordinal()];
        if (cached instanceof EnergyView view && view.mode == mode) return view;
        EnergyView view = new EnergyView(energy, mode);
        cache[2][side.ordinal()] = view;
        return view;
    }

    private record ItemView(IItemHandler base, IoMode mode) implements IItemHandler {
        public int getSlots() {
            return base.getSlots();
        }

        public ItemStack getStackInSlot(int slot) {
            return base.getStackInSlot(slot);
        }

        public ItemStack insertItem(int slot, ItemStack stack, boolean simulate) {
            return mode.canInput() ? base.insertItem(slot, stack, simulate) : stack;
        }

        public ItemStack extractItem(int slot, int amount, boolean simulate) {
            return mode.canOutput() ? base.extractItem(slot, amount, simulate) : ItemStack.EMPTY;
        }

        public int getSlotLimit(int slot) {
            return base.getSlotLimit(slot);
        }

        public boolean isItemValid(int slot, ItemStack stack) {
            return mode.canInput() && base.isItemValid(slot, stack);
        }
    }

    private record FluidView(IFluidHandler base, IoMode mode) implements IFluidHandler {
        public int getTanks() {
            return base.getTanks();
        }

        public FluidStack getFluidInTank(int tank) {
            return base.getFluidInTank(tank);
        }

        public int getTankCapacity(int tank) {
            return base.getTankCapacity(tank);
        }

        public boolean isFluidValid(int tank, FluidStack stack) {
            return mode.canInput() && base.isFluidValid(tank, stack);
        }

        public int fill(FluidStack resource, FluidAction action) {
            return mode.canInput() ? base.fill(resource, action) : 0;
        }

        public FluidStack drain(FluidStack resource, FluidAction action) {
            return mode.canOutput() ? base.drain(resource, action) : FluidStack.EMPTY;
        }

        public FluidStack drain(int maxDrain, FluidAction action) {
            return mode.canOutput() ? base.drain(maxDrain, action) : FluidStack.EMPTY;
        }
    }

    private record EnergyView(IEnergyStorage base, IoMode mode) implements IEnergyStorage {
        public int receiveEnergy(int maxReceive, boolean simulate) {
            return mode.canInput() ? base.receiveEnergy(maxReceive, simulate) : 0;
        }

        public int extractEnergy(int maxExtract, boolean simulate) {
            return mode.canOutput() ? base.extractEnergy(maxExtract, simulate) : 0;
        }

        public int getEnergyStored() {
            return base.getEnergyStored();
        }

        public int getMaxEnergyStored() {
            return base.getMaxEnergyStored();
        }

        public boolean canExtract() {
            return mode.canOutput() && base.canExtract();
        }

        public boolean canReceive() {
            return mode.canInput() && base.canReceive();
        }
    }
}
