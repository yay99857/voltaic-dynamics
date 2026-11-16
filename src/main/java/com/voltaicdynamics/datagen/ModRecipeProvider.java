package com.voltaicdynamics.datagen;

import com.voltaicdynamics.VoltaicDynamics;
import com.voltaicdynamics.core.VoltaicCore;
import com.voltaicdynamics.gadgets.VoltaicGadgets;
import com.voltaicdynamics.generators.VoltaicGenerators;
import com.voltaicdynamics.logistics.VoltaicLogistics;
import com.voltaicdynamics.machines.VoltaicMachines;
import com.voltaicdynamics.machines.recipe.AlloyingRecipe;
import com.voltaicdynamics.power.VoltaicPower;

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
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.common.conditions.ICondition;
import net.neoforged.neoforge.common.conditions.NotCondition;
import net.neoforged.neoforge.common.conditions.OrCondition;
import net.neoforged.neoforge.common.crafting.SizedIngredient;
import java.util.List;
import net.neoforged.neoforge.common.conditions.TagEmptyCondition;

import java.util.concurrent.CompletableFuture;

/**
 * Voltaic ingots and galvanic alloy come only from smelting their dust, so the dust step
 * matters and no recipe shares its ingredients with another.
 */
public class ModRecipeProvider extends RecipeProvider {
    public ModRecipeProvider(PackOutput output, CompletableFuture<HolderLookup.Provider> lookup) {
        super(output, lookup);
    }

    @Override
    protected void buildRecipes(RecipeOutput output) {
        // Dusts. Voltaic Dust takes iron and copper dust, never ingots, so it needs an ore-processing mod.
        // Without one the dust tags are empty and the recipe is not loaded at all (no dead recipe in JEI/EMI).
        ShapelessRecipeBuilder.shapeless(RecipeCategory.MISC, VoltaicCore.VOLTAIC_DUST.get(), 2)
                .requires(ModTags.DUSTS_IRON)
                .requires(ModTags.DUSTS_COPPER)
                .requires(Tags.Items.DUSTS_REDSTONE)
                .unlockedBy("has_copper_dust", has(ModTags.DUSTS_COPPER))
                .save(output.withConditions(
                        new NotCondition(new TagEmptyCondition(ModTags.DUSTS_IRON)),
                        new NotCondition(new TagEmptyCondition(ModTags.DUSTS_COPPER))));
        // Packs without an ore-processing mod still need a way in: a weaker ingot recipe, loaded only when
        // iron or copper dust does not exist. The Alloy Smelter is the better path either way.
        ShapelessRecipeBuilder.shapeless(RecipeCategory.MISC, VoltaicCore.VOLTAIC_DUST.get(), 1)
                .requires(Tags.Items.INGOTS_IRON)
                .requires(Tags.Items.INGOTS_COPPER)
                .requires(Tags.Items.DUSTS_REDSTONE)
                .unlockedBy("has_copper_ingot", has(Tags.Items.INGOTS_COPPER))
                .save(output.withConditions(new OrCondition(List.of(
                        new TagEmptyCondition(ModTags.DUSTS_IRON),
                        new TagEmptyCondition(ModTags.DUSTS_COPPER)))), id("voltaic_dust_from_ingots"));
        ShapelessRecipeBuilder.shapeless(RecipeCategory.MISC, VoltaicCore.GALVANIC_DUST.get(), 2)
                .requires(ModTags.INGOTS_VOLTAIC)
                .requires(Tags.Items.INGOTS_GOLD)
                .requires(Tags.Items.DUSTS_REDSTONE)
                .unlockedBy("has_voltaic_ingot", has(ModTags.INGOTS_VOLTAIC))
                .save(output);

        // Dust -> ingot
        cook(output, ModTags.DUSTS_VOLTAIC, VoltaicCore.VOLTAIC_INGOT.get(), 0.7F, "voltaic_ingot");
        cook(output, ModTags.DUSTS_GALVANIC_ALLOY, VoltaicCore.GALVANIC_ALLOY.get(), 0.8F, "galvanic_alloy");

        // Components
        ShapedRecipeBuilder.shaped(RecipeCategory.MISC, VoltaicCore.THERMOPILE_CORE.get())
                .pattern(" C ").pattern("RIR").pattern(" C ")
                .define('C', Tags.Items.INGOTS_COPPER)
                .define('R', Tags.Items.DUSTS_REDSTONE)
                .define('I', ModTags.INGOTS_VOLTAIC)
                .unlockedBy("has_voltaic_ingot", has(ModTags.INGOTS_VOLTAIC))
                .save(output);
        ShapedRecipeBuilder.shaped(RecipeCategory.TOOLS, VoltaicGadgets.VOLTAIC_WRENCH.get())
                .pattern("I I").pattern(" V ").pattern(" I ")
                .define('I', Tags.Items.INGOTS_IRON)
                .define('V', ModTags.INGOTS_VOLTAIC)
                .unlockedBy("has_voltaic_ingot", has(ModTags.INGOTS_VOLTAIC))
                .save(output);
        ShapedRecipeBuilder.shaped(RecipeCategory.TOOLS, VoltaicGadgets.POCKET_ACCUMULATOR.get())
                .pattern(" R ").pattern("CVC").pattern(" R ")
                .define('R', Tags.Items.DUSTS_REDSTONE)
                .define('C', Tags.Items.INGOTS_COPPER)
                .define('V', ModTags.INGOTS_VOLTAIC)
                .unlockedBy("has_voltaic_ingot", has(ModTags.INGOTS_VOLTAIC))
                .save(output);

        // Generators
        ShapedRecipeBuilder.shaped(RecipeCategory.REDSTONE, VoltaicGenerators.THERMOELECTRIC_GENERATOR.get())
                .pattern("ICI").pattern("GTG").pattern("IBI")
                .define('I', Tags.Items.INGOTS_IRON)
                .define('C', Tags.Items.INGOTS_COPPER)
                .define('G', ModTags.INGOTS_VOLTAIC)
                .define('T', VoltaicCore.THERMOPILE_CORE.get())
                .define('B', Items.BUCKET)
                .unlockedBy("has_thermopile_core", has(VoltaicCore.THERMOPILE_CORE.get()))
                .save(output);
        ShapedRecipeBuilder.shaped(RecipeCategory.REDSTONE, VoltaicGenerators.COMBUSTION_GENERATOR.get())
                .pattern("III").pattern("IFI").pattern("GVG")
                .define('I', Tags.Items.INGOTS_IRON)
                .define('F', Items.FURNACE)
                .define('G', ModTags.INGOTS_VOLTAIC)
                .define('V', Tags.Items.INGOTS_COPPER)
                .unlockedBy("has_voltaic_ingot", has(ModTags.INGOTS_VOLTAIC))
                .save(output);

        // Machines
        ShapedRecipeBuilder.shaped(RecipeCategory.REDSTONE, VoltaicMachines.ELECTRIC_FURNACE.get())
                .pattern("ICI").pattern("VFV").pattern("IRI")
                .define('I', Tags.Items.INGOTS_IRON)
                .define('C', ModTags.ALLOYS_GALVANIC)
                .define('V', ModTags.INGOTS_VOLTAIC)
                .define('F', Items.FURNACE)
                .define('R', Tags.Items.DUSTS_REDSTONE)
                .unlockedBy("has_galvanic_alloy", has(ModTags.ALLOYS_GALVANIC))
                .save(output);

        // Alloy Smelter: no Galvanic Alloy in it, since it is the machine that makes Galvanic Alloy.
        ShapedRecipeBuilder.shaped(RecipeCategory.REDSTONE, VoltaicMachines.ALLOY_SMELTER.get())
                .pattern("IVI").pattern("CFC").pattern("IRI")
                .define('I', Tags.Items.INGOTS_IRON)
                .define('V', ModTags.INGOTS_VOLTAIC)
                .define('C', Tags.Items.INGOTS_COPPER)
                .define('F', Items.FURNACE)
                .define('R', Tags.Items.DUSTS_REDSTONE)
                .unlockedBy("has_voltaic_ingot", has(ModTags.INGOTS_VOLTAIC))
                .save(output);

        alloying(output);

        // Fluids and logistics
        ShapedRecipeBuilder.shaped(RecipeCategory.MISC, VoltaicLogistics.FLUID_TANK.get())
                .pattern("CGC").pattern("GBG").pattern("CGC")
                .define('C', Tags.Items.INGOTS_COPPER)
                .define('G', Tags.Items.GLASS_BLOCKS)
                .define('B', Items.BUCKET)
                .unlockedBy("has_bucket", has(Items.BUCKET))
                .save(output);
        ShapedRecipeBuilder.shaped(RecipeCategory.REDSTONE, VoltaicLogistics.ITEM_CABLE.get(), 8)
                .pattern("IGI").pattern("VHV").pattern("IGI")
                .define('I', Tags.Items.INGOTS_IRON)
                .define('G', Tags.Items.GLASS_PANES)
                .define('V', ModTags.INGOTS_VOLTAIC)
                .define('H', Items.HOPPER)
                .unlockedBy("has_voltaic_ingot", has(ModTags.INGOTS_VOLTAIC))
                .save(output);
        ShapedRecipeBuilder.shaped(RecipeCategory.REDSTONE, VoltaicLogistics.FLUID_CABLE.get(), 8)
                .pattern("IGI").pattern("VBV").pattern("IGI")
                .define('I', Tags.Items.INGOTS_COPPER)
                .define('G', Tags.Items.GLASS_PANES)
                .define('V', ModTags.INGOTS_VOLTAIC)
                .define('B', Items.BUCKET)
                .unlockedBy("has_voltaic_ingot", has(ModTags.INGOTS_VOLTAIC))
                .save(output);

        // Storage and transport
        ShapedRecipeBuilder.shaped(RecipeCategory.REDSTONE, VoltaicPower.BASIC_ACCUMULATOR.get())
                .pattern("ICI").pattern("RPR").pattern("ICI")
                .define('I', ModTags.INGOTS_VOLTAIC)
                .define('C', Tags.Items.INGOTS_COPPER)
                .define('R', Tags.Items.DUSTS_REDSTONE)
                .define('P', VoltaicGadgets.POCKET_ACCUMULATOR.get())
                .unlockedBy("has_pocket_accumulator", has(VoltaicGadgets.POCKET_ACCUMULATOR.get()))
                .save(output);
        ShapedRecipeBuilder.shaped(RecipeCategory.REDSTONE, VoltaicPower.ADVANCED_ACCUMULATOR.get())
                .pattern("AGA").pattern("RBR").pattern("AGA")
                .define('A', ModTags.ALLOYS_GALVANIC)
                .define('G', Tags.Items.INGOTS_GOLD)
                .define('R', Tags.Items.DUSTS_REDSTONE)
                .define('B', VoltaicPower.BASIC_ACCUMULATOR.get())
                .unlockedBy("has_basic_accumulator", has(VoltaicPower.BASIC_ACCUMULATOR.get()))
                .save(output);
        ShapedRecipeBuilder.shaped(RecipeCategory.REDSTONE, VoltaicPower.VOLTAIC_CABLE.get(), 8)
                .pattern("III").pattern("RRR").pattern("III")
                .define('I', ModTags.INGOTS_VOLTAIC)
                .define('R', Tags.Items.DUSTS_REDSTONE)
                .unlockedBy("has_voltaic_ingot", has(ModTags.INGOTS_VOLTAIC))
                .save(output);
    }

    /** Alloy Smelter recipes. Dust versions yield more, rewarding ore processing, and load only when the dusts exist. */
    private static void alloying(RecipeOutput output) {
        alloy(output, "voltaic_ingot", List.of(
                SizedIngredient.of(Tags.Items.INGOTS_IRON, 1),
                SizedIngredient.of(Tags.Items.INGOTS_COPPER, 1),
                SizedIngredient.of(Tags.Items.DUSTS_REDSTONE, 1)),
                new ItemStack(VoltaicCore.VOLTAIC_INGOT.get(), 2), 100, 0.5F);
        alloy(output, "voltaic_ingot_from_dusts", List.of(
                SizedIngredient.of(ModTags.DUSTS_IRON, 1),
                SizedIngredient.of(ModTags.DUSTS_COPPER, 1),
                SizedIngredient.of(Tags.Items.DUSTS_REDSTONE, 1)),
                new ItemStack(VoltaicCore.VOLTAIC_INGOT.get(), 3), 80, 0.6F,
                new NotCondition(new TagEmptyCondition(ModTags.DUSTS_IRON)),
                new NotCondition(new TagEmptyCondition(ModTags.DUSTS_COPPER)));
        alloy(output, "voltaic_ingot_from_voltaic_dust", List.of(
                SizedIngredient.of(ModTags.DUSTS_VOLTAIC, 2)),
                new ItemStack(VoltaicCore.VOLTAIC_INGOT.get(), 3), 60, 0.4F);
        alloy(output, "galvanic_alloy", List.of(
                SizedIngredient.of(ModTags.INGOTS_VOLTAIC, 1),
                SizedIngredient.of(Tags.Items.INGOTS_GOLD, 1),
                SizedIngredient.of(Tags.Items.DUSTS_REDSTONE, 1)),
                new ItemStack(VoltaicCore.GALVANIC_ALLOY.get(), 2), 120, 0.7F);
        alloy(output, "galvanic_alloy_from_dusts", List.of(
                SizedIngredient.of(ModTags.DUSTS_VOLTAIC, 1),
                SizedIngredient.of(ModTags.DUSTS_GOLD, 1),
                SizedIngredient.of(Tags.Items.DUSTS_REDSTONE, 1)),
                new ItemStack(VoltaicCore.GALVANIC_ALLOY.get(), 3), 100, 0.8F,
                new NotCondition(new TagEmptyCondition(ModTags.DUSTS_GOLD)));
        alloy(output, "galvanic_alloy_from_galvanic_dust", List.of(
                SizedIngredient.of(ModTags.DUSTS_GALVANIC_ALLOY, 2)),
                new ItemStack(VoltaicCore.GALVANIC_ALLOY.get(), 3), 60, 0.5F);
    }

    private static void alloy(RecipeOutput output, String name, List<SizedIngredient> inputs, ItemStack result, int time,
                              float xp, ICondition... conditions) {
        output.accept(id("alloying/" + name), new AlloyingRecipe(inputs, result, time, xp), null, conditions);
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
