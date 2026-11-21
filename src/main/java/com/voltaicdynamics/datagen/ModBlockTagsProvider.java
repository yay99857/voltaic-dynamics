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
import net.neoforged.neoforge.common.data.ExistingFileHelper;

import java.util.concurrent.CompletableFuture;

public class ModBlockTagsProvider extends BlockTagsProvider {
    public ModBlockTagsProvider(PackOutput output, CompletableFuture<HolderLookup.Provider> lookup, ExistingFileHelper files) {
        super(output, lookup, VoltaicDynamics.MOD_ID, files);
    }

    @Override
    protected void addTags(HolderLookup.Provider provider) {
        // Without this tag, requiresCorrectToolForDrops() makes the machines drop nothing.
        tag(BlockTags.MINEABLE_WITH_PICKAXE).add(
                VoltaicGenerators.THERMOELECTRIC_GENERATOR.get(),
                VoltaicGenerators.COMBUSTION_GENERATOR.get(),
                VoltaicMachines.ELECTRIC_FURNACE.get(),
                VoltaicPower.BASIC_ACCUMULATOR.get(),
                VoltaicPower.ADVANCED_ACCUMULATOR.get(),
                VoltaicPower.ELITE_ACCUMULATOR.get(),
                VoltaicPower.ULTIMATE_ACCUMULATOR.get(),
                VoltaicPower.CREATIVE_ACCUMULATOR.get(),
                VoltaicPower.VOLTAIC_CABLE.get(),
                VoltaicMachines.ALLOY_SMELTER.get(),
                VoltaicMachines.PULVERIZER.get(),
                VoltaicGenerators.FLUID_GENERATOR.get(),
                VoltaicLogistics.FLUID_TANK.get(),
                VoltaicLogistics.ADVANCED_FLUID_TANK.get(),
                VoltaicLogistics.ELITE_FLUID_TANK.get(),
                VoltaicLogistics.ULTIMATE_FLUID_TANK.get(),
                VoltaicLogistics.CREATIVE_FLUID_TANK.get(),
                VoltaicLogistics.ITEM_CABLE.get(),
                VoltaicLogistics.FLUID_CABLE.get());
    }
}
