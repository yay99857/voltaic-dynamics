package com.voltaicdynamics.compat.jei;

import com.voltaicdynamics.VoltaicDynamics;
import com.voltaicdynamics.compat.VoltaicInfo;
import com.voltaicdynamics.core.client.MachineScreen;
import com.voltaicdynamics.generators.VoltaicGenerators;
import com.voltaicdynamics.generators.client.CombustionGeneratorScreen;
import com.voltaicdynamics.machines.AlloySmelterMenu;
import com.voltaicdynamics.machines.ElectricFurnaceMenu;
import com.voltaicdynamics.machines.VoltaicMachines;
import com.voltaicdynamics.machines.client.AlloySmelterScreen;
import com.voltaicdynamics.machines.client.ElectricFurnaceScreen;
import com.voltaicdynamics.machines.recipe.ModRecipes;

import mezz.jei.api.IModPlugin;
import mezz.jei.api.gui.handlers.IGuiContainerHandler;
import mezz.jei.api.registration.IRecipeCategoryRegistration;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.Rect2i;
import java.util.List;
import mezz.jei.api.JeiPlugin;
import mezz.jei.api.constants.RecipeTypes;
import mezz.jei.api.registration.IGuiHandlerRegistration;
import mezz.jei.api.registration.IRecipeCatalystRegistration;
import mezz.jei.api.registration.IRecipeRegistration;
import mezz.jei.api.registration.IRecipeTransferRegistration;
import net.minecraft.resources.ResourceLocation;

/** JEI: machines as catalysts, clickable progress areas, the "+" transfer for the furnace and info pages. */
@JeiPlugin
public class VoltaicJeiPlugin implements IModPlugin {
    @Override
    public ResourceLocation getPluginUid() {
        return ResourceLocation.fromNamespaceAndPath(VoltaicDynamics.MOD_ID, "jei_plugin");
    }

    @Override
    public void registerCategories(IRecipeCategoryRegistration registration) {
        registration.addRecipeCategories(new AlloyingCategory(registration.getJeiHelpers().getGuiHelper()));
    }

    @Override
    public void registerRecipeCatalysts(IRecipeCatalystRegistration registration) {
        registration.addRecipeCatalyst(VoltaicMachines.ALLOY_SMELTER.get(), AlloyingCategory.TYPE);
        registration.addRecipeCatalyst(VoltaicMachines.ELECTRIC_FURNACE.get(), RecipeTypes.SMELTING);
        registration.addRecipeCatalyst(VoltaicGenerators.COMBUSTION_GENERATOR.get(), RecipeTypes.FUELING);
    }

    @Override
    public void registerGuiHandlers(IGuiHandlerRegistration registration) {
        // Same rectangles the screens draw: furnace arrow and combustion flame.
        registration.addRecipeClickArea(ElectricFurnaceScreen.class, 79, 34, 24, 17, RecipeTypes.SMELTING);
        registration.addRecipeClickArea(CombustionGeneratorScreen.class, 80, 54, 14, 14, RecipeTypes.FUELING);
        registration.addRecipeClickArea(AlloySmelterScreen.class, AlloySmelterScreen.ARROW_X, AlloySmelterScreen.ARROW_Y, 24, 17,
                AlloyingCategory.TYPE);
        // Keep JEI's ingredient list clear of the side configuration tab.
        registration.addGenericGuiContainerHandler(MachineScreen.class, new IGuiContainerHandler<MachineScreen<?>>() {
            @Override
            public List<Rect2i> getGuiExtraAreas(MachineScreen<?> screen) {
                return screen.getExtraAreas();
            }
        });
    }

    @Override
    public void registerRecipeTransferHandlers(IRecipeTransferRegistration registration) {
        // Slot 0 = input, 1 = output, 2..37 = player inventory.
        registration.addRecipeTransferHandler(ElectricFurnaceMenu.class, VoltaicMachines.ELECTRIC_FURNACE_MENU.get(),
                RecipeTypes.SMELTING, 0, 1, 2, 36);
        // Slots 0..2 = inputs, 3 = output, 4..39 = player inventory.
        registration.addRecipeTransferHandler(AlloySmelterMenu.class, VoltaicMachines.ALLOY_SMELTER_MENU.get(),
                AlloyingCategory.TYPE, 0, 3, 4, 36);
    }

    @Override
    public void registerRecipes(IRecipeRegistration registration) {
        ClientLevel level = Minecraft.getInstance().level;
        if (level != null) {
            registration.addRecipes(AlloyingCategory.TYPE,
                    List.copyOf(level.getRecipeManager().getAllRecipesFor(ModRecipes.ALLOYING.get())));
        }
        for (VoltaicInfo.Entry entry : VoltaicInfo.ENTRIES) {
            registration.addIngredientInfo(entry.item().get(), entry.text());
        }
    }
}
