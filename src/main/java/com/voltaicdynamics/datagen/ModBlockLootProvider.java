package com.voltaicdynamics.datagen;

import com.voltaicdynamics.VoltaicDynamics;
import com.voltaicdynamics.core.ModDataComponents;
import com.voltaicdynamics.generators.VoltaicGenerators;
import com.voltaicdynamics.logistics.VoltaicLogistics;
import com.voltaicdynamics.machines.VoltaicMachines;
import com.voltaicdynamics.power.VoltaicPower;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.HolderLookup;
import net.minecraft.data.loot.BlockLootSubProvider;
import net.minecraft.world.flag.FeatureFlags;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.storage.loot.LootPool;
import net.minecraft.world.level.storage.loot.LootTable;
import net.minecraft.world.level.storage.loot.entries.LootItem;
import net.minecraft.world.level.storage.loot.functions.CopyComponentsFunction;
import net.minecraft.world.level.storage.loot.providers.number.ConstantValue;

import java.util.Set;

public class ModBlockLootProvider extends BlockLootSubProvider {
    public ModBlockLootProvider(HolderLookup.Provider registries) {
        super(Set.of(), FeatureFlags.REGISTRY.allFlags(), registries);
    }

    @Override
    protected void generate() {
        dropSelf(VoltaicGenerators.THERMOELECTRIC_GENERATOR.get());
        dropSelf(VoltaicGenerators.COMBUSTION_GENERATOR.get());
        add(VoltaicMachines.ELECTRIC_FURNACE.get(), block -> keeping(block, ModDataComponents.MACHINE_TIER.get()));
        dropSelf(VoltaicPower.VOLTAIC_CABLE.get());
        add(VoltaicMachines.ALLOY_SMELTER.get(), block -> keeping(block, ModDataComponents.MACHINE_TIER.get()));
        add(VoltaicMachines.PULVERIZER.get(), block -> keeping(block, ModDataComponents.MACHINE_TIER.get()));
        dropSelf(VoltaicGenerators.FLUID_GENERATOR.get());
        dropSelf(VoltaicLogistics.ITEM_CABLE.get());
        dropSelf(VoltaicLogistics.FLUID_CABLE.get());
        for (var tank : VoltaicLogistics.tanks()) add(tank.get(), block -> keeping(block, ModDataComponents.FLUID.get()));
        add(VoltaicPower.BASIC_ACCUMULATOR.get(), this::cellKeepingEnergy);
        add(VoltaicPower.ADVANCED_ACCUMULATOR.get(), this::cellKeepingEnergy);
        add(VoltaicPower.ELITE_ACCUMULATOR.get(), this::cellKeepingEnergy);
        add(VoltaicPower.ULTIMATE_ACCUMULATOR.get(), this::cellKeepingEnergy);
        dropSelf(VoltaicPower.CREATIVE_ACCUMULATOR.get());
    }

    /** The dropped cell carries the energy it had stored. */
    private LootTable.Builder cellKeepingEnergy(Block block) {
        return keeping(block, ModDataComponents.ENERGY.get());
    }

    /** Drops the block with one data component copied from its block entity (energy, fluid). */
    private LootTable.Builder keeping(Block block, net.minecraft.core.component.DataComponentType<?> component) {
        return LootTable.lootTable().withPool(applyExplosionCondition(block, LootPool.lootPool()
                .setRolls(ConstantValue.exactly(1.0F))
                .add(LootItem.lootTableItem(block)
                        .apply(CopyComponentsFunction.copyComponents(CopyComponentsFunction.Source.BLOCK_ENTITY)
                                .include(component)))));
    }

    @Override
    protected Iterable<Block> getKnownBlocks() {
        // Every module registers its own blocks, so collect them by namespace.
        return BuiltInRegistries.BLOCK.stream()
                .filter(block -> BuiltInRegistries.BLOCK.getKey(block).getNamespace().equals(VoltaicDynamics.MOD_ID))
                .toList();
    }
}
