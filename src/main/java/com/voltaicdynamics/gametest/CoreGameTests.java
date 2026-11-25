package com.voltaicdynamics.gametest;

import com.voltaicdynamics.core.transfer.Transfers;
import static com.voltaicdynamics.gametest.GameTestSupport.items;
import com.voltaicdynamics.VoltaicDynamics;
import com.voltaicdynamics.core.blockentity.DeferredSaves;
import com.voltaicdynamics.core.sideconfig.IoMode;
import com.voltaicdynamics.core.sideconfig.RelativeSide;
import com.voltaicdynamics.core.sideconfig.TransferType;
import com.voltaicdynamics.generators.VoltaicGenerators;
import com.voltaicdynamics.machines.ElectricFurnaceBlockEntity;
import com.voltaicdynamics.machines.VoltaicMachines;
import com.voltaicdynamics.power.AccumulatorBlockEntity;
import com.voltaicdynamics.power.AccumulatorMenu;
import com.voltaicdynamics.power.VoltaicPower;
import net.minecraft.core.BlockPos;
import net.minecraft.core.NonNullList;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerSynchronizer;
import net.minecraft.world.level.GameType;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.ComparatorBlock;
import net.minecraft.world.level.block.entity.ComparatorBlockEntity;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.core.Direction;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static com.voltaicdynamics.gametest.GameTestSupport.storageOf;

/**
 * Shared plumbing: mining tags, deferred saves, menu sync and side configuration. Run with {@code ./gradlew runGameTestServer};
 * the server exits non-zero when a required test fails. Only loaded when game tests are enabled.
 */
public class CoreGameTests {
    /** Machines are pickaxe-mineable, so requiresCorrectToolForDrops() lets them drop. */
    @VoltaicTest
    public static void machinesAreMineableWithPickaxe(GameTestHelper helper) {
        ItemStack pickaxe = new ItemStack(Items.STONE_PICKAXE);
        for (Block block : List.of(VoltaicGenerators.THERMOELECTRIC_GENERATOR.get(), VoltaicGenerators.COMBUSTION_GENERATOR.get(),
                VoltaicMachines.ELECTRIC_FURNACE.get(), VoltaicPower.BASIC_ACCUMULATOR.get(), VoltaicPower.ADVANCED_ACCUMULATOR.get())) {
            BlockState state = block.defaultBlockState();
            helper.assertTrue(state.is(BlockTags.MINEABLE_WITH_PICKAXE), block + " is not in mineable/pickaxe");
            helper.assertTrue(pickaxe.isCorrectToolForDrops(state), "a pickaxe is not the correct tool for " + block);
        }
        helper.succeed();
    }

    /** saveChanges() only marks the chunk once the queue is flushed (end of tick), however often it was called. */
    @VoltaicTest
    public static void saveChangesIsDeferredToEndOfTick(GameTestHelper helper) {
        BlockPos pos = new BlockPos(2, 1, 2);
        helper.setBlock(pos, VoltaicPower.BASIC_ACCUMULATOR.get());
        AccumulatorBlockEntity cell = helper.getBlockEntity(pos, AccumulatorBlockEntity.class);
        DeferredSaves.flush();
        LevelChunk chunk = helper.getLevel().getChunkAt(helper.absolutePos(pos));
        chunk.tryMarkSaved();

        for (int i = 0; i < 50; i++) {
            Transfers.insertEnergy(storageOf(cell), 100, false);
        }
        helper.assertFalse(chunk.isUnsaved(), "chunk marked before the end of the tick");
        DeferredSaves.flush();
        helper.assertTrue(chunk.isUnsaved(), "chunk not marked after the flush");
        helper.succeed();
    }

    /** Comparators still follow a cell whose changes are batched to the end of the tick. */
    @VoltaicTest
    public static void comparatorFollowsBatchedChanges(GameTestHelper helper) {
        BlockPos cellPos = new BlockPos(2, 1, 2);
        BlockPos comparatorPos = new BlockPos(3, 1, 2);
        helper.setBlock(cellPos, VoltaicPower.BASIC_ACCUMULATOR.get());
        helper.setBlock(comparatorPos.below(), Blocks.STONE); // a comparator needs a block under it
        helper.setBlock(comparatorPos, Blocks.COMPARATOR.defaultBlockState().setValue(ComparatorBlock.FACING, Direction.WEST));
        AccumulatorBlockEntity cell = helper.getBlockEntity(cellPos, AccumulatorBlockEntity.class);
        for (int i = 0; i < 1_000 && cell.getEnergyStored() < cell.getEnergyCapacity(); i++) {
            Transfers.insertEnergy(storageOf(cell), Integer.MAX_VALUE, false);
        }
        helper.succeedWhen(() -> {
            ComparatorBlockEntity comparator = helper.getBlockEntity(comparatorPos, ComparatorBlockEntity.class);
            helper.assertValueEqual(comparator.getOutputSignal(), cell.getComparatorSignal(), "comparator output");
            helper.assertTrue(comparator.getOutputSignal() > 0, "comparator output is 0");
        });
    }

    /**
     * Menu values travel as two 16-bit halves, so energy above 65,535 FE reaches the client whole, and a value
     * that did not change is not sent again. Decodes the data the way the client does (the packet carries shorts).
     */
    @VoltaicTest
    public static void menuSyncsFullIntValues(GameTestHelper helper) {
        BlockPos pos = new BlockPos(2, 1, 2);
        helper.setBlock(pos, VoltaicPower.BASIC_ACCUMULATOR.get());
        AccumulatorBlockEntity cell = helper.getBlockEntity(pos, AccumulatorBlockEntity.class);
        storageOf(cell).setEnergy(123_456);

        Player player = helper.makeMockPlayer(GameType.SURVIVAL);
        AccumulatorMenu menu = new AccumulatorMenu(0, player.getInventory(), cell);
        int[][] initial = new int[1][];
        Map<Integer, Integer> changes = new HashMap<>();
        menu.setSynchronizer(new ContainerSynchronizer() {
            @Override
            public void sendInitialData(AbstractContainerMenu m, java.util.List<ItemStack> items, ItemStack carried, int[] data) {
                initial[0] = data.clone();
            }

            @Override
            public void sendSlotChange(AbstractContainerMenu m, int slot, ItemStack stack) {
            }

            @Override
            public void sendCarriedChange(AbstractContainerMenu m, ItemStack stack) {
            }

            @Override
            public void sendDataChange(AbstractContainerMenu m, int id, int value) {
                changes.put(id, value);
            }

            @Override
            public net.minecraft.world.inventory.RemoteSlot createSlot() {
                return net.minecraft.world.inventory.RemoteSlot.PLACEHOLDER;
            }
        });

        // Side configuration comes first (one value per transfer type), then energy and capacity.
        int energyId = 2 * TransferType.values().length;
        helper.assertValueEqual(decode(initial[0][energyId], initial[0][energyId + 1]), 123_456, "synced energy");
        helper.assertValueEqual(decode(initial[0][energyId + 2], initial[0][energyId + 3]), cell.getEnergyCapacity(),
                "synced capacity");

        menu.broadcastChanges();
        helper.assertTrue(changes.isEmpty(), "unchanged values were sent again: " + changes);

        storageOf(cell).setEnergy(65_000); // 0x1E240 -> 0xFDE8: both halves change
        menu.broadcastChanges();
        helper.assertValueEqual(changes.keySet(), Set.of(energyId, energyId + 1), "data slots sent after an energy change");
        helper.assertValueEqual(decode(changes.get(energyId), changes.get(energyId + 1)), 65_000, "synced energy after change");
        helper.succeed();
    }

    /** What the client rebuilds from two data slots that went over the wire as shorts. */
    private static int decode(int low, int high) {
        return (((short) high) & 0xFFFF) << 16 | (((short) low) & 0xFFFF);
    }

    /** Switching a face off removes its capabilities; input-only faces refuse extraction. */
    @VoltaicTest
    public static void sideConfigControlsFaces(GameTestHelper helper) {
        BlockPos pos = new BlockPos(2, 1, 2);
        helper.setBlock(pos, VoltaicMachines.ELECTRIC_FURNACE.get());
        ElectricFurnaceBlockEntity furnace = helper.getBlockEntity(pos, ElectricFurnaceBlockEntity.class);
        Direction facing = furnace.getFacing();
        Direction back = facing.getOpposite();
        helper.assertTrue(furnace.getItemHandler(back) != null, "back face has no items by default");

        furnace.getSideConfig().set(TransferType.ITEM, RelativeSide.BACK, IoMode.NONE);
        furnace.getSideConfig().set(TransferType.ENERGY, RelativeSide.BACK, IoMode.NONE);
        furnace.onSideConfigChanged();
        helper.assertTrue(furnace.getItemHandler(back) == null, "disabled face still exposes items");
        helper.assertTrue(furnace.getEnergyStorage(back) == null, "disabled face still takes energy");
        helper.assertTrue(furnace.getItemHandler(facing) != null, "front face lost its items");

        furnace.getSideConfig().set(TransferType.ITEM, RelativeSide.TOP, IoMode.INPUT);
        furnace.onSideConfigChanged();
        items(furnace.getMenuItemHandler()).insertItem(ElectricFurnaceBlockEntity.SLOT_INPUT, new ItemStack(Items.RAW_IRON), false);
        items(furnace.getMenuItemHandler())
                .setStackInSlot(ElectricFurnaceBlockEntity.SLOT_OUTPUT, new ItemStack(Items.IRON_INGOT));
        helper.assertTrue(items(furnace.getItemHandler(Direction.UP)).extractItem(ElectricFurnaceBlockEntity.SLOT_OUTPUT, 1, true).isEmpty(),
                "input-only face gave items out");
        helper.succeed();
    }

    /** Auto-eject pushes finished items into the block on an output face. */
    @VoltaicTest
    public static void autoEjectPushesOutput(GameTestHelper helper) {
        BlockPos pos = new BlockPos(2, 1, 2);
        helper.setBlock(pos, VoltaicMachines.ELECTRIC_FURNACE.get());
        helper.setBlock(pos.above(), Blocks.CHEST);
        ElectricFurnaceBlockEntity furnace = helper.getBlockEntity(pos, ElectricFurnaceBlockEntity.class);
        items(furnace.getMenuItemHandler())
                .setStackInSlot(ElectricFurnaceBlockEntity.SLOT_OUTPUT, new ItemStack(Items.IRON_INGOT, 10));
        furnace.getSideConfig().set(TransferType.ITEM, RelativeSide.TOP, IoMode.OUTPUT);
        furnace.getSideConfig().setEject(TransferType.ITEM, true);
        furnace.onSideConfigChanged();
        helper.succeedWhen(() -> {
            var chest = items(helper.getLevel().getCapability(net.neoforged.neoforge.capabilities.Capabilities.Item.BLOCK,
                    helper.absolutePos(pos.above()), null));
            int count = 0;
            for (int i = 0; i < chest.getSlots(); i++) {
                if (chest.getStackInSlot(i).is(Items.IRON_INGOT)) count += chest.getStackInSlot(i).getCount();
            }
            helper.assertValueEqual(count, 10, "ingots ejected into the chest");
        });
    }
}
