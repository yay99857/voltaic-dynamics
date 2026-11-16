package com.voltaicdynamics.blockentity;

import com.voltaicdynamics.ModDataComponents;
import com.voltaicdynamics.block.EnergyCellBlock;
import com.voltaicdynamics.block.EnergyCellTier;
import com.voltaicdynamics.energy.NeighborEnergyCache;
import com.voltaicdynamics.energy.VoltaicEnergyStorage;
import com.voltaicdynamics.menu.EnergyCellMenu;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.component.DataComponentMap;
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
    public static final int SLOT_CHARGE = 0;
    public static final int SLOT_DISCHARGE = 1;

    private final EnergyCellTier tier;
    private final VoltaicEnergyStorage energyStorage;
    private final NeighborEnergyCache neighbors = new NeighborEnergyCache(this);

    private final ItemStackHandler itemHandler = new ItemStackHandler(2) {
        @Override
        protected void onContentsChanged(int slot) {
            setChanged();
        }

        @Override
        public boolean isItemValid(int slot, ItemStack stack) {
            IEnergyStorage itemEnergy = stack.getCapability(Capabilities.EnergyStorage.ITEM);
            if (itemEnergy == null) return false;
            return slot == SLOT_CHARGE ? itemEnergy.canReceive() : itemEnergy.canExtract();
        }

        @Override
        public int getSlotLimit(int slot) {
            return 1;
        }
    };

    /** What pipes and hoppers see: insert into either slot, but only take items out once they are done. */
    private final IItemHandler automationHandler = new IItemHandler() {
        @Override
        public int getSlots() {
            return itemHandler.getSlots();
        }

        @Override
        public ItemStack getStackInSlot(int slot) {
            return itemHandler.getStackInSlot(slot);
        }

        @Override
        public ItemStack insertItem(int slot, ItemStack stack, boolean simulate) {
            return itemHandler.insertItem(slot, stack, simulate);
        }

        @Override
        public ItemStack extractItem(int slot, int amount, boolean simulate) {
            ItemStack stack = itemHandler.getStackInSlot(slot);
            IEnergyStorage itemEnergy = stack.getCapability(Capabilities.EnergyStorage.ITEM);
            boolean done = itemEnergy == null
                    || (slot == SLOT_CHARGE ? itemEnergy.getEnergyStored() >= itemEnergy.getMaxEnergyStored()
                    : itemEnergy.getEnergyStored() <= 0);
            return done ? itemHandler.extractItem(slot, amount, simulate) : ItemStack.EMPTY;
        }

        @Override
        public int getSlotLimit(int slot) {
            return itemHandler.getSlotLimit(slot);
        }

        @Override
        public boolean isItemValid(int slot, ItemStack stack) {
            return itemHandler.isItemValid(slot, stack);
        }
    };

    protected final ContainerData dataAccess = new ContainerData() {
        @Override
        public int get(int index) {
            int energy = energyStorage.getEnergyStored();
            int capacity = energyStorage.getMaxEnergyStored();
            return switch (index) {
                case 0 -> energy & 0xFFFF;
                case 1 -> (energy >>> 16) & 0xFFFF;
                case 2 -> capacity & 0xFFFF;
                case 3 -> (capacity >>> 16) & 0xFFFF;
                case 4 -> tier.transfer() & 0xFFFF;
                case 5 -> (tier.transfer() >>> 16) & 0xFFFF;
                case 6 -> tier.ordinal();
                default -> 0;
            };
        }

        @Override
        public void set(int index, int value) {
        }

        @Override
        public int getCount() {
            return 7;
        }
    };

    public EnergyCellBlockEntity(BlockPos pos, BlockState blockState) {
        super(typeFor(tierOf(blockState)), pos, blockState);
        this.tier = tierOf(blockState);
        this.energyStorage = new VoltaicEnergyStorage(tier.capacity(), tier.transfer(), tier.transfer(), this::setChanged);
    }

    private static EnergyCellTier tierOf(BlockState state) {
        return state.getBlock() instanceof EnergyCellBlock cell ? cell.getTier() : EnergyCellTier.BASIC;
    }

    public static BlockEntityType<EnergyCellBlockEntity> typeFor(EnergyCellTier tier) {
        return tier == EnergyCellTier.ADVANCED
                ? ModBlockEntities.ADVANCED_ENERGY_CELL.get()
                : ModBlockEntities.BASIC_ENERGY_CELL.get();
    }

    public static void tick(Level level, BlockPos pos, BlockState state, EnergyCellBlockEntity entity) {
        int transfer = entity.tier.transfer();

        ItemStack chargeStack = entity.itemHandler.getStackInSlot(SLOT_CHARGE);
        if (!chargeStack.isEmpty() && entity.energyStorage.getEnergyStored() > 0) {
            IEnergyStorage itemEnergy = chargeStack.getCapability(Capabilities.EnergyStorage.ITEM);
            if (itemEnergy != null && itemEnergy.canReceive()) {
                int accepted = itemEnergy.receiveEnergy(Math.min(entity.energyStorage.getEnergyStored(), transfer), false);
                if (accepted > 0) entity.energyStorage.consumeEnergy(accepted);
            }
        }

        ItemStack dischargeStack = entity.itemHandler.getStackInSlot(SLOT_DISCHARGE);
        if (!dischargeStack.isEmpty() && entity.energyStorage.getSpace() > 0) {
            IEnergyStorage itemEnergy = dischargeStack.getCapability(Capabilities.EnergyStorage.ITEM);
            if (itemEnergy != null && itemEnergy.canExtract()) {
                int extracted = itemEnergy.extractEnergy(Math.min(entity.energyStorage.getSpace(), transfer), false);
                if (extracted > 0) entity.energyStorage.generateEnergy(extracted);
            }
        }

        // Only into blocks that cannot give energy back: machines and cables, never other batteries.
        entity.neighbors.push(entity.energyStorage, transfer, true);
    }

    public int getComparatorSignal() {
        int energy = energyStorage.getEnergyStored();
        if (energy <= 0) return 0;
        return 1 + (int) ((long) energy * 14 / Math.max(1, energyStorage.getMaxEnergyStored()));
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

    public IEnergyStorage getEnergyStorage(@Nullable Direction side) {
        return energyStorage;
    }

    public IItemHandler getItemHandler(@Nullable Direction side) {
        return automationHandler;
    }

    /** Unrestricted handler for the GUI slots. */
    public IItemHandler getMenuItemHandler() {
        return itemHandler;
    }

    public int getEnergyStored() {
        return energyStorage.getEnergyStored();
    }

    public EnergyCellTier getTier() {
        return tier;
    }

    @Override
    public Component getDisplayName() {
        return getBlockState().getBlock().getName();
    }

    @Nullable
    @Override
    public AbstractContainerMenu createMenu(int containerId, Inventory playerInventory, Player player) {
        return new EnergyCellMenu(containerId, playerInventory, this, dataAccess);
    }

    // Stored energy travels with the item when the cell is broken and placed again.
    @Override
    protected void applyImplicitComponents(DataComponentInput input) {
        super.applyImplicitComponents(input);
        energyStorage.setEnergy(input.getOrDefault(ModDataComponents.ENERGY.get(), 0));
    }

    @Override
    protected void collectImplicitComponents(DataComponentMap.Builder builder) {
        super.collectImplicitComponents(builder);
        if (energyStorage.getEnergyStored() > 0) {
            builder.set(ModDataComponents.ENERGY.get(), energyStorage.getEnergyStored());
        }
    }

    @Override
    public void removeComponentsFromTag(CompoundTag tag) {
        super.removeComponentsFromTag(tag);
        tag.remove("Energy");
    }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        tag.put("Inventory", itemHandler.serializeNBT(registries));
        tag.put("Energy", energyStorage.serializeNBT(registries));
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        if (tag.contains("Inventory")) itemHandler.deserializeNBT(registries, tag.getCompound("Inventory"));
        if (tag.contains("Energy")) energyStorage.deserializeNBT(registries, tag.getCompound("Energy"));
    }
}
