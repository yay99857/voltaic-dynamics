package com.voltaicdynamics.core.energy;

import net.minecraft.world.level.storage.ValueInput;
import net.neoforged.neoforge.transfer.TransferPreconditions;
import net.neoforged.neoforge.transfer.transaction.TransactionContext;

/**
 * A bottomless FE buffer for creative blocks: always full, gives up to its transfer rate without running down and
 * swallows whatever it is given.
 */
public class CreativeEnergyStorage extends VoltaicEnergyStorage {
    public CreativeEnergyStorage(int transfer) {
        super(Integer.MAX_VALUE, transfer, transfer, null);
        this.energy = Integer.MAX_VALUE;
    }

    @Override
    public int insert(int amount, TransactionContext transaction) {
        TransferPreconditions.checkNonNegative(amount);
        return Math.min(amount, maxInsert);
    }

    @Override
    public int extract(int amount, TransactionContext transaction) {
        TransferPreconditions.checkNonNegative(amount);
        return Math.min(amount, maxExtract);
    }

    @Override
    public long getAmountAsLong() {
        return Integer.MAX_VALUE;
    }

    @Override
    public int getEnergyStored() {
        return Integer.MAX_VALUE;
    }

    @Override
    public void setEnergy(int energy) {
        // always full
    }

    @Override
    public int generateEnergy(int amount) {
        return Math.max(0, amount);
    }

    @Override
    public boolean consumeEnergy(int amount) {
        return true;
    }

    /** Room to take more: always, since it voids it. */
    @Override
    public int getSpace() {
        return Integer.MAX_VALUE;
    }

    @Override
    public void deserialize(ValueInput input) {
        // always full
    }
}
