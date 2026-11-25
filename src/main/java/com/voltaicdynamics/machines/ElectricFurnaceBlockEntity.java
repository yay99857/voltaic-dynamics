package com.voltaicdynamics.machines;

import com.voltaicdynamics.VoltaicConfig;

import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.crafting.RecipePropertySet;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.item.crafting.SingleRecipeInput;
import net.minecraft.world.item.crafting.SmeltingRecipe;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.Nullable;

import java.util.Optional;

/** Smelts any vanilla furnace recipe with FE. */
public class ElectricFurnaceBlockEntity extends ProcessingMachineBlockEntity {
    public static final int SLOT_INPUT = 0;
    public static final int SLOT_OUTPUT = 1;

    @Nullable
    private RecipeHolder<SmeltingRecipe> cachedRecipe;

    public ElectricFurnaceBlockEntity(BlockPos pos, BlockState blockState) {
        super(VoltaicMachines.ELECTRIC_FURNACE_BE.get(), pos, blockState, 1, VoltaicConfig.get(VoltaicConfig.FURNACE_CAPACITY));
    }

    public static int ticksPerItem() {
        return VoltaicConfig.get(VoltaicConfig.FURNACE_TICKS_PER_ITEM);
    }

    @Override
    public int energyPerTick() {
        return VoltaicConfig.get(VoltaicConfig.FURNACE_ENERGY_PER_TICK);
    }

    @Override
    protected int defaultTicks() {
        return ticksPerItem();
    }

    private Optional<RecipeHolder<SmeltingRecipe>> findRecipe(ItemStack input) {
        if (level == null || input.isEmpty()) return Optional.empty();
        SingleRecipeInput recipeInput = new SingleRecipeInput(input);
        if (cachedRecipe != null && cachedRecipe.value().matches(recipeInput, level)) {
            return Optional.of(cachedRecipe);
        }
        if (!(level instanceof ServerLevel serverLevel)) return Optional.empty();
        Optional<RecipeHolder<SmeltingRecipe>> found =
                serverLevel.recipeAccess().getRecipeFor(RecipeType.SMELTING, recipeInput, level);
        found.ifPresent(recipe -> cachedRecipe = recipe);
        return found;
    }

    @Override
    protected boolean isValidInput(ItemStack stack) {
        // The furnace input set reaches the client too, unlike the recipes.
        return level != null && !stack.isEmpty()
                && level.recipeAccess().propertySet(RecipePropertySet.FURNACE_INPUT).test(stack);
    }

    @Nullable
    @Override
    protected Craft findCraft(Level level) {
        ItemStack input = itemHandler.getStackInSlot(SLOT_INPUT);
        Optional<RecipeHolder<SmeltingRecipe>> recipe = findRecipe(input);
        if (recipe.isEmpty()) return null;
        ItemStack result = recipe.get().value().assemble(new SingleRecipeInput(input));
        return new Craft(result, ticksPerItem(), recipe.get().value().experience(),
                times -> itemHandler.extractItem(SLOT_INPUT, times, false), input.getCount());
    }

    @Override
    public Component getDisplayName() {
        return Component.translatable("block.voltaicdynamics.electric_furnace");
    }

    @Nullable
    @Override
    public AbstractContainerMenu createMenu(int containerId, Inventory playerInventory, Player player) {
        return new ElectricFurnaceMenu(containerId, playerInventory, this);
    }
}
