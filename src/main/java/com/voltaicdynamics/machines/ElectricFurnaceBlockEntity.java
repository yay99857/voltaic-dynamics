package com.voltaicdynamics.blockentity;

import com.voltaicdynamics.VoltaicConfig;
import com.voltaicdynamics.energy.VoltaicEnergyStorage;
import com.voltaicdynamics.menu.ElectricFurnaceMenu;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.Mth;
import net.minecraft.world.Containers;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.ExperienceOrb;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.item.crafting.SingleRecipeInput;
import net.minecraft.world.item.crafting.SmeltingRecipe;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.neoforged.neoforge.energy.IEnergyStorage;
import net.neoforged.neoforge.items.IItemHandler;
import net.neoforged.neoforge.items.ItemStackHandler;
import org.jetbrains.annotations.Nullable;

import java.util.Optional;

public class ElectricFurnaceBlockEntity extends BlockEntity implements MenuProvider {
    public static final int SLOT_INPUT = 0;
    public static final int SLOT_OUTPUT = 1;
    public static final int MAX_INPUT = 1_000;

    private final VoltaicEnergyStorage energyStorage;

    private final ItemStackHandler itemHandler = new ItemStackHandler(2) {
        @Override
        protected void onContentsChanged(int slot) {
            setChanged();
        }

        @Override
        public boolean isItemValid(int slot, ItemStack stack) {
            return slot == SLOT_INPUT && findRecipe(stack).isPresent();
        }
    };

    /** Pipes and hoppers: insert into the input, extract only the output. */
    private final IItemHandler automationHandler = new IItemHandler() {
        @Override
        public int getSlots() {
            return 2;
        }

        @Override
        public ItemStack getStackInSlot(int slot) {
            return itemHandler.getStackInSlot(slot);
        }

        @Override
        public ItemStack insertItem(int slot, ItemStack stack, boolean simulate) {
            return slot == SLOT_INPUT ? itemHandler.insertItem(slot, stack, simulate) : stack;
        }

        @Override
        public ItemStack extractItem(int slot, int amount, boolean simulate) {
            return slot == SLOT_OUTPUT ? itemHandler.extractItem(slot, amount, simulate) : ItemStack.EMPTY;
        }

        @Override
        public int getSlotLimit(int slot) {
            return itemHandler.getSlotLimit(slot);
        }

        @Override
        public boolean isItemValid(int slot, ItemStack stack) {
            return slot == SLOT_INPUT && itemHandler.isItemValid(slot, stack);
        }
    };

    private int progress = 0;
    private float storedExperience = 0.0F;
    private boolean active = false;
    @Nullable
    private RecipeHolder<SmeltingRecipe> cachedRecipe;

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
                case 4 -> progress;
                case 5 -> ticksPerItem();
                case 6 -> active ? VoltaicConfig.get(VoltaicConfig.FURNACE_ENERGY_PER_TICK) : 0;
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

    public ElectricFurnaceBlockEntity(BlockPos pos, BlockState blockState) {
        super(ModBlockEntities.ELECTRIC_FURNACE.get(), pos, blockState);
        this.energyStorage = new VoltaicEnergyStorage(VoltaicConfig.get(VoltaicConfig.FURNACE_CAPACITY),
                MAX_INPUT, 0, this::setChanged);
    }

    private static int ticksPerItem() {
        return VoltaicConfig.get(VoltaicConfig.FURNACE_TICKS_PER_ITEM);
    }

    private Optional<RecipeHolder<SmeltingRecipe>> findRecipe(ItemStack input) {
        if (level == null || input.isEmpty()) return Optional.empty();
        SingleRecipeInput recipeInput = new SingleRecipeInput(input);
        if (cachedRecipe != null && cachedRecipe.value().matches(recipeInput, level)) {
            return Optional.of(cachedRecipe);
        }
        Optional<RecipeHolder<SmeltingRecipe>> found =
                level.getRecipeManager().getRecipeFor(RecipeType.SMELTING, recipeInput, level);
        found.ifPresent(recipe -> cachedRecipe = recipe);
        return found;
    }

    public static void tick(Level level, BlockPos pos, BlockState state, ElectricFurnaceBlockEntity entity) {
        boolean working = entity.work(level);
        entity.active = working;
        if (state.getValue(BlockStateProperties.LIT) != working) {
            level.setBlock(pos, state.setValue(BlockStateProperties.LIT, working), 3);
        }
    }

    private boolean work(Level level) {
        ItemStack input = itemHandler.getStackInSlot(SLOT_INPUT);
        Optional<RecipeHolder<SmeltingRecipe>> recipe = findRecipe(input);
        if (recipe.isEmpty()) {
            if (progress != 0) {
                progress = 0;
                setChanged();
            }
            return false;
        }

        ItemStack result = recipe.get().value().assemble(new SingleRecipeInput(input), level.registryAccess());
        if (result.isEmpty() || !canOutput(result)) return false;

        // Without enough energy the machine pauses and keeps its progress.
        if (!energyStorage.consumeEnergy(VoltaicConfig.get(VoltaicConfig.FURNACE_ENERGY_PER_TICK))) return false;

        progress++;
        if (progress >= ticksPerItem()) {
            progress = 0;
            itemHandler.extractItem(SLOT_INPUT, 1, false);
            ItemStack output = itemHandler.getStackInSlot(SLOT_OUTPUT);
            if (output.isEmpty()) {
                itemHandler.setStackInSlot(SLOT_OUTPUT, result.copy());
            } else {
                output.grow(result.getCount());
                itemHandler.setStackInSlot(SLOT_OUTPUT, output);
            }
            storedExperience += recipe.get().value().getExperience();
        }
        setChanged();
        return true;
    }

    private boolean canOutput(ItemStack result) {
        ItemStack output = itemHandler.getStackInSlot(SLOT_OUTPUT);
        if (output.isEmpty()) return true;
        if (!ItemStack.isSameItemSameComponents(output, result)) return false;
        return output.getCount() + result.getCount() <= Math.min(output.getMaxStackSize(), itemHandler.getSlotLimit(SLOT_OUTPUT));
    }

    /** Called when a player takes items out of the output slot. */
    public void awardExperience(Player player) {
        if (!(level instanceof ServerLevel serverLevel) || storedExperience <= 0) return;
        int whole = Mth.floor(storedExperience);
        float fraction = Mth.frac(storedExperience);
        if (fraction > 0 && serverLevel.random.nextFloat() < fraction) whole++;
        storedExperience = 0;
        setChanged();
        if (whole > 0) ExperienceOrb.award(serverLevel, player.position(), whole);
    }

    public int getComparatorSignal() {
        ItemStack input = itemHandler.getStackInSlot(SLOT_INPUT);
        if (input.isEmpty()) return 0;
        return Math.max(1, Math.round(15.0F * input.getCount() / input.getMaxStackSize()));
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

    public IItemHandler getMenuItemHandler() {
        return itemHandler;
    }

    public int getEnergyStored() {
        return energyStorage.getEnergyStored();
    }

    public int getProgress() {
        return progress;
    }

    @Override
    public Component getDisplayName() {
        return Component.translatable("block.voltaicdynamics.electric_furnace");
    }

    @Nullable
    @Override
    public AbstractContainerMenu createMenu(int containerId, Inventory playerInventory, Player player) {
        return new ElectricFurnaceMenu(containerId, playerInventory, this, dataAccess);
    }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        tag.put("Inventory", itemHandler.serializeNBT(registries));
        tag.put("Energy", energyStorage.serializeNBT(registries));
        tag.putInt("Progress", progress);
        tag.putFloat("Experience", storedExperience);
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        if (tag.contains("Inventory")) itemHandler.deserializeNBT(registries, tag.getCompound("Inventory"));
        if (tag.contains("Energy")) energyStorage.deserializeNBT(registries, tag.getCompound("Energy"));
        progress = tag.getInt("Progress");
        storedExperience = tag.getFloat("Experience");
    }
}
