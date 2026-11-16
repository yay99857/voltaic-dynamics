package com.voltaicdynamics.gametest;

import com.voltaicdynamics.VoltaicDynamics;
import com.voltaicdynamics.core.ModDataComponents;
import com.voltaicdynamics.gadgets.VoltaicGadgets;
import com.voltaicdynamics.logistics.ConnectionType;
import com.voltaicdynamics.logistics.LogisticsCableBlock;
import com.voltaicdynamics.logistics.VoltaicLogistics;
import com.voltaicdynamics.machines.ProcessingMachineBlock;
import com.voltaicdynamics.machines.VoltaicMachines;
import com.voltaicdynamics.power.AccumulatorBlockEntity;
import com.voltaicdynamics.power.VoltaicPower;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

import static com.voltaicdynamics.gametest.GameTestSupport.EMPTY;
import static com.voltaicdynamics.gametest.GameTestSupport.storageOf;

/**
 * Pocket Accumulator and Voltaic Wrench. Run with {@code ./gradlew runGameTestServer};
 * the server exits non-zero when a required test fails. Only loaded when game tests are enabled.
 */
@GameTestHolder(VoltaicDynamics.MOD_ID)
@PrefixGameTestTemplate(false)
public class GadgetGameTests {
    private static Player playerWithWrench(GameTestHelper helper, boolean sneaking) {
        Player player = helper.makeMockPlayer(GameType.SURVIVAL);
        player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(VoltaicGadgets.VOLTAIC_WRENCH.get()));
        player.setShiftKeyDown(sneaking);
        return player;
    }

    /** Clicks {@code relPos} at {@code offset} inside the block (0..1 on each axis), as the item's first use. */
    private static BlockHitResult hit(GameTestHelper helper, BlockPos relPos, Vec3 offset, Direction face) {
        BlockPos abs = helper.absolutePos(relPos);
        return new BlockHitResult(Vec3.atLowerCornerOf(abs).add(offset), face, abs, false);
    }

    /** A plain right-click turns a machine a quarter turn instead of opening its GUI. */
    @GameTest(template = EMPTY)
    public static void wrenchTurnsMachine(GameTestHelper helper) {
        BlockPos pos = new BlockPos(2, 1, 2);
        helper.setBlock(pos, VoltaicMachines.ELECTRIC_FURNACE.get().defaultBlockState()
                .setValue(ProcessingMachineBlock.FACING, Direction.NORTH));
        Player player = playerWithWrench(helper, false);
        ItemStack wrench = player.getMainHandItem();
        wrench.onItemUseFirst(new UseOnContext(player, InteractionHand.MAIN_HAND,
                hit(helper, pos, new Vec3(0.5, 1, 0.5), Direction.UP)));
        helper.assertValueEqual(helper.getBlockState(pos).getValue(ProcessingMachineBlock.FACING), Direction.EAST, "facing");
        helper.succeed();
    }

    /** Sneak + right-click picks a charged Accumulator up into the inventory, energy included. */
    @GameTest(template = EMPTY)
    public static void wrenchPicksUpAccumulatorWithEnergy(GameTestHelper helper) {
        BlockPos pos = new BlockPos(2, 1, 2);
        helper.setBlock(pos, VoltaicPower.BASIC_ACCUMULATOR.get());
        AccumulatorBlockEntity cell = helper.getBlockEntity(pos);
        storageOf(cell).setEnergy(123_456);
        Player player = playerWithWrench(helper, true);
        player.getMainHandItem().onItemUseFirst(new UseOnContext(player, InteractionHand.MAIN_HAND,
                hit(helper, pos, new Vec3(0.5, 1, 0.5), Direction.UP)));

        helper.assertBlockPresent(Blocks.AIR, pos);
        ItemStack picked = ItemStack.EMPTY;
        for (ItemStack stack : player.getInventory().items) {
            if (stack.is(VoltaicPower.BASIC_ACCUMULATOR.asItem())) picked = stack;
        }
        helper.assertFalse(picked.isEmpty(), "the Accumulator is not in the inventory");
        helper.assertValueEqual(picked.get(ModDataComponents.ENERGY.get()), 123_456, "energy on the picked-up cell");
        helper.succeed();
    }

    /** Sneak + right-click with the wrench on a cable arm cycles it (the wrench lets the click through). */
    @GameTest(template = EMPTY)
    public static void wrenchCyclesCableArm(GameTestHelper helper) {
        BlockPos chest = new BlockPos(1, 1, 2);
        BlockPos cable = new BlockPos(2, 1, 2);
        helper.setBlock(chest, Blocks.CHEST);
        helper.setBlock(cable, VoltaicLogistics.ITEM_CABLE.get());
        BlockPos abs = helper.absolutePos(cable);
        BlockState state = helper.getLevel().getBlockState(abs);
        helper.getLevel().setBlock(abs, Block.updateFromNeighbourShapes(state, helper.getLevel(), abs), Block.UPDATE_ALL);
        helper.assertValueEqual(LogisticsCableBlock.connection(helper.getBlockState(cable), Direction.WEST),
                ConnectionType.BLOCK, "arm toward the chest");

        Player player = playerWithWrench(helper, true);
        ItemStack wrench = player.getMainHandItem();
        helper.assertTrue(wrench.doesSneakBypassUse(helper.getLevel(), abs, player), "sneaking would skip the cable");
        BlockHitResult armHit = hit(helper, cable, new Vec3(0.1, 0.5, 0.5), Direction.WEST);
        helper.assertValueEqual(wrench.onItemUseFirst(new UseOnContext(player, InteractionHand.MAIN_HAND, armHit)),
                net.minecraft.world.InteractionResult.PASS, "the wrench must leave cables to the cable");
        helper.getBlockState(cable).useItemOn(wrench, helper.getLevel(), player, InteractionHand.MAIN_HAND, armHit);
        helper.assertValueEqual(LogisticsCableBlock.connection(helper.getBlockState(cable), Direction.WEST),
                ConnectionType.PULL, "arm after one wrench click");
        helper.succeed();
    }
}
