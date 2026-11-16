package com.voltaicdynamics.machines;

import com.voltaicdynamics.VoltaicConfig;
import com.voltaicdynamics.machines.recipe.CrushingRecipe;
import com.voltaicdynamics.machines.recipe.ModRecipes;

import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.item.crafting.SingleRecipeInput;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.Nullable;

/** One input, one output: runs {@code voltaicdynamics:crushing} recipes with FE. */
public class PulverizerBlockEntity extends ProcessingMachineBlockEntity {
    public static final int SLOT_INPUT = 0;
    public static final int SLOT_OUTPUT = 1;

    @Nullable
    private RecipeHolder<CrushingRecipe> cachedRecipe;

    public PulverizerBlockEntity(BlockPos pos, BlockState blockState) {
        super(VoltaicMachines.PULVERIZER_BE.get(), pos, blockState, 1, VoltaicConfig.get(VoltaicConfig.PULVERIZER_CAPACITY));
    }

    @Override
    public int energyPerTick() {
        return VoltaicConfig.get(VoltaicConfig.PULVERIZER_ENERGY_PER_TICK);
    }

    @Override
    protected int defaultTicks() {
        return CrushingRecipe.DEFAULT_TIME;
    }

    /** Recipe time scaled by the config speed multiplier. */
    public static int scaledTime(int recipeTime) {
        return Math.max(1, (int) Math.round(recipeTime * VoltaicConfig.get(VoltaicConfig.PULVERIZER_TIME_MULTIPLIER)));
    }

    @Override
    protected boolean isValidInput(ItemStack stack) {
        if (level == null || stack.isEmpty()) return false;
        for (RecipeHolder<CrushingRecipe> holder : level.getRecipeManager().getAllRecipesFor(ModRecipes.CRUSHING.get())) {
            if (holder.value().uses(stack)) return true;
        }
        return false;
    }

    @Nullable
    @Override
    protected Craft findCraft(Level level) {
        ItemStack stack = itemHandler.getStackInSlot(SLOT_INPUT);
        if (stack.isEmpty()) return null;
        SingleRecipeInput input = new SingleRecipeInput(stack);
        RecipeHolder<CrushingRecipe> holder = cachedRecipe;
        if (holder == null || !holder.value().matches(input, level)) {
            holder = level.getRecipeManager().getRecipeFor(ModRecipes.CRUSHING.get(), input, level).orElse(null);
            if (holder == null) return null;
            cachedRecipe = holder;
        }
        CrushingRecipe recipe = holder.value();
        int count = recipe.ingredient().count();
        return new Craft(recipe.assemble(input, level.registryAccess()), scaledTime(recipe.time()), recipe.experience(),
                () -> itemHandler.extractItem(SLOT_INPUT, count, false), recipe.bonusChance());
    }

    @Override
    public Component getDisplayName() {
        return Component.translatable("block.voltaicdynamics.pulverizer");
    }

    @Nullable
    @Override
    public AbstractContainerMenu createMenu(int containerId, Inventory playerInventory, Player player) {
        return new PulverizerMenu(containerId, playerInventory, this);
    }
}
