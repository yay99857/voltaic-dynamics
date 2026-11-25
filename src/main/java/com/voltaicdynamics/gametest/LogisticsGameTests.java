package com.voltaicdynamics.gametest;

import static com.voltaicdynamics.gametest.GameTestSupport.fluids;
import static com.voltaicdynamics.gametest.GameTestSupport.items;
import com.voltaicdynamics.VoltaicDynamics;
import com.voltaicdynamics.core.ModDataComponents;
import com.voltaicdynamics.logistics.ConnectionType;
import com.voltaicdynamics.logistics.FluidTankBlockEntity;
import com.voltaicdynamics.logistics.LogisticsCableBlock;
import com.voltaicdynamics.logistics.VoltaicLogistics;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.core.Direction;
import net.minecraft.world.level.material.Fluids;
import net.neoforged.neoforge.fluids.FluidStack;
import java.util.List;


/**
 * Item and fluid cables and the Fluid Tank. Run with {@code ./gradlew runGameTestServer};
 * the server exits non-zero when a required test fails. Only loaded when game tests are enabled.
 */
public class LogisticsGameTests {
    /** Places every cable's arms from its neighbours, as a player placing them would. */
    private static void connectCables(GameTestHelper helper, BlockPos... cables) {
        for (BlockPos rel : cables) {
            BlockPos abs = helper.absolutePos(rel);
            BlockState state = helper.getLevel().getBlockState(abs);
            helper.getLevel().setBlock(abs, Block.updateFromNeighbourShapes(state, helper.getLevel(), abs), Block.UPDATE_ALL);
        }
    }

    private static void setArm(GameTestHelper helper, BlockPos cable, Direction side, ConnectionType type) {
        BlockPos abs = helper.absolutePos(cable);
        BlockState state = helper.getLevel().getBlockState(abs);
        helper.getLevel().setBlock(abs, state.setValue(LogisticsCableBlock.property(side), type), Block.UPDATE_ALL);
    }

    /** Chest -> pulling connector -> item cables -> chest. */
    /** The everyday order: the cable is already there and the chest is placed next to it. */
    @VoltaicTest(timeoutTicks = 20)
    public static void itemCableConnectsToAChestPlacedLater(GameTestHelper helper) {
        BlockPos cable = new BlockPos(2, 1, 2), chest = new BlockPos(3, 1, 2), furnace = new BlockPos(2, 1, 3);
        helper.setBlock(cable, VoltaicLogistics.ITEM_CABLE.get());
        connectCables(helper, cable);
        helper.assertTrue(LogisticsCableBlock.connection(helper.getBlockState(cable), Direction.EAST) == ConnectionType.NONE,
                "nothing to the east yet");
        helper.setBlock(chest, Blocks.CHEST);
        helper.setBlock(furnace, Blocks.FURNACE);
        helper.assertTrue(LogisticsCableBlock.connection(helper.getBlockState(cable), Direction.EAST) == ConnectionType.BLOCK,
                "the arm reaches the chest placed after the cable");
        helper.assertTrue(LogisticsCableBlock.connection(helper.getBlockState(cable), Direction.SOUTH) == ConnectionType.BLOCK,
                "and the furnace");
        helper.succeed();
    }

    @VoltaicTest(timeoutTicks = 200)
    public static void itemCableMovesItems(GameTestHelper helper) {
        BlockPos source = new BlockPos(1, 1, 2);
        BlockPos sink = new BlockPos(5, 1, 2);
        helper.setBlock(source, Blocks.CHEST);
        helper.setBlock(sink, Blocks.CHEST);
        BlockPos[] cables = {new BlockPos(2, 1, 2), new BlockPos(3, 1, 2), new BlockPos(4, 1, 2)};
        for (BlockPos cable : cables) helper.setBlock(cable, VoltaicLogistics.ITEM_CABLE.get());
        connectCables(helper, cables);
        helper.assertTrue(LogisticsCableBlock.connection(helper.getBlockState(cables[0]), Direction.WEST) == ConnectionType.BLOCK,
                "cable did not connect to the chest");
        setArm(helper, cables[0], Direction.WEST, ConnectionType.PULL);
        var chest = items(helper.getLevel().getCapability(net.neoforged.neoforge.capabilities.Capabilities.Item.BLOCK,
                helper.absolutePos(source), null));
        chest.insertItem(0, new ItemStack(Items.COBBLESTONE, 40), false);
        helper.succeedWhen(() -> {
            var target = items(helper.getLevel().getCapability(net.neoforged.neoforge.capabilities.Capabilities.Item.BLOCK,
                    helper.absolutePos(sink), null));
            int moved = 0;
            for (int i = 0; i < target.getSlots(); i++) moved += target.getStackInSlot(i).getCount();
            helper.assertValueEqual(moved, 40, "items in the target chest");
        });
    }

    /** Tank -> pulling connector -> fluid cables -> tank. */
    @VoltaicTest(timeoutTicks = 200)
    public static void fluidCableMovesFluid(GameTestHelper helper) {
        BlockPos source = new BlockPos(1, 1, 2);
        BlockPos sink = new BlockPos(5, 1, 2);
        helper.setBlock(source, VoltaicLogistics.FLUID_TANK.get());
        helper.setBlock(sink, VoltaicLogistics.FLUID_TANK.get());
        BlockPos[] cables = {new BlockPos(2, 1, 2), new BlockPos(3, 1, 2), new BlockPos(4, 1, 2)};
        for (BlockPos cable : cables) helper.setBlock(cable, VoltaicLogistics.FLUID_CABLE.get());
        connectCables(helper, cables);
        setArm(helper, cables[0], Direction.WEST, ConnectionType.PULL);
        FluidTankBlockEntity from = helper.getBlockEntity(source, FluidTankBlockEntity.class);
        fluids(from.getTank()).fill(new FluidStack(Fluids.WATER, 5_000), false);
        helper.succeedWhen(() -> {
            FluidTankBlockEntity to = helper.getBlockEntity(sink, FluidTankBlockEntity.class);
            helper.assertValueEqual(to.getFluid().getAmount(), 5_000, "water in the target tank");
        });
    }

    /** A disabled arm splits two cables into separate networks. */
    @VoltaicTest(timeoutTicks = 100)
    public static void disabledArmCutsNetwork(GameTestHelper helper) {
        BlockPos source = new BlockPos(1, 1, 2);
        BlockPos sink = new BlockPos(5, 1, 2);
        helper.setBlock(source, Blocks.CHEST);
        helper.setBlock(sink, Blocks.CHEST);
        BlockPos[] cables = {new BlockPos(2, 1, 2), new BlockPos(3, 1, 2), new BlockPos(4, 1, 2)};
        for (BlockPos cable : cables) helper.setBlock(cable, VoltaicLogistics.ITEM_CABLE.get());
        connectCables(helper, cables);
        setArm(helper, cables[0], Direction.WEST, ConnectionType.PULL);
        setArm(helper, cables[1], Direction.EAST, ConnectionType.DISABLED);
        connectCables(helper, cables[2]);
        var chest = items(helper.getLevel().getCapability(net.neoforged.neoforge.capabilities.Capabilities.Item.BLOCK,
                helper.absolutePos(source), null));
        chest.insertItem(0, new ItemStack(Items.COBBLESTONE, 16), false);
        helper.runAfterDelay(60, () -> {
            helper.assertContainerEmpty(sink);
            helper.succeed();
        });
    }

    /** Breaking a filled tank drops it with its fluid. */
    @VoltaicTest
    public static void tankKeepsFluidWhenBroken(GameTestHelper helper) {
        BlockPos pos = new BlockPos(2, 1, 2);
        helper.setBlock(pos, VoltaicLogistics.FLUID_TANK.get());
        FluidTankBlockEntity tank = helper.getBlockEntity(pos, FluidTankBlockEntity.class);
        fluids(tank.getTank()).fill(new FluidStack(Fluids.LAVA, 3_000), false);
        helper.getLevel().destroyBlock(helper.absolutePos(pos), true);
        List<ItemEntity> drops = helper.getEntities(net.minecraft.world.entity.EntityTypes.ITEM, pos, 2.0);
        helper.assertTrue(drops.size() == 1, "expected one drop, got " + drops.size());
        var content = drops.get(0).getItem().get(ModDataComponents.FLUID.get());
        helper.assertTrue(content != null && content.is(Fluids.LAVA) && content.getAmount() == 3_000, "dropped tank holds " + content);
        helper.succeed();
    }
}
