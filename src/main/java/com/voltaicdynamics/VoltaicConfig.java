package com.voltaicdynamics;

import net.neoforged.neoforge.common.ModConfigSpec;

/**
 * Server config (per world, synced to clients). Every value has a safe default that is
 * returned while the config is not loaded yet (main menu, creative tab search, datagen).
 */
public final class VoltaicConfig {
    public static final ModConfigSpec SPEC;

    public static final ModConfigSpec.IntValue THERMO_MAX_GENERATION;
    public static final ModConfigSpec.IntValue THERMO_CAPACITY;
    public static final ModConfigSpec.DoubleValue THERMO_WEAR_CHANCE;

    public static final ModConfigSpec.IntValue COMBUSTION_GENERATION;
    public static final ModConfigSpec.IntValue COMBUSTION_CAPACITY;

    public static final ModConfigSpec.IntValue FURNACE_ENERGY_PER_TICK;
    public static final ModConfigSpec.IntValue FURNACE_TICKS_PER_ITEM;
    public static final ModConfigSpec.IntValue FURNACE_CAPACITY;

    public static final ModConfigSpec.IntValue BASIC_CELL_CAPACITY;
    public static final ModConfigSpec.IntValue BASIC_CELL_TRANSFER;
    public static final ModConfigSpec.IntValue ADVANCED_CELL_CAPACITY;
    public static final ModConfigSpec.IntValue ADVANCED_CELL_TRANSFER;

    public static final ModConfigSpec.IntValue CABLE_TRANSFER;

    public static final ModConfigSpec.IntValue CAPACITOR_CAPACITY;
    public static final ModConfigSpec.IntValue CAPACITOR_TRANSFER;

    static {
        ModConfigSpec.Builder b = new ModConfigSpec.Builder();

        b.push("thermoelectric_generator");
        THERMO_MAX_GENERATION = b.comment("Maximum FE/t a single Thermoelectric Generator can produce.")
                .defineInRange("maxGeneration", 100, 1, 1_000_000);
        THERMO_CAPACITY = b.comment("Internal buffer in FE.")
                .defineInRange("capacity", 50_000, 1, Integer.MAX_VALUE);
        THERMO_WEAR_CHANCE = b.comment(
                        "Chance, checked once per second for every adjacent heat/cold block, that the block degrades",
                        "(lava -> obsidian, magma -> netherrack, blue ice -> packed ice -> ice -> water, snow melts).",
                        "Default 0.0003 means roughly one hour per block. Set to 0 for infinite free power.")
                .defineInRange("wearChance", 0.0003, 0.0, 1.0);
        b.pop();

        b.push("combustion_generator");
        COMBUSTION_GENERATION = b.comment("FE/t produced while burning fuel.")
                .defineInRange("generation", 40, 1, 32_000);
        COMBUSTION_CAPACITY = b.comment("Internal buffer in FE.")
                .defineInRange("capacity", 100_000, 1, Integer.MAX_VALUE);
        b.pop();

        b.push("electric_furnace");
        FURNACE_ENERGY_PER_TICK = b.comment("FE/t consumed while smelting.")
                .defineInRange("energyPerTick", 20, 0, 32_000);
        FURNACE_TICKS_PER_ITEM = b.comment("Ticks to smelt one item (a vanilla furnace takes 200).")
                .defineInRange("ticksPerItem", 100, 1, 32_000);
        FURNACE_CAPACITY = b.comment("Internal buffer in FE.")
                .defineInRange("capacity", 20_000, 1, Integer.MAX_VALUE);
        b.pop();

        b.push("energy_cells");
        BASIC_CELL_CAPACITY = b.defineInRange("basicCapacity", 1_000_000, 1, Integer.MAX_VALUE);
        BASIC_CELL_TRANSFER = b.defineInRange("basicTransfer", 2_000, 1, Integer.MAX_VALUE);
        ADVANCED_CELL_CAPACITY = b.defineInRange("advancedCapacity", 8_000_000, 1, Integer.MAX_VALUE);
        ADVANCED_CELL_TRANSFER = b.defineInRange("advancedTransfer", 10_000, 1, Integer.MAX_VALUE);
        b.pop();

        b.push("energy_cable");
        CABLE_TRANSFER = b.comment("Max FE/t through each connection between the cable network and a block.")
                .defineInRange("transferPerConnection", 2_000, 1, Integer.MAX_VALUE);
        b.pop();

        b.push("pocket_capacitor");
        CAPACITOR_CAPACITY = b.defineInRange("capacity", 100_000, 1, Integer.MAX_VALUE);
        CAPACITOR_TRANSFER = b.comment("Max FE/t in and out, shared between all items it charges.")
                .defineInRange("transfer", 500, 1, Integer.MAX_VALUE);
        b.pop();

        SPEC = b.build();
    }

    private VoltaicConfig() {
    }

    public static int get(ModConfigSpec.IntValue value) {
        return SPEC.isLoaded() ? value.getAsInt() : value.getDefault();
    }

    public static double get(ModConfigSpec.DoubleValue value) {
        return SPEC.isLoaded() ? value.getAsDouble() : value.getDefault();
    }
}
