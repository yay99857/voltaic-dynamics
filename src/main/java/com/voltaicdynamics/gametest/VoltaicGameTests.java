package com.voltaicdynamics.gametest;

import com.voltaicdynamics.ModDataComponents;
import com.voltaicdynamics.VoltaicConfig;
import com.voltaicdynamics.VoltaicDynamics;
import com.voltaicdynamics.block.ModBlocks;
import com.voltaicdynamics.blockentity.CombustionGeneratorBlockEntity;
import com.voltaicdynamics.blockentity.ElectricFurnaceBlockEntity;
import com.voltaicdynamics.blockentity.EnergyCellBlockEntity;
import com.voltaicdynamics.blockentity.ThermoelectricGeneratorBlockEntity;
import com.voltaicdynamics.energy.VoltaicEnergyStorage;
import com.voltaicdynamics.item.ModItems;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.CraftingInput;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

import java.util.List;

/**
 * In-world checks for the fixes in this version. Run with {@code ./gradlew runGameTestServer};
 * the server exits non-zero when a required test fails. Only loaded when game tests are enabled.
 */
@GameTestHolder(VoltaicDynamics.MOD_ID)
@PrefixGameTestTemplate(false)
public class VoltaicGameTests {
    private static final String EMPTY = "empty";

    private static VoltaicEnergyStorage storageOf(EnergyCellBlockEntity cell) {
        return (VoltaicEnergyStorage) cell.getEnergyStorage(null);
    }

    /** Machines are pickaxe-mineable, so requiresCorrectToolForDrops() lets them drop. */
    @GameTest(template = EMPTY)
    public static void machinesAreMineableWithPickaxe(GameTestHelper helper) {
        ItemStack pickaxe = new ItemStack(Items.STONE_PICKAXE);
        for (Block block : List.of(ModBlocks.THERMOELECTRIC_GENERATOR.get(), ModBlocks.COMBUSTION_GENERATOR.get(),
                ModBlocks.ELECTRIC_FURNACE.get(), ModBlocks.BASIC_ENERGY_CELL.get(), ModBlocks.ADVANCED_ENERGY_CELL.get())) {
            BlockState state = block.defaultBlockState();
            helper.assertTrue(state.is(BlockTags.MINEABLE_WITH_PICKAXE), block + " is not in mineable/pickaxe");
            helper.assertTrue(pickaxe.isCorrectToolForDrops(state), "a pickaxe is not the correct tool for " + block);
        }
        helper.succeed();
    }

    /** Breaking a charged cell drops it with its energy; placing it restores the energy. */
    @GameTest(template = EMPTY)
    public static void energyCellKeepsChargeWhenBroken(GameTestHelper helper) {
        BlockPos pos = new BlockPos(2, 1, 2);
        helper.setBlock(pos, ModBlocks.BASIC_ENERGY_CELL.get());
        EnergyCellBlockEntity cell = helper.getBlockEntity(pos);
        storageOf(cell).setEnergy(123_456);

        helper.getLevel().destroyBlock(helper.absolutePos(pos), true);

        List<ItemEntity> drops = helper.getEntities(EntityType.ITEM, pos, 2.0);
        helper.assertTrue(drops.size() == 1, "expected one drop, got " + drops.size());
        ItemStack dropped = drops.get(0).getItem();
        helper.assertTrue(dropped.is(ModBlocks.BASIC_ENERGY_CELL.asItem()), "dropped " + dropped);
        helper.assertValueEqual(dropped.get(ModDataComponents.ENERGY.get()), 123_456, "energy on the dropped cell");

        BlockPos placed = new BlockPos(5, 1, 5);
        helper.setBlock(placed, ModBlocks.BASIC_ENERGY_CELL.get());
        EnergyCellBlockEntity replaced = helper.getBlockEntity(placed);
        replaced.applyComponentsFromItemStack(dropped);
        helper.assertValueEqual(replaced.getEnergyStored(), 123_456, "energy after placing the cell again");
        helper.succeed();
    }

    /** Two touching cells used to bounce energy every tick; now a full cell never feeds an empty one. */
    @GameTest(template = EMPTY)
    public static void adjacentCellsDoNotPingPong(GameTestHelper helper) {
        BlockPos full = new BlockPos(2, 1, 2);
        BlockPos empty = new BlockPos(3, 1, 2);
        // The empty cell is placed (and so ticks) first: a bounce would leave energy in it at the end of each tick.
        helper.setBlock(empty, ModBlocks.BASIC_ENERGY_CELL.get());
        helper.setBlock(full, ModBlocks.BASIC_ENERGY_CELL.get());
        storageOf(helper.getBlockEntity(full)).setEnergy(500_000);

        helper.runAfterDelay(20, () -> {
            EnergyCellBlockEntity a = helper.getBlockEntity(full);
            EnergyCellBlockEntity b = helper.getBlockEntity(empty);
            helper.assertValueEqual(b.getEnergyStored(), 0, "energy that leaked into the second cell");
            helper.assertValueEqual(a.getEnergyStored(), 500_000, "energy left in the first cell");
            helper.succeed();
        });
    }

    /** Cell -> cable -> cell: storage never charges storage through the network. */
    @GameTest(template = EMPTY)
    public static void cableDoesNotMoveEnergyBetweenCells(GameTestHelper helper) {
        BlockPos source = new BlockPos(1, 1, 2);
        BlockPos sink = new BlockPos(5, 1, 2);
        helper.setBlock(sink, ModBlocks.BASIC_ENERGY_CELL.get());
        for (int x = 2; x <= 4; x++) helper.setBlock(new BlockPos(x, 1, 2), ModBlocks.ENERGY_CABLE.get());
        helper.setBlock(source, ModBlocks.BASIC_ENERGY_CELL.get());
        storageOf(helper.getBlockEntity(source)).setEnergy(500_000);

        helper.runAfterDelay(20, () -> {
            EnergyCellBlockEntity b = helper.getBlockEntity(sink);
            helper.assertValueEqual(b.getEnergyStored(), 0, "energy that reached the second cell");
            helper.succeed();
        });
    }

    /** Cell -> 3 cables -> Electric Furnace: the network delivers energy to the machine. */
    @GameTest(template = EMPTY)
    public static void cableNetworkFeedsMachineFromCell(GameTestHelper helper) {
        BlockPos source = new BlockPos(1, 1, 2);
        BlockPos furnacePos = new BlockPos(5, 1, 2);
        helper.setBlock(source, ModBlocks.BASIC_ENERGY_CELL.get());
        for (int x = 2; x <= 4; x++) helper.setBlock(new BlockPos(x, 1, 2), ModBlocks.ENERGY_CABLE.get());
        helper.setBlock(furnacePos, ModBlocks.ELECTRIC_FURNACE.get());
        storageOf(helper.getBlockEntity(source)).setEnergy(500_000);

        helper.succeedWhen(() -> {
            ElectricFurnaceBlockEntity furnace = helper.getBlockEntity(furnacePos);
            helper.assertTrue(furnace.getEnergyStored() > 0, "furnace has no energy yet");
        });
    }

    /** Combustion Generator -> cable -> Energy Cell: generators do charge storage. */
    @GameTest(template = EMPTY, timeoutTicks = 200)
    public static void generatorChargesCellThroughCable(GameTestHelper helper) {
        BlockPos generatorPos = new BlockPos(1, 1, 2);
        BlockPos cellPos = new BlockPos(4, 1, 2);
        helper.setBlock(generatorPos, ModBlocks.COMBUSTION_GENERATOR.get());
        helper.setBlock(new BlockPos(2, 1, 2), ModBlocks.ENERGY_CABLE.get());
        helper.setBlock(new BlockPos(3, 1, 2), ModBlocks.ENERGY_CABLE.get());
        helper.setBlock(cellPos, ModBlocks.BASIC_ENERGY_CELL.get());
        CombustionGeneratorBlockEntity generator = helper.getBlockEntity(generatorPos);
        generator.getMenuItemHandler().insertItem(0, new ItemStack(Items.COAL), false);

        helper.succeedWhen(() -> {
            EnergyCellBlockEntity cell = helper.getBlockEntity(cellPos);
            helper.assertTrue(cell.getEnergyStored() > 0, "cell has not been charged yet");
        });
    }

    /** Burning a lava bucket leaves the empty bucket in the slot instead of deleting it. */
    @GameTest(template = EMPTY)
    public static void combustionReturnsEmptyBucket(GameTestHelper helper) {
        BlockPos pos = new BlockPos(2, 1, 2);
        helper.setBlock(pos, ModBlocks.COMBUSTION_GENERATOR.get());
        CombustionGeneratorBlockEntity generator = helper.getBlockEntity(pos);
        generator.getMenuItemHandler().insertItem(0, new ItemStack(Items.LAVA_BUCKET), false);

        helper.succeedWhen(() -> {
            helper.assertTrue(generator.getBurnTime() > 0, "lava bucket was not burned");
            ItemStack left = generator.getMenuItemHandler().getStackInSlot(0);
            helper.assertTrue(left.is(Items.BUCKET), "slot holds " + left + " instead of an empty bucket");
            helper.assertTrue(generator.getItemHandler(null).extractItem(0, 1, true).is(Items.BUCKET),
                    "pipes cannot take the empty bucket out");
        });
    }

    /** Pipes cannot pull fuel back out of the generator. */
    @GameTest(template = EMPTY)
    public static void combustionFuelIsNotExtractable(GameTestHelper helper) {
        BlockPos pos = new BlockPos(2, 1, 2);
        helper.setBlock(pos, ModBlocks.COMBUSTION_GENERATOR.get());
        CombustionGeneratorBlockEntity generator = helper.getBlockEntity(pos);
        generator.getItemHandler(null).insertItem(0, new ItemStack(Items.COAL, 8), false);
        helper.assertTrue(generator.getItemHandler(null).extractItem(0, 8, true).isEmpty(), "fuel could be pulled out");
        helper.succeed();
    }

    /** The Electric Furnace smelts with FE and pauses without it. */
    @GameTest(template = EMPTY, timeoutTicks = 300)
    public static void electricFurnaceSmelts(GameTestHelper helper) {
        BlockPos pos = new BlockPos(2, 1, 2);
        helper.setBlock(pos, ModBlocks.ELECTRIC_FURNACE.get());
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

    /** Lava next to ice produces power. */
    @GameTest(template = EMPTY)
    public static void thermoelectricGenerates(GameTestHelper helper) {
        BlockPos pos = new BlockPos(3, 2, 3);
        helper.setBlock(pos, ModBlocks.THERMOELECTRIC_GENERATOR.get());
        helper.setBlock(pos.west(), Blocks.MAGMA_BLOCK);
        helper.setBlock(pos.east(), Blocks.BLUE_ICE);
        helper.succeedWhen(() -> {
            ThermoelectricGeneratorBlockEntity generator = helper.getBlockEntity(pos);
            helper.assertTrue(generator.getEnergyStored() > 0, "no energy generated");
        });
    }

    /** Ingots come only from smelting dust: the old shapeless shortcut recipes are gone. */
    @GameTest(template = EMPTY)
    public static void noDuplicateAlloyRecipes(GameTestHelper helper) {
        var recipes = helper.getLevel().getRecipeManager();
        helper.assertTrue(recipes.byKey(ResourceLocation.fromNamespaceAndPath(VoltaicDynamics.MOD_ID, "voltaic_ingot")).isEmpty(),
                "shapeless voltaic_ingot recipe still exists");
        helper.assertTrue(recipes.byKey(ResourceLocation.fromNamespaceAndPath(VoltaicDynamics.MOD_ID, "conductive_alloy")).isEmpty(),
                "shapeless conductive_alloy recipe still exists");

        CraftingInput input = CraftingInput.of(3, 1, List.of(
                new ItemStack(Items.IRON_INGOT), new ItemStack(Items.COPPER_INGOT), new ItemStack(Items.REDSTONE)));
        var matches = recipes.getRecipesFor(RecipeType.CRAFTING, input, helper.getLevel());
        helper.assertValueEqual(matches.size(), 1, "recipes matching iron + copper + redstone");
        helper.assertTrue(matches.get(0).value().getResultItem(helper.getLevel().registryAccess()).is(ModItems.VOLTAIC_DUST.get()),
                "iron + copper + redstone does not make voltaic dust");
        helper.succeed();
    }
}
