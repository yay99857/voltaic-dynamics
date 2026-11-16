package com.voltaicdynamics.blockentity;

import com.voltaicdynamics.VoltaicConfig;
import com.voltaicdynamics.energy.NeighborEnergyCache;
import com.voltaicdynamics.energy.VoltaicEnergyStorage;
import com.voltaicdynamics.menu.CombustionGeneratorMenu;
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
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.neoforged.neoforge.energy.IEnergyStorage;
import net.neoforged.neoforge.items.IItemHandler;
import net.neoforged.neoforge.items.ItemStackHandler;
import org.jetbrains.annotations.Nullable;

public class CombustionGeneratorBlockEntity extends BlockEntity implements MenuProvider {
    /** FE/t pushed out to neighbours. */
    public static final int MAX_OUTPUT = 400;

    private final VoltaicEnergyStorage energyStorage;
    private final NeighborEnergyCache neighbors = new NeighborEnergyCache(this);

    private final ItemStackHandler itemHandler = new ItemStackHandler(1) {
        @Override
        protected void onContentsChanged(int slot) {
            setChanged();
        }

        @Override
        public boolean isItemValid(int slot, ItemStack stack) {
            return burnTimeOf(stack) > 0;
        }
    };

    /** Pipes may insert fuel, and may only take out what is not fuel (the empty bucket left by lava). */
    private final IItemHandler automationHandler = new IItemHandler() {
        @Override
        public int getSlots() {
            return 1;
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
            if (burnTimeOf(itemHandler.getStackInSlot(slot)) > 0) return ItemStack.EMPTY;
            return itemHandler.extractItem(slot, amount, simulate);
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

    private int burnTime = 0;
    private int maxBurnTime = 0;
    private int currentRate = 0;

    protected final ContainerData dataAccess = new ContainerData() {
        @Override
        public int get(int index) {
            int energy = energyStorage.getEnergyStored();
            int capacity = energyStorage.getMaxEnergyStored();
            return switch (index) {
                case 0 -> energy & 0xFFFF;
                case 1 -> (energy >>> 16) & 0xFFFF;
                case 2 -> burnTime & 0xFFFF;
                case 3 -> maxBurnTime & 0xFFFF;
                case 4 -> capacity & 0xFFFF;
                case 5 -> (capacity >>> 16) & 0xFFFF;
                case 6 -> currentRate;
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

    public CombustionGeneratorBlockEntity(BlockPos pos, BlockState blockState) {
        super(ModBlockEntities.COMBUSTION_GENERATOR.get(), pos, blockState);
        this.energyStorage = new VoltaicEnergyStorage(VoltaicConfig.get(VoltaicConfig.COMBUSTION_CAPACITY),
                0, MAX_OUTPUT, this::setChanged);
    }

    public static void tick(Level level, BlockPos pos, BlockState state, CombustionGeneratorBlockEntity entity) {
        int rate = VoltaicConfig.get(VoltaicConfig.COMBUSTION_GENERATION);
        boolean hasRoom = entity.energyStorage.getSpace() >= rate;

        // Only start a new item when the buffer can take a full tick of output,
        // and pause (keeping the fuel's remaining time) while the buffer is full.
        if (entity.burnTime <= 0 && hasRoom) {
            entity.consumeFuel(level);
        }

        boolean generating = false;
        if (entity.burnTime > 0 && hasRoom) {
            entity.burnTime--;
            entity.energyStorage.generateEnergy(rate);
            generating = true;
        }
        entity.currentRate = generating ? rate : 0;

        if (state.getValue(BlockStateProperties.LIT) != generating) {
            level.setBlock(pos, state.setValue(BlockStateProperties.LIT, generating), 3);
        }

        entity.neighbors.push(entity.energyStorage, MAX_OUTPUT, false);
    }

    private void consumeFuel(Level level) {
        ItemStack fuel = itemHandler.getStackInSlot(0);
        int fuelBurn = burnTimeOf(fuel);
        if (fuelBurn <= 0) return;

        ItemStack remainder = fuel.getCraftingRemainingItem();
        maxBurnTime = fuelBurn;
        burnTime = fuelBurn;
        if (fuel.getCount() == 1) {
            // Lava bucket -> empty bucket stays in the slot, like a vanilla furnace.
            itemHandler.setStackInSlot(0, remainder);
        } else {
            itemHandler.extractItem(0, 1, false);
            if (!remainder.isEmpty()) {
                Containers.dropItemStack(level, worldPosition.getX() + 0.5, worldPosition.getY() + 1.0,
                        worldPosition.getZ() + 0.5, remainder);
            }
        }
        setChanged();
    }

    private static int burnTimeOf(ItemStack stack) {
        if (stack.isEmpty()) return 0;
        return stack.getBurnTime(RecipeType.SMELTING);
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

    /** Unrestricted handler for the GUI slot, so players can take the empty bucket or fuel back. */
    public IItemHandler getMenuItemHandler() {
        return itemHandler;
    }

    public int getEnergyStored() {
        return energyStorage.getEnergyStored();
    }

    public int getBurnTime() {
        return burnTime;
    }

    @Override
    public Component getDisplayName() {
        return Component.translatable("block.voltaicdynamics.combustion_generator");
    }

    @Nullable
    @Override
    public AbstractContainerMenu createMenu(int containerId, Inventory playerInventory, Player player) {
        return new CombustionGeneratorMenu(containerId, playerInventory, this, dataAccess);
    }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        tag.put("Inventory", itemHandler.serializeNBT(registries));
        tag.put("Energy", energyStorage.serializeNBT(registries));
        tag.putInt("BurnTime", burnTime);
        tag.putInt("MaxBurnTime", maxBurnTime);
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        if (tag.contains("Inventory")) itemHandler.deserializeNBT(registries, tag.getCompound("Inventory"));
        if (tag.contains("Energy")) energyStorage.deserializeNBT(registries, tag.getCompound("Energy"));
        burnTime = tag.getInt("BurnTime");
        maxBurnTime = tag.getInt("MaxBurnTime");
    }
}
