package com.voltaicdynamics.datagen;

import com.voltaicdynamics.VoltaicDynamics;
import com.voltaicdynamics.core.VoltaicCore;
import com.voltaicdynamics.gadgets.VoltaicGadgets;
import com.voltaicdynamics.generators.VoltaicGenerators;
import com.voltaicdynamics.logistics.VoltaicLogistics;
import com.voltaicdynamics.machines.VoltaicMachines;
import com.voltaicdynamics.machines.recipe.AlloyingRecipe;
import com.voltaicdynamics.machines.recipe.CrushingRecipe;
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
        // Dusts. The Pulverizer makes iron and copper dust (other mods' dusts work too, through the c: tags).
        ShapelessRecipeBuilder.shapeless(RecipeCategory.MISC, VoltaicCore.VOLTAIC_DUST.get(), 2)
                .requires(ModTags.DUSTS_IRON)
                .requires(ModTags.DUSTS_COPPER)
                .requires(Tags.Items.DUSTS_REDSTONE)
                .unlockedBy("has_copper_dust", has(ModTags.DUSTS_COPPER))
                .save(output);
        // The way in before any machine exists (the generators need Voltaic Ingots, the Pulverizer needs power):
        // a weaker recipe from ingots. Dusts give twice as much, and the Alloy Smelter more again.
        ShapelessRecipeBuilder.shapeless(RecipeCategory.MISC, VoltaicCore.VOLTAIC_DUST.get(), 1)
                .requires(Tags.Items.INGOTS_IRON)
                .requires(Tags.Items.INGOTS_COPPER)
                .requires(Tags.Items.DUSTS_REDSTONE)
                .unlockedBy("has_copper_ingot", has(Tags.Items.INGOTS_COPPER))
                .save(output, id("voltaic_dust_from_ingots"));
        ShapelessRecipeBuilder.shapeless(RecipeCategory.MISC, VoltaicCore.GALVANIC_DUST.get(), 2)
                .requires(ModTags.INGOTS_VOLTAIC)
                .requires(Tags.Items.INGOTS_GOLD)
                .requires(Tags.Items.DUSTS_REDSTONE)
                .unlockedBy("has_voltaic_ingot", has(ModTags.INGOTS_VOLTAIC))
                .save(output);

        // Dust -> ingot
        cook(output, ModTags.DUSTS_VOLTAIC, VoltaicCore.VOLTAIC_INGOT.get(), 0.7F, "voltaic_ingot");
        cook(output, ModTags.DUSTS_GALVANIC_ALLOY, VoltaicCore.GALVANIC_ALLOY.get(), 0.8F, "galvanic_alloy");
        cook(output, ModTags.DUSTS_IRON, Items.IRON_INGOT, 0.7F, "iron_ingot_from_dust");
        cook(output, ModTags.DUSTS_COPPER, Items.COPPER_INGOT, 0.7F, "copper_ingot_from_dust");
        cook(output, ModTags.DUSTS_GOLD, Items.GOLD_INGOT, 1.0F, "gold_ingot_from_dust");

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

        // Pulverizer: no Voltaic Ingot in it, since its dusts are the good way to make Voltaic Ingots.
        ShapedRecipeBuilder.shaped(RecipeCategory.REDSTONE, VoltaicMachines.PULVERIZER.get())
                .pattern("IFI").pattern("CPC").pattern("IRI")
                .define('I', Tags.Items.INGOTS_IRON)
                .define('F', Items.FLINT)
                .define('C', Tags.Items.INGOTS_COPPER)
                .define('P', Items.PISTON)
                .define('R', Tags.Items.DUSTS_REDSTONE)
                .unlockedBy("has_copper_ingot", has(Tags.Items.INGOTS_COPPER))
                .save(output);

        alloying(output);
        crushing(output);

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
                new ItemStack(VoltaicCore.VOLTAIC_INGOT.get(), 3), 80, 0.6F);
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
                new ItemStack(VoltaicCore.GALVANIC_ALLOY.get(), 3), 100, 0.8F);
        alloy(output, "galvanic_alloy_from_galvanic_dust", List.of(
                SizedIngredient.of(ModTags.DUSTS_GALVANIC_ALLOY, 2)),
                new ItemStack(VoltaicCore.GALVANIC_ALLOY.get(), 3), 60, 0.5F);
    }

    /**
     * Pulverizer recipes: an ore block gives 2 dusts, a raw metal 1 plus a 50% chance of a second, an ingot 1.
     * Ores and raw metals load only when the tag has items, so packs without, say, tin show no dead recipes.
     */
    private static void crushing(RecipeOutput output) {
        metal(output, "iron", Tags.Items.ORES_IRON, Tags.Items.RAW_MATERIALS_IRON, Tags.Items.INGOTS_IRON, VoltaicCore.IRON_DUST.get());
        metal(output, "copper", Tags.Items.ORES_COPPER, Tags.Items.RAW_MATERIALS_COPPER, Tags.Items.INGOTS_COPPER, VoltaicCore.COPPER_DUST.get());
        metal(output, "gold", Tags.Items.ORES_GOLD, Tags.Items.RAW_MATERIALS_GOLD, Tags.Items.INGOTS_GOLD, VoltaicCore.GOLD_DUST.get());
        crush(output, "voltaic_dust_from_ingot", SizedIngredient.of(ModTags.INGOTS_VOLTAIC, 1),
                new ItemStack(VoltaicCore.VOLTAIC_DUST.get()), 0F, 80, 0F);
        crush(output, "galvanic_dust_from_alloy", SizedIngredient.of(ModTags.ALLOYS_GALVANIC, 1),
                new ItemStack(VoltaicCore.GALVANIC_DUST.get()), 0F, 80, 0F);
        // Stone down to sand, like a pulverizer should.
        crush(output, "gravel_from_cobblestone", SizedIngredient.of(Tags.Items.COBBLESTONES_NORMAL, 1),
                new ItemStack(Items.GRAVEL), 0F, 60, 0F);
        crush(output, "sand_from_gravel", SizedIngredient.of(Tags.Items.GRAVELS, 1),
                new ItemStack(Items.SAND), 0F, 60, 0F);
        crush(output, "flint_from_gravel", SizedIngredient.of(Items.GRAVEL, 2),
                new ItemStack(Items.FLINT), 0F, 80, 0F);
    }

    private static void metal(RecipeOutput output, String metal, TagKey<Item> ores, TagKey<Item> raw, TagKey<Item> ingots,
                              Item dust) {
        crush(output, metal + "_dust_from_ore", SizedIngredient.of(ores, 1), new ItemStack(dust, 2), 0F, 120, 0.2F,
                new NotCondition(new TagEmptyCondition(ores)));
        crush(output, metal + "_dust_from_raw", SizedIngredient.of(raw, 1), new ItemStack(dust), 0.5F, 100, 0.1F,
                new NotCondition(new TagEmptyCondition(raw)));
        crush(output, metal + "_dust_from_ingot", SizedIngredient.of(ingots, 1), new ItemStack(dust), 0F, 80, 0F);
    }

    private static void crush(RecipeOutput output, String name, SizedIngredient input, ItemStack result, float bonus,
                              int time, float xp, ICondition... conditions) {
        output.accept(id("crushing/" + name), new CrushingRecipe(input, result, bonus, time, xp), null, conditions);
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
