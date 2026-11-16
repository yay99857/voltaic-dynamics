package com.voltaicdynamics.machines;

import com.voltaicdynamics.VoltaicConfig;
import com.voltaicdynamics.machines.recipe.AlloyingInput;
import com.voltaicdynamics.machines.recipe.AlloyingRecipe;
import com.voltaicdynamics.machines.recipe.ModRecipes;

import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

/** Three input slots, one output: runs {@code voltaicdynamics:alloying} recipes with FE. */
public class AlloySmelterBlockEntity extends ProcessingMachineBlockEntity {
    public static final int INPUTS = 3;
    public static final int SLOT_OUTPUT = INPUTS;

    @Nullable
    private RecipeHolder<AlloyingRecipe> cachedRecipe;

    public AlloySmelterBlockEntity(BlockPos pos, BlockState blockState) {
        super(VoltaicMachines.ALLOY_SMELTER_BE.get(), pos, blockState, INPUTS, VoltaicConfig.get(VoltaicConfig.ALLOY_CAPACITY));
    }

    @Override
    public int energyPerTick() {
        return VoltaicConfig.get(VoltaicConfig.ALLOY_ENERGY_PER_TICK);
    }

    @Override
    protected int defaultTicks() {
        return AlloyingRecipe.DEFAULT_TIME;
    }

    /** Recipe time scaled by the config speed multiplier. */
    public static int scaledTime(int recipeTime) {
        return Math.max(1, (int) Math.round(recipeTime * VoltaicConfig.get(VoltaicConfig.ALLOY_TIME_MULTIPLIER)));
    }

    private AlloyingInput input() {
        List<ItemStack> stacks = new ArrayList<>(INPUTS);
        for (int slot = 0; slot < INPUTS; slot++) stacks.add(itemHandler.getStackInSlot(slot));
        return new AlloyingInput(stacks);
    }

    @Override
    protected boolean isValidInput(ItemStack stack) {
        if (level == null || stack.isEmpty()) return false;
        for (RecipeHolder<AlloyingRecipe> holder : level.getRecipeManager().getAllRecipesFor(ModRecipes.ALLOYING.get())) {
            if (holder.value().uses(stack)) return true;
        }
        return false;
    }

    @Nullable
    @Override
    protected Craft findCraft(Level level) {
        AlloyingInput input = input();
        if (input.isEmpty()) return null;
        RecipeHolder<AlloyingRecipe> holder = cachedRecipe;
        if (holder == null || !holder.value().matches(input, level)) {
            holder = level.getRecipeManager().getRecipeFor(ModRecipes.ALLOYING.get(), input, level).orElse(null);
            if (holder == null) return null;
            cachedRecipe = holder;
        }
        AlloyingRecipe recipe = holder.value();
        int[] slots = recipe.match(input);
        if (slots == null) return null;
        return new Craft(recipe.assemble(input, level.registryAccess()), scaledTime(recipe.time()), recipe.experience(), () -> {
            for (int i = 0; i < slots.length; i++) {
                itemHandler.extractItem(slots[i], recipe.ingredients().get(i).count(), false);
            }
        });
    }

    @Override
    public Component getDisplayName() {
        return Component.translatable("block.voltaicdynamics.alloy_smelter");
    }

    @Nullable
    @Override
    public AbstractContainerMenu createMenu(int containerId, Inventory playerInventory, Player player) {
        return new AlloySmelterMenu(containerId, playerInventory, this);
    }
}
