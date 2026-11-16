package com.voltaicdynamics.compat.emi;

import com.voltaicdynamics.VoltaicConfig;
import com.voltaicdynamics.machines.AlloySmelterBlockEntity;
import com.voltaicdynamics.machines.recipe.AlloyingRecipe;

import dev.emi.emi.api.recipe.BasicEmiRecipe;
import dev.emi.emi.api.render.EmiTexture;
import dev.emi.emi.api.stack.EmiIngredient;
import dev.emi.emi.api.stack.EmiStack;
import dev.emi.emi.api.widget.WidgetHolder;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.neoforged.neoforge.common.crafting.SizedIngredient;

/** EMI page for one alloying recipe. */
public class AlloyingEmiRecipe extends BasicEmiRecipe {
    private final int ticks;

    public AlloyingEmiRecipe(RecipeHolder<AlloyingRecipe> holder) {
        super(VoltaicEmiPlugin.ALLOYING, holder.id(), 120, 40);
        AlloyingRecipe recipe = holder.value();
        for (SizedIngredient ingredient : recipe.ingredients()) {
            inputs.add(EmiIngredient.of(ingredient.ingredient(), ingredient.count()));
        }
        outputs.add(EmiStack.of(recipe.result()));
        this.ticks = AlloySmelterBlockEntity.scaledTime(recipe.time());
    }

    @Override
    public void addWidgets(WidgetHolder widgets) {
        for (int i = 0; i < AlloyingRecipe.MAX_INGREDIENTS; i++) {
            widgets.addSlot(i < inputs.size() ? inputs.get(i) : EmiStack.EMPTY, i * 18, 3);
        }
        widgets.addTexture(EmiTexture.SHAPELESS, 100, 30);
        widgets.addFillingArrow(61, 4, ticks * 50);
        widgets.addSlot(outputs.get(0), 92, 0).large(true).recipeContext(this);
        int energy = ticks * VoltaicConfig.get(VoltaicConfig.ALLOY_ENERGY_PER_TICK);
        widgets.addText(Component.translatable("jei.voltaicdynamics.alloying.info",
                String.format("%.1f", ticks / 20.0), String.format("%,d", energy)), 0, 30, 0xFF606060, false);
    }
}
