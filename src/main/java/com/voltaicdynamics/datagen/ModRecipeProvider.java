package com.voltaicdynamics.datagen;

import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.item.crafting.CookingBookCategory;
import net.minecraft.world.item.ItemStackTemplate;
import net.minecraft.resources.ResourceKey;
import net.minecraft.data.worldgen.BootstrapContext;
import net.minecraft.core.registries.Registries;
import net.minecraft.advancements.Advancement;
import com.voltaicdynamics.VoltaicDynamics;
import com.voltaicdynamics.core.VoltaicCore;
import com.voltaicdynamics.gadgets.VoltaicGadgets;
import com.voltaicdynamics.generators.VoltaicGenerators;
import com.voltaicdynamics.logistics.VoltaicLogistics;
import com.voltaicdynamics.machines.VoltaicMachines;
import com.voltaicdynamics.machines.recipe.AlloyingRecipe;
import com.voltaicdynamics.machines.recipe.CrushingRecipe;
import com.voltaicdynamics.power.VoltaicPower;

import net.minecraft.data.recipes.RecipeCategory;
import net.minecraft.data.recipes.RecipeOutput;
import net.minecraft.data.recipes.RecipeProvider;
import net.minecraft.data.recipes.SimpleCookingRecipeBuilder;
import net.minecraft.resources.Identifier;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.level.ItemLike;
import net.neoforged.neoforge.common.Tags;
import net.neoforged.neoforge.common.conditions.ICondition;
import net.neoforged.neoforge.common.conditions.NotCondition;
import net.neoforged.neoforge.common.crafting.SizedIngredient;
import java.util.List;
import net.neoforged.neoforge.common.conditions.TagEmptyCondition;


/**
 * Voltaic ingots come from smelting their dust, like any metal. Galvanic Alloy is an alloy like Mekanism's:
 * it has no dust and is made only in the Alloy Smelter, the mod's infuser, so it never comes out of a furnace.
 */
public class ModRecipeProvider extends RecipeProvider {
    public ModRecipeProvider(BootstrapContext<Recipe<?>> recipes, BootstrapContext<Advancement> advancements) {
        super(recipes, advancements);
    }

    @Override
    protected void buildRecipes() {
        RecipeOutput output = this.output;
        // Dusts. The Pulverizer makes iron and copper dust (other mods' dusts work too, through the c: tags).
        shapeless(RecipeCategory.MISC, VoltaicCore.VOLTAIC_DUST.get(), 2)
                .requires(ModTags.DUSTS_IRON)
                .requires(ModTags.DUSTS_COPPER)
                .requires(Tags.Items.DUSTS_REDSTONE)
                .unlockedBy("has_copper_dust", has(ModTags.DUSTS_COPPER))
                .save(output);
        // The way in before any machine exists (the generators need Voltaic Ingots, the Pulverizer needs power):
        // a weaker recipe from ingots. Dusts give twice as much, and the Alloy Smelter more again.
        shapeless(RecipeCategory.MISC, VoltaicCore.VOLTAIC_DUST.get(), 1)
                .requires(Tags.Items.INGOTS_IRON)
                .requires(Tags.Items.INGOTS_COPPER)
                .requires(Tags.Items.DUSTS_REDSTONE)
                .unlockedBy("has_copper_ingot", has(Tags.Items.INGOTS_COPPER))
                .save(output, id("voltaic_dust_from_ingots"));

        // Dust -> ingot
        cook(output, ModTags.DUSTS_VOLTAIC, VoltaicCore.VOLTAIC_INGOT.get(), 0.7F, "voltaic_ingot");
        cook(output, ModTags.DUSTS_IRON, Items.IRON_INGOT, 0.7F, "iron_ingot_from_dust");
        cook(output, ModTags.DUSTS_COPPER, Items.COPPER_INGOT, 0.7F, "copper_ingot_from_dust");
        cook(output, ModTags.DUSTS_GOLD, Items.GOLD_INGOT, 1.0F, "gold_ingot_from_dust");

        // Components
        shaped(RecipeCategory.MISC, VoltaicCore.THERMOPILE_CORE.get())
                .pattern(" C ").pattern("RIR").pattern(" C ")
                .define('C', Tags.Items.INGOTS_COPPER)
                .define('R', Tags.Items.DUSTS_REDSTONE)
                .define('I', ModTags.INGOTS_VOLTAIC)
                .unlockedBy("has_voltaic_ingot", has(ModTags.INGOTS_VOLTAIC))
                .save(output);
        shaped(RecipeCategory.TOOLS, VoltaicGadgets.VOLTAIC_WRENCH.get())
                .pattern("I I").pattern(" V ").pattern(" I ")
                .define('I', Tags.Items.INGOTS_IRON)
                .define('V', ModTags.INGOTS_VOLTAIC)
                .unlockedBy("has_voltaic_ingot", has(ModTags.INGOTS_VOLTAIC))
                .save(output);
        shaped(RecipeCategory.TOOLS, VoltaicGadgets.POCKET_ACCUMULATOR.get())
                .pattern(" R ").pattern("CVC").pattern(" R ")
                .define('R', Tags.Items.DUSTS_REDSTONE)
                .define('C', Tags.Items.INGOTS_COPPER)
                .define('V', ModTags.INGOTS_VOLTAIC)
                .unlockedBy("has_voltaic_ingot", has(ModTags.INGOTS_VOLTAIC))
                .save(output);

        // Generators
        shaped(RecipeCategory.REDSTONE, VoltaicGenerators.THERMOELECTRIC_GENERATOR.get())
                .pattern("ICI").pattern("GTG").pattern("IBI")
                .define('I', Tags.Items.INGOTS_IRON)
                .define('C', Tags.Items.INGOTS_COPPER)
                .define('G', ModTags.INGOTS_VOLTAIC)
                .define('T', VoltaicCore.THERMOPILE_CORE.get())
                .define('B', Items.BUCKET)
                .unlockedBy("has_thermopile_core", has(VoltaicCore.THERMOPILE_CORE.get()))
                .save(output);
        shaped(RecipeCategory.REDSTONE, VoltaicGenerators.COMBUSTION_GENERATOR.get())
                .pattern("III").pattern("IFI").pattern("GVG")
                .define('I', Tags.Items.INGOTS_IRON)
                .define('F', Items.FURNACE)
                .define('G', ModTags.INGOTS_VOLTAIC)
                .define('V', Tags.Items.INGOTS_COPPER)
                .unlockedBy("has_voltaic_ingot", has(ModTags.INGOTS_VOLTAIC))
                .save(output);

        // Fluid Generator: a Combustion Generator that takes its fuel through pipes.
        shaped(RecipeCategory.REDSTONE, VoltaicGenerators.FLUID_GENERATOR.get())
                .pattern("IGI").pattern("VCV").pattern("IBI")
                .define('I', Tags.Items.INGOTS_IRON)
                .define('G', Tags.BlockItems.GLASS_BLOCKS.item())
                .define('V', ModTags.INGOTS_VOLTAIC)
                .define('C', VoltaicGenerators.COMBUSTION_GENERATOR.get())
                .define('B', Items.BUCKET)
                .unlockedBy("has_combustion_generator", has(VoltaicGenerators.COMBUSTION_GENERATOR.get()))
                .save(output);

        // Machines
        shaped(RecipeCategory.REDSTONE, VoltaicMachines.ELECTRIC_FURNACE.get())
                .pattern("ICI").pattern("VFV").pattern("IRI")
                .define('I', Tags.Items.INGOTS_IRON)
                .define('C', ModTags.ALLOYS_GALVANIC)
                .define('V', ModTags.INGOTS_VOLTAIC)
                .define('F', Items.FURNACE)
                .define('R', Tags.Items.DUSTS_REDSTONE)
                .unlockedBy("has_galvanic_alloy", has(ModTags.ALLOYS_GALVANIC))
                .save(output);

        // Alloy Smelter: no Galvanic Alloy in it, since it is the machine that makes Galvanic Alloy.
        shaped(RecipeCategory.REDSTONE, VoltaicMachines.ALLOY_SMELTER.get())
                .pattern("IVI").pattern("CFC").pattern("IRI")
                .define('I', Tags.Items.INGOTS_IRON)
                .define('V', ModTags.INGOTS_VOLTAIC)
                .define('C', Tags.Items.INGOTS_COPPER)
                .define('F', Items.FURNACE)
                .define('R', Tags.Items.DUSTS_REDSTONE)
                .unlockedBy("has_voltaic_ingot", has(ModTags.INGOTS_VOLTAIC))
                .save(output);

        // Pulverizer: no Voltaic Ingot in it, since its dusts are the good way to make Voltaic Ingots.
        shaped(RecipeCategory.REDSTONE, VoltaicMachines.PULVERIZER.get())
                .pattern("IFI").pattern("CPC").pattern("IRI")
                .define('I', Tags.Items.INGOTS_IRON)
                .define('F', Items.FLINT)
                .define('C', Tags.Items.INGOTS_COPPER)
                .define('P', Items.PISTON)
                .define('R', Tags.Items.DUSTS_REDSTONE)
                .unlockedBy("has_copper_ingot", has(Tags.Items.INGOTS_COPPER))
                .save(output);

        // Upgrades: the use for Galvanic Alloy once the Advanced Accumulator is built.
        shaped(RecipeCategory.MISC, VoltaicMachines.SPEED_UPGRADE.get())
                .pattern(" R ").pattern("RAR").pattern(" Q ")
                .define('R', Tags.Items.DUSTS_REDSTONE)
                .define('A', ModTags.ALLOYS_GALVANIC)
                .define('Q', Tags.Items.GEMS_QUARTZ)
                .unlockedBy("has_galvanic_alloy", has(ModTags.ALLOYS_GALVANIC))
                .save(output);
        // Tier Installers: raise a machine one tier in place (how many items it processes at once).
        shaped(RecipeCategory.MISC, VoltaicMachines.ADVANCED_TIER_INSTALLER.get())
                .pattern("GAG").pattern("ARA").pattern("GAG")
                .define('G', Tags.Items.INGOTS_GOLD)
                .define('A', ModTags.ALLOYS_GALVANIC)
                .define('R', Tags.BlockItems.STORAGE_BLOCKS_REDSTONE.item())
                .unlockedBy("has_galvanic_alloy", has(ModTags.ALLOYS_GALVANIC))
                .save(output);
        shaped(RecipeCategory.MISC, VoltaicMachines.ELITE_TIER_INSTALLER.get())
                .pattern("DAD").pattern("ARA").pattern("DAD")
                .define('D', Tags.Items.GEMS_DIAMOND)
                .define('A', ModTags.ALLOYS_GALVANIC)
                .define('R', Tags.BlockItems.STORAGE_BLOCKS_REDSTONE.item())
                .unlockedBy("has_advanced_tier_installer", has(VoltaicMachines.ADVANCED_TIER_INSTALLER.get()))
                .save(output);
        shaped(RecipeCategory.MISC, VoltaicMachines.ULTIMATE_TIER_INSTALLER.get())
                .pattern("DND").pattern("ARA").pattern("DAD")
                .define('D', Tags.Items.GEMS_DIAMOND)
                .define('N', Tags.Items.INGOTS_NETHERITE)
                .define('A', ModTags.ALLOYS_GALVANIC)
                .define('R', Tags.BlockItems.STORAGE_BLOCKS_REDSTONE.item())
                .unlockedBy("has_elite_tier_installer", has(VoltaicMachines.ELITE_TIER_INSTALLER.get()))
                .save(output);
        shaped(RecipeCategory.MISC, VoltaicMachines.EFFICIENCY_UPGRADE.get())
                .pattern(" L ").pattern("LAL").pattern(" G ")
                .define('L', Tags.Items.GEMS_LAPIS)
                .define('A', ModTags.ALLOYS_GALVANIC)
                .define('G', Tags.Items.INGOTS_GOLD)
                .unlockedBy("has_galvanic_alloy", has(ModTags.ALLOYS_GALVANIC))
                .save(output);

        alloying(output);
        crushing(output);

        // Fluids and logistics
        shaped(RecipeCategory.MISC, VoltaicLogistics.FLUID_TANK.get())
                .pattern("CGC").pattern("GBG").pattern("CGC")
                .define('C', Tags.Items.INGOTS_COPPER)
                .define('G', Tags.BlockItems.GLASS_BLOCKS.item())
                .define('B', Items.BUCKET)
                .unlockedBy("has_bucket", has(Items.BUCKET))
                .save(output);
        shaped(RecipeCategory.MISC, VoltaicLogistics.ADVANCED_FLUID_TANK.get())
                .pattern("AGA").pattern("GTG").pattern("AGA")
                .define('A', ModTags.ALLOYS_GALVANIC)
                .define('G', Tags.BlockItems.GLASS_BLOCKS.item())
                .define('T', VoltaicLogistics.FLUID_TANK.get())
                .unlockedBy("has_fluid_tank", has(VoltaicLogistics.FLUID_TANK.get()))
                .save(output);
        shaped(RecipeCategory.MISC, VoltaicLogistics.ELITE_FLUID_TANK.get())
                .pattern("DGD").pattern("GTG").pattern("DGD")
                .define('D', Tags.Items.GEMS_DIAMOND)
                .define('G', Tags.BlockItems.GLASS_BLOCKS.item())
                .define('T', VoltaicLogistics.ADVANCED_FLUID_TANK.get())
                .unlockedBy("has_advanced_fluid_tank", has(VoltaicLogistics.ADVANCED_FLUID_TANK.get()))
                .save(output);
        shaped(RecipeCategory.MISC, VoltaicLogistics.ULTIMATE_FLUID_TANK.get())
                .pattern("OGO").pattern("GTG").pattern("ONO")
                .define('O', Tags.Items.OBSIDIANS)
                .define('G', Tags.BlockItems.GLASS_BLOCKS.item())
                .define('N', Tags.Items.INGOTS_NETHERITE)
                .define('T', VoltaicLogistics.ELITE_FLUID_TANK.get())
                .unlockedBy("has_elite_fluid_tank", has(VoltaicLogistics.ELITE_FLUID_TANK.get()))
                .save(output);
        shaped(RecipeCategory.REDSTONE, VoltaicLogistics.ITEM_CABLE.get(), 8)
                .pattern("IGI").pattern("VHV").pattern("IGI")
                .define('I', Tags.Items.INGOTS_IRON)
                .define('G', Tags.Items.GLASS_PANES)
                .define('V', ModTags.INGOTS_VOLTAIC)
                .define('H', Items.HOPPER)
                .unlockedBy("has_voltaic_ingot", has(ModTags.INGOTS_VOLTAIC))
                .save(output);
        shaped(RecipeCategory.REDSTONE, VoltaicLogistics.FLUID_CABLE.get(), 8)
                .pattern("IGI").pattern("VBV").pattern("IGI")
                .define('I', Tags.Items.INGOTS_COPPER)
                .define('G', Tags.Items.GLASS_PANES)
                .define('V', ModTags.INGOTS_VOLTAIC)
                .define('B', Items.BUCKET)
                .unlockedBy("has_voltaic_ingot", has(ModTags.INGOTS_VOLTAIC))
                .save(output);

        // Storage and transport
        shaped(RecipeCategory.REDSTONE, VoltaicPower.BASIC_ACCUMULATOR.get())
                .pattern("ICI").pattern("RPR").pattern("ICI")
                .define('I', ModTags.INGOTS_VOLTAIC)
                .define('C', Tags.Items.INGOTS_COPPER)
                .define('R', Tags.Items.DUSTS_REDSTONE)
                .define('P', VoltaicGadgets.POCKET_ACCUMULATOR.get())
                .unlockedBy("has_pocket_accumulator", has(VoltaicGadgets.POCKET_ACCUMULATOR.get()))
                .save(output);
        shaped(RecipeCategory.REDSTONE, VoltaicPower.ADVANCED_ACCUMULATOR.get())
                .pattern("AGA").pattern("RBR").pattern("AGA")
                .define('A', ModTags.ALLOYS_GALVANIC)
                .define('G', Tags.Items.INGOTS_GOLD)
                .define('R', Tags.Items.DUSTS_REDSTONE)
                .define('B', VoltaicPower.BASIC_ACCUMULATOR.get())
                .unlockedBy("has_basic_accumulator", has(VoltaicPower.BASIC_ACCUMULATOR.get()))
                .save(output);
        // Higher tiers: each one takes the one below. Creative blocks have no recipe.
        shaped(RecipeCategory.REDSTONE, VoltaicPower.ELITE_ACCUMULATOR.get())
                .pattern("DAD").pattern("RBR").pattern("DAD")
                .define('D', Tags.Items.GEMS_DIAMOND)
                .define('A', ModTags.ALLOYS_GALVANIC)
                .define('R', Tags.BlockItems.STORAGE_BLOCKS_REDSTONE.item())
                .define('B', VoltaicPower.ADVANCED_ACCUMULATOR.get())
                .unlockedBy("has_advanced_accumulator", has(VoltaicPower.ADVANCED_ACCUMULATOR.get()))
                .save(output);
        shaped(RecipeCategory.REDSTONE, VoltaicPower.ULTIMATE_ACCUMULATOR.get())
                .pattern("ANA").pattern("RBR").pattern("ANA")
                .define('N', Tags.Items.INGOTS_NETHERITE)
                .define('A', ModTags.ALLOYS_GALVANIC)
                .define('R', Tags.BlockItems.STORAGE_BLOCKS_REDSTONE.item())
                .define('B', VoltaicPower.ELITE_ACCUMULATOR.get())
                .unlockedBy("has_elite_accumulator", has(VoltaicPower.ELITE_ACCUMULATOR.get()))
                .save(output);
        shaped(RecipeCategory.REDSTONE, VoltaicPower.VOLTAIC_CABLE.get(), 8)
                .pattern("III").pattern("RRR").pattern("III")
                .define('I', ModTags.INGOTS_VOLTAIC)
                .define('R', Tags.Items.DUSTS_REDSTONE)
                .unlockedBy("has_voltaic_ingot", has(ModTags.INGOTS_VOLTAIC))
                .save(output);
    }

    /** Alloy Smelter recipes. Dust versions yield more, rewarding ore processing, and load only when the dusts exist. */
    private void alloying(RecipeOutput output) {
        alloy(output, "voltaic_ingot", List.of(
                sized(Tags.Items.INGOTS_IRON, 1),
                sized(Tags.Items.INGOTS_COPPER, 1),
                sized(Tags.Items.DUSTS_REDSTONE, 1)),
                new ItemStackTemplate(VoltaicCore.VOLTAIC_INGOT.get(), 2), 100, 0.5F);
        alloy(output, "voltaic_ingot_from_dusts", List.of(
                sized(ModTags.DUSTS_IRON, 1),
                sized(ModTags.DUSTS_COPPER, 1),
                sized(Tags.Items.DUSTS_REDSTONE, 1)),
                new ItemStackTemplate(VoltaicCore.VOLTAIC_INGOT.get(), 3), 80, 0.6F);
        alloy(output, "voltaic_ingot_from_voltaic_dust", List.of(
                sized(ModTags.DUSTS_VOLTAIC, 2)),
                new ItemStackTemplate(VoltaicCore.VOLTAIC_INGOT.get(), 3), 60, 0.4F);
        alloy(output, "galvanic_alloy", List.of(
                sized(ModTags.INGOTS_VOLTAIC, 1),
                sized(Tags.Items.INGOTS_GOLD, 1),
                sized(Tags.Items.DUSTS_REDSTONE, 1)),
                new ItemStackTemplate(VoltaicCore.GALVANIC_ALLOY.get(), 2), 120, 0.7F);
        alloy(output, "galvanic_alloy_from_dusts", List.of(
                sized(ModTags.DUSTS_VOLTAIC, 1),
                sized(ModTags.DUSTS_GOLD, 1),
                sized(Tags.Items.DUSTS_REDSTONE, 1)),
                new ItemStackTemplate(VoltaicCore.GALVANIC_ALLOY.get(), 3), 100, 0.8F);
    }

    /**
     * Pulverizer recipes: an ore block gives 2 dusts, a raw metal 1 plus a 50% chance of a second, an ingot 1.
     * Ores and raw metals load only when the tag has items, so packs without, say, tin show no dead recipes.
     */
    private void crushing(RecipeOutput output) {
        metal(output, "iron", Tags.BlockItems.ORES_IRON.item(), Tags.Items.RAW_MATERIALS_IRON, Tags.Items.INGOTS_IRON, VoltaicCore.IRON_DUST.get());
        metal(output, "copper", Tags.BlockItems.ORES_COPPER.item(), Tags.Items.RAW_MATERIALS_COPPER, Tags.Items.INGOTS_COPPER, VoltaicCore.COPPER_DUST.get());
        metal(output, "gold", Tags.BlockItems.ORES_GOLD.item(), Tags.Items.RAW_MATERIALS_GOLD, Tags.Items.INGOTS_GOLD, VoltaicCore.GOLD_DUST.get());
        crush(output, "voltaic_dust_from_ingot", sized(ModTags.INGOTS_VOLTAIC, 1),
                new ItemStackTemplate(VoltaicCore.VOLTAIC_DUST.get()), 0F, 80, 0F);
        // Stone down to sand, like a pulverizer should.
        crush(output, "gravel_from_cobblestone", sized(Tags.BlockItems.COBBLESTONES_NORMAL.item(), 1),
                new ItemStackTemplate(Items.GRAVEL), 0F, 60, 0F);
        crush(output, "sand_from_gravel", sized(Tags.BlockItems.GRAVELS.item(), 1),
                new ItemStackTemplate(Items.SAND), 0F, 60, 0F);
        crush(output, "flint_from_gravel", SizedIngredient.of(Items.GRAVEL, 2),
                new ItemStackTemplate(Items.FLINT), 0F, 80, 0F);
    }

    private void metal(RecipeOutput output, String metal, TagKey<Item> ores, TagKey<Item> raw, TagKey<Item> ingots,
                              Item dust) {
        crush(output, metal + "_dust_from_ore", sized(ores, 1), new ItemStackTemplate(dust, 2), 0F, 120, 0.2F,
                new NotCondition(new TagEmptyCondition(ores)));
        crush(output, metal + "_dust_from_raw", sized(raw, 1), new ItemStackTemplate(dust), 0.5F, 100, 0.1F,
                new NotCondition(new TagEmptyCondition(raw)));
        crush(output, metal + "_dust_from_ingot", sized(ingots, 1), new ItemStackTemplate(dust), 0F, 80, 0F);
    }

    private static void crush(RecipeOutput output, String name, SizedIngredient input, ItemStackTemplate result, float bonus,
                              int time, float xp, ICondition... conditions) {
        output.accept(key("crushing/" + name), new CrushingRecipe(input, result, bonus, time, xp), null, conditions);
    }

    private static void alloy(RecipeOutput output, String name, List<SizedIngredient> inputs, ItemStackTemplate result,
                              int time, float xp, ICondition... conditions) {
        output.accept(key("alloying/" + name), new AlloyingRecipe(inputs, result, time, xp), null, conditions);
    }

    private void cook(RecipeOutput output, TagKey<Item> input, ItemLike result, float xp, String name) {
        SimpleCookingRecipeBuilder.smelting(tag(input), RecipeCategory.MISC, CookingBookCategory.MISC, result, xp, 200)
                .unlockedBy("has_dust", has(input))
                .save(output, id(name + "_from_smelting"));
        SimpleCookingRecipeBuilder.blasting(tag(input), RecipeCategory.MISC, CookingBookCategory.MISC, result, xp, 100)
                .unlockedBy("has_dust", has(input))
                .save(output, id(name + "_from_blasting"));
    }

    /** {@code count} of anything in the tag. */
    private SizedIngredient sized(TagKey<Item> tag, int count) {
        return new SizedIngredient(tag(tag), count);
    }

    private static String id(String path) {
        return VoltaicDynamics.MOD_ID + ":" + path;
    }

    private static ResourceKey<Recipe<?>> key(String path) {
        return ResourceKey.create(Registries.RECIPE, Identifier.fromNamespaceAndPath(VoltaicDynamics.MOD_ID, path));
    }
}
