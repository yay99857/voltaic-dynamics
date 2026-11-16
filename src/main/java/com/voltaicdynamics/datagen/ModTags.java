package com.voltaicdynamics.datagen;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.ItemTags;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;

public final class ModTags {
    public static final TagKey<Item> INGOTS_VOLTAIC = common("ingots/voltaic");
    public static final TagKey<Item> INGOTS_CONDUCTIVE_ALLOY = common("ingots/conductive_alloy");
    public static final TagKey<Item> DUSTS_VOLTAIC = common("dusts/voltaic");
    public static final TagKey<Item> DUSTS_CONDUCTIVE_ALLOY = common("dusts/conductive_alloy");

    private ModTags() {
    }

    private static TagKey<Item> common(String path) {
        return ItemTags.create(ResourceLocation.fromNamespaceAndPath("c", path));
    }
}
