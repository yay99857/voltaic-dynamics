package com.voltaicdynamics.machines.recipe;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import com.voltaicdynamics.machines.VoltaicMachines;
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
import net.minecraft.world.item.crafting.SingleRecipeInput;
import net.minecraft.world.level.Level;
import net.neoforged.neoforge.common.crafting.SizedIngredient;

/**
 * {@code voltaicdynamics:crushing}, run by the Pulverizer: one sized ingredient makes one result, plus one more
 * with {@code bonus_chance}. Packs and other mods can add their own through datapacks:
 * <pre>{"type": "voltaicdynamics:crushing", "ingredient": {"tag": "c:raw_materials/iron", "count": 1},
 *  "result": {"id": "voltaicdynamics:iron_dust", "count": 1}, "bonus_chance": 0.5, "time": 100, "experience": 0.1}</pre>
 */
public record CrushingRecipe(SizedIngredient ingredient, ItemStack result, float bonusChance, int time, float experience)
        implements Recipe<SingleRecipeInput> {
    public static final int DEFAULT_TIME = 100;

    public static final MapCodec<CrushingRecipe> CODEC = RecordCodecBuilder.mapCodec(instance -> instance.group(
            SizedIngredient.FLAT_CODEC.fieldOf("ingredient").forGetter(CrushingRecipe::ingredient),
            ItemStack.STRICT_CODEC.fieldOf("result").forGetter(CrushingRecipe::result),
            Codec.floatRange(0, 1).optionalFieldOf("bonus_chance", 0.0F).forGetter(CrushingRecipe::bonusChance),
            Codec.intRange(1, 72_000).optionalFieldOf("time", DEFAULT_TIME).forGetter(CrushingRecipe::time),
            Codec.floatRange(0, 100).optionalFieldOf("experience", 0.0F).forGetter(CrushingRecipe::experience)
    ).apply(instance, CrushingRecipe::new));

    public static final StreamCodec<RegistryFriendlyByteBuf, CrushingRecipe> STREAM_CODEC = StreamCodec.composite(
            SizedIngredient.STREAM_CODEC, CrushingRecipe::ingredient,
            ItemStack.STREAM_CODEC, CrushingRecipe::result,
            ByteBufCodecs.FLOAT, CrushingRecipe::bonusChance,
            ByteBufCodecs.VAR_INT, CrushingRecipe::time,
            ByteBufCodecs.FLOAT, CrushingRecipe::experience,
            CrushingRecipe::new);

    @Override
    public boolean matches(SingleRecipeInput input, Level level) {
        ItemStack stack = input.item();
        return ingredient.ingredient().test(stack) && stack.getCount() >= ingredient.count();
    }

    /** True when the stack could go into a Pulverizer for this recipe (count aside). */
    public boolean uses(ItemStack stack) {
        return ingredient.ingredient().test(stack);
    }

    @Override
    public ItemStack assemble(SingleRecipeInput input, HolderLookup.Provider registries) {
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
        return NonNullList.of(Ingredient.EMPTY, ingredient.ingredient());
    }

    @Override
    public ItemStack getToastSymbol() {
        return new ItemStack(VoltaicMachines.PULVERIZER.get());
    }

    @Override
    public boolean isSpecial() {
        return true; // no recipe book
    }

    @Override
    public RecipeSerializer<?> getSerializer() {
        return ModRecipes.CRUSHING_SERIALIZER.get();
    }

    @Override
    public RecipeType<?> getType() {
        return ModRecipes.CRUSHING.get();
    }

    public static final class Serializer implements RecipeSerializer<CrushingRecipe> {
        @Override
        public MapCodec<CrushingRecipe> codec() {
            return CODEC;
        }

        @Override
        public StreamCodec<RegistryFriendlyByteBuf, CrushingRecipe> streamCodec() {
            return STREAM_CODEC;
        }
    }
}
