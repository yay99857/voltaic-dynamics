package com.voltaicdynamics.core.sideconfig;

import net.minecraft.core.Direction;
import net.neoforged.neoforge.transfer.ResourceHandler;
import net.neoforged.neoforge.transfer.energy.EnergyHandler;
import net.neoforged.neoforge.transfer.fluid.FluidResource;
import net.neoforged.neoforge.transfer.item.ItemResource;
import net.neoforged.neoforge.transfer.resource.Resource;
import net.neoforged.neoforge.transfer.transaction.TransactionContext;
import org.jetbrains.annotations.Nullable;

/**
 * Capability views of one machine filtered by its {@link SideConfig}: an OFF face exposes nothing (so cables
 * and pipes do not even connect), an INPUT face only accepts, an OUTPUT face only gives. Views are cached per
 * face and rebuilt after the configuration changes ({@link #invalidate()}).
 */
public final class SidedHandlers {
    private final SideConfigurable owner;
    @Nullable
    private final ResourceHandler<ItemResource> items;
    @Nullable
    private final ResourceHandler<FluidResource> fluids;
    @Nullable
    private final EnergyHandler energy;
    private final Object[][] cache = new Object[3][6];

    public SidedHandlers(SideConfigurable owner, @Nullable ResourceHandler<ItemResource> items,
                         @Nullable ResourceHandler<FluidResource> fluids, @Nullable EnergyHandler energy) {
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
    public ResourceHandler<ItemResource> items(@Nullable Direction side) {
        return view(items, TransferType.ITEM, 0, side);
    }

    @Nullable
    public ResourceHandler<FluidResource> fluids(@Nullable Direction side) {
        return view(fluids, TransferType.FLUID, 1, side);
    }

    @SuppressWarnings("unchecked")
    @Nullable
    private <T extends Resource> ResourceHandler<T> view(@Nullable ResourceHandler<T> base, TransferType type, int row,
                                                         @Nullable Direction side) {
        if (base == null) return null;
        if (side == null) return base;
        IoMode mode = mode(type, side);
        if (mode == IoMode.NONE) return null;
        if (mode == IoMode.BOTH) return base;
        Object cached = cache[row][side.ordinal()];
        if (cached instanceof ResourceView<?> view && view.mode == mode) return (ResourceHandler<T>) view;
        ResourceView<T> view = new ResourceView<>(base, mode);
        cache[row][side.ordinal()] = view;
        return view;
    }

    @Nullable
    public EnergyHandler energy(@Nullable Direction side) {
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

    private record ResourceView<T extends Resource>(ResourceHandler<T> base, IoMode mode) implements ResourceHandler<T> {
        public int size() {
            return base.size();
        }

        public T getResource(int index) {
            return base.getResource(index);
        }

        public long getAmountAsLong(int index) {
            return base.getAmountAsLong(index);
        }

        public long getCapacityAsLong(int index, T resource) {
            return base.getCapacityAsLong(index, resource);
        }

        public boolean isValid(int index, T resource) {
            return mode.canInput() && base.isValid(index, resource);
        }

        public int insert(int index, T resource, int amount, TransactionContext transaction) {
            return mode.canInput() ? base.insert(index, resource, amount, transaction) : 0;
        }

        public int extract(int index, T resource, int amount, TransactionContext transaction) {
            return mode.canOutput() ? base.extract(index, resource, amount, transaction) : 0;
        }
    }

    private record EnergyView(EnergyHandler base, IoMode mode) implements EnergyHandler {
        public long getAmountAsLong() {
            return base.getAmountAsLong();
        }

        public long getCapacityAsLong() {
            return base.getCapacityAsLong();
        }

        public int insert(int amount, TransactionContext transaction) {
            return mode.canInput() ? base.insert(amount, transaction) : 0;
        }

        public int extract(int amount, TransactionContext transaction) {
            return mode.canOutput() ? base.extract(amount, transaction) : 0;
        }
    }
}
