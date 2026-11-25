package com.voltaicdynamics.gametest;

import com.voltaicdynamics.core.transfer.Transfers;
import net.neoforged.neoforge.transfer.energy.EnergyHandler;
import static com.voltaicdynamics.gametest.GameTestSupport.fluids;
import static com.voltaicdynamics.gametest.GameTestSupport.items;
import com.voltaicdynamics.VoltaicConfig;
import com.voltaicdynamics.VoltaicDynamics;
import com.voltaicdynamics.core.ModDataComponents;
import com.voltaicdynamics.logistics.FluidTankBlockEntity;
import com.voltaicdynamics.logistics.FluidTankTier;
import com.voltaicdynamics.logistics.VoltaicLogistics;
import com.voltaicdynamics.machines.ElectricFurnaceBlockEntity;
import com.voltaicdynamics.machines.MachineTier;
import com.voltaicdynamics.machines.VoltaicMachines;
import com.voltaicdynamics.power.AccumulatorBlockEntity;
import com.voltaicdynamics.power.VoltaicPower;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.material.Fluids;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.fluids.FluidStack;

import java.util.List;


/** Higher tiers: Elite/Ultimate/Creative Accumulators, tank tiers and the Creative tank, machine tiers and installers. */
public class TierGameTests {
    /** The new Accumulator and tank tiers hold what the config says. */
    @VoltaicTest
    public static void tierCapacities(GameTestHelper helper) {
        helper.setBlock(new BlockPos(1, 1, 1), VoltaicPower.ELITE_ACCUMULATOR.get());
        helper.setBlock(new BlockPos(2, 1, 1), VoltaicPower.ULTIMATE_ACCUMULATOR.get());
        AccumulatorBlockEntity elite = helper.getBlockEntity(new BlockPos(1, 1, 1), AccumulatorBlockEntity.class);
        AccumulatorBlockEntity ultimate = helper.getBlockEntity(new BlockPos(2, 1, 1), AccumulatorBlockEntity.class);
        helper.assertValueEqual(elite.getEnergyCapacity(), VoltaicConfig.get(VoltaicConfig.ELITE_CELL_CAPACITY), "elite capacity");
        helper.assertValueEqual(ultimate.getEnergyCapacity(), VoltaicConfig.get(VoltaicConfig.ULTIMATE_CELL_CAPACITY), "ultimate capacity");

        int basic = VoltaicConfig.get(VoltaicConfig.TANK_CAPACITY);
        int[] multipliers = {2, 4, 8};
        var tanks = List.of(VoltaicLogistics.ADVANCED_FLUID_TANK, VoltaicLogistics.ELITE_FLUID_TANK, VoltaicLogistics.ULTIMATE_FLUID_TANK);
        for (int i = 0; i < tanks.size(); i++) {
            BlockPos pos = new BlockPos(1 + i, 1, 3);
            helper.setBlock(pos, tanks.get(i).get());
            FluidTankBlockEntity tank = helper.getBlockEntity(pos, FluidTankBlockEntity.class);
            helper.assertValueEqual(tank.getTank().getCapacity(), basic * multipliers[i], tanks.get(i).getId().getPath());
        }
        helper.succeed();
    }

    /** The Creative Accumulator stays full while it runs a furnace and fills a Basic Accumulator next to it. */
    @VoltaicTest(timeoutTicks = 100)
    public static void creativeAccumulatorNeverRunsOut(GameTestHelper helper) {
        BlockPos creativePos = new BlockPos(2, 1, 2);
        helper.setBlock(creativePos, VoltaicPower.CREATIVE_ACCUMULATOR.get());
        helper.setBlock(creativePos.east(), VoltaicMachines.ELECTRIC_FURNACE.get());
        helper.setBlock(creativePos.west(), VoltaicPower.BASIC_ACCUMULATOR.get());
        ElectricFurnaceBlockEntity furnace = helper.getBlockEntity(creativePos.east(), ElectricFurnaceBlockEntity.class);
        items(furnace.getItemHandler(null)).insertItem(ElectricFurnaceBlockEntity.SLOT_INPUT, new ItemStack(Items.RAW_IRON, 16), false);
        AccumulatorBlockEntity creative = helper.getBlockEntity(creativePos, AccumulatorBlockEntity.class);
        AccumulatorBlockEntity basic = helper.getBlockEntity(creativePos.west(), AccumulatorBlockEntity.class);

        EnergyHandler storage = creative.getEnergyStorage(null);
        helper.assertValueEqual(Transfers.extractEnergy(storage, 5_000, false), 5_000, "gives energy");
        helper.assertValueEqual(storage.getAmountAsInt(), Integer.MAX_VALUE, "still full after giving");
        helper.assertValueEqual(Transfers.insertEnergy(storage, 5_000, false), 5_000, "swallows energy");

        helper.succeedWhen(() -> {
            helper.assertTrue(furnace.getProgress() > 0, "the furnace runs on it");
            helper.assertTrue(basic.getEnergyStored() > 0, "it fills the Basic Accumulator");
            helper.assertValueEqual(creative.getEnergyStored(), Integer.MAX_VALUE, "still full");
        });
    }

    /** The Creative tank: the first fluid fills it forever, more of it is voided, other fluids are refused, a clear resets it. */
    @VoltaicTest
    public static void creativeTankIsBottomless(GameTestHelper helper) {
        BlockPos pos = new BlockPos(2, 1, 2);
        helper.setBlock(pos, VoltaicLogistics.CREATIVE_FLUID_TANK.get());
        FluidTankBlockEntity tank = helper.getBlockEntity(pos, FluidTankBlockEntity.class);
        helper.assertTrue(tank.getTier() == FluidTankTier.CREATIVE, "creative tier");
        var handler = fluids(tank.getFluidHandler(null));

        helper.assertValueEqual(handler.fill(new FluidStack(Fluids.WATER, 1_000), false), 1_000, "takes water");
        for (int i = 0; i < 100; i++) {
            FluidStack out = handler.drain(1_000, false);
            helper.assertTrue(out.is(Fluids.WATER) && out.getAmount() == 1_000, "bucket " + i + " gave " + out.getAmount());
        }
        helper.assertValueEqual(tank.getTank().getFluidAmount(), Integer.MAX_VALUE, "still full after 100 buckets");
        helper.assertValueEqual(handler.fill(new FluidStack(Fluids.WATER, 5_000), false), 5_000, "voids more water");
        helper.assertValueEqual(handler.fill(new FluidStack(Fluids.LAVA, 1_000), false), 0, "refuses lava");

        helper.assertTrue(tank.clearCreative(), "clears");
        helper.assertTrue(tank.getTank().isEmpty(), "empty after clearing");
        helper.assertValueEqual(handler.fill(new FluidStack(Fluids.LAVA, 1_000), false), 1_000, "then takes lava");
        helper.assertTrue(tank.getFluid().is(Fluids.LAVA), "now lava");
        helper.succeed();
    }

    /** An Ultimate furnace smelts 8 at once at 8x FE/t; with 3 items it does 3; a nearly full output slot limits it. */
    @VoltaicTest(timeoutTicks = 400)
    public static void machineTierProcessesInParallel(GameTestHelper helper) {
        BlockPos power = new BlockPos(2, 1, 2);
        helper.setBlock(power, VoltaicPower.CREATIVE_ACCUMULATOR.get());
        BlockPos[] at = {power.east(), power.west(), power.north()};
        ElectricFurnaceBlockEntity[] furnaces = new ElectricFurnaceBlockEntity[3];
        int[] inputs = {8, 3, 8};
        for (int i = 0; i < 3; i++) {
            helper.setBlock(at[i], VoltaicMachines.ELECTRIC_FURNACE.get());
            furnaces[i] = helper.getBlockEntity(at[i], ElectricFurnaceBlockEntity.class);
            furnaces[i].setTier(MachineTier.ULTIMATE);
            items(furnaces[i].getItemHandler(null)).insertItem(ElectricFurnaceBlockEntity.SLOT_INPUT, new ItemStack(Items.RAW_IRON, inputs[i]), false);
        }
        // The third one has room for only 2 more ingots.
        items(furnaces[2].getMenuItemHandler())
                .setStackInSlot(ElectricFurnaceBlockEntity.SLOT_OUTPUT, new ItemStack(Items.IRON_INGOT, 62));
        int ticks = furnaces[0].getMaxProgress();

        helper.runAfterDelay(5, () -> {
            helper.assertValueEqual(furnaces[0].activeEnergyPerTick(), furnaces[0].currentEnergyPerTick() * 8, "8x FE/t");
            helper.assertValueEqual(furnaces[1].activeEnergyPerTick(), furnaces[1].currentEnergyPerTick() * 3, "3x FE/t");
        });
        helper.runAfterDelay(ticks + 10, () -> {
            helper.assertValueEqual(output(furnaces[0]), 8, "ultimate: 8 ingots in one operation");
            helper.assertValueEqual(output(furnaces[1]), 3, "3 raw iron: 3 ingots");
            helper.assertValueEqual(output(furnaces[2]), 64, "output room for 2: 2 made");
            helper.assertValueEqual(items(furnaces[2].getItemHandler(null)).getStackInSlot(ElectricFurnaceBlockEntity.SLOT_INPUT).getCount(), 6,
                    "the rest stays in the input");
            helper.succeed();
        });
    }

    private static int output(ElectricFurnaceBlockEntity furnace) {
        ItemStack out = items(furnace.getItemHandler(null)).getStackInSlot(ElectricFurnaceBlockEntity.SLOT_OUTPUT);
        return out.is(Items.IRON_INGOT) ? out.getCount() : -1;
    }

    /** Installers go one step at a time, are used up, and the broken machine keeps its tier. */
    @VoltaicTest
    public static void tierInstallersStepAndTierIsKept(GameTestHelper helper) {
        BlockPos pos = new BlockPos(2, 1, 2);
        helper.setBlock(pos, VoltaicMachines.PULVERIZER.get());
        Player player = helper.makeMockPlayer(GameType.SURVIVAL);
        ItemStack elite = new ItemStack(VoltaicMachines.ELITE_TIER_INSTALLER.get());
        ItemStack advanced = new ItemStack(VoltaicMachines.ADVANCED_TIER_INSTALLER.get(), 2);

        helper.assertTrue(use(helper, player, elite, pos) == InteractionResult.FAIL, "elite on a basic machine fails");
        helper.assertValueEqual(elite.getCount(), 1, "a failed install keeps the installer");
        use(helper, player, advanced, pos);
        var machine = helper.getBlockEntity(pos, com.voltaicdynamics.machines.ProcessingMachineBlockEntity.class);
        helper.assertTrue(machine.getTier() == MachineTier.ADVANCED, "advanced now, is " + machine.getTier());
        helper.assertValueEqual(advanced.getCount(), 1, "the installer is used up");
        helper.assertTrue(use(helper, player, advanced, pos) == InteractionResult.FAIL, "advanced twice fails");
        use(helper, player, elite, pos);
        helper.assertTrue(machine.getTier() == MachineTier.ELITE, "elite now");

        ServerLevel level = helper.getLevel();
        BlockPos abs = helper.absolutePos(pos);
        List<ItemStack> drops = Block.getDrops(level.getBlockState(abs), level, abs, machine, player, new ItemStack(Items.IRON_PICKAXE));
        helper.assertTrue(drops.size() == 1 && drops.getFirst().get(ModDataComponents.MACHINE_TIER.get()) == MachineTier.ELITE,
                "the dropped machine keeps its tier: " + drops);
        helper.succeed();
    }

    private static InteractionResult use(GameTestHelper helper, Player player, ItemStack stack, BlockPos pos) {
        BlockPos abs = helper.absolutePos(pos);
        player.setItemInHand(InteractionHand.MAIN_HAND, stack);
        UseOnContext context = new UseOnContext(player, InteractionHand.MAIN_HAND,
                new BlockHitResult(Vec3.atCenterOf(abs), Direction.UP, abs, false));
        return stack.getItem().onItemUseFirst(stack, context);
    }
}
