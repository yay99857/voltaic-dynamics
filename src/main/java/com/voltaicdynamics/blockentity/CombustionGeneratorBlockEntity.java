package com.voltaicdynamics.blockentity;

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
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.energy.IEnergyStorage;
import net.neoforged.neoforge.items.IItemHandler;
import net.neoforged.neoforge.items.ItemStackHandler;
import org.jetbrains.annotations.Nullable;

public class CombustionGeneratorBlockEntity extends BlockEntity implements MenuProvider {
    public static final int CAPACITY = 100_000;
    public static final int MAX_OUTPUT = 400;
    public static final int GENERATION_RATE = 40; // 40 FE/t

    private final VoltaicEnergyStorage energyStorage;
    private final ItemStackHandler itemHandler = new ItemStackHandler(1) {
        @Override
        protected void onContentsChanged(int slot) {
            setChanged();
        }

        @Override
        public boolean isItemValid(int slot, ItemStack stack) {
            return getBurnTime(stack) > 0;
        }
    };

    private int burnTime = 0;
    private int maxBurnTime = 0;

    // High precision client-server data synchronization
    protected final ContainerData dataAccess = new ContainerData() {
        @Override
        public int get(int index) {
            return switch (index) {
                case 0 -> energyStorage.getEnergyStored() & 0xFFFF;
                case 1 -> (energyStorage.getEnergyStored() >> 16) & 0xFFFF;
                case 2 -> burnTime;
                case 3 -> maxBurnTime;
                default -> 0;
            };
        }

        @Override
        public void set(int index, int value) {
            switch (index) {
                case 0 -> {
                    int high = (energyStorage.getEnergyStored() >> 16) & 0xFFFF;
                    energyStorage.setEnergy((high << 16) | (value & 0xFFFF));
                }
                case 1 -> {
                    int low = energyStorage.getEnergyStored() & 0xFFFF;
                    energyStorage.setEnergy(((value & 0xFFFF) << 16) | low);
                }
                case 2 -> burnTime = value;
                case 3 -> maxBurnTime = value;
            }
        }

        @Override
        public int getCount() {
            return 4;
        }
    };

    public CombustionGeneratorBlockEntity(BlockPos pos, BlockState blockState) {
        super(ModBlockEntities.COMBUSTION_GENERATOR.get(), pos, blockState);
        this.energyStorage = new VoltaicEnergyStorage(CAPACITY, 0, MAX_OUTPUT, 0, this::setChanged);
    }

    public static void tick(Level level, BlockPos pos, BlockState state, CombustionGeneratorBlockEntity entity) {
        if (level == null || level.isClientSide) return;

        boolean wasLit = state.getValue(BlockStateProperties.LIT);
        boolean isGenerating = false;

        if (entity.burnTime > 0) {
            entity.burnTime--;
            entity.energyStorage.generateEnergy(GENERATION_RATE);
            isGenerating = true;
        } else {
            // Burns fuel when internal buffer is not full
            if (entity.energyStorage.getEnergyStored() < CAPACITY) {
                ItemStack fuel = entity.itemHandler.getStackInSlot(0);
                if (!fuel.isEmpty()) {
                    int fuelBurn = entity.getBurnTime(fuel);
                    if (fuelBurn > 0) {
                        entity.maxBurnTime = fuelBurn;
                        entity.burnTime = fuelBurn;
                        fuel.shrink(1);
                        entity.setChanged();
                        isGenerating = true;
                    }
                }
            }
        }

        if (wasLit != isGenerating) {
            level.setBlock(pos, state.setValue(BlockStateProperties.LIT, isGenerating), 3);
        }

        // Push energy to adjacent machines and cables
        entity.pushEnergyToNeighbors(level, pos);
    }

    private void pushEnergyToNeighbors(Level level, BlockPos pos) {
        if (this.energyStorage.getEnergyStored() <= 0) return;

        for (Direction direction : Direction.values()) {
            BlockPos targetPos = pos.relative(direction);
            IEnergyStorage target = level.getCapability(Capabilities.EnergyStorage.BLOCK, targetPos, direction.getOpposite());
            if (target != null && target.canReceive()) {
                int toSend = Math.min(this.energyStorage.getEnergyStored(), MAX_OUTPUT);
                int accepted = target.receiveEnergy(toSend, false);
                if (accepted > 0) {
                    this.energyStorage.extractEnergy(accepted, false);
                }
                if (this.energyStorage.getEnergyStored() <= 0) break;
            }
        }
    }

    private int getBurnTime(ItemStack stack) {
        if (stack.isEmpty() || level == null) return 0;
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

    public IEnergyStorage getEnergyStorage(Direction side) {
        return this.energyStorage;
    }

    public IItemHandler getItemHandler(Direction side) {
        return this.itemHandler;
    }

    public int getEnergyStored() {
        return this.energyStorage.getEnergyStored();
    }

    public int getBurnTime() {
        return this.burnTime;
    }

    public int getMaxBurnTime() {
        return this.maxBurnTime;
    }

    @Override
    public Component getDisplayName() {
        return Component.translatable("block.voltaicdynamics.combustion_generator");
    }

    @Nullable
    @Override
    public AbstractContainerMenu createMenu(int containerId, Inventory playerInventory, Player player) {
        return new CombustionGeneratorMenu(containerId, playerInventory, this, this.dataAccess);
    }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        tag.put("Inventory", this.itemHandler.serializeNBT(registries));
        tag.put("Energy", this.energyStorage.serializeNBT(registries));
        tag.putInt("BurnTime", this.burnTime);
        tag.putInt("MaxBurnTime", this.maxBurnTime);
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
        this.burnTime = tag.getInt("BurnTime");
        this.maxBurnTime = tag.getInt("MaxBurnTime");
    }
}
