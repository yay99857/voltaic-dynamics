package com.voltaicdynamics.machines.recipe;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ItemStackTemplate;
import net.minecraft.world.item.crafting.PlacementInfo;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.item.crafting.RecipeBookCategories;
import net.minecraft.world.item.crafting.RecipeBookCategory;
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
 *  "ingredients": [{"ingredient": "#c:ingots/iron", "count": 1}, {"ingredient": "minecraft:redstone", "count": 1}],
 *  "result": {"id": "voltaicdynamics:voltaic_ingot", "count": 2}, "time": 100, "experience": 0.5}</pre>
 */
public record AlloyingRecipe(List<SizedIngredient> ingredients, ItemStackTemplate result, int time, float experience)
        implements Recipe<AlloyingInput> {
    public static final int MAX_INGREDIENTS = 3;
    public static final int DEFAULT_TIME = 100;

    public static final MapCodec<AlloyingRecipe> CODEC = RecordCodecBuilder.mapCodec(instance -> instance.group(
            SizedIngredient.NESTED_CODEC.listOf(1, MAX_INGREDIENTS).fieldOf("ingredients").forGetter(AlloyingRecipe::ingredients),
            ItemStackTemplate.CODEC.fieldOf("result").forGetter(AlloyingRecipe::result),
            Codec.intRange(1, 72_000).optionalFieldOf("time", DEFAULT_TIME).forGetter(AlloyingRecipe::time),
            Codec.floatRange(0, 100).optionalFieldOf("experience", 0.0F).forGetter(AlloyingRecipe::experience)
    ).apply(instance, AlloyingRecipe::new));

    public static final StreamCodec<RegistryFriendlyByteBuf, AlloyingRecipe> STREAM_CODEC = StreamCodec.composite(
            SizedIngredient.STREAM_CODEC.apply(ByteBufCodecs.list(MAX_INGREDIENTS)), AlloyingRecipe::ingredients,
            ItemStackTemplate.STREAM_CODEC, AlloyingRecipe::result,
            ByteBufCodecs.VAR_INT, AlloyingRecipe::time,
            ByteBufCodecs.FLOAT, AlloyingRecipe::experience,
            AlloyingRecipe::new);

    public static final RecipeSerializer<AlloyingRecipe> SERIALIZER = new RecipeSerializer<>(CODEC, STREAM_CODEC);

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

    /** The result as a new stack. */
    public ItemStack resultStack() {
        return result.create();
    }

    @Override
    public ItemStack assemble(AlloyingInput input) {
        return result.create();
    }

    @Override
    public boolean isSpecial() {
        return true; // no recipe book
    }

    @Override
    public boolean showNotification() {
        return false;
    }

    @Override
    public String group() {
        return "";
    }

    @Override
    public PlacementInfo placementInfo() {
        return PlacementInfo.NOT_PLACEABLE;
    }

    @Override
    public RecipeBookCategory recipeBookCategory() {
        return RecipeBookCategories.CRAFTING_MISC;
    }

    @Override
    public RecipeSerializer<AlloyingRecipe> getSerializer() {
        return ModRecipes.ALLOYING_SERIALIZER.get();
    }

    @Override
    public RecipeType<AlloyingRecipe> getType() {
        return ModRecipes.ALLOYING.get();
    }
}
