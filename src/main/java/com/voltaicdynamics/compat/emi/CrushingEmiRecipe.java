package com.voltaicdynamics.compat.emi;

import com.voltaicdynamics.VoltaicConfig;
import com.voltaicdynamics.machines.PulverizerBlockEntity;
import com.voltaicdynamics.machines.recipe.CrushingRecipe;

import dev.emi.emi.api.recipe.BasicEmiRecipe;
import dev.emi.emi.api.stack.EmiIngredient;
import dev.emi.emi.api.stack.EmiStack;
import dev.emi.emi.api.widget.WidgetHolder;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.crafting.RecipeHolder;

/** EMI page for one crushing recipe. */
public class CrushingEmiRecipe extends BasicEmiRecipe {
    private final int ticks;
    private final float bonusChance;

    public CrushingEmiRecipe(RecipeHolder<CrushingRecipe> holder) {
        super(VoltaicEmiPlugin.CRUSHING, holder.id(), 100, 40);
        CrushingRecipe recipe = holder.value();
        inputs.add(EmiIngredient.of(recipe.ingredient().ingredient(), recipe.ingredient().count()));
        outputs.add(EmiStack.of(recipe.result()));
        this.ticks = PulverizerBlockEntity.scaledTime(recipe.time());
        this.bonusChance = recipe.bonusChance();
    }

    @Override
    public void addWidgets(WidgetHolder widgets) {
        widgets.addSlot(inputs.get(0), 0, 3);
        widgets.addFillingArrow(25, 4, ticks * 50);
        widgets.addSlot(outputs.get(0), 56, 0).large(true).recipeContext(this);
        if (bonusChance > 0) {
            widgets.addText(Component.translatable("jei.voltaicdynamics.crushing.bonus", Math.round(bonusChance * 100)),
                    84, 9, 0xFF606060, false);
        }
        int energy = ticks * VoltaicConfig.get(VoltaicConfig.PULVERIZER_ENERGY_PER_TICK);
        widgets.addText(Component.translatable("jei.voltaicdynamics.alloying.info",
                String.format("%.1f", ticks / 20.0), String.format("%,d", energy)), 0, 30, 0xFF606060, false);
    }
}
