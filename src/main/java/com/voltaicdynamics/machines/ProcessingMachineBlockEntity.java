package com.voltaicdynamics.machines;

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
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.Mth;
import net.minecraft.world.Containers;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.ExperienceOrb;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.RecipeManager;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.neoforged.neoforge.energy.IEnergyStorage;
import net.neoforged.neoforge.items.IItemHandler;
import net.neoforged.neoforge.items.ItemStackHandler;
import org.jetbrains.annotations.Nullable;

import java.util.List;

/**
 * Shared logic of the FE machines that turn input slots into one output slot (Electric Furnace, Alloy Smelter):
 * energy buffer, progress that pauses without power, XP, comparator, side configuration and auto-eject.
 */
public abstract class ProcessingMachineBlockEntity extends VoltaicBlockEntity implements MenuProvider, SideConfigurable {
    public static final int MAX_INPUT = 1_000;
    private static final int EJECT_INTERVAL = 10;
    private static final int EJECT_ITEMS = 16;

    protected final int inputSlots;
    protected final VoltaicEnergyStorage energyStorage;
    protected final ItemStackHandler itemHandler;
    private final IItemHandler automationHandler;
    private final SideConfig sideConfig;
    private final SidedHandlers sided;
    private final SideConfigHelper.Neighbors neighbors = new SideConfigHelper.Neighbors(this);

    protected int progress = 0;
    protected int maxProgress = 0;
    private float storedExperience = 0.0F;
    private boolean active = false;

    // Recipe lookup cache: findCraft runs only when the inputs (item, count, components) or the recipe set change,
    // so idle or steadily working machines skip the recipe search every tick. The inputs are compared against a
    // snapshot instead of trusting onContentsChanged, because menus can mutate a slot's stack in place.
    private final ItemStack[] craftInputs;
    @Nullable
    private Craft cachedCraft;
    @Nullable
    private RecipeManager cachedRecipes;

    /**
     * One operation the machine can run with what is in its input slots right now. {@code bonusChance} (0..1) is
     * the chance of one extra result item; the output slot must have room for it before the operation starts.
     */
    public record Craft(ItemStack result, int ticks, float experience, Runnable consume, float bonusChance) {
        public Craft(ItemStack result, int ticks, float experience, Runnable consume) {
            this(result, ticks, experience, consume, 0F);
        }

        /** The most the operation can put in the output slot. */
        ItemStack maxResult() {
            return bonusChance > 0 ? result.copyWithCount(result.getCount() + 1) : result;
        }
    }

    protected ProcessingMachineBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state, int inputSlots, int capacity) {
        super(type, pos, state);
        this.inputSlots = inputSlots;
        this.craftInputs = new ItemStack[inputSlots];
        this.energyStorage = new VoltaicEnergyStorage(capacity, MAX_INPUT, 0, this::saveChanges);
        this.itemHandler = new ItemStackHandler(inputSlots + 1) {
            @Override
            protected void onContentsChanged(int slot) {
                saveChanges();
            }

            @Override
            public boolean isItemValid(int slot, ItemStack stack) {
                return slot < ProcessingMachineBlockEntity.this.inputSlots && isValidInput(stack);
            }
        };
        this.automationHandler = new IItemHandler() {
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
                if (slot >= ProcessingMachineBlockEntity.this.inputSlots) return stack;
                // Pipes spread one item type over one slot only, so a stream of iron cannot fill every slot.
                if (ProcessingMachineBlockEntity.this.inputSlots > 1 && itemHandler.getStackInSlot(slot).isEmpty()) {
                    for (int other = 0; other < ProcessingMachineBlockEntity.this.inputSlots; other++) {
                        if (other != slot && ItemStack.isSameItemSameComponents(itemHandler.getStackInSlot(other), stack)) {
                            return stack;
                        }
                    }
                }
                return itemHandler.insertItem(slot, stack, simulate);
            }

            @Override
            public ItemStack extractItem(int slot, int amount, boolean simulate) {
                return slot == outputSlot() ? itemHandler.extractItem(slot, amount, simulate) : ItemStack.EMPTY;
            }

            @Override
            public int getSlotLimit(int slot) {
                return itemHandler.getSlotLimit(slot);
            }

            @Override
            public boolean isItemValid(int slot, ItemStack stack) {
                return slot < ProcessingMachineBlockEntity.this.inputSlots && itemHandler.isItemValid(slot, stack);
            }
        };
        this.sideConfig = SideConfig.builder()
                .add(TransferType.ITEM, List.of(IoMode.BOTH, IoMode.INPUT, IoMode.OUTPUT, IoMode.NONE), IoMode.BOTH, true, false)
                .add(TransferType.ENERGY, List.of(IoMode.INPUT, IoMode.NONE), IoMode.INPUT, false, false)
                .build();
        this.sided = new SidedHandlers(this, automationHandler, null, energyStorage);
    }

    /** True when {@code stack} is part of at least one recipe of this machine. */
    protected abstract boolean isValidInput(ItemStack stack);

    /** The operation matching the current inputs, or null. */
    @Nullable
    protected abstract Craft findCraft(Level level);

    public abstract int energyPerTick();

    /** Shown while idle: the default duration of one operation. */
    protected abstract int defaultTicks();

    public int outputSlot() {
        return inputSlots;
    }

    public int getInputSlots() {
        return inputSlots;
    }

    public int getMaxProgress() {
        return maxProgress > 0 ? maxProgress : defaultTicks();
    }

    public static void tick(Level level, BlockPos pos, BlockState state, ProcessingMachineBlockEntity entity) {
        boolean working = entity.work(level);
        entity.active = working;
        if (state.hasProperty(BlockStateProperties.LIT) && state.getValue(BlockStateProperties.LIT) != working) {
            level.setBlock(pos, state.setValue(BlockStateProperties.LIT, working), 3);
        }
        if (entity.sideConfig.eject(TransferType.ITEM) && Math.floorMod(level.getGameTime() + pos.asLong(), EJECT_INTERVAL) == 0) {
            SideConfigHelper.ejectItems(entity, entity.neighbors, entity.itemHandler, new int[]{entity.outputSlot()}, EJECT_ITEMS);
        }
    }

    /** {@link #findCraft} for the current inputs, reused while the inputs and the loaded recipes stay the same. */
    @Nullable
    private Craft currentCraft(Level level) {
        RecipeManager recipes = level.getRecipeManager();
        boolean same = cachedRecipes == recipes;
        for (int slot = 0; same && slot < inputSlots; slot++) {
            ItemStack previous = craftInputs[slot];
            same = previous != null && ItemStack.matches(previous, itemHandler.getStackInSlot(slot));
        }
        if (same) return cachedCraft;

        cachedCraft = findCraft(level);
        cachedRecipes = recipes;
        for (int slot = 0; slot < inputSlots; slot++) {
            craftInputs[slot] = itemHandler.getStackInSlot(slot).copy();
        }
        return cachedCraft;
    }

    private boolean work(Level level) {
        Craft craft = currentCraft(level);
        if (craft == null || craft.result().isEmpty()) {
            if (progress != 0) {
                progress = 0;
                saveChanges();
            }
            maxProgress = 0;
            return false;
        }
        maxProgress = Math.max(1, craft.ticks());
        if (!canOutput(craft.maxResult())) return false;

        // Without enough energy the machine pauses and keeps its progress.
        if (!energyStorage.consumeEnergy(energyPerTick())) return false;

        progress++;
        if (progress >= maxProgress) {
            progress = 0;
            craft.consume().run();
            ItemStack made = craft.result().copy();
            if (craft.bonusChance() > 0 && level.random.nextFloat() < craft.bonusChance()) made.grow(1);
            ItemStack output = itemHandler.getStackInSlot(outputSlot());
            if (output.isEmpty()) {
                itemHandler.setStackInSlot(outputSlot(), made);
            } else {
                output.grow(made.getCount());
                itemHandler.setStackInSlot(outputSlot(), output);
            }
            storedExperience += craft.experience();
        }
        saveChanges();
        return true;
    }

    private boolean canOutput(ItemStack result) {
        ItemStack output = itemHandler.getStackInSlot(outputSlot());
        if (output.isEmpty()) return true;
        if (!ItemStack.isSameItemSameComponents(output, result)) return false;
        return output.getCount() + result.getCount() <= Math.min(output.getMaxStackSize(), itemHandler.getSlotLimit(outputSlot()));
    }

    /** Called when a player takes items out of the output slot. */
    public void awardExperience(Player player) {
        if (!(level instanceof ServerLevel serverLevel) || storedExperience <= 0) return;
        int whole = Mth.floor(storedExperience);
        float fraction = Mth.frac(storedExperience);
        if (fraction > 0 && serverLevel.random.nextFloat() < fraction) whole++;
        storedExperience = 0;
        saveChanges();
        if (whole > 0) ExperienceOrb.award(serverLevel, player.position(), whole);
    }

    /** Fill level of the input slots. */
    public int getComparatorSignal() {
        float fill = 0;
        int used = 0;
        for (int slot = 0; slot < inputSlots; slot++) {
            ItemStack input = itemHandler.getStackInSlot(slot);
            if (input.isEmpty()) continue;
            fill += (float) input.getCount() / input.getMaxStackSize();
            used++;
        }
        if (used == 0) return 0;
        return Math.max(1, Math.round(15.0F * fill / inputSlots));
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

    @Nullable
    public IEnergyStorage getEnergyStorage(@Nullable Direction side) {
        return sided.energy(side);
    }

    @Nullable
    public IItemHandler getItemHandler(@Nullable Direction side) {
        return sided.items(side);
    }

    public IItemHandler getMenuItemHandler() {
        return itemHandler;
    }

    public int getEnergyStored() {
        return energyStorage.getEnergyStored();
    }

    public int getEnergyCapacity() {
        return energyStorage.getMaxEnergyStored();
    }

    public int getProgress() {
        return progress;
    }

    public boolean isActive() {
        return active;
    }

    @Override
    public SideConfig getSideConfig() {
        return sideConfig;
    }

    @Override
    public Direction getFacing() {
        BlockState state = getBlockState();
        return state.hasProperty(HorizontalDirectionalBlock.FACING) ? state.getValue(HorizontalDirectionalBlock.FACING) : Direction.NORTH;
    }

    @Override
    public void onSideConfigChanged() {
        SideConfigHelper.applyChange(this, sided);
    }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        tag.put("Inventory", itemHandler.serializeNBT(registries));
        tag.put("Energy", energyStorage.serializeNBT(registries));
        tag.putInt("Progress", progress);
        tag.putFloat("Experience", storedExperience);
        tag.put("SideConfig", sideConfig.save());
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        if (tag.contains("Inventory")) itemHandler.deserializeNBT(registries, tag.getCompound("Inventory"));
        if (tag.contains("Energy")) energyStorage.deserializeNBT(registries, tag.getCompound("Energy"));
        progress = tag.getInt("Progress");
        storedExperience = tag.getFloat("Experience");
        if (tag.contains("SideConfig")) sideConfig.load(tag.getCompound("SideConfig"));
        sided.invalidate();
    }
}
