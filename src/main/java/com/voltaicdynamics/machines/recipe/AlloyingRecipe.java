package com.voltaicdynamics.machines.recipe;

import com.voltaicdynamics.machines.VoltaicMachines;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.NonNullList;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.level.Level;
import net.neoforged.neoforge.common.crafting.SizedIngredient;
import org.jetbrains.annotations.Nullable;

import java.util.List;

/**
 * {@code voltaicdynamics:alloying}: up to three sized ingredients in any slot order make one result.
 * Packs and other mods can add their own through datapacks:
 * <pre>{"type": "voltaicdynamics:alloying",
 *  "ingredients": [{"tag": "c:ingots/iron", "count": 1}, {"item": "minecraft:redstone", "count": 1}],
 *  "result": {"id": "voltaicdynamics:voltaic_ingot", "count": 2}, "time": 100, "experience": 0.5}</pre>
 */
public record AlloyingRecipe(List<SizedIngredient> ingredients, ItemStack result, int time, float experience)
        implements Recipe<AlloyingInput> {
    public static final int MAX_INGREDIENTS = 3;
    public static final int DEFAULT_TIME = 100;

    public static final MapCodec<AlloyingRecipe> CODEC = RecordCodecBuilder.mapCodec(instance -> instance.group(
            SizedIngredient.FLAT_CODEC.listOf(1, MAX_INGREDIENTS).fieldOf("ingredients").forGetter(AlloyingRecipe::ingredients),
            ItemStack.STRICT_CODEC.fieldOf("result").forGetter(AlloyingRecipe::result),
            Codec.intRange(1, 72_000).optionalFieldOf("time", DEFAULT_TIME).forGetter(AlloyingRecipe::time),
            Codec.floatRange(0, 100).optionalFieldOf("experience", 0.0F).forGetter(AlloyingRecipe::experience)
    ).apply(instance, AlloyingRecipe::new));

    public static final StreamCodec<RegistryFriendlyByteBuf, AlloyingRecipe> STREAM_CODEC = StreamCodec.composite(
            SizedIngredient.STREAM_CODEC.apply(ByteBufCodecs.list(MAX_INGREDIENTS)), AlloyingRecipe::ingredients,
            ItemStack.STREAM_CODEC, AlloyingRecipe::result,
            ByteBufCodecs.VAR_INT, AlloyingRecipe::time,
            ByteBufCodecs.FLOAT, AlloyingRecipe::experience,
            AlloyingRecipe::new);

    /**
     * Which input slot feeds each ingredient, or null when the inputs do not fit. Every non-empty slot must be
     * used, so leftovers of another recipe never get consumed by accident.
     */
    @Nullable
    public int[] match(AlloyingInput input) {
        int[] assignment = new int[ingredients.size()];
        boolean[] used = new boolean[input.size()];
        if (!assign(input, 0, assignment, used)) return null;
        for (int slot = 0; slot < input.size(); slot++) {
            if (!used[slot] && !input.getItem(slot).isEmpty()) return null;
        }
        return assignment;
    }

    private boolean assign(AlloyingInput input, int index, int[] assignment, boolean[] used) {
        if (index == ingredients.size()) return true;
        SizedIngredient ingredient = ingredients.get(index);
        for (int slot = 0; slot < input.size(); slot++) {
            if (used[slot]) continue;
            ItemStack stack = input.getItem(slot);
            if (stack.isEmpty() || !ingredient.ingredient().test(stack)) continue;
            if (stack.getCount() < ingredient.count()) continue;
            used[slot] = true;
            assignment[index] = slot;
            if (assign(input, index + 1, assignment, used)) return true;
            used[slot] = false;
        }
        return false;
    }

    /** True when the stack could be an ingredient of this recipe (used to accept it in an input slot). */
    public boolean uses(ItemStack stack) {
        for (SizedIngredient ingredient : ingredients) {
            if (ingredient.ingredient().test(stack)) return true;
        }
        return false;
    }

    @Override
    public boolean matches(AlloyingInput input, Level level) {
        return match(input) != null;
    }

    @Override
    public ItemStack assemble(AlloyingInput input, HolderLookup.Provider registries) {
        return result.copy();
    }

    @Override
    public boolean canCraftInDimensions(int width, int height) {
        return true;
    }

    @Override
    public ItemStack getResultItem(HolderLookup.Provider registries) {
        return result;
    }

    @Override
    public NonNullList<Ingredient> getIngredients() {
        NonNullList<Ingredient> list = NonNullList.create();
        for (SizedIngredient ingredient : ingredients) list.add(ingredient.ingredient());
        return list;
    }

    @Override
    public ItemStack getToastSymbol() {
        return new ItemStack(VoltaicMachines.ALLOY_SMELTER.get());
    }

    @Override
    public boolean isSpecial() {
        return true; // no recipe book
    }

    @Override
    public RecipeSerializer<?> getSerializer() {
        return ModRecipes.ALLOYING_SERIALIZER.get();
    }

    @Override
    public RecipeType<?> getType() {
        return ModRecipes.ALLOYING.get();
    }

    public static final class Serializer implements RecipeSerializer<AlloyingRecipe> {
        @Override
        public MapCodec<AlloyingRecipe> codec() {
            return CODEC;
        }

        @Override
        public StreamCodec<RegistryFriendlyByteBuf, AlloyingRecipe> streamCodec() {
            return STREAM_CODEC;
        }
    }
}
