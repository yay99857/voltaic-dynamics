package com.voltaicdynamics.datagen;

import com.voltaicdynamics.VoltaicDynamics;
import com.voltaicdynamics.block.ModBlocks;
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
                ModBlocks.THERMOELECTRIC_GENERATOR.get(),
                ModBlocks.COMBUSTION_GENERATOR.get(),
                ModBlocks.ELECTRIC_FURNACE.get(),
                ModBlocks.BASIC_ENERGY_CELL.get(),
                ModBlocks.ADVANCED_ENERGY_CELL.get(),
                ModBlocks.ENERGY_CABLE.get());
    }
}
