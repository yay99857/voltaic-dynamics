package com.voltaicdynamics.energy;

import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.neoforged.neoforge.energy.EnergyStorage;

public class VoltaicEnergyStorage extends EnergyStorage {
    private final Runnable onContentsChanged;

    public VoltaicEnergyStorage(int capacity, int maxReceive, int maxExtract, int energy, Runnable onContentsChanged) {
        super(capacity, maxReceive, maxExtract, energy);
        this.onContentsChanged = onContentsChanged;
    }

    public VoltaicEnergyStorage(int capacity, int maxTransfer, Runnable onContentsChanged) {
        this(capacity, maxTransfer, maxTransfer, 0, onContentsChanged);
    }

    @Override
    public int receiveEnergy(int maxReceive, boolean simulate) {
        int received = super.receiveEnergy(maxReceive, simulate);
        if (received > 0 && !simulate && onContentsChanged != null) {
            onContentsChanged.run();
        }
        return received;
    }

    @Override
    public int extractEnergy(int maxExtract, boolean simulate) {
        int extracted = super.extractEnergy(maxExtract, simulate);
        if (extracted > 0 && !simulate && onContentsChanged != null) {
            onContentsChanged.run();
        }
        return extracted;
    }

    public void setEnergy(int energy) {
        this.energy = Math.max(0, Math.min(energy, capacity));
        if (onContentsChanged != null) {
            onContentsChanged.run();
        }
    }

    public int generateEnergy(int amount) {
        int energyReceived = Math.min(capacity - energy, amount);
        this.energy += energyReceived;
        if (energyReceived > 0 && onContentsChanged != null) {
            onContentsChanged.run();
        }
        return energyReceived;
    }

    public CompoundTag serializeNBT(HolderLookup.Provider registries) {
        CompoundTag tag = new CompoundTag();
        tag.putInt("Energy", this.energy);
        tag.putInt("Capacity", this.capacity);
        tag.putInt("MaxReceive", this.maxReceive);
        tag.putInt("MaxExtract", this.maxExtract);
        return tag;
    }

    public void deserializeNBT(HolderLookup.Provider registries, CompoundTag tag) {
        if (tag.contains("Energy")) {
            this.energy = tag.getInt("Energy");
        }
    }
}
