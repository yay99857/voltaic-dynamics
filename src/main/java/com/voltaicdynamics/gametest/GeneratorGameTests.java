package com.voltaicdynamics.gametest;

import com.voltaicdynamics.VoltaicDynamics;
import com.voltaicdynamics.generators.CombustionGeneratorBlockEntity;
import com.voltaicdynamics.generators.ThermoelectricGeneratorBlockEntity;
import com.voltaicdynamics.generators.VoltaicGenerators;
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
 * Thermoelectric and Combustion Generators. Run with {@code ./gradlew runGameTestServer};
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
}
