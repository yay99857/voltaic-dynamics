package com.voltaicdynamics.machines.recipe;

import com.voltaicdynamics.VoltaicDynamics;

import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.RecipePropertySet;
import net.neoforged.neoforge.common.crafting.CompoundIngredient;
import net.neoforged.neoforge.common.crafting.SizedIngredient;
import net.neoforged.neoforge.event.OnDatapackSyncEvent;
import net.neoforged.neoforge.event.RegisterRecipePropertiesEvent;
import net.minecraft.world.item.crafting.RecipeMap;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.item.crafting.RecipeType;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

import java.util.Optional;

public final class ModRecipes {
    public static final DeferredRegister<RecipeType<?>> TYPES =
            DeferredRegister.create(Registries.RECIPE_TYPE, VoltaicDynamics.MOD_ID);
    public static final DeferredRegister<RecipeSerializer<?>> SERIALIZERS =
            DeferredRegister.create(Registries.RECIPE_SERIALIZER, VoltaicDynamics.MOD_ID);

    public static final DeferredHolder<RecipeType<?>, RecipeType<AlloyingRecipe>> ALLOYING =
            TYPES.register("alloying", () -> RecipeType.simple(
                    VoltaicDynamics.id("alloying")));
    public static final DeferredHolder<RecipeSerializer<?>, RecipeSerializer<AlloyingRecipe>> ALLOYING_SERIALIZER =
            SERIALIZERS.register("alloying", () -> AlloyingRecipe.SERIALIZER);

    public static final DeferredHolder<RecipeType<?>, RecipeType<CrushingRecipe>> CRUSHING =
            TYPES.register("crushing", () -> RecipeType.simple(
                    VoltaicDynamics.id("crushing")));
    public static final DeferredHolder<RecipeSerializer<?>, RecipeSerializer<CrushingRecipe>> CRUSHING_SERIALIZER =
            SERIALIZERS.register("crushing", () -> CrushingRecipe.SERIALIZER);

    /**
     * Every item some recipe takes, per machine. Recipes only live on the server; these sets reach the client too,
     * so input slots can tell what fits on both sides.
     */
    public static final ResourceKey<RecipePropertySet> ALLOYING_INPUT =
            ResourceKey.create(RecipePropertySet.TYPE_KEY, VoltaicDynamics.id("alloying_input"));
    public static final ResourceKey<RecipePropertySet> CRUSHING_INPUT =
            ResourceKey.create(RecipePropertySet.TYPE_KEY, VoltaicDynamics.id("crushing_input"));

    private ModRecipes() {
    }

    /** Our recipes as last received from the server, for recipe viewers on the client. */
    private static volatile RecipeMap clientRecipes = RecipeMap.EMPTY;

    public static RecipeMap clientRecipes() {
        return clientRecipes;
    }

    public static void setClientRecipes(RecipeMap recipes) {
        clientRecipes = recipes;
    }

    /** Recipes stay on the server unless sent: recipe viewers (JEI) need ours on the client. */
    public static void syncToClients(OnDatapackSyncEvent event) {
        event.sendRecipes(ALLOYING.get(), CRUSHING.get());
    }

    public static void registerPropertySets(RegisterRecipePropertiesEvent event) {
        event.register(ALLOYING_INPUT, recipe -> recipe instanceof AlloyingRecipe alloying
                ? Optional.of(CompoundIngredient.of(alloying.ingredients().stream()
                        .map(SizedIngredient::ingredient).toArray(Ingredient[]::new)))
                : Optional.empty());
        event.register(CRUSHING_INPUT, recipe -> recipe instanceof CrushingRecipe crushing
                ? Optional.of(crushing.ingredient().ingredient())
                : Optional.empty());
    }
}
