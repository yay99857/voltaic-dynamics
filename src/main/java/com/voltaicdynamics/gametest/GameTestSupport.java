package com.voltaicdynamics.gametest;

import com.voltaicdynamics.core.energy.VoltaicEnergyStorage;
import com.voltaicdynamics.machines.ProcessingMachineBlockEntity;
import com.voltaicdynamics.power.AccumulatorBlockEntity;

/** Helpers shared by the game test classes. */
final class GameTestSupport {
    private GameTestSupport() {
    }

    static final String EMPTY = "empty";

    static VoltaicEnergyStorage storageOf(AccumulatorBlockEntity cell) {
        return (VoltaicEnergyStorage) cell.getEnergyStorage(null);
    }

    static void power(ProcessingMachineBlockEntity machine, int amount) {
        ((VoltaicEnergyStorage) machine.getEnergyStorage(null)).setEnergy(amount);
    }
}
