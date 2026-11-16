package com.voltaicdynamics;

import com.voltaicdynamics.block.ModBlocks;
import com.voltaicdynamics.item.ModItems;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

public class ModCreativeModeTabs {
    public static final DeferredRegister<CreativeModeTab> CREATIVE_MODE_TABS =
            DeferredRegister.create(Registries.CREATIVE_MODE_TAB, VoltaicDynamics.MOD_ID);

    public static final DeferredHolder<CreativeModeTab, CreativeModeTab> VOLTAIC_TAB = CREATIVE_MODE_TABS.register("voltaic_tab",
            () -> CreativeModeTab.builder()
                    .title(Component.translatable("itemGroup.voltaicdynamics"))
                    .icon(() -> new ItemStack(ModBlocks.BASIC_ENERGY_CELL.get()))
                    .displayItems((parameters, output) -> {
                        // Ingots
                        output.accept(ModItems.VOLTAIC_INGOT.get());
                        output.accept(ModItems.CONDUCTIVE_ALLOY.get());

                        // Dusts
                        output.accept(ModItems.VOLTAIC_DUST.get());
                        output.accept(ModItems.CONDUCTIVE_DUST.get());

                        // Components & Storage
                        output.accept(ModItems.THERMOPILE_CORE.get());
                        output.accept(ModItems.POCKET_CAPACITOR.get());

                        // Generators
                        output.accept(ModBlocks.THERMOELECTRIC_GENERATOR.get());
                        output.accept(ModBlocks.COMBUSTION_GENERATOR.get());

                        // Machines
                        output.accept(ModBlocks.ELECTRIC_FURNACE.get());

                        // Energy Storage & Distribution
                        output.accept(ModBlocks.BASIC_ENERGY_CELL.get());
                        output.accept(ModBlocks.ADVANCED_ENERGY_CELL.get());
                        output.accept(ModBlocks.ENERGY_CABLE.get());
                    })
                    .build());
}
