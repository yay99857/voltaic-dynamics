package com.voltaicdynamics.machines.recipe;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.util.ExtraCodecs;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ItemStackTemplate;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.PlacementInfo;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.item.crafting.RecipeBookCategories;
import net.minecraft.world.item.crafting.RecipeBookCategory;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.item.crafting.SingleRecipeInput;
import net.minecraft.world.level.Level;
import net.neoforged.neoforge.common.crafting.SizedIngredient;

/**
 * {@code voltaicdynamics:crushing}, run by the Pulverizer: one sized ingredient makes one result, plus one more
 * with {@code bonus_chance}. Packs and other mods can add their own through datapacks:
 * <pre>{"type": "voltaicdynamics:crushing", "ingredient": "#c:raw_materials/iron", "count": 1,
 *  "result": {"id": "voltaicdynamics:iron_dust", "count": 1}, "bonus_chance": 0.5, "time": 100, "experience": 0.1}</pre>
 */
public record CrushingRecipe(SizedIngredient ingredient, ItemStackTemplate result, float bonusChance, int time, float experience)
        implements Recipe<SingleRecipeInput> {
    public static final int DEFAULT_TIME = 100;

    public static final MapCodec<CrushingRecipe> CODEC = RecordCodecBuilder.mapCodec(instance -> instance.group(
            Ingredient.CODEC.fieldOf("ingredient").forGetter(r -> r.ingredient().ingredient()),
            ExtraCodecs.POSITIVE_INT.optionalFieldOf("count", 1).forGetter(r -> r.ingredient().count()),
            ItemStackTemplate.CODEC.fieldOf("result").forGetter(CrushingRecipe::result),
            Codec.floatRange(0, 1).optionalFieldOf("bonus_chance", 0.0F).forGetter(CrushingRecipe::bonusChance),
            Codec.intRange(1, 72_000).optionalFieldOf("time", DEFAULT_TIME).forGetter(CrushingRecipe::time),
            Codec.floatRange(0, 100).optionalFieldOf("experience", 0.0F).forGetter(CrushingRecipe::experience)
    ).apply(instance, (ingredient, count, result, bonus, time, xp) ->
            new CrushingRecipe(new SizedIngredient(ingredient, count), result, bonus, time, xp)));

    public static final StreamCodec<RegistryFriendlyByteBuf, CrushingRecipe> STREAM_CODEC = StreamCodec.composite(
            SizedIngredient.STREAM_CODEC, CrushingRecipe::ingredient,
            ItemStackTemplate.STREAM_CODEC, CrushingRecipe::result,
            ByteBufCodecs.FLOAT, CrushingRecipe::bonusChance,
            ByteBufCodecs.VAR_INT, CrushingRecipe::time,
            ByteBufCodecs.FLOAT, CrushingRecipe::experience,
            CrushingRecipe::new);

    public static final RecipeSerializer<CrushingRecipe> SERIALIZER = new RecipeSerializer<>(CODEC, STREAM_CODEC);

    @Override
    public boolean matches(SingleRecipeInput input, Level level) {
        ItemStack stack = input.item();
        return ingredient.ingredient().test(stack) && stack.getCount() >= ingredient.count();
    }

    /** True when the stack could go into a Pulverizer for this recipe (count aside). */
    public boolean uses(ItemStack stack) {
        return ingredient.ingredient().test(stack);
    }

    /** The result as a new stack. */
    public ItemStack resultStack() {
        return result.create();
    }

    @Override
    public ItemStack assemble(SingleRecipeInput input) {
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
    public RecipeSerializer<CrushingRecipe> getSerializer() {
        return ModRecipes.CRUSHING_SERIALIZER.get();
    }

    @Override
    public RecipeType<CrushingRecipe> getType() {
        return ModRecipes.CRUSHING.get();
    }
}
