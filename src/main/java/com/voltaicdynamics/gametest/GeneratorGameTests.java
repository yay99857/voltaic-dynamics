package com.voltaicdynamics.gametest;

import com.voltaicdynamics.VoltaicDynamics;
import com.voltaicdynamics.generators.CombustionGeneratorBlockEntity;
import com.voltaicdynamics.generators.ThermoelectricGeneratorBlockEntity;
import com.voltaicdynamics.generators.VoltaicGenerators;
import com.voltaicdynamics.generators.FluidGeneratorBlockEntity;
import net.minecraft.core.Direction;
import net.minecraft.world.level.material.Fluids;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

import static com.voltaicdynamics.gametest.GameTestSupport.EMPTY;

/**
 * Thermoelectric, Combustion and Fluid Generators. Run with {@code ./gradlew runGameTestServer};
 * the server exits non-zero when a required test fails. Only loaded when game tests are enabled.
 */
@GameTestHolder(VoltaicDynamics.MOD_ID)
@PrefixGameTestTemplate(false)
public class GeneratorGameTests {
    /** Burning a lava bucket leaves the empty bucket in the slot instead of deleting it. */
    @GameTest(template = EMPTY)
    public static void combustionReturnsEmptyBucket(GameTestHelper helper) {
        BlockPos pos = new BlockPos(2, 1, 2);
        helper.setBlock(pos, VoltaicGenerators.COMBUSTION_GENERATOR.get());
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
        helper.setBlock(pos, VoltaicGenerators.COMBUSTION_GENERATOR.get());
        CombustionGeneratorBlockEntity generator = helper.getBlockEntity(pos);
        generator.getItemHandler(null).insertItem(0, new ItemStack(Items.COAL, 8), false);
        helper.assertTrue(generator.getItemHandler(null).extractItem(0, 8, true).isEmpty(), "fuel could be pulled out");
        helper.succeed();
    }

    /** Lava next to ice produces power. */
    @GameTest(template = EMPTY)
    public static void thermoelectricGenerates(GameTestHelper helper) {
        BlockPos pos = new BlockPos(3, 2, 3);
        helper.setBlock(pos, VoltaicGenerators.THERMOELECTRIC_GENERATOR.get());
        helper.setBlock(pos.west(), Blocks.MAGMA_BLOCK);
        helper.setBlock(pos.east(), Blocks.BLUE_ICE);
        helper.succeedWhen(() -> {
            ThermoelectricGeneratorBlockEntity generator = helper.getBlockEntity(pos);
            helper.assertTrue(generator.getEnergyStored() > 0, "no energy generated");
        });
    }

    /** Lava poured in through a pipe face burns at 80 FE/t and 200 FE per mB. */
    @GameTest(template = EMPTY, timeoutTicks = 100)
    public static void fluidGeneratorBurnsLava(GameTestHelper helper) {
        BlockPos pos = new BlockPos(2, 1, 2);
        helper.setBlock(pos, VoltaicGenerators.FLUID_GENERATOR.get());
        IFluidHandler pipe = helper.getLevel().getCapability(Capabilities.FluidHandler.BLOCK, helper.absolutePos(pos), Direction.UP);
        helper.assertTrue(pipe != null, "no fluid capability on top");
        helper.assertValueEqual(pipe.fill(new FluidStack(Fluids.WATER, 1_000), IFluidHandler.FluidAction.EXECUTE), 0,
                "water accepted as fuel");
        helper.assertValueEqual(pipe.fill(new FluidStack(Fluids.LAVA, 1_000), IFluidHandler.FluidAction.EXECUTE), 1_000,
                "lava accepted");
        helper.runAfterDelay(20, () -> {
            FluidGeneratorBlockEntity generator = helper.getBlockEntity(pos);
            helper.assertValueEqual(generator.getCurrentRate(), 80, "FE/t");
            int energy = generator.getEnergyStored();
            int burned = 1_000 - generator.getTank().getFluidAmount();
            helper.assertTrue(energy >= 20 * 80 - 80 && energy <= 21 * 80, "energy after 20 ticks: " + energy);
            // FE made so far (plus at most one mB of prepaid credit) matches 200 FE per mB burned.
            helper.assertTrue(Math.abs(burned * 200 - energy) <= 200, burned + " mB burned for " + energy + " FE");
            helper.succeed();
        });
    }

    /** A lava bucket in the in slot is poured into the tank and the empty bucket moves to the out slot. */
    @GameTest(template = EMPTY)
    public static void fluidGeneratorEmptiesBuckets(GameTestHelper helper) {
        BlockPos pos = new BlockPos(2, 1, 2);
        helper.setBlock(pos, VoltaicGenerators.FLUID_GENERATOR.get());
        FluidGeneratorBlockEntity generator = helper.getBlockEntity(pos);
        generator.getMenuItemHandler().insertItem(FluidGeneratorBlockEntity.SLOT_IN, new ItemStack(Items.LAVA_BUCKET), false);
        helper.succeedWhen(() -> {
            helper.assertTrue(generator.getMenuItemHandler().getStackInSlot(FluidGeneratorBlockEntity.SLOT_OUT).is(Items.BUCKET),
                    "no empty bucket in the out slot");
            helper.assertTrue(generator.getTank().getFluidAmount() > 900, "lava not in the tank");
        });
    }

    /** Fuel lasts half its furnace burn time (config burnTimeMultiplier 0.5): a coal is 800 ticks, 32,000 FE. */
    @GameTest(template = EMPTY)
    public static void combustionBurnTimeIsScaled(GameTestHelper helper) {
        BlockPos pos = new BlockPos(2, 1, 2);
        helper.setBlock(pos, VoltaicGenerators.COMBUSTION_GENERATOR.get());
        CombustionGeneratorBlockEntity generator = helper.getBlockEntity(pos);
        generator.getMenuItemHandler().insertItem(0, new ItemStack(Items.COAL), false);
        helper.succeedWhen(() -> helper.assertValueEqual(generator.getMaxBurnTime(), 800, "coal burn time"));
    }
}
