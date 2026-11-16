package com.voltaicdynamics.menu;

import com.voltaicdynamics.VoltaicDynamics;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.inventory.MenuType;
import net.neoforged.neoforge.common.extensions.IMenuTypeExtension;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

public class ModMenus {
    public static final DeferredRegister<MenuType<?>> MENUS =
            DeferredRegister.create(Registries.MENU, VoltaicDynamics.MOD_ID);

    public static final DeferredHolder<MenuType<?>, MenuType<CombustionGeneratorMenu>> COMBUSTION_GENERATOR_MENU =
            MENUS.register("combustion_generator_menu", () ->
                    IMenuTypeExtension.create(CombustionGeneratorMenu::new));

    public static final DeferredHolder<MenuType<?>, MenuType<EnergyCellMenu>> ENERGY_CELL_MENU =
            MENUS.register("energy_cell_menu", () ->
                    IMenuTypeExtension.create(EnergyCellMenu::new));

    public static final DeferredHolder<MenuType<?>, MenuType<ElectricFurnaceMenu>> ELECTRIC_FURNACE_MENU =
            MENUS.register("electric_furnace_menu", () ->
                    IMenuTypeExtension.create(ElectricFurnaceMenu::new));
}
