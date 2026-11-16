package com.voltaicdynamics.datagen;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.ItemTags;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;

public final class ModTags {
    public static final TagKey<Item> INGOTS_VOLTAIC = common("ingots/voltaic");
    /** Galvanic Alloy is a tier component like Mekanism's alloys, not an ingot, and has its own tag. */
    public static final TagKey<Item> ALLOYS_GALVANIC = ItemTags.create(
            ResourceLocation.fromNamespaceAndPath("voltaicdynamics", "alloys/galvanic"));
    public static final TagKey<Item> DUSTS_VOLTAIC = common("dusts/voltaic");
    /** Filled by ore-processing mods (Mekanism, Create, Ender IO, All the Ores); NeoForge has no constant for them. */
    public static final TagKey<Item> DUSTS_IRON = common("dusts/iron");
    public static final TagKey<Item> DUSTS_COPPER = common("dusts/copper");
    public static final TagKey<Item> DUSTS_GOLD = common("dusts/gold");
    public static final TagKey<Item> DUSTS_GALVANIC_ALLOY = common("dusts/galvanic_alloy");

    private ModTags() {
    }

    private static TagKey<Item> common(String path) {
        return ItemTags.create(ResourceLocation.fromNamespaceAndPath("c", path));
    }
}
