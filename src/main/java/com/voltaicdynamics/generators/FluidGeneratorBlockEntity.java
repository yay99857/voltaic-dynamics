package com.voltaicdynamics.generators;

import com.voltaicdynamics.VoltaicConfig;
import com.voltaicdynamics.core.blockentity.VoltaicBlockEntity;
import com.voltaicdynamics.core.energy.VoltaicEnergyStorage;
import com.voltaicdynamics.core.sideconfig.IoMode;
import com.voltaicdynamics.core.sideconfig.SideConfig;
import com.voltaicdynamics.core.sideconfig.SideConfigHelper;
import com.voltaicdynamics.core.sideconfig.SideConfigurable;
import com.voltaicdynamics.core.sideconfig.SidedHandlers;
import com.voltaicdynamics.core.sideconfig.TransferType;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.Containers;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.neoforged.neoforge.energy.IEnergyStorage;
import net.neoforged.neoforge.fluids.FluidActionResult;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.FluidUtil;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;
import net.neoforged.neoforge.fluids.capability.templates.FluidTank;
import net.neoforged.neoforge.items.IItemHandler;
import net.neoforged.neoforge.items.ItemStackHandler;
import org.jetbrains.annotations.Nullable;

import java.util.List;

/**
 * Burns liquid fuel ({@link FluidFuel}, lava by default) into FE. Takes fuel from pipes, cables, buckets clicked
 * on it or containers in its in slot (empties go to the out slot), and pushes energy out of its output faces.
 */
public class FluidGeneratorBlockEntity extends VoltaicBlockEntity implements MenuProvider, SideConfigurable {
    public static final int SLOT_IN = 0;
    public static final int SLOT_OUT = 1;
    public static final int MAX_OUTPUT = 400;

    private final VoltaicEnergyStorage energyStorage;
    private final FluidTank tank;
    private final ItemStackHandler itemHandler = new ItemStackHandler(2) {
        @Override
        protected void onContentsChanged(int slot) {
            saveChanges();
        }

        @Override
        public boolean isItemValid(int slot, ItemStack stack) {
            return slot == SLOT_IN && FluidUtil.getFluidContained(stack).map(f -> FluidFuel.of(f) != null).orElse(false);
        }
    };
    /** Pipes: containers into the in slot, empties out of the out slot. */
    private final IItemHandler automationHandler = new IItemHandler() {
        public int getSlots() {
            return 2;
        }

        public ItemStack getStackInSlot(int slot) {
            return itemHandler.getStackInSlot(slot);
        }

        public ItemStack insertItem(int slot, ItemStack stack, boolean simulate) {
            return slot == SLOT_IN ? itemHandler.insertItem(slot, stack, simulate) : stack;
        }

        public ItemStack extractItem(int slot, int amount, boolean simulate) {
            return slot == SLOT_OUT ? itemHandler.extractItem(slot, amount, simulate) : ItemStack.EMPTY;
        }

        public int getSlotLimit(int slot) {
            return itemHandler.getSlotLimit(slot);
        }

        public boolean isItemValid(int slot, ItemStack stack) {
            return slot == SLOT_IN && itemHandler.isItemValid(slot, stack);
        }
    };
    private final SideConfig sideConfig = SideConfig.builder()
            .add(TransferType.FLUID, List.of(IoMode.INPUT, IoMode.NONE), IoMode.INPUT, false, false)
            .add(TransferType.ITEM, List.of(IoMode.BOTH, IoMode.INPUT, IoMode.OUTPUT, IoMode.NONE), IoMode.BOTH, true, false)
            .add(TransferType.ENERGY, List.of(IoMode.OUTPUT, IoMode.NONE), IoMode.OUTPUT, true, true)
            .build();
    private final SidedHandlers sided;
    private final SideConfigHelper.Neighbors neighbors = new SideConfigHelper.Neighbors(this);

    /** FE already paid for by burned fuel and not produced yet (fuel is burned 1 mB at a time). */
    private int credit;
    private int currentRate;
    /** Rate of the last fuel burned, to use up the credit left after the tank runs dry. */
    private int lastRate = 80;

    public FluidGeneratorBlockEntity(BlockPos pos, BlockState state) {
        super(VoltaicGenerators.FLUID_GENERATOR_BE.get(), pos, state);
        this.energyStorage = new VoltaicEnergyStorage(VoltaicConfig.get(VoltaicConfig.FLUID_GEN_CAPACITY), 0, MAX_OUTPUT, this::saveChanges);
        this.tank = new FluidTank(VoltaicConfig.get(VoltaicConfig.FLUID_GEN_TANK), stack -> FluidFuel.of(stack) != null) {
            @Override
            protected void onContentsChanged() {
                saveChanges();
            }
        };
        this.sided = new SidedHandlers(this, automationHandler, tank, energyStorage);
    }

    public static void tick(Level level, BlockPos pos, BlockState state, FluidGeneratorBlockEntity entity) {
        entity.processContainers();
        entity.currentRate = entity.generate();
        boolean lit = entity.currentRate > 0;
        if (state.hasProperty(BlockStateProperties.LIT) && state.getValue(BlockStateProperties.LIT) != lit) {
            level.setBlock(pos, state.setValue(BlockStateProperties.LIT, lit), 3);
        }
        if (entity.sideConfig.eject(TransferType.ENERGY)) {
            SideConfigHelper.ejectEnergy(entity, entity.neighbors, entity.energyStorage, MAX_OUTPUT, false);
        }
        if (entity.sideConfig.eject(TransferType.ITEM) && Math.floorMod(level.getGameTime() + pos.asLong(), 10) == 0) {
            SideConfigHelper.ejectItems(entity, entity.neighbors, entity.automationHandler, new int[]{SLOT_OUT}, 16);
        }
    }

    /** Produces one tick of energy; returns the FE/t made (0 when out of fuel or the buffer is full). */
    private int generate() {
        FluidFuel fuel = FluidFuel.of(tank.getFluid());
        if (fuel == null && credit <= 0) return 0;
        if (fuel != null) lastRate = fuel.rate();
        int rate = fuel != null ? fuel.rate() : Math.min(credit, lastRate);
        if (energyStorage.getSpace() < rate) return 0;
        while (credit < rate && fuel != null) {
            if (tank.drain(1, IFluidHandler.FluidAction.EXECUTE).isEmpty()) break;
            credit += fuel.energyPerMb();
        }
        int made = Math.min(rate, credit);
        if (made <= 0) return 0;
        credit -= made;
        energyStorage.generateEnergy(made);
        return made;
    }

    /** A fuel container in the in slot is emptied into the tank; the empty one goes to the out slot. */
    private void processContainers() {
        ItemStack input = itemHandler.getStackInSlot(SLOT_IN);
        if (input.isEmpty()) return;
        ItemStack output = itemHandler.getStackInSlot(SLOT_OUT);
        FluidActionResult result = FluidUtil.tryEmptyContainer(input, tank, Integer.MAX_VALUE, null, false);
        if (!result.isSuccess()) return;
        ItemStack after = result.getResult();
        if (!after.isEmpty() && !output.isEmpty()
                && (!ItemStack.isSameItemSameComponents(output, after) || output.getCount() + after.getCount() > output.getMaxStackSize())) {
            return;
        }
        FluidActionResult real = FluidUtil.tryEmptyContainer(input, tank, Integer.MAX_VALUE, null, true);
        if (!real.isSuccess()) return;
        itemHandler.setStackInSlot(SLOT_IN, ItemStack.EMPTY);
        ItemStack produced = real.getResult();
        if (!produced.isEmpty()) {
            if (output.isEmpty()) {
                itemHandler.setStackInSlot(SLOT_OUT, produced.copy());
            } else {
                output.grow(produced.getCount());
                itemHandler.setStackInSlot(SLOT_OUT, output);
            }
        }
    }

    public void drops() {
        if (level != null && !level.isClientSide) {
            SimpleContainer inventory = new SimpleContainer(itemHandler.getSlots());
            for (int i = 0; i < itemHandler.getSlots(); i++) inventory.setItem(i, itemHandler.getStackInSlot(i));
            Containers.dropContents(level, worldPosition, inventory);
        }
    }

    @Nullable
    public IEnergyStorage getEnergyStorage(@Nullable Direction side) {
        return sided.energy(side);
    }

    @Nullable
    public IFluidHandler getFluidHandler(@Nullable Direction side) {
        return sided.fluids(side);
    }

    @Nullable
    public IItemHandler getItemHandler(@Nullable Direction side) {
        return sided.items(side);
    }

    public IItemHandler getMenuItemHandler() {
        return itemHandler;
    }

    public FluidTank getTank() {
        return tank;
    }

    public FluidStack getFluid() {
        return tank.getFluid();
    }

    public int getEnergyStored() {
        return energyStorage.getEnergyStored();
    }

    public int getEnergyCapacity() {
        return energyStorage.getMaxEnergyStored();
    }

    /** FE/t produced this tick (0 when idle or full). */
    public int getCurrentRate() {
        return currentRate;
    }

    @Override
    public SideConfig getSideConfig() {
        return sideConfig;
    }

    @Override
    public Direction getFacing() {
        BlockState state = getBlockState();
        return state.hasProperty(BlockStateProperties.HORIZONTAL_FACING)
                ? state.getValue(BlockStateProperties.HORIZONTAL_FACING) : Direction.NORTH;
    }

    @Override
    public void onSideConfigChanged() {
        SideConfigHelper.applyChange(this, sided);
    }

    @Override
    public Component getDisplayName() {
        return Component.translatable("block.voltaicdynamics.fluid_generator");
    }

    @Nullable
    @Override
    public AbstractContainerMenu createMenu(int containerId, Inventory playerInventory, Player player) {
        return new FluidGeneratorMenu(containerId, playerInventory, this);
    }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        tag.put("Inventory", itemHandler.serializeNBT(registries));
        tag.put("Energy", energyStorage.serializeNBT(registries));
        tag.put("Tank", tank.writeToNBT(registries, new CompoundTag()));
        tag.putInt("Credit", credit);
        tag.put("SideConfig", sideConfig.save());
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        if (tag.contains("Inventory")) itemHandler.deserializeNBT(registries, tag.getCompound("Inventory"));
        if (tag.contains("Energy")) energyStorage.deserializeNBT(registries, tag.getCompound("Energy"));
        if (tag.contains("Tank")) tank.readFromNBT(registries, tag.getCompound("Tank"));
        credit = tag.getInt("Credit");
        if (tag.contains("SideConfig")) sideConfig.load(tag.getCompound("SideConfig"));
        sided.invalidate();
    }
}
