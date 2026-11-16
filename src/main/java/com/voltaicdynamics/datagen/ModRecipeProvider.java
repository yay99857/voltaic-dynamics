package com.voltaicdynamics.datagen;

import com.voltaicdynamics.VoltaicDynamics;
import com.voltaicdynamics.block.ModBlocks;
import com.voltaicdynamics.item.ModItems;
import net.minecraft.core.HolderLookup;
import net.minecraft.data.PackOutput;
import net.minecraft.data.recipes.RecipeCategory;
import net.minecraft.data.recipes.RecipeOutput;
import net.minecraft.data.recipes.RecipeProvider;
import net.minecraft.data.recipes.ShapedRecipeBuilder;
import net.minecraft.data.recipes.ShapelessRecipeBuilder;
import net.minecraft.data.recipes.SimpleCookingRecipeBuilder;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.level.ItemLike;
import net.neoforged.neoforge.common.Tags;

import java.util.concurrent.CompletableFuture;

/**
 * Voltaic ingots and conductive alloy come only from smelting their dust, so the dust step
 * matters and no recipe shares its ingredients with another.
 */
public class ModRecipeProvider extends RecipeProvider {
    public ModRecipeProvider(PackOutput output, CompletableFuture<HolderLookup.Provider> lookup) {
        super(output, lookup);
    }

    @Override
    protected void buildRecipes(RecipeOutput output) {
        // Dusts
        ShapelessRecipeBuilder.shapeless(RecipeCategory.MISC, ModItems.VOLTAIC_DUST.get(), 2)
                .requires(Tags.Items.INGOTS_IRON)
                .requires(Tags.Items.INGOTS_COPPER)
                .requires(Tags.Items.DUSTS_REDSTONE)
                .unlockedBy("has_copper", has(Tags.Items.INGOTS_COPPER))
                .save(output);
        ShapelessRecipeBuilder.shapeless(RecipeCategory.MISC, ModItems.CONDUCTIVE_DUST.get(), 2)
                .requires(ModTags.INGOTS_VOLTAIC)
                .requires(Tags.Items.INGOTS_GOLD)
                .requires(Tags.Items.DUSTS_REDSTONE)
                .unlockedBy("has_voltaic_ingot", has(ModTags.INGOTS_VOLTAIC))
                .save(output);

        // Dust -> ingot
        cook(output, ModTags.DUSTS_VOLTAIC, ModItems.VOLTAIC_INGOT.get(), 0.7F, "voltaic_ingot");
        cook(output, ModTags.DUSTS_CONDUCTIVE_ALLOY, ModItems.CONDUCTIVE_ALLOY.get(), 0.8F, "conductive_alloy");

        // Components
        ShapedRecipeBuilder.shaped(RecipeCategory.MISC, ModItems.THERMOPILE_CORE.get())
                .pattern(" C ").pattern("RIR").pattern(" C ")
                .define('C', Tags.Items.INGOTS_COPPER)
                .define('R', Tags.Items.DUSTS_REDSTONE)
                .define('I', ModTags.INGOTS_VOLTAIC)
                .unlockedBy("has_voltaic_ingot", has(ModTags.INGOTS_VOLTAIC))
                .save(output);
        ShapedRecipeBuilder.shaped(RecipeCategory.TOOLS, ModItems.POCKET_CAPACITOR.get())
                .pattern(" R ").pattern("CVC").pattern(" R ")
                .define('R', Tags.Items.DUSTS_REDSTONE)
                .define('C', Tags.Items.INGOTS_COPPER)
                .define('V', ModTags.INGOTS_VOLTAIC)
                .unlockedBy("has_voltaic_ingot", has(ModTags.INGOTS_VOLTAIC))
                .save(output);

        // Generators
        ShapedRecipeBuilder.shaped(RecipeCategory.REDSTONE, ModBlocks.THERMOELECTRIC_GENERATOR.get())
                .pattern("ICI").pattern("GTG").pattern("IBI")
                .define('I', Tags.Items.INGOTS_IRON)
                .define('C', Tags.Items.INGOTS_COPPER)
                .define('G', ModTags.INGOTS_VOLTAIC)
                .define('T', ModItems.THERMOPILE_CORE.get())
                .define('B', Items.BUCKET)
                .unlockedBy("has_thermopile_core", has(ModItems.THERMOPILE_CORE.get()))
                .save(output);
        ShapedRecipeBuilder.shaped(RecipeCategory.REDSTONE, ModBlocks.COMBUSTION_GENERATOR.get())
                .pattern("III").pattern("IFI").pattern("GVG")
                .define('I', Tags.Items.INGOTS_IRON)
                .define('F', Items.FURNACE)
                .define('G', ModTags.INGOTS_VOLTAIC)
                .define('V', Tags.Items.INGOTS_COPPER)
                .unlockedBy("has_voltaic_ingot", has(ModTags.INGOTS_VOLTAIC))
                .save(output);

        // Machines
        ShapedRecipeBuilder.shaped(RecipeCategory.REDSTONE, ModBlocks.ELECTRIC_FURNACE.get())
                .pattern("ICI").pattern("VFV").pattern("IRI")
                .define('I', Tags.Items.INGOTS_IRON)
                .define('C', ModTags.INGOTS_CONDUCTIVE_ALLOY)
                .define('V', ModTags.INGOTS_VOLTAIC)
                .define('F', Items.FURNACE)
                .define('R', Tags.Items.DUSTS_REDSTONE)
                .unlockedBy("has_conductive_alloy", has(ModTags.INGOTS_CONDUCTIVE_ALLOY))
                .save(output);

        // Storage and transport
        ShapedRecipeBuilder.shaped(RecipeCategory.REDSTONE, ModBlocks.BASIC_ENERGY_CELL.get())
                .pattern("ICI").pattern("RPR").pattern("ICI")
                .define('I', ModTags.INGOTS_VOLTAIC)
                .define('C', Tags.Items.INGOTS_COPPER)
                .define('R', Tags.Items.DUSTS_REDSTONE)
                .define('P', ModItems.POCKET_CAPACITOR.get())
                .unlockedBy("has_pocket_capacitor", has(ModItems.POCKET_CAPACITOR.get()))
                .save(output);
        ShapedRecipeBuilder.shaped(RecipeCategory.REDSTONE, ModBlocks.ADVANCED_ENERGY_CELL.get())
                .pattern("AGA").pattern("RBR").pattern("AGA")
                .define('A', ModTags.INGOTS_CONDUCTIVE_ALLOY)
                .define('G', Tags.Items.INGOTS_GOLD)
                .define('R', Tags.Items.DUSTS_REDSTONE)
                .define('B', ModBlocks.BASIC_ENERGY_CELL.get())
                .unlockedBy("has_basic_energy_cell", has(ModBlocks.BASIC_ENERGY_CELL.get()))
                .save(output);
        ShapedRecipeBuilder.shaped(RecipeCategory.REDSTONE, ModBlocks.ENERGY_CABLE.get(), 8)
                .pattern("III").pattern("RRR").pattern("III")
                .define('I', ModTags.INGOTS_VOLTAIC)
                .define('R', Tags.Items.DUSTS_REDSTONE)
                .unlockedBy("has_voltaic_ingot", has(ModTags.INGOTS_VOLTAIC))
                .save(output);
    }

    private static void cook(RecipeOutput output, TagKey<Item> input, ItemLike result, float xp, String name) {
        SimpleCookingRecipeBuilder.smelting(Ingredient.of(input), RecipeCategory.MISC, result, xp, 200)
                .unlockedBy("has_dust", has(input))
                .save(output, id(name + "_from_smelting"));
        SimpleCookingRecipeBuilder.blasting(Ingredient.of(input), RecipeCategory.MISC, result, xp, 100)
                .unlockedBy("has_dust", has(input))
                .save(output, id(name + "_from_blasting"));
    }

    private static ResourceLocation id(String path) {
        return ResourceLocation.fromNamespaceAndPath(VoltaicDynamics.MOD_ID, path);
    }
}
