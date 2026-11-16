package com.voltaicdynamics.gametest;

import com.voltaicdynamics.VoltaicConfig;
import com.voltaicdynamics.VoltaicDynamics;
import com.voltaicdynamics.core.VoltaicCore;
import com.voltaicdynamics.core.energy.VoltaicEnergyStorage;
import com.voltaicdynamics.datagen.ModTags;
import com.voltaicdynamics.machines.AlloySmelterBlockEntity;
import com.voltaicdynamics.machines.ElectricFurnaceBlockEntity;
import com.voltaicdynamics.machines.VoltaicMachines;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.CraftingInput;
import net.minecraft.world.item.crafting.RecipeType;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
import net.minecraft.core.Direction;
import java.util.List;

import static com.voltaicdynamics.gametest.GameTestSupport.EMPTY;
import static com.voltaicdynamics.gametest.GameTestSupport.power;

/**
 * Electric Furnace, Alloy Smelter and the recipe set. Run with {@code ./gradlew runGameTestServer};
 * the server exits non-zero when a required test fails. Only loaded when game tests are enabled.
 */
@GameTestHolder(VoltaicDynamics.MOD_ID)
@PrefixGameTestTemplate(false)
public class MachineGameTests {
    /** The Electric Furnace smelts with FE and pauses without it. */
    @GameTest(template = EMPTY, timeoutTicks = 300)
    public static void electricFurnaceSmelts(GameTestHelper helper) {
        BlockPos pos = new BlockPos(2, 1, 2);
        helper.setBlock(pos, VoltaicMachines.ELECTRIC_FURNACE.get());
        ElectricFurnaceBlockEntity furnace = helper.getBlockEntity(pos);
        furnace.getItemHandler(null).insertItem(ElectricFurnaceBlockEntity.SLOT_INPUT, new ItemStack(Items.RAW_IRON, 2), false);

        helper.runAfterDelay(10, () -> {
            helper.assertValueEqual(furnace.getProgress(), 0, "progress without energy");
            int perItem = VoltaicConfig.get(VoltaicConfig.FURNACE_ENERGY_PER_TICK) * VoltaicConfig.get(VoltaicConfig.FURNACE_TICKS_PER_ITEM);
            ((VoltaicEnergyStorage) furnace.getEnergyStorage(null)).setEnergy(perItem * 2);
            helper.succeedWhen(() -> {
                ItemStack out = furnace.getItemHandler(null).getStackInSlot(ElectricFurnaceBlockEntity.SLOT_OUTPUT);
                helper.assertTrue(out.is(Items.IRON_INGOT) && out.getCount() == 2, "output is " + out);
            });
        });
    }

    /** The cached recipe follows the inputs even when a stack is changed in place (as menus do), so nothing smelts for free. */
    @GameTest(template = EMPTY, timeoutTicks = 400)
    public static void electricFurnaceRecipeCacheFollowsInputs(GameTestHelper helper) {
        BlockPos pos = new BlockPos(2, 1, 2);
        helper.setBlock(pos, VoltaicMachines.ELECTRIC_FURNACE.get());
        ElectricFurnaceBlockEntity furnace = helper.getBlockEntity(pos);
        var items = furnace.getMenuItemHandler();
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

    /** Ingots come only from smelting dust: the old shapeless shortcut recipes are gone. */
    @GameTest(template = EMPTY)
    public static void noDuplicateAlloyRecipes(GameTestHelper helper) {
        var recipes = helper.getLevel().getRecipeManager();
        helper.assertTrue(recipes.byKey(ResourceLocation.fromNamespaceAndPath(VoltaicDynamics.MOD_ID, "voltaic_ingot")).isEmpty(),
                "shapeless voltaic_ingot recipe still exists");
        helper.assertTrue(recipes.byKey(ResourceLocation.fromNamespaceAndPath(VoltaicDynamics.MOD_ID, "galvanic_alloy")).isEmpty(),
                "shapeless galvanic_alloy recipe still exists");

        // Voltaic Dust takes dusts; ingots only work as a fallback when no mod provides the dusts.
        CraftingInput ingots = CraftingInput.of(3, 1, List.of(
                new ItemStack(Items.IRON_INGOT), new ItemStack(Items.COPPER_INGOT), new ItemStack(Items.REDSTONE)));
        var registries = helper.getLevel().registryAccess();
        boolean dustsExist = BuiltInRegistries.ITEM.getTag(ModTags.DUSTS_IRON).map(t -> t.size() > 0).orElse(false)
                && BuiltInRegistries.ITEM.getTag(ModTags.DUSTS_COPPER).map(t -> t.size() > 0).orElse(false);
        boolean ingotsMakeDust = recipes.getRecipesFor(RecipeType.CRAFTING, ingots, helper.getLevel()).stream()
                .anyMatch(r -> r.value().getResultItem(registries).is(VoltaicCore.VOLTAIC_DUST.get()));
        helper.assertTrue(ingotsMakeDust != dustsExist,
                dustsExist ? "ingots still make voltaic dust although dusts exist" : "no fallback voltaic dust recipe without dusts");

        // With an ore-processing mod installed (as in Bubie Pack) the dust recipe must exist and work.
        var ironDust = BuiltInRegistries.ITEM.getTag(ModTags.DUSTS_IRON).flatMap(t -> t.stream().findFirst());
        var copperDust = BuiltInRegistries.ITEM.getTag(ModTags.DUSTS_COPPER).flatMap(t -> t.stream().findFirst());
        if (ironDust.isPresent() && copperDust.isPresent()) {
            CraftingInput dusts = CraftingInput.of(3, 1, List.of(new ItemStack(ironDust.get()),
                    new ItemStack(copperDust.get()), new ItemStack(Items.REDSTONE)));
            helper.assertTrue(recipes.getRecipesFor(RecipeType.CRAFTING, dusts, helper.getLevel()).stream()
                            .anyMatch(r -> r.value().getResultItem(registries).is(VoltaicCore.VOLTAIC_DUST.get())),
                    "iron dust + copper dust + redstone does not make voltaic dust");
        } else {
            helper.assertTrue(recipes.byKey(ResourceLocation.fromNamespaceAndPath(VoltaicDynamics.MOD_ID, "voltaic_dust")).isEmpty(),
                    "voltaic_dust recipe loaded although no mod provides iron or copper dust");
        }
        helper.succeed();
    }

    /** Iron + copper + redstone ingots go straight to Voltaic Ingots, no dust needed. */
    @GameTest(template = EMPTY, timeoutTicks = 400)
    public static void alloySmelterMakesVoltaicIngots(GameTestHelper helper) {
        BlockPos pos = new BlockPos(2, 1, 2);
        helper.setBlock(pos, VoltaicMachines.ALLOY_SMELTER.get());
        AlloySmelterBlockEntity smelter = helper.getBlockEntity(pos);
        var items = smelter.getItemHandler(null);
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
    @GameTest(template = EMPTY)
    public static void alloySmelterSpreadsPipeInput(GameTestHelper helper) {
        BlockPos pos = new BlockPos(2, 1, 2);
        helper.setBlock(pos, VoltaicMachines.ALLOY_SMELTER.get());
        AlloySmelterBlockEntity smelter = helper.getBlockEntity(pos);
        var items = smelter.getItemHandler(Direction.UP);
        helper.assertTrue(items.insertItem(0, new ItemStack(Items.IRON_INGOT, 8), false).isEmpty(), "iron refused");
        helper.assertTrue(!items.insertItem(1, new ItemStack(Items.IRON_INGOT, 8), false).isEmpty(), "iron went into a second slot");
        helper.assertTrue(items.insertItem(1, new ItemStack(Items.COPPER_INGOT, 8), false).isEmpty(), "copper refused");
        helper.assertTrue(!items.insertItem(2, new ItemStack(Items.DIRT), false).isEmpty(), "dirt accepted");
        helper.assertTrue(items.extractItem(0, 1, true).isEmpty(), "pipes could pull an input back out");
        helper.succeed();
    }

    /** The Alloy Smelter recipe does not need the Galvanic Alloy that only it can make. */
    @GameTest(template = EMPTY)
    public static void alloySmelterHasNoBootstrapLoop(GameTestHelper helper) {
        var recipe = helper.getLevel().getRecipeManager()
                .byKey(ResourceLocation.fromNamespaceAndPath(VoltaicDynamics.MOD_ID, "alloy_smelter"));
        helper.assertTrue(recipe.isPresent(), "alloy_smelter recipe missing");
        for (var ingredient : recipe.get().value().getIngredients()) {
            helper.assertTrue(!ingredient.test(new ItemStack(VoltaicCore.GALVANIC_ALLOY.get())), "recipe needs Galvanic Alloy");
        }
        helper.succeed();
    }
}
