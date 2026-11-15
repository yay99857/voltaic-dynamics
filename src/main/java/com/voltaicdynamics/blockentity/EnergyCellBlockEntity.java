package com.voltaicdynamics.blockentity;

import com.voltaicdynamics.energy.VoltaicEnergyStorage;
import com.voltaicdynamics.menu.EnergyCellMenu;
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
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.energy.IEnergyStorage;
import net.neoforged.neoforge.items.IItemHandler;
import net.neoforged.neoforge.items.ItemStackHandler;
import org.jetbrains.annotations.Nullable;

public class EnergyCellBlockEntity extends BlockEntity implements MenuProvider {
    private final int capacity;
    private final int maxTransfer;
    private final boolean isAdvanced;

    private final VoltaicEnergyStorage energyStorage;
    private final ItemStackHandler itemHandler = new ItemStackHandler(2) {
        @Override
        protected void onContentsChanged(int slot) {
            setChanged();
        }

        @Override
        public boolean isItemValid(int slot, ItemStack stack) {
            IEnergyStorage itemEnergy = stack.getCapability(Capabilities.EnergyStorage.ITEM);
            if (itemEnergy == null) return false;
            if (slot == 0) return itemEnergy.canReceive(); // Charge slot
            if (slot == 1) return itemEnergy.canExtract(); // Discharge slot
            return false;
        }
    };

    protected final ContainerData dataAccess = new ContainerData() {
        @Override
        public int get(int index) {
            return switch (index) {
                case 0 -> energyStorage.getEnergyStored() & 0xFFFF;
                case 1 -> (energyStorage.getEnergyStored() >> 16) & 0xFFFF;
                case 2 -> capacity & 0xFFFF;
                case 3 -> (capacity >> 16) & 0xFFFF;
                case 4 -> maxTransfer;
                case 5 -> isAdvanced ? 1 : 0;
                default -> 0;
            };
        }

        @Override
        public void set(int index, int value) {
            if (index == 0) {
                int high = (energyStorage.getEnergyStored() >> 16) & 0xFFFF;
                energyStorage.setEnergy((high << 16) | (value & 0xFFFF));
            } else if (index == 1) {
                int low = energyStorage.getEnergyStored() & 0xFFFF;
                energyStorage.setEnergy(((value & 0xFFFF) << 16) | low);
            }
        }

        @Override
        public int getCount() {
            return 6;
        }
    };

    public EnergyCellBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState blockState, int capacity, int maxTransfer, boolean isAdvanced) {
        super(type, pos, blockState);
        this.capacity = capacity;
        this.maxTransfer = maxTransfer;
        this.isAdvanced = isAdvanced;
        this.energyStorage = new VoltaicEnergyStorage(capacity, maxTransfer, maxTransfer, 0, this::setChanged);
    }

    public static void tick(Level level, BlockPos pos, BlockState state, EnergyCellBlockEntity entity) {
        if (level == null || level.isClientSide) return;

        // Slot 0: Charge Item in slot
        ItemStack chargeStack = entity.itemHandler.getStackInSlot(0);
        if (!chargeStack.isEmpty() && entity.energyStorage.getEnergyStored() > 0) {
            IEnergyStorage itemEnergy = chargeStack.getCapability(Capabilities.EnergyStorage.ITEM);
            if (itemEnergy != null && itemEnergy.canReceive()) {
                int toSend = Math.min(entity.energyStorage.getEnergyStored(), entity.maxTransfer);
                int accepted = itemEnergy.receiveEnergy(toSend, false);
                if (accepted > 0) {
                    entity.energyStorage.extractEnergy(accepted, false);
                }
            }
        }

        // Slot 1: Discharge Item in slot into Cell
        ItemStack dischargeStack = entity.itemHandler.getStackInSlot(1);
        if (!dischargeStack.isEmpty() && entity.energyStorage.getEnergyStored() < entity.capacity) {
            IEnergyStorage itemEnergy = dischargeStack.getCapability(Capabilities.EnergyStorage.ITEM);
            if (itemEnergy != null && itemEnergy.canExtract()) {
                int maxCanReceive = Math.min(entity.capacity - entity.energyStorage.getEnergyStored(), entity.maxTransfer);
                int extracted = itemEnergy.extractEnergy(maxCanReceive, false);
                if (extracted > 0) {
                    entity.energyStorage.generateEnergy(extracted);
                }
            }
        }

        // Auto-Push to adjacent blocks and cables
        entity.pushEnergyToNeighbors(level, pos);
    }

    private void pushEnergyToNeighbors(Level level, BlockPos pos) {
        if (this.energyStorage.getEnergyStored() <= 0) return;

        for (Direction direction : Direction.values()) {
            BlockPos targetPos = pos.relative(direction);
            IEnergyStorage target = level.getCapability(Capabilities.EnergyStorage.BLOCK, targetPos, direction.getOpposite());
            if (target != null && target.canReceive()) {
                int toSend = Math.min(this.energyStorage.getEnergyStored(), this.maxTransfer);
                int accepted = target.receiveEnergy(toSend, false);
                if (accepted > 0) {
                    this.energyStorage.extractEnergy(accepted, false);
                }
                if (this.energyStorage.getEnergyStored() <= 0) break;
            }
        }
    }

    public void drops() {
        if (level != null && !level.isClientSide) {
            SimpleContainer inventory = new SimpleContainer(itemHandler.getSlots());
            for (int i = 0; i < itemHandler.getSlots(); i++) {
                inventory.setItem(i, itemHandler.getStackInSlot(i));
            }
            Containers.dropContents(level, worldPosition, inventory);
        }
    }

    public IEnergyStorage getEnergyStorage(Direction side) {
        return this.energyStorage;
    }

    public IItemHandler getItemHandler(Direction side) {
        return this.itemHandler;
    }

    public int getEnergyStored() {
        return this.energyStorage.getEnergyStored();
    }

    public int getCapacity() {
        return this.capacity;
    }

    public int getMaxTransfer() {
        return this.maxTransfer;
    }

    public boolean isAdvanced() {
        return this.isAdvanced;
    }

    @Override
    public Component getDisplayName() {
        return Component.translatable(isAdvanced ? "block.voltaicdynamics.advanced_energy_cell" : "block.voltaicdynamics.basic_energy_cell");
    }

    @Nullable
    @Override
    public AbstractContainerMenu createMenu(int containerId, Inventory playerInventory, Player player) {
        return new EnergyCellMenu(containerId, playerInventory, this, this.dataAccess);
    }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        tag.put("Inventory", this.itemHandler.serializeNBT(registries));
        tag.put("Energy", this.energyStorage.serializeNBT(registries));
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        if (tag.contains("Inventory")) {
            this.itemHandler.deserializeNBT(registries, tag.getCompound("Inventory"));
        }
        if (tag.contains("Energy")) {
            this.energyStorage.deserializeNBT(registries, tag.getCompound("Energy"));
        }
    }
}
