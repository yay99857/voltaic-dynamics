package com.voltaicdynamics.datagen;

import com.voltaicdynamics.ModDataComponents;
import com.voltaicdynamics.block.ModBlocks;
import net.minecraft.core.Holder;
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
        dropSelf(ModBlocks.THERMOELECTRIC_GENERATOR.get());
        dropSelf(ModBlocks.COMBUSTION_GENERATOR.get());
        dropSelf(ModBlocks.ELECTRIC_FURNACE.get());
        dropSelf(ModBlocks.ENERGY_CABLE.get());
        add(ModBlocks.BASIC_ENERGY_CELL.get(), this::cellKeepingEnergy);
        add(ModBlocks.ADVANCED_ENERGY_CELL.get(), this::cellKeepingEnergy);
    }

    /** The dropped cell carries the energy it had stored. */
    private LootTable.Builder cellKeepingEnergy(Block block) {
        return LootTable.lootTable().withPool(applyExplosionCondition(block, LootPool.lootPool()
                .setRolls(ConstantValue.exactly(1.0F))
                .add(LootItem.lootTableItem(block)
                        .apply(CopyComponentsFunction.copyComponents(CopyComponentsFunction.Source.BLOCK_ENTITY)
                                .include(ModDataComponents.ENERGY.get())))));
    }

    @Override
    protected Iterable<Block> getKnownBlocks() {
        return ModBlocks.BLOCKS.getEntries().stream().map(Holder::value).map(Block.class::cast)::iterator;
    }
}
