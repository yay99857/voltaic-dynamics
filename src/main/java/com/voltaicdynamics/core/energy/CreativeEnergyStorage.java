package com.voltaicdynamics.core.energy;

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
    public int receiveEnergy(int maxReceive, boolean simulate) {
        return Math.max(0, Math.min(maxReceive, this.maxReceive));
    }

    @Override
    public int extractEnergy(int maxExtract, boolean simulate) {
        return Math.max(0, Math.min(maxExtract, this.maxExtract));
    }

    @Override
    public int getEnergyStored() {
        return Integer.MAX_VALUE;
    }

    @Override
    public boolean canReceive() {
        return true;
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
}
