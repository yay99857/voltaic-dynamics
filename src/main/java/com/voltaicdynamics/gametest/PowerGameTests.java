package com.voltaicdynamics.gametest;

import com.voltaicdynamics.VoltaicDynamics;
import com.voltaicdynamics.core.ModDataComponents;
import com.voltaicdynamics.generators.CombustionGeneratorBlockEntity;
import com.voltaicdynamics.generators.VoltaicGenerators;
import com.voltaicdynamics.machines.ElectricFurnaceBlockEntity;
import com.voltaicdynamics.machines.VoltaicMachines;
import com.voltaicdynamics.power.AccumulatorBlockEntity;
import com.voltaicdynamics.power.VoltaicPower;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
import java.util.List;

import static com.voltaicdynamics.gametest.GameTestSupport.EMPTY;
import static com.voltaicdynamics.gametest.GameTestSupport.storageOf;

/**
 * Accumulators and the Voltaic Cable network. Run with {@code ./gradlew runGameTestServer};
 * the server exits non-zero when a required test fails. Only loaded when game tests are enabled.
 */
@GameTestHolder(VoltaicDynamics.MOD_ID)
@PrefixGameTestTemplate(false)
public class PowerGameTests {
    /** Breaking a charged cell drops it with its energy; placing it restores the energy. */
    @GameTest(template = EMPTY)
    public static void accumulatorKeepsChargeWhenBroken(GameTestHelper helper) {
        BlockPos pos = new BlockPos(2, 1, 2);
        helper.setBlock(pos, VoltaicPower.BASIC_ACCUMULATOR.get());
        AccumulatorBlockEntity cell = helper.getBlockEntity(pos);
        storageOf(cell).setEnergy(123_456);

        helper.getLevel().destroyBlock(helper.absolutePos(pos), true);

        List<ItemEntity> drops = helper.getEntities(EntityType.ITEM, pos, 2.0);
        helper.assertTrue(drops.size() == 1, "expected one drop, got " + drops.size());
        ItemStack dropped = drops.get(0).getItem();
        helper.assertTrue(dropped.is(VoltaicPower.BASIC_ACCUMULATOR.asItem()), "dropped " + dropped);
        helper.assertValueEqual(dropped.get(ModDataComponents.ENERGY.get()), 123_456, "energy on the dropped cell");

        BlockPos placed = new BlockPos(5, 1, 5);
        helper.setBlock(placed, VoltaicPower.BASIC_ACCUMULATOR.get());
        AccumulatorBlockEntity replaced = helper.getBlockEntity(placed);
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
        helper.setBlock(empty, VoltaicPower.BASIC_ACCUMULATOR.get());
        helper.setBlock(full, VoltaicPower.BASIC_ACCUMULATOR.get());
        storageOf(helper.getBlockEntity(full)).setEnergy(500_000);

        helper.runAfterDelay(20, () -> {
            AccumulatorBlockEntity a = helper.getBlockEntity(full);
            AccumulatorBlockEntity b = helper.getBlockEntity(empty);
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
        helper.setBlock(sink, VoltaicPower.BASIC_ACCUMULATOR.get());
        for (int x = 2; x <= 4; x++) helper.setBlock(new BlockPos(x, 1, 2), VoltaicPower.VOLTAIC_CABLE.get());
        helper.setBlock(source, VoltaicPower.BASIC_ACCUMULATOR.get());
        storageOf(helper.getBlockEntity(source)).setEnergy(500_000);

        helper.runAfterDelay(20, () -> {
            AccumulatorBlockEntity b = helper.getBlockEntity(sink);
            helper.assertValueEqual(b.getEnergyStored(), 0, "energy that reached the second cell");
            helper.succeed();
        });
    }

    /** Cell -> 3 cables -> Electric Furnace: the network delivers energy to the machine. */
    @GameTest(template = EMPTY)
    public static void cableNetworkFeedsMachineFromCell(GameTestHelper helper) {
        BlockPos source = new BlockPos(1, 1, 2);
        BlockPos furnacePos = new BlockPos(5, 1, 2);
        helper.setBlock(source, VoltaicPower.BASIC_ACCUMULATOR.get());
        for (int x = 2; x <= 4; x++) helper.setBlock(new BlockPos(x, 1, 2), VoltaicPower.VOLTAIC_CABLE.get());
        helper.setBlock(furnacePos, VoltaicMachines.ELECTRIC_FURNACE.get());
        storageOf(helper.getBlockEntity(source)).setEnergy(500_000);

        helper.succeedWhen(() -> {
            ElectricFurnaceBlockEntity furnace = helper.getBlockEntity(furnacePos);
            helper.assertTrue(furnace.getEnergyStored() > 0, "furnace has no energy yet");
        });
    }

    /** Combustion Generator -> cable -> Accumulator: generators do charge storage. */
    @GameTest(template = EMPTY, timeoutTicks = 200)
    public static void generatorChargesCellThroughCable(GameTestHelper helper) {
        BlockPos generatorPos = new BlockPos(1, 1, 2);
        BlockPos cellPos = new BlockPos(4, 1, 2);
        helper.setBlock(generatorPos, VoltaicGenerators.COMBUSTION_GENERATOR.get());
        helper.setBlock(new BlockPos(2, 1, 2), VoltaicPower.VOLTAIC_CABLE.get());
        helper.setBlock(new BlockPos(3, 1, 2), VoltaicPower.VOLTAIC_CABLE.get());
        helper.setBlock(cellPos, VoltaicPower.BASIC_ACCUMULATOR.get());
        CombustionGeneratorBlockEntity generator = helper.getBlockEntity(generatorPos);
        generator.getMenuItemHandler().insertItem(0, new ItemStack(Items.COAL), false);

        helper.succeedWhen(() -> {
            AccumulatorBlockEntity cell = helper.getBlockEntity(cellPos);
            helper.assertTrue(cell.getEnergyStored() > 0, "cell has not been charged yet");
        });
    }
}
