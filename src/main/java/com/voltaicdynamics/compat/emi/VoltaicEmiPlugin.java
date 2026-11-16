package com.voltaicdynamics.compat.emi;

import com.voltaicdynamics.VoltaicDynamics;
import com.voltaicdynamics.compat.VoltaicInfo;
import com.voltaicdynamics.core.client.MachineScreen;
import com.voltaicdynamics.generators.VoltaicGenerators;
import com.voltaicdynamics.machines.AlloySmelterMenu;
import com.voltaicdynamics.machines.ElectricFurnaceMenu;
import com.voltaicdynamics.machines.VoltaicMachines;
import com.voltaicdynamics.machines.recipe.AlloyingRecipe;
import com.voltaicdynamics.machines.recipe.ModRecipes;

import dev.emi.emi.api.EmiEntrypoint;
import dev.emi.emi.api.recipe.EmiRecipeCategory;
import dev.emi.emi.api.widget.Bounds;
import net.minecraft.client.renderer.Rect2i;
import net.minecraft.world.item.crafting.RecipeHolder;
import dev.emi.emi.api.EmiPlugin;
import dev.emi.emi.api.EmiRegistry;
import dev.emi.emi.api.recipe.EmiInfoRecipe;
import dev.emi.emi.api.recipe.EmiRecipe;
import dev.emi.emi.api.recipe.VanillaEmiRecipeCategories;
import dev.emi.emi.api.recipe.handler.StandardRecipeHandler;
import dev.emi.emi.api.stack.EmiStack;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.inventory.Slot;

import java.util.List;

/**
 * EMI: workstations, the "+" fill for the furnace and info pages. Because this native plugin exists, EMI's
 * JEI bridge skips the JEI plugin of this mod, so nothing shows up twice when both viewers are installed.
 */
@EmiEntrypoint
public class VoltaicEmiPlugin implements EmiPlugin {
    public static final EmiRecipeCategory ALLOYING = new EmiRecipeCategory(
            ResourceLocation.fromNamespaceAndPath(VoltaicDynamics.MOD_ID, "alloying"), EmiStack.of(VoltaicMachines.ALLOY_SMELTER.get()));

    @Override
    public void register(EmiRegistry registry) {
        registry.addCategory(ALLOYING);
        registry.addWorkstation(ALLOYING, EmiStack.of(VoltaicMachines.ALLOY_SMELTER.get()));
        for (RecipeHolder<AlloyingRecipe> holder : registry.getRecipeManager().getAllRecipesFor(ModRecipes.ALLOYING.get())) {
            registry.addRecipe(new AlloyingEmiRecipe(holder));
        }
        registry.addRecipeHandler(VoltaicMachines.ALLOY_SMELTER_MENU.get(), new StandardRecipeHandler<AlloySmelterMenu>() {
            @Override
            public List<Slot> getInputSources(AlloySmelterMenu menu) {
                return menu.slots.subList(4, 40);
            }

            @Override
            public List<Slot> getCraftingSlots(AlloySmelterMenu menu) {
                return menu.slots.subList(0, 3);
            }

            @Override
            public boolean supportsRecipe(EmiRecipe recipe) {
                return recipe.getCategory() == ALLOYING && recipe.supportsRecipeTree();
            }
        });
        // Keep EMI's panels clear of the side configuration tab.
        registry.addGenericExclusionArea((screen, consumer) -> {
            if (screen instanceof MachineScreen<?> machine) {
                for (Rect2i area : machine.getExtraAreas()) {
                    consumer.accept(new Bounds(area.getX(), area.getY(), area.getWidth(), area.getHeight()));
                }
            }
        });

        registry.addWorkstation(VanillaEmiRecipeCategories.SMELTING, EmiStack.of(VoltaicMachines.ELECTRIC_FURNACE.get()));
        registry.addWorkstation(VanillaEmiRecipeCategories.FUEL, EmiStack.of(VoltaicGenerators.COMBUSTION_GENERATOR.get()));

        registry.addRecipeHandler(VoltaicMachines.ELECTRIC_FURNACE_MENU.get(), new StandardRecipeHandler<ElectricFurnaceMenu>() {
            @Override
            public List<Slot> getInputSources(ElectricFurnaceMenu menu) {
                return menu.slots.subList(2, 38);
            }

            @Override
            public List<Slot> getCraftingSlots(ElectricFurnaceMenu menu) {
                return List.of(menu.slots.get(0));
            }

            @Override
            public boolean supportsRecipe(EmiRecipe recipe) {
                return recipe.getCategory() == VanillaEmiRecipeCategories.SMELTING && recipe.supportsRecipeTree();
            }
        });

        for (VoltaicInfo.Entry entry : VoltaicInfo.ENTRIES) {
            registry.addRecipe(new EmiInfoRecipe(List.of(EmiStack.of(entry.item().get())), List.of(entry.text()),
                    ResourceLocation.fromNamespaceAndPath(VoltaicDynamics.MOD_ID, "/info/" + entry.name())));
        }
    }
}
