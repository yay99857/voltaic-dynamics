package com.voltaicdynamics.datagen;

import net.minecraft.resources.Identifier;
import net.minecraft.tags.ItemTags;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;

public final class ModTags {
    public static final TagKey<Item> INGOTS_VOLTAIC = common("ingots/voltaic");
    /** Galvanic Alloy is a tier component like Mekanism's alloys, not an ingot, and has its own tag. */
    public static final TagKey<Item> ALLOYS_GALVANIC = ItemTags.create(
            Identifier.fromNamespaceAndPath("voltaicdynamics", "alloys/galvanic"));
    public static final TagKey<Item> DUSTS_VOLTAIC = common("dusts/voltaic");
    /** Our Pulverizer's dusts and other ore-processing mods' (Mekanism, Create...); NeoForge has no constant for them. */
    public static final TagKey<Item> DUSTS_IRON = common("dusts/iron");
    public static final TagKey<Item> DUSTS_COPPER = common("dusts/copper");
    public static final TagKey<Item> DUSTS_GOLD = common("dusts/gold");

    private ModTags() {
    }

    private static TagKey<Item> common(String path) {
        return ItemTags.create(Identifier.fromNamespaceAndPath("c", path));
    }
}
