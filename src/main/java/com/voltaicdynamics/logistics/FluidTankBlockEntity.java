package com.voltaicdynamics.logistics;

import com.voltaicdynamics.core.blockentity.VoltaicBlockEntity;
import com.voltaicdynamics.VoltaicConfig;
import com.voltaicdynamics.core.ModDataComponents;
import com.voltaicdynamics.core.sideconfig.IoMode;
import com.voltaicdynamics.core.sideconfig.SideConfig;
import com.voltaicdynamics.core.sideconfig.SideConfigHelper;
import com.voltaicdynamics.core.sideconfig.SideConfigurable;
import com.voltaicdynamics.core.sideconfig.SidedHandlers;
import com.voltaicdynamics.core.sideconfig.TransferType;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.component.DataComponentMap;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.world.Containers;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.fluids.FluidActionResult;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.FluidUtil;
import net.neoforged.neoforge.fluids.SimpleFluidContent;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;
import net.neoforged.neoforge.fluids.capability.templates.FluidTank;
import net.neoforged.neoforge.items.IItemHandler;
import net.neoforged.neoforge.items.ItemStackHandler;
import org.jetbrains.annotations.Nullable;

import java.util.List;

/**
 * Single-fluid tank. Keeps its fluid when broken, fills and empties buckets and other containers (right-click
 * or the GUI slots), shows the fluid level through the glass and can auto-eject from configured faces.
 */
public class FluidTankBlockEntity extends VoltaicBlockEntity implements MenuProvider, SideConfigurable {
    public static final int SLOT_IN = 0;
    public static final int SLOT_OUT = 1;
    private static final int SYNC_INTERVAL = 4;

    private final FluidTankTier tier;
    private final FluidTank tank;
    private final ItemStackHandler itemHandler = new ItemStackHandler(2) {
        @Override
        protected void onContentsChanged(int slot) {
            saveChanges();
        }

        @Override
        public boolean isItemValid(int slot, ItemStack stack) {
            return slot == SLOT_IN && stack.getCapability(Capabilities.FluidHandler.ITEM) != null;
        }

        @Override
        public int getSlotLimit(int slot) {
            return slot == SLOT_IN ? 1 : 64;
        }
    };
    /** Pipes: insert containers into the in slot, take them from the out slot. */
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
            .add(TransferType.FLUID, List.of(IoMode.BOTH, IoMode.INPUT, IoMode.OUTPUT, IoMode.NONE), IoMode.BOTH, true, false)
            .add(TransferType.ITEM, List.of(IoMode.BOTH, IoMode.INPUT, IoMode.OUTPUT, IoMode.NONE), IoMode.BOTH, false, false)
            .build();
    private final SidedHandlers sided;
    private final SideConfigHelper.Neighbors neighbors = new SideConfigHelper.Neighbors(this);
    private boolean needsSync;
    private int lastComparator = -1;

    public FluidTankBlockEntity(BlockPos pos, BlockState state) {
        super(VoltaicLogistics.FLUID_TANK_BE.get(), pos, state);
        this.tier = state.getBlock() instanceof FluidTankBlock block ? block.getTier() : FluidTankTier.BASIC;
        this.tank = tier.isCreative() ? new CreativeFluidTank(this::tankChanged) : new FluidTank(tier.capacity()) {
            @Override
            protected void onContentsChanged() {
                tankChanged();
            }
        };
        this.sided = new SidedHandlers(this, automationHandler, tank, null);
    }

    /** Capacity of the basic tank in mB. */
    public static int capacity() {
        return FluidTankTier.BASIC.capacity();
    }

    private void tankChanged() {
        saveChanges();
        needsSync = true;
    }

    public FluidTankTier getTier() {
        return tier;
    }

    /** Empties a Creative tank so it can take another fluid; other tanks keep their fluid. */
    public boolean clearCreative() {
        if (!(tank instanceof CreativeFluidTank creative) || creative.isEmpty()) return false;
        creative.clear();
        return true;
    }

    public static void tick(Level level, BlockPos pos, BlockState state, FluidTankBlockEntity tank) {
        tank.processContainers();
        if (tank.sideConfig.eject(TransferType.FLUID)) {
            SideConfigHelper.ejectFluid(tank, tank.neighbors, tank.tank, VoltaicConfig.get(VoltaicConfig.TANK_EJECT_RATE));
        }
        if (tank.needsSync && level.getGameTime() % SYNC_INTERVAL == 0) {
            tank.needsSync = false;
            level.sendBlockUpdated(pos, state, state, 2);
            int comparator = tank.getComparatorSignal();
            if (comparator != tank.lastComparator) {
                tank.lastComparator = comparator;
                level.updateNeighbourForOutputSignal(pos, state.getBlock());
            }
        }
    }

    /** Container in the in slot: empty it into the tank, or fill it from the tank when it is empty. */
    private void processContainers() {
        ItemStack input = itemHandler.getStackInSlot(SLOT_IN);
        if (input.isEmpty()) return;
        ItemStack output = itemHandler.getStackInSlot(SLOT_OUT);
        FluidActionResult result = FluidUtil.tryEmptyContainer(input, tank, Integer.MAX_VALUE, null, false);
        if (!result.isSuccess()) result = FluidUtil.tryFillContainer(input, tank, Integer.MAX_VALUE, null, false);
        if (!result.isSuccess()) return;
        ItemStack after = result.getResult();
        if (!after.isEmpty() && !output.isEmpty()
                && (!ItemStack.isSameItemSameComponents(output, after) || output.getCount() + after.getCount() > output.getMaxStackSize())) {
            return;
        }
        // Run it for real now that the output slot can take the result.
        FluidActionResult real = FluidUtil.tryEmptyContainer(input, tank, Integer.MAX_VALUE, null, true);
        if (!real.isSuccess()) real = FluidUtil.tryFillContainer(input, tank, Integer.MAX_VALUE, null, true);
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

    public FluidStack getFluid() {
        return tank.getFluid();
    }

    public FluidTank getTank() {
        return tank;
    }

    public int getComparatorSignal() {
        if (tank.isEmpty()) return 0;
        return 1 + (int) ((long) tank.getFluidAmount() * 14 / Math.max(1, tank.getCapacity()));
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

    public void drops() {
        if (level != null && !level.isClientSide) {
            SimpleContainer inventory = new SimpleContainer(itemHandler.getSlots());
            for (int i = 0; i < itemHandler.getSlots(); i++) inventory.setItem(i, itemHandler.getStackInSlot(i));
            Containers.dropContents(level, worldPosition, inventory);
        }
    }

    @Override
    public SideConfig getSideConfig() {
        return sideConfig;
    }

    @Override
    public Direction getFacing() {
        return Direction.NORTH;
    }

    @Override
    public boolean hasFront() {
        return false;
    }

    @Override
    public void onSideConfigChanged() {
        SideConfigHelper.applyChange(this, sided);
    }

    @Override
    public Component getDisplayName() {
        return getBlockState().getBlock().getName();
    }

    @Nullable
    @Override
    public AbstractContainerMenu createMenu(int containerId, Inventory playerInventory, Player player) {
        return new FluidTankMenu(containerId, playerInventory, this);
    }

    // Fluid travels with the item when the tank is broken.
    @Override
    protected void applyImplicitComponents(DataComponentInput input) {
        super.applyImplicitComponents(input);
        tank.setFluid(input.getOrDefault(ModDataComponents.FLUID.get(), SimpleFluidContent.EMPTY).copy());
    }

    @Override
    protected void collectImplicitComponents(DataComponentMap.Builder builder) {
        super.collectImplicitComponents(builder);
        if (!tank.isEmpty()) builder.set(ModDataComponents.FLUID.get(), SimpleFluidContent.copyOf(tank.getFluid()));
    }

    @Override
    public void removeComponentsFromTag(CompoundTag tag) {
        super.removeComponentsFromTag(tag);
        tag.remove("Tank");
    }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        tag.put("Tank", tank.writeToNBT(registries, new CompoundTag()));
        tag.put("Inventory", itemHandler.serializeNBT(registries));
        tag.put("SideConfig", sideConfig.save());
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        if (tag.contains("Tank")) tank.readFromNBT(registries, tag.getCompound("Tank"));
        if (tag.contains("Inventory")) itemHandler.deserializeNBT(registries, tag.getCompound("Inventory"));
        if (tag.contains("SideConfig")) sideConfig.load(tag.getCompound("SideConfig"));
        sided.invalidate();
    }

    /** Only the fluid goes to clients (for the renderer). */
    @Override
    public CompoundTag getUpdateTag(HolderLookup.Provider registries) {
        CompoundTag tag = new CompoundTag();
        tag.put("Tank", tank.writeToNBT(registries, new CompoundTag()));
        return tag;
    }

    @Nullable
    @Override
    public Packet<ClientGamePacketListener> getUpdatePacket() {
        return ClientboundBlockEntityDataPacket.create(this);
    }
}
