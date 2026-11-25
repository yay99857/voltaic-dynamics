package com.voltaicdynamics.compat.jei;

import com.voltaicdynamics.VoltaicConfig;
import com.voltaicdynamics.VoltaicDynamics;
import com.voltaicdynamics.machines.PulverizerBlockEntity;
import com.voltaicdynamics.machines.VoltaicMachines;
import com.voltaicdynamics.machines.recipe.CrushingRecipe;

import mezz.jei.api.gui.builder.IRecipeLayoutBuilder;
import mezz.jei.api.gui.drawable.IDrawable;
import mezz.jei.api.gui.drawable.IDrawableAnimated;
import mezz.jei.api.gui.ingredient.IRecipeSlotsView;
import mezz.jei.api.helpers.IGuiHelper;
import mezz.jei.api.recipe.IFocusGroup;
import mezz.jei.api.recipe.types.IRecipeHolderType;
import mezz.jei.api.recipe.types.IRecipeType;
import mezz.jei.api.recipe.category.IRecipeCategory;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.crafting.RecipeHolder;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

/** JEI page for {@code voltaicdynamics:crushing}: input, animated arrow, result, bonus chance, time and energy. */
public class CrushingCategory implements IRecipeCategory<RecipeHolder<CrushingRecipe>> {
    public static final IRecipeHolderType<CrushingRecipe> TYPE = IRecipeHolderType.create(VoltaicDynamics.id("crushing"));
    private static final int WIDTH = 100;
    private static final int HEIGHT = 40;

    private final IGuiHelper guiHelper;
    private final IDrawable icon;
    private final Map<Integer, IDrawableAnimated> arrows = new HashMap<>();

    public CrushingCategory(IGuiHelper guiHelper) {
        this.guiHelper = guiHelper;
        this.icon = guiHelper.createDrawableItemLike(VoltaicMachines.PULVERIZER.get());
    }

    @Override
    public IRecipeType<RecipeHolder<CrushingRecipe>> getRecipeType() {
        return TYPE;
    }

    @Override
    public Component getTitle() {
        return Component.translatable("jei.voltaicdynamics.crushing");
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
    public void setRecipe(IRecipeLayoutBuilder builder, RecipeHolder<CrushingRecipe> holder, IFocusGroup focuses) {
        CrushingRecipe recipe = holder.value();
        builder.addInputSlot(1, 4).setStandardSlotBackground()
                .add(recipe.ingredient().ingredient());
        builder.addOutputSlot(61, 4).setOutputSlotBackground().add(recipe.result());
    }

    @Override
    public void draw(RecipeHolder<CrushingRecipe> holder, IRecipeSlotsView slots, GuiGraphicsExtractor g, double mouseX, double mouseY) {
        CrushingRecipe recipe = holder.value();
        int ticks = PulverizerBlockEntity.scaledTime(recipe.time());
        arrows.computeIfAbsent(ticks, t -> guiHelper.createAnimatedRecipeArrow(t)).draw(g, 26, 4);
        var font = Minecraft.getInstance().font;
        if (recipe.bonusChance() > 0) {
            g.text(font, Component.translatable("jei.voltaicdynamics.crushing.bonus",
                    Math.round(recipe.bonusChance() * 100)), 80, 9, 0xFF606060, false);
        }
        int energy = ticks * VoltaicConfig.get(VoltaicConfig.PULVERIZER_ENERGY_PER_TICK);
        g.text(font, Component.translatable("jei.voltaicdynamics.alloying.info",
                String.format("%.1f", ticks / 20.0), String.format("%,d", energy)), 0, HEIGHT - 10, 0xFF606060, false);
    }
}
