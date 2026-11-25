package com.voltaicdynamics.datagen;

import com.voltaicdynamics.VoltaicDynamics;
import com.voltaicdynamics.core.VoltaicCore;
import com.voltaicdynamics.gadgets.VoltaicGadgets;

import net.minecraft.core.HolderLookup;
import net.minecraft.data.PackOutput;
import net.neoforged.neoforge.common.data.ItemTagsProvider;
import net.minecraft.world.level.block.Block;
import net.neoforged.neoforge.common.Tags;

import java.util.concurrent.CompletableFuture;

public class ModItemTagsProvider extends ItemTagsProvider {
    public ModItemTagsProvider(PackOutput output, CompletableFuture<HolderLookup.Provider> lookup) {
        super(output, lookup, VoltaicDynamics.MOD_ID);
    }

    @Override
    protected void addTags(HolderLookup.Provider provider) {
        tag(ModTags.INGOTS_VOLTAIC).add(VoltaicCore.VOLTAIC_INGOT.getKey());
        tag(ModTags.ALLOYS_GALVANIC).add(VoltaicCore.GALVANIC_ALLOY.getKey());
        tag(ModTags.DUSTS_VOLTAIC).add(VoltaicCore.VOLTAIC_DUST.getKey());

        tag(Tags.Items.TOOLS_WRENCH).add(VoltaicGadgets.VOLTAIC_WRENCH.getKey());
        tag(Tags.Items.INGOTS).addTag(ModTags.INGOTS_VOLTAIC);
        tag(ModTags.DUSTS_IRON).add(VoltaicCore.IRON_DUST.getKey());
        tag(ModTags.DUSTS_COPPER).add(VoltaicCore.COPPER_DUST.getKey());
        tag(ModTags.DUSTS_GOLD).add(VoltaicCore.GOLD_DUST.getKey());
        tag(Tags.Items.DUSTS).addTag(ModTags.DUSTS_VOLTAIC)
                .addTag(ModTags.DUSTS_IRON).addTag(ModTags.DUSTS_COPPER).addTag(ModTags.DUSTS_GOLD);
    }
}
