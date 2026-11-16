package com.voltaicdynamics.datagen;

import com.voltaicdynamics.VoltaicDynamics;
import com.voltaicdynamics.item.ModItems;
import net.minecraft.core.HolderLookup;
import net.minecraft.data.PackOutput;
import net.minecraft.data.tags.ItemTagsProvider;
import net.minecraft.world.level.block.Block;
import net.neoforged.neoforge.common.Tags;
import net.neoforged.neoforge.common.data.ExistingFileHelper;

import java.util.concurrent.CompletableFuture;

public class ModItemTagsProvider extends ItemTagsProvider {
    public ModItemTagsProvider(PackOutput output, CompletableFuture<HolderLookup.Provider> lookup,
                               CompletableFuture<TagLookup<Block>> blockTags, ExistingFileHelper files) {
        super(output, lookup, blockTags, VoltaicDynamics.MOD_ID, files);
    }

    @Override
    protected void addTags(HolderLookup.Provider provider) {
        tag(ModTags.INGOTS_VOLTAIC).add(ModItems.VOLTAIC_INGOT.get());
        tag(ModTags.INGOTS_CONDUCTIVE_ALLOY).add(ModItems.CONDUCTIVE_ALLOY.get());
        tag(ModTags.DUSTS_VOLTAIC).add(ModItems.VOLTAIC_DUST.get());
        tag(ModTags.DUSTS_CONDUCTIVE_ALLOY).add(ModItems.CONDUCTIVE_DUST.get());

        tag(Tags.Items.INGOTS).addTag(ModTags.INGOTS_VOLTAIC).addTag(ModTags.INGOTS_CONDUCTIVE_ALLOY);
        tag(Tags.Items.DUSTS).addTag(ModTags.DUSTS_VOLTAIC).addTag(ModTags.DUSTS_CONDUCTIVE_ALLOY);
    }
}
