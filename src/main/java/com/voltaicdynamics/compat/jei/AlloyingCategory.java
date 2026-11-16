package com.voltaicdynamics.compat.jei;

import com.voltaicdynamics.VoltaicConfig;
import com.voltaicdynamics.VoltaicDynamics;
import com.voltaicdynamics.machines.AlloySmelterBlockEntity;
import com.voltaicdynamics.machines.VoltaicMachines;
import com.voltaicdynamics.machines.recipe.AlloyingRecipe;

import mezz.jei.api.gui.builder.IRecipeLayoutBuilder;
import mezz.jei.api.gui.drawable.IDrawable;
import mezz.jei.api.gui.drawable.IDrawableAnimated;
import mezz.jei.api.gui.ingredient.IRecipeSlotsView;
import mezz.jei.api.helpers.IGuiHelper;
import mezz.jei.api.recipe.IFocusGroup;
import mezz.jei.api.recipe.RecipeType;
import mezz.jei.api.recipe.category.IRecipeCategory;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.neoforged.neoforge.common.crafting.SizedIngredient;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

/** JEI page for {@code voltaicdynamics:alloying}: three inputs, an animated arrow, the result, time and energy. */
public class AlloyingCategory implements IRecipeCategory<RecipeHolder<AlloyingRecipe>> {
    public static final RecipeType<RecipeHolder<AlloyingRecipe>> TYPE =
            RecipeType.createRecipeHolderType(ResourceLocation.fromNamespaceAndPath(VoltaicDynamics.MOD_ID, "alloying"));
    private static final int WIDTH = 120;
    private static final int HEIGHT = 40;

    private final IGuiHelper guiHelper;
    private final IDrawable icon;
    private final IDrawable arrow;
    private final Map<Integer, IDrawableAnimated> arrows = new HashMap<>();

    public AlloyingCategory(IGuiHelper guiHelper) {
        this.guiHelper = guiHelper;
        this.icon = guiHelper.createDrawableItemLike(VoltaicMachines.ALLOY_SMELTER.get());
        this.arrow = guiHelper.getRecipeArrow();
    }

    @Override
    public RecipeType<RecipeHolder<AlloyingRecipe>> getRecipeType() {
        return TYPE;
    }

    @Override
    public Component getTitle() {
        return Component.translatable("jei.voltaicdynamics.alloying");
    }

    @Override
    public int getWidth() {
        return WIDTH;
    }

    @Override
    public int getHeight() {
        return HEIGHT;
    }

    @Override
    public IDrawable getIcon() {
        return icon;
    }

    @Override
    public void setRecipe(IRecipeLayoutBuilder builder, RecipeHolder<AlloyingRecipe> holder, IFocusGroup focuses) {
        AlloyingRecipe recipe = holder.value();
        for (int i = 0; i < recipe.ingredients().size(); i++) {
            SizedIngredient ingredient = recipe.ingredients().get(i);
            builder.addInputSlot(1 + i * 18, 4)
                    .setStandardSlotBackground()
                    .addItemStacks(Arrays.asList(ingredient.getItems()));
        }
        for (int i = recipe.ingredients().size(); i < AlloyingRecipe.MAX_INGREDIENTS; i++) {
            builder.addInputSlot(1 + i * 18, 4).setStandardSlotBackground();
        }
        builder.addOutputSlot(97, 4).setOutputSlotBackground().addItemStack(recipe.result());
        builder.setShapeless(WIDTH - 10, 0);
    }

    @Override
    public void draw(RecipeHolder<AlloyingRecipe> holder, IRecipeSlotsView slots, GuiGraphics g, double mouseX, double mouseY) {
        int ticks = AlloySmelterBlockEntity.scaledTime(holder.value().time());
        arrows.computeIfAbsent(ticks, t -> guiHelper.createAnimatedRecipeArrow(t)).draw(g, 62, 4);
        var font = Minecraft.getInstance().font;
        int energy = ticks * VoltaicConfig.get(VoltaicConfig.ALLOY_ENERGY_PER_TICK);
        Component text = Component.translatable("jei.voltaicdynamics.alloying.info",
                String.format("%.1f", ticks / 20.0), String.format("%,d", energy));
        g.drawString(font, text, 0, HEIGHT - 10, 0xFF606060, false);
    }
}
