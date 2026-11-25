package com.voltaicdynamics.gametest;

import static com.voltaicdynamics.gametest.GameTestSupport.fluids;
import static com.voltaicdynamics.gametest.GameTestSupport.items;
import com.voltaicdynamics.VoltaicDynamics;
import com.voltaicdynamics.generators.CombustionGeneratorBlockEntity;
import com.voltaicdynamics.generators.VoltaicGenerators;
import com.voltaicdynamics.machines.ElectricFurnaceBlockEntity;
import com.voltaicdynamics.machines.VoltaicMachines;
import com.voltaicdynamics.power.AccumulatorBlockEntity;
import com.voltaicdynamics.power.VoltaicPower;
import com.voltaicdynamics.core.sideconfig.TransferType;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.HopperBlock;
import net.minecraft.world.level.block.entity.HopperBlockEntity;
import com.voltaicdynamics.logistics.ConnectionType;
import com.voltaicdynamics.logistics.FluidTankBlockEntity;
import com.voltaicdynamics.logistics.LogisticsCableBlock;
import com.voltaicdynamics.logistics.VoltaicLogistics;
import net.minecraft.core.Direction;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.Fluids;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.fluids.FluidStack;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Block;
import net.neoforged.fml.ModList;

import static com.voltaicdynamics.gametest.GameTestSupport.power;
import static com.voltaicdynamics.gametest.GameTestSupport.storageOf;

/**
 * Our blocks against other mods' FE cables. Mekanism is loaded in the dev runs (see build.gradle); without it
 * these tests pass without checking anything.
 */
public class CompatGameTests {
    private static final Identifier MEKANISM_CABLE = Identifier.fromNamespaceAndPath("mekanism", "basic_universal_cable");

    private static boolean placeMekanismCables(GameTestHelper helper, int fromX, int toX) {
        if (!ModList.get().isLoaded("mekanism")) return false;
        Block cable = BuiltInRegistries.BLOCK.getValue(MEKANISM_CABLE);
        for (int x = fromX; x <= toX; x++) helper.setBlock(new BlockPos(x, 1, 2), cable);
        return true;
    }

    /**
     * Accumulator -> Mekanism Universal Cables -> Electric Furnace. Mekanism's cable reports canExtract(), which
     * the Accumulator used to take for another battery and skip.
     */
    @VoltaicTest(timeoutTicks = 200)
    public static void accumulatorFeedsMekanismCable(GameTestHelper helper) {
        BlockPos cellPos = new BlockPos(1, 1, 2);
        BlockPos furnacePos = new BlockPos(5, 1, 2);
        helper.setBlock(cellPos, VoltaicPower.BASIC_ACCUMULATOR.get());
        helper.setBlock(furnacePos, VoltaicMachines.ELECTRIC_FURNACE.get());
        if (!placeMekanismCables(helper, 2, 4)) {
            helper.succeed();
            return;
        }
        storageOf(helper.getBlockEntity(cellPos, AccumulatorBlockEntity.class)).setEnergy(500_000);
        helper.succeedWhen(() -> {
            ElectricFurnaceBlockEntity furnace = helper.getBlockEntity(furnacePos, ElectricFurnaceBlockEntity.class);
            helper.assertTrue(furnace.getEnergyStored() > 0, "furnace got no energy through the Mekanism cable");
        });
    }

    /**
     * Combustion Generator -> Mekanism Universal Cables -> Accumulator. The cell used to push everything it got
     * straight back into the cable, so it never charged.
     */
    @VoltaicTest(timeoutTicks = 200)
    public static void generatorFeedsMekanismCable(GameTestHelper helper) {
        BlockPos generatorPos = new BlockPos(1, 1, 2);
        BlockPos cellPos = new BlockPos(5, 1, 2);
        helper.setBlock(generatorPos, VoltaicGenerators.COMBUSTION_GENERATOR.get());
        helper.setBlock(cellPos, VoltaicPower.BASIC_ACCUMULATOR.get());
        if (!placeMekanismCables(helper, 2, 4)) {
            helper.succeed();
            return;
        }
        CombustionGeneratorBlockEntity generator = helper.getBlockEntity(generatorPos, CombustionGeneratorBlockEntity.class);
        items(generator.getMenuItemHandler()).insertItem(0, new ItemStack(Items.COAL_BLOCK), false);

        helper.succeedWhen(() -> {
            AccumulatorBlockEntity cell = helper.getBlockEntity(cellPos, AccumulatorBlockEntity.class);
            // Well above what the cables buffer: the cell keeps the energy instead of handing it back.
            helper.assertTrue(cell.getEnergyStored() > 5_000, "accumulator holds only " + cell.getEnergyStored() + " FE");
        });
    }

    /**
     * Mekanism Basic Fluid Tank -> our Fluid Cable with a pulling arm on its top -> our Fluid Tank. Mekanism's
     * tank does not push by itself, so something has to pull: our cable's pull arm does.
     */
    @VoltaicTest(timeoutTicks = 200)
    public static void fluidCablePullsFromMekanismTank(GameTestHelper helper) {
        if (!ModList.get().isLoaded("mekanism")) {
            helper.succeed();
            return;
        }
        BlockPos mekTank = new BlockPos(1, 1, 2);
        BlockPos ourTank = new BlockPos(3, 1, 2);
        BlockPos[] cables = {new BlockPos(1, 2, 2), new BlockPos(2, 2, 2), new BlockPos(3, 2, 2)};
        helper.setBlock(mekTank, BuiltInRegistries.BLOCK.getValue(Identifier.fromNamespaceAndPath("mekanism", "basic_fluid_tank")));
        helper.setBlock(ourTank, VoltaicLogistics.FLUID_TANK.get());
        for (BlockPos cable : cables) helper.setBlock(cable, VoltaicLogistics.FLUID_CABLE.get());
        for (BlockPos rel : cables) {
            BlockPos abs = helper.absolutePos(rel);
            BlockState state = helper.getLevel().getBlockState(abs);
            helper.getLevel().setBlock(abs, Block.updateFromNeighbourShapes(state, helper.getLevel(), abs), Block.UPDATE_ALL);
        }
        BlockPos pullAbs = helper.absolutePos(cables[0]);
        helper.getLevel().setBlock(pullAbs, helper.getLevel().getBlockState(pullAbs)
                .setValue(LogisticsCableBlock.property(Direction.DOWN), ConnectionType.PULL), Block.UPDATE_ALL);

        // Mekanism's tank refuses fills without a side.
        var mekanism = fluids(helper.getLevel().getCapability(Capabilities.Fluid.BLOCK,
                helper.absolutePos(mekTank), Direction.DOWN));
        helper.assertTrue(mekanism != null, "Mekanism tank has no fluid capability");
        mekanism.fill(new FluidStack(Fluids.WATER, 8_000), false);

        helper.succeedWhen(() -> {
            FluidTankBlockEntity tank = helper.getBlockEntity(ourTank, FluidTankBlockEntity.class);
            helper.assertValueEqual(tank.getFluid().getAmount(), 8_000, "water pulled into our tank");
        });
    }

    /** Our Fluid Tank with fluid auto-eject on pushes into a Mekanism Basic Fluid Tank below it. */
    @VoltaicTest(timeoutTicks = 200)
    public static void fluidTankEjectsIntoMekanismTank(GameTestHelper helper) {
        if (!ModList.get().isLoaded("mekanism")) {
            helper.succeed();
            return;
        }
        BlockPos ourTank = new BlockPos(2, 2, 2);
        BlockPos mekTank = new BlockPos(2, 1, 2);
        helper.setBlock(mekTank, BuiltInRegistries.BLOCK.getValue(Identifier.fromNamespaceAndPath("mekanism", "basic_fluid_tank")));
        helper.setBlock(ourTank, VoltaicLogistics.FLUID_TANK.get());
        FluidTankBlockEntity tank = helper.getBlockEntity(ourTank, FluidTankBlockEntity.class);
        fluids(tank.getTank()).fill(new FluidStack(Fluids.WATER, 4_000), false);
        tank.getSideConfig().setEject(TransferType.FLUID, true);
        tank.onSideConfigChanged();

        helper.succeedWhen(() -> {
            var mekanism = fluids(helper.getLevel().getCapability(Capabilities.Fluid.BLOCK,
                    helper.absolutePos(mekTank), Direction.UP));
            helper.assertValueEqual(mekanism.handler().getAmountAsInt(0), 4_000, "water in the Mekanism tank");
        });
    }

    /** Vanilla hoppers: one above feeds the Electric Furnace, one below takes the ingots out. */
    @VoltaicTest(timeoutTicks = 400)
    public static void hoppersFeedAndEmptyFurnace(GameTestHelper helper) {
        BlockPos furnacePos = new BlockPos(2, 2, 2);
        helper.setBlock(furnacePos.above(), Blocks.HOPPER.defaultBlockState().setValue(HopperBlock.FACING, Direction.DOWN));
        helper.setBlock(furnacePos, VoltaicMachines.ELECTRIC_FURNACE.get());
        helper.setBlock(furnacePos.below(), Blocks.HOPPER.defaultBlockState().setValue(HopperBlock.FACING, Direction.DOWN));
        helper.setBlock(furnacePos.below(2), Blocks.CHEST);
        ElectricFurnaceBlockEntity furnace = helper.getBlockEntity(furnacePos, ElectricFurnaceBlockEntity.class);
        power(furnace, 100_000);
        HopperBlockEntity top = helper.getBlockEntity(furnacePos.above(), HopperBlockEntity.class);
        top.setItem(0, new ItemStack(Items.RAW_IRON, 2));

        helper.succeedWhen(() -> {
            var chest = items(helper.getLevel().getCapability(Capabilities.Item.BLOCK,
                    helper.absolutePos(furnacePos.below(2)), null));
            int ingots = 0;
            for (int i = 0; i < chest.getSlots(); i++) {
                if (chest.getStackInSlot(i).is(Items.IRON_INGOT)) ingots += chest.getStackInSlot(i).getCount();
            }
            helper.assertValueEqual(ingots, 2, "ingots that reached the chest through the hoppers");
        });
    }
}
