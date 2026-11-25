package com.voltaicdynamics.gametest;

import static com.voltaicdynamics.gametest.GameTestSupport.fluids;
import static com.voltaicdynamics.gametest.GameTestSupport.items;
import static com.voltaicdynamics.gametest.GameTestSupport.power;

import com.voltaicdynamics.core.sideconfig.IoMode;
import com.voltaicdynamics.core.sideconfig.RelativeSide;
import com.voltaicdynamics.core.sideconfig.TransferType;
import com.voltaicdynamics.logistics.ConnectionType;
import com.voltaicdynamics.logistics.FluidTankBlockEntity;
import com.voltaicdynamics.logistics.LogisticsCableBlock;
import com.voltaicdynamics.logistics.VoltaicLogistics;
import com.voltaicdynamics.machines.ElectricFurnaceBlockEntity;
import com.voltaicdynamics.machines.VoltaicMachines;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.Fluids;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.fluids.FluidStack;

import java.util.Optional;

/**
 * Our machines and cables against other mods' storage: Iron Chests, Storage Drawers and Sophisticated Storage.
 * (A placed Sophisticated Backpack only gets its inventory from a real player placing it, so it is checked by hand.) Each test checks that a machine pushes its output into the block (auto-eject) and that an
 * item cable pulls from it into a machine. Run with the mods in runs/compat/mods
 * ({@code ./gradlew runCompatGameTestServer}); without a mod, its tests pass without checking anything.
 */
public class ModStorageGameTests {
    private static final String IRON_CHEST = "ironchest:iron_chest";
    private static final String DRAWER = "storagedrawers:oak_full_drawers_1";
    private static final String SOPHISTICATED_CHEST = "sophisticatedstorage:chest";
    private static final String SOPHISTICATED_BARREL = "sophisticatedstorage:barrel";
    private static final String SOPHISTICATED_TANK = "sophisticatedstorage:iron_tank";

    private static Optional<Block> block(String id) {
        return BuiltInRegistries.BLOCK.getOptional(Identifier.parse(id));
    }

    private static int count(GameTestHelper helper, BlockPos pos, net.minecraft.world.item.Item item) {
        var handler = helper.getLevel().getCapability(Capabilities.Item.BLOCK, helper.absolutePos(pos), Direction.DOWN);
        if (handler == null) handler = helper.getLevel().getCapability(Capabilities.Item.BLOCK, helper.absolutePos(pos), null);
        helper.assertTrue(handler != null, "no item handler on " + helper.getBlockState(pos).getBlock());
        var view = items(handler);
        int total = 0;
        for (int i = 0; i < view.getSlots(); i++) {
            if (view.getStackInSlot(i).is(item)) total += view.getStackInSlot(i).getCount();
        }
        return total;
    }

    /** An Electric Furnace with 10 iron ingots in its output and item auto-eject on, pushing up into {@code id}. */
    private static void furnaceEjectsInto(GameTestHelper helper, String id) {
        Optional<Block> storage = block(id);
        if (storage.isEmpty()) {
            helper.succeed();
            return;
        }
        BlockPos pos = new BlockPos(2, 1, 2);
        helper.setBlock(pos, VoltaicMachines.ELECTRIC_FURNACE.get());
        helper.setBlock(pos.above(), storage.get());
        ElectricFurnaceBlockEntity furnace = helper.getBlockEntity(pos, ElectricFurnaceBlockEntity.class);
        items(furnace.getMenuItemHandler()).setStackInSlot(ElectricFurnaceBlockEntity.SLOT_OUTPUT, new ItemStack(Items.IRON_INGOT, 10));
        furnace.getSideConfig().set(TransferType.ITEM, RelativeSide.TOP, IoMode.OUTPUT);
        furnace.getSideConfig().setEject(TransferType.ITEM, true);
        furnace.onSideConfigChanged();
        helper.succeedWhen(() -> helper.assertValueEqual(count(helper, pos.above(), Items.IRON_INGOT), 10,
                "ingots ejected into " + id));
    }

    /** {@code id} with 4 raw iron -> pulling item cable -> powered Electric Furnace, which smelts them. */
    private static void cablePullsFrom(GameTestHelper helper, String id) {
        Optional<Block> storage = block(id);
        if (storage.isEmpty()) {
            helper.succeed();
            return;
        }
        BlockPos source = new BlockPos(1, 1, 2), cable = new BlockPos(2, 1, 2), furnacePos = new BlockPos(3, 1, 2);
        helper.setBlock(source, storage.get());
        helper.setBlock(furnacePos, VoltaicMachines.ELECTRIC_FURNACE.get());
        helper.setBlock(cable, VoltaicLogistics.ITEM_CABLE.get());
        BlockPos abs = helper.absolutePos(cable);
        BlockState state = Block.updateFromNeighbourShapes(helper.getLevel().getBlockState(abs), helper.getLevel(), abs);
        helper.assertTrue(LogisticsCableBlock.connection(state, Direction.WEST) == ConnectionType.BLOCK,
                "the item cable does not connect to " + id);
        helper.getLevel().setBlock(abs, state.setValue(LogisticsCableBlock.property(Direction.WEST), ConnectionType.PULL), Block.UPDATE_ALL);

        var handler = helper.getLevel().getCapability(Capabilities.Item.BLOCK, helper.absolutePos(source), Direction.EAST);
        helper.assertTrue(handler != null, id + " has no item handler");
        ItemStack rest = items(handler).insertItem(0, new ItemStack(Items.RAW_IRON, 4), false);
        helper.assertTrue(rest.isEmpty(), id + " did not take the raw iron");
        ElectricFurnaceBlockEntity furnace = helper.getBlockEntity(furnacePos, ElectricFurnaceBlockEntity.class);
        power(furnace, 1_000_000);
        // Done when the cable has emptied the storage and the furnace is smelting what it got.
        helper.succeedWhen(() -> {
            helper.assertValueEqual(count(helper, source, Items.RAW_IRON), 0, "raw iron left in " + id);
            helper.assertTrue(!items(furnace.getMenuItemHandler()).getStackInSlot(ElectricFurnaceBlockEntity.SLOT_OUTPUT).isEmpty(),
                    "nothing smelted from " + id);
        });
    }

    @VoltaicTest(timeoutTicks = 100)
    public static void furnaceEjectsIntoIronChest(GameTestHelper helper) {
        furnaceEjectsInto(helper, IRON_CHEST);
    }

    @VoltaicTest(timeoutTicks = 400)
    public static void cablePullsFromIronChest(GameTestHelper helper) {
        cablePullsFrom(helper, IRON_CHEST);
    }

    @VoltaicTest(timeoutTicks = 100)
    public static void furnaceEjectsIntoDrawer(GameTestHelper helper) {
        furnaceEjectsInto(helper, DRAWER);
    }

    @VoltaicTest(timeoutTicks = 400)
    public static void cablePullsFromDrawer(GameTestHelper helper) {
        cablePullsFrom(helper, DRAWER);
    }

    @VoltaicTest(timeoutTicks = 100)
    public static void furnaceEjectsIntoSophisticatedChest(GameTestHelper helper) {
        furnaceEjectsInto(helper, SOPHISTICATED_CHEST);
    }

    @VoltaicTest(timeoutTicks = 400)
    public static void cablePullsFromSophisticatedChest(GameTestHelper helper) {
        cablePullsFrom(helper, SOPHISTICATED_CHEST);
    }

    @VoltaicTest(timeoutTicks = 100)
    public static void furnaceEjectsIntoSophisticatedBarrel(GameTestHelper helper) {
        furnaceEjectsInto(helper, SOPHISTICATED_BARREL);
    }

    @VoltaicTest(timeoutTicks = 400)
    public static void cablePullsFromSophisticatedBarrel(GameTestHelper helper) {
        cablePullsFrom(helper, SOPHISTICATED_BARREL);
    }



    /** Our Fluid Tank with fluid auto-eject on pushes water down into a Sophisticated Storage tank. */
    @VoltaicTest(timeoutTicks = 200)
    public static void tankEjectsIntoSophisticatedTank(GameTestHelper helper) {
        Optional<Block> other = block(SOPHISTICATED_TANK);
        if (other.isEmpty()) {
            helper.succeed();
            return;
        }
        BlockPos ours = new BlockPos(2, 2, 2), theirs = new BlockPos(2, 1, 2);
        helper.setBlock(theirs, other.get());
        helper.setBlock(ours, VoltaicLogistics.FLUID_TANK.get());
        FluidTankBlockEntity tank = helper.getBlockEntity(ours, FluidTankBlockEntity.class);
        fluids(tank.getTank()).fill(new FluidStack(Fluids.WATER, 4_000), false);
        tank.getSideConfig().set(TransferType.FLUID, RelativeSide.BOTTOM, IoMode.OUTPUT);
        tank.getSideConfig().setEject(TransferType.FLUID, true);
        tank.onSideConfigChanged();
        helper.succeedWhen(() -> {
            var handler = helper.getLevel().getCapability(Capabilities.Fluid.BLOCK, helper.absolutePos(theirs), Direction.UP);
            helper.assertTrue(handler != null, "the Sophisticated tank has no fluid handler");
            helper.assertValueEqual(handler.getAmountAsInt(0), 4_000, "water in the Sophisticated tank");
        });
    }

    /** Sophisticated tank -> pulling fluid cable -> our Fluid Tank. */
    @VoltaicTest(timeoutTicks = 200)
    public static void fluidCablePullsFromSophisticatedTank(GameTestHelper helper) {
        Optional<Block> other = block(SOPHISTICATED_TANK);
        if (other.isEmpty()) {
            helper.succeed();
            return;
        }
        BlockPos source = new BlockPos(1, 1, 2), cable = new BlockPos(2, 1, 2), sink = new BlockPos(3, 1, 2);
        helper.setBlock(source, other.get());
        helper.setBlock(sink, VoltaicLogistics.FLUID_TANK.get());
        helper.setBlock(cable, VoltaicLogistics.FLUID_CABLE.get());
        BlockPos abs = helper.absolutePos(cable);
        BlockState state = Block.updateFromNeighbourShapes(helper.getLevel().getBlockState(abs), helper.getLevel(), abs);
        helper.assertTrue(LogisticsCableBlock.connection(state, Direction.WEST) == ConnectionType.BLOCK,
                "the fluid cable does not connect to the Sophisticated tank");
        helper.getLevel().setBlock(abs, state.setValue(LogisticsCableBlock.property(Direction.WEST), ConnectionType.PULL), Block.UPDATE_ALL);
        var handler = helper.getLevel().getCapability(Capabilities.Fluid.BLOCK, helper.absolutePos(source), Direction.EAST);
        helper.assertTrue(handler != null, "the Sophisticated tank has no fluid handler");
        helper.assertValueEqual(fluids(handler).fill(new FluidStack(Fluids.WATER, 3_000), false), 3_000, "water into the Sophisticated tank");
        FluidTankBlockEntity tank = helper.getBlockEntity(sink, FluidTankBlockEntity.class);
        helper.succeedWhen(() -> helper.assertValueEqual(tank.getTank().getFluidAmount(), 3_000, "water pulled into our tank"));
    }

}
