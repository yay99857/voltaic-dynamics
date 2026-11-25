package com.voltaicdynamics.core.energy;

import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.neoforged.neoforge.transfer.energy.SimpleEnergyHandler;
import org.jetbrains.annotations.Nullable;

/**
 * FE buffer with a change callback. {@link #generateEnergy} and {@link #consumeEnergy} are the machine's own
 * internal operations and ignore the external insert/extract limits; they must not run inside a transaction.
 */
public class VoltaicEnergyStorage extends SimpleEnergyHandler {
    @Nullable
    private final Runnable onContentsChanged;

    public VoltaicEnergyStorage(int capacity, int maxReceive, int maxExtract, @Nullable Runnable onContentsChanged) {
        super(capacity, maxReceive, maxExtract, 0);
        this.onContentsChanged = onContentsChanged;
    }

    @Override
    protected void onEnergyChanged(int previousAmount) {
        changed();
    }

    public int getEnergyStored() {
        return energy;
    }

    public int getMaxEnergyStored() {
        return capacity;
    }

    /** Whether the outside may insert at all (face rules aside). */
    public boolean canReceive() {
        return maxInsert > 0;
    }

    /** Whether the outside may extract at all (face rules aside). */
    public boolean canExtract() {
        return maxExtract > 0;
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

    @Override
    public void serialize(ValueOutput output) {
        output.putInt("Energy", energy);
    }

    @Override
    public void deserialize(ValueInput input) {
        energy = Math.max(0, Math.min(input.getIntOr("Energy", 0), capacity));
    }
}
