package com.voltaicdynamics.machines.recipe;

import com.voltaicdynamics.VoltaicDynamics;

import net.minecraft.core.registries.Registries;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.item.crafting.RecipeType;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

public final class ModRecipes {
    public static final DeferredRegister<RecipeType<?>> TYPES =
            DeferredRegister.create(Registries.RECIPE_TYPE, VoltaicDynamics.MOD_ID);
    public static final DeferredRegister<RecipeSerializer<?>> SERIALIZERS =
            DeferredRegister.create(Registries.RECIPE_SERIALIZER, VoltaicDynamics.MOD_ID);

    public static final DeferredHolder<RecipeType<?>, RecipeType<AlloyingRecipe>> ALLOYING =
            TYPES.register("alloying", () -> RecipeType.simple(
                    net.minecraft.resources.ResourceLocation.fromNamespaceAndPath(VoltaicDynamics.MOD_ID, "alloying")));
    public static final DeferredHolder<RecipeSerializer<?>, RecipeSerializer<AlloyingRecipe>> ALLOYING_SERIALIZER =
            SERIALIZERS.register("alloying", AlloyingRecipe.Serializer::new);

    private ModRecipes() {
    }
}
