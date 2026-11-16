package com.voltaicdynamics.core.energy;

import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.neoforged.neoforge.energy.EnergyStorage;

/**
 * FE buffer with a change callback. {@link #generateEnergy} and {@link #consumeEnergy} are the
 * machine's own internal operations and ignore the external receive/extract limits.
 */
public class VoltaicEnergyStorage extends EnergyStorage {
    private final Runnable onContentsChanged;

    public VoltaicEnergyStorage(int capacity, int maxReceive, int maxExtract, Runnable onContentsChanged) {
        super(capacity, maxReceive, maxExtract, 0);
        this.onContentsChanged = onContentsChanged;
    }

    @Override
    public int receiveEnergy(int maxReceive, boolean simulate) {
        int received = super.receiveEnergy(maxReceive, simulate);
        if (received > 0 && !simulate) changed();
        return received;
    }

    @Override
    public int extractEnergy(int maxExtract, boolean simulate) {
        int extracted = super.extractEnergy(maxExtract, simulate);
        if (extracted > 0 && !simulate) changed();
        return extracted;
    }

    public void setEnergy(int energy) {
        int clamped = Math.max(0, Math.min(energy, capacity));
        if (clamped != this.energy) {
            this.energy = clamped;
            changed();
        }
    }

    public int generateEnergy(int amount) {
        int received = Math.max(0, Math.min(capacity - energy, amount));
        if (received > 0) {
            energy += received;
            changed();
        }
        return received;
    }

    public boolean consumeEnergy(int amount) {
        if (amount <= 0) return true;
        if (energy < amount) return false;
        energy -= amount;
        changed();
        return true;
    }

    public int getSpace() {
        return capacity - energy;
    }

    private void changed() {
        if (onContentsChanged != null) onContentsChanged.run();
    }

    public CompoundTag serializeNBT(HolderLookup.Provider registries) {
        CompoundTag tag = new CompoundTag();
        tag.putInt("Energy", energy);
        return tag;
    }

    public void deserializeNBT(HolderLookup.Provider registries, CompoundTag tag) {
        if (tag.contains("Energy")) {
            energy = Math.max(0, Math.min(tag.getInt("Energy"), capacity));
        }
    }
}
