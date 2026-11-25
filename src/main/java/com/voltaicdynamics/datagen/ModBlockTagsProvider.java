package com.voltaicdynamics.datagen;

import com.voltaicdynamics.VoltaicDynamics;
import com.voltaicdynamics.generators.VoltaicGenerators;
import com.voltaicdynamics.logistics.VoltaicLogistics;
import com.voltaicdynamics.machines.VoltaicMachines;
import com.voltaicdynamics.power.VoltaicPower;

import net.minecraft.core.HolderLookup;
import net.minecraft.data.PackOutput;
import net.minecraft.tags.BlockTags;
import net.neoforged.neoforge.common.data.BlockTagsProvider;

import java.util.concurrent.CompletableFuture;

public class ModBlockTagsProvider extends BlockTagsProvider {
    public ModBlockTagsProvider(PackOutput output, CompletableFuture<HolderLookup.Provider> lookup) {
        super(output, lookup, VoltaicDynamics.MOD_ID);
    }

    @Override
    protected void addTags(HolderLookup.Provider provider) {
        // Without this tag, requiresCorrectToolForDrops() makes the machines drop nothing.
        tag(BlockTags.MINEABLE_WITH_PICKAXE).add(
                VoltaicGenerators.THERMOELECTRIC_GENERATOR.getKey(),
                VoltaicGenerators.COMBUSTION_GENERATOR.getKey(),
                VoltaicMachines.ELECTRIC_FURNACE.getKey(),
                VoltaicPower.BASIC_ACCUMULATOR.getKey(),
                VoltaicPower.ADVANCED_ACCUMULATOR.getKey(),
                VoltaicPower.ELITE_ACCUMULATOR.getKey(),
                VoltaicPower.ULTIMATE_ACCUMULATOR.getKey(),
                VoltaicPower.CREATIVE_ACCUMULATOR.getKey(),
                VoltaicPower.VOLTAIC_CABLE.getKey(),
                VoltaicMachines.ALLOY_SMELTER.getKey(),
                VoltaicMachines.PULVERIZER.getKey(),
                VoltaicGenerators.FLUID_GENERATOR.getKey(),
                VoltaicLogistics.FLUID_TANK.getKey(),
                VoltaicLogistics.ADVANCED_FLUID_TANK.getKey(),
                VoltaicLogistics.ELITE_FLUID_TANK.getKey(),
                VoltaicLogistics.ULTIMATE_FLUID_TANK.getKey(),
                VoltaicLogistics.CREATIVE_FLUID_TANK.getKey(),
                VoltaicLogistics.ITEM_CABLE.getKey(),
                VoltaicLogistics.FLUID_CABLE.getKey());
    }
}
