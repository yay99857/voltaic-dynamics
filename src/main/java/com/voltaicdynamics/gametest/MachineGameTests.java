package com.voltaicdynamics.gametest;

import static com.voltaicdynamics.gametest.GameTestSupport.items;
import com.voltaicdynamics.VoltaicConfig;
import com.voltaicdynamics.VoltaicDynamics;
import com.voltaicdynamics.core.VoltaicCore;
import com.voltaicdynamics.core.energy.VoltaicEnergyStorage;
import com.voltaicdynamics.datagen.ModTags;
import com.voltaicdynamics.machines.AlloySmelterBlockEntity;
import com.voltaicdynamics.machines.ElectricFurnaceBlockEntity;
import com.voltaicdynamics.machines.PulverizerBlockEntity;
import com.voltaicdynamics.machines.VoltaicMachines;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.CraftingInput;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.item.crafting.SingleRecipeInput;
import net.minecraft.core.Direction;
import java.util.List;

import static com.voltaicdynamics.gametest.GameTestSupport.power;

/**
 * Electric Furnace, Alloy Smelter, Pulverizer and the recipe set. Run with {@code ./gradlew runGameTestServer};
 * the server exits non-zero when a required test fails. Only loaded when game tests are enabled.
 */
public class MachineGameTests {
    /** The Electric Furnace smelts with FE and pauses without it. */
    @VoltaicTest(timeoutTicks = 300)
    public static void electricFurnaceSmelts(GameTestHelper helper) {
        BlockPos pos = new BlockPos(2, 1, 2);
        helper.setBlock(pos, VoltaicMachines.ELECTRIC_FURNACE.get());
        ElectricFurnaceBlockEntity furnace = helper.getBlockEntity(pos, ElectricFurnaceBlockEntity.class);
        items(furnace.getItemHandler(null)).insertItem(ElectricFurnaceBlockEntity.SLOT_INPUT, new ItemStack(Items.RAW_IRON, 2), false);

        helper.runAfterDelay(10, () -> {
            helper.assertValueEqual(furnace.getProgress(), 0, "progress without energy");
            int perItem = VoltaicConfig.get(VoltaicConfig.FURNACE_ENERGY_PER_TICK) * VoltaicConfig.get(VoltaicConfig.FURNACE_TICKS_PER_ITEM);
            ((VoltaicEnergyStorage) furnace.getEnergyStorage(null)).setEnergy(perItem * 2);
            helper.succeedWhen(() -> {
                ItemStack out = items(furnace.getItemHandler(null)).getStackInSlot(ElectricFurnaceBlockEntity.SLOT_OUTPUT);
                helper.assertTrue(out.is(Items.IRON_INGOT) && out.getCount() == 2, "output is " + out);
            });
        });
    }

    /** The cached recipe follows the inputs even when a stack is changed in place (as menus do), so nothing smelts for free. */
    @VoltaicTest(timeoutTicks = 400)
    public static void electricFurnaceRecipeCacheFollowsInputs(GameTestHelper helper) {
        BlockPos pos = new BlockPos(2, 1, 2);
        helper.setBlock(pos, VoltaicMachines.ELECTRIC_FURNACE.get());
        ElectricFurnaceBlockEntity furnace = helper.getBlockEntity(pos, ElectricFurnaceBlockEntity.class);
        var items = items(furnace.getMenuItemHandler());
        items.insertItem(ElectricFurnaceBlockEntity.SLOT_INPUT, new ItemStack(Items.RAW_IRON), false);
        power(furnace, 1_000_000);

        helper.runAfterDelay(5, () -> {
            helper.assertTrue(furnace.getProgress() > 0, "furnace did not start");
            items.getStackInSlot(ElectricFurnaceBlockEntity.SLOT_INPUT).shrink(1);
            helper.runAfterDelay(ElectricFurnaceBlockEntity.ticksPerItem() + 10, () -> {
                helper.assertTrue(items.getStackInSlot(ElectricFurnaceBlockEntity.SLOT_OUTPUT).isEmpty(), "smelted an item that was removed");
                helper.assertValueEqual(furnace.getProgress(), 0, "progress with no input");
                // A new, different input is picked up again.
                items.insertItem(ElectricFurnaceBlockEntity.SLOT_INPUT, new ItemStack(Items.RAW_GOLD), false);
                helper.succeedWhen(() -> helper.assertTrue(
                        items.getStackInSlot(ElectricFurnaceBlockEntity.SLOT_OUTPUT).is(Items.GOLD_INGOT), "gold was not smelted"));
            });
        });
    }

    /** Galvanic Alloy is an alloy, not an ingot: only the Alloy Smelter makes it; no crafting, furnace or crushing recipe. */
    @VoltaicTest
    public static void galvanicAlloyOnlyFromTheAlloySmelter(GameTestHelper helper) {
        var registries = helper.getLevel().registryAccess();
        for (var holder : helper.getLevel().recipeAccess().getRecipes()) {
            var recipe = holder.value();
            if (!GameTestSupport.resultOf(recipe, helper.getLevel()).is(VoltaicCore.GALVANIC_ALLOY.get())) continue;
            helper.assertTrue(recipe.getType() == com.voltaicdynamics.machines.recipe.ModRecipes.ALLOYING.get(),
                    holder.id() + " makes Galvanic Alloy outside the Alloy Smelter");
        }
        helper.assertTrue(net.minecraft.core.registries.BuiltInRegistries.ITEM.getOptional(
                VoltaicDynamics.id("galvanic_dust")).isEmpty(), "Galvanic Dust still exists");
        helper.succeed();
    }

    /** No shapeless shortcuts to ingots or alloy; Voltaic Dust from dusts, and from ingots to get started. */
    @VoltaicTest
    public static void noDuplicateAlloyRecipes(GameTestHelper helper) {
        var recipes = helper.getLevel().recipeAccess();
        helper.assertTrue(recipes.byKey(GameTestSupport.recipeKey("voltaic_ingot")).isEmpty(),
                "shapeless voltaic_ingot recipe still exists");
        helper.assertTrue(recipes.byKey(GameTestSupport.recipeKey("galvanic_alloy")).isEmpty(),
                "shapeless galvanic_alloy recipe still exists");

        // Voltaic Dust: from iron + copper dust (2), and from ingots (1) so the first Voltaic Ingots can be made
        // before any machine exists.
        var registries = helper.getLevel().registryAccess();
        CraftingInput ingots = CraftingInput.of(3, 1, List.of(
                new ItemStack(Items.IRON_INGOT), new ItemStack(Items.COPPER_INGOT), new ItemStack(Items.REDSTONE)));
        helper.assertTrue(recipes.recipeMap().getRecipesFor(RecipeType.CRAFTING, ingots, helper.getLevel())
                        .anyMatch(r -> r.value().assemble(ingots).is(VoltaicCore.VOLTAIC_DUST.get())),
                "no voltaic dust from ingots: nothing to start with");
        CraftingInput dusts = CraftingInput.of(3, 1, List.of(new ItemStack(VoltaicCore.IRON_DUST.get()),
                new ItemStack(VoltaicCore.COPPER_DUST.get()), new ItemStack(Items.REDSTONE)));
        helper.assertTrue(recipes.recipeMap().getRecipesFor(RecipeType.CRAFTING, dusts, helper.getLevel())
                        .anyMatch(r -> r.value().assemble(dusts).getCount() == 2
                                && r.value().assemble(dusts).is(VoltaicCore.VOLTAIC_DUST.get())),
                "iron dust + copper dust + redstone does not make 2 voltaic dust");
        helper.succeed();
    }

    /** Iron + copper + redstone ingots go straight to Voltaic Ingots, no dust needed. */
    @VoltaicTest(timeoutTicks = 400)
    public static void alloySmelterMakesVoltaicIngots(GameTestHelper helper) {
        BlockPos pos = new BlockPos(2, 1, 2);
        helper.setBlock(pos, VoltaicMachines.ALLOY_SMELTER.get());
        AlloySmelterBlockEntity smelter = helper.getBlockEntity(pos, AlloySmelterBlockEntity.class);
        var items = items(smelter.getItemHandler(null));
        // Any slot order works.
        items.insertItem(0, new ItemStack(Items.REDSTONE), false);
        items.insertItem(1, new ItemStack(Items.COPPER_INGOT), false);
        items.insertItem(2, new ItemStack(Items.IRON_INGOT), false);
        power(smelter, 100_000);
        helper.succeedWhen(() -> {
            ItemStack out = items.getStackInSlot(AlloySmelterBlockEntity.SLOT_OUTPUT);
            helper.assertTrue(out.is(VoltaicCore.VOLTAIC_INGOT.get()) && out.getCount() == 2, "output is " + out);
            helper.assertTrue(items.getStackInSlot(0).isEmpty() && items.getStackInSlot(1).isEmpty()
                    && items.getStackInSlot(2).isEmpty(), "inputs were not consumed");
        });
    }

    /** Pipes cannot put the same item into two input slots (it would block the recipe). */
    @VoltaicTest
    public static void alloySmelterSpreadsPipeInput(GameTestHelper helper) {
        BlockPos pos = new BlockPos(2, 1, 2);
        helper.setBlock(pos, VoltaicMachines.ALLOY_SMELTER.get());
        AlloySmelterBlockEntity smelter = helper.getBlockEntity(pos, AlloySmelterBlockEntity.class);
        var items = items(smelter.getItemHandler(Direction.UP));
        helper.assertTrue(items.insertItem(0, new ItemStack(Items.IRON_INGOT, 8), false).isEmpty(), "iron refused");
        helper.assertTrue(!items.insertItem(1, new ItemStack(Items.IRON_INGOT, 8), false).isEmpty(), "iron went into a second slot");
        helper.assertTrue(items.insertItem(1, new ItemStack(Items.COPPER_INGOT, 8), false).isEmpty(), "copper refused");
        helper.assertTrue(!items.insertItem(2, new ItemStack(Items.DIRT), false).isEmpty(), "dirt accepted");
        helper.assertTrue(items.extractItem(0, 1, true).isEmpty(), "pipes could pull an input back out");
        helper.succeed();
    }

    /** The Alloy Smelter recipe does not need the Galvanic Alloy that only it can make. */
    @VoltaicTest
    public static void alloySmelterHasNoBootstrapLoop(GameTestHelper helper) {
        var recipe = helper.getLevel().recipeAccess()
                .byKey(GameTestSupport.recipeKey("alloy_smelter"));
        helper.assertTrue(recipe.isPresent(), "alloy_smelter recipe missing");
        for (var ingredient : recipe.get().value().placementInfo().ingredients()) {
            helper.assertTrue(!ingredient.test(new ItemStack(VoltaicCore.GALVANIC_ALLOY.get())), "recipe needs Galvanic Alloy");
        }
        helper.succeed();
    }

    /** An iron ore block gives exactly 2 Iron Dust. */
    @VoltaicTest(timeoutTicks = 400)
    public static void pulverizerCrushesOre(GameTestHelper helper) {
        BlockPos pos = new BlockPos(2, 1, 2);
        helper.setBlock(pos, VoltaicMachines.PULVERIZER.get());
        PulverizerBlockEntity pulverizer = helper.getBlockEntity(pos, PulverizerBlockEntity.class);
        var items = items(pulverizer.getItemHandler(null));
        items.insertItem(PulverizerBlockEntity.SLOT_INPUT, new ItemStack(Items.IRON_ORE), false);
        power(pulverizer, 100_000);
        helper.succeedWhen(() -> {
            ItemStack out = items.getStackInSlot(PulverizerBlockEntity.SLOT_OUTPUT);
            helper.assertTrue(out.is(VoltaicCore.IRON_DUST.get()) && out.getCount() == 2, "output is " + out);
            helper.assertTrue(items.getStackInSlot(PulverizerBlockEntity.SLOT_INPUT).isEmpty(), "ore was not consumed");
        });
    }

    /** 8 Raw Copper give 8 to 16 Copper Dust (one each, plus the 50% bonus), and nothing is left behind. */
    @VoltaicTest(timeoutTicks = 1200)
    public static void pulverizerBonusOnRawMetal(GameTestHelper helper) {
        BlockPos pos = new BlockPos(2, 1, 2);
        helper.setBlock(pos, VoltaicMachines.PULVERIZER.get());
        PulverizerBlockEntity pulverizer = helper.getBlockEntity(pos, PulverizerBlockEntity.class);
        var items = items(pulverizer.getItemHandler(null));
        items.insertItem(PulverizerBlockEntity.SLOT_INPUT, new ItemStack(Items.RAW_COPPER, 8), false);
        power(pulverizer, 1_000_000);
        helper.succeedWhen(() -> {
            helper.assertTrue(items.getStackInSlot(PulverizerBlockEntity.SLOT_INPUT).isEmpty(), "raw copper left");
            ItemStack out = items.getStackInSlot(PulverizerBlockEntity.SLOT_OUTPUT);
            helper.assertTrue(out.is(VoltaicCore.COPPER_DUST.get()) && out.getCount() >= 8 && out.getCount() <= 16,
                    "output is " + out);
        });
    }

    /** The dusts smelt back into the vanilla ingots. */
    @VoltaicTest
    public static void metalDustsSmeltIntoIngots(GameTestHelper helper) {
        var recipes = helper.getLevel().recipeAccess();
        var registries = helper.getLevel().registryAccess();
        for (var pair : List.of(List.of(VoltaicCore.IRON_DUST.get(), Items.IRON_INGOT),
                List.of(VoltaicCore.COPPER_DUST.get(), Items.COPPER_INGOT),
                List.of(VoltaicCore.GOLD_DUST.get(), Items.GOLD_INGOT))) {
            var result = recipes.getRecipeFor(RecipeType.SMELTING, new SingleRecipeInput(new ItemStack(pair.get(0))), helper.getLevel())
                    .map(r -> r.value().assemble(new SingleRecipeInput(new ItemStack(pair.get(0)))));
            helper.assertTrue(result.isPresent() && result.get().is(pair.get(1)), pair.get(0) + " does not smelt into " + pair.get(1));
        }
        helper.succeed();
    }

    /** The Pulverizer is how good Voltaic Ingots are made, so it must not need one. */
    @VoltaicTest
    public static void pulverizerHasNoBootstrapLoop(GameTestHelper helper) {
        var recipe = helper.getLevel().recipeAccess().byKey(GameTestSupport.recipeKey("pulverizer"));
        helper.assertTrue(recipe.isPresent(), "pulverizer recipe missing");
        for (var ingredient : recipe.get().value().placementInfo().ingredients()) {
            helper.assertTrue(!ingredient.test(new ItemStack(VoltaicCore.VOLTAIC_INGOT.get())), "recipe needs a Voltaic Ingot");
            helper.assertTrue(!ingredient.test(new ItemStack(VoltaicCore.GALVANIC_ALLOY.get())), "recipe needs Galvanic Alloy");
        }
        helper.succeed();
    }

    /** Four Speed Upgrades: three times faster at three times the FE/t; four Efficiency Upgrades: 60% less FE/t. */
    @VoltaicTest
    public static void upgradesChangeSpeedAndEnergy(GameTestHelper helper) {
        BlockPos pos = new BlockPos(2, 1, 2);
        helper.setBlock(pos, VoltaicMachines.ELECTRIC_FURNACE.get());
        ElectricFurnaceBlockEntity furnace = helper.getBlockEntity(pos, ElectricFurnaceBlockEntity.class);
        int baseTicks = furnace.getMaxProgress();
        int baseEnergy = furnace.currentEnergyPerTick();

        furnace.getUpgradeHandler().insertItem(0, new ItemStack(VoltaicMachines.SPEED_UPGRADE.get(), 4), false);
        helper.assertValueEqual(furnace.getMaxProgress(), Math.round(baseTicks / 3.0F), "ticks with 4 speed upgrades");
        helper.assertValueEqual(furnace.currentEnergyPerTick(), baseEnergy * 3, "FE/t with 4 speed upgrades");

        furnace.getUpgradeHandler().extractItem(0, 4, false);
        furnace.getUpgradeHandler().insertItem(1, new ItemStack(VoltaicMachines.EFFICIENCY_UPGRADE.get(), 4), false);
        helper.assertValueEqual(furnace.getMaxProgress(), baseTicks, "ticks with efficiency upgrades only");
        helper.assertValueEqual(furnace.currentEnergyPerTick(), Math.round(baseEnergy * 0.4F), "FE/t with 4 efficiency upgrades");

        helper.assertTrue(furnace.getUpgradeHandler().insertItem(0, new ItemStack(Items.IRON_INGOT), true).getCount() == 1,
                "the upgrade slots take an ingot");
        helper.succeed();
    }

    /**
     * Shift-clicking 64 dusts from the inventory into an Electric Furnace keeps every item: into an empty input slot,
     * and merged into dusts already there (the merge changes the slot's stack in place).
     */
    @VoltaicTest
    public static void shiftClickIntoFurnaceKeepsItems(GameTestHelper helper) {
        for (int already : new int[]{0, 10}) {
            BlockPos pos = new BlockPos(1 + already / 5, 1, 2);
            helper.setBlock(pos, VoltaicMachines.ELECTRIC_FURNACE.get());
            ElectricFurnaceBlockEntity furnace = helper.getBlockEntity(pos, ElectricFurnaceBlockEntity.class);
            if (already > 0) {
                items(furnace.getMenuItemHandler()).setStackInSlot(ElectricFurnaceBlockEntity.SLOT_INPUT,
                        new ItemStack(VoltaicCore.IRON_DUST.get(), already));
            }
            net.minecraft.world.entity.player.Player player = helper.makeMockPlayer(net.minecraft.world.level.GameType.SURVIVAL);
            player.getInventory().setItem(9, new ItemStack(VoltaicCore.IRON_DUST.get(), 64));
            var menu = new com.voltaicdynamics.machines.ElectricFurnaceMenu(0, player.getInventory(), furnace);
            // Slot 0 = input, 1 = output, 2 = first player slot (inventory index 9).
            menu.quickMoveStack(player, 2);
            int inFurnace = items(furnace.getMenuItemHandler()).getStackInSlot(ElectricFurnaceBlockEntity.SLOT_INPUT).getCount();
            int inPlayer = player.getInventory().getItem(9).getCount();
            helper.assertValueEqual(inFurnace + inPlayer, 64 + already, "dusts after shift-click with " + already + " already in");
            helper.assertValueEqual(inFurnace, 64, "dusts in the furnace with " + already + " already in");
        }
        helper.succeed();
    }
}
