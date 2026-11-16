package com.voltaicdynamics;

import com.voltaicdynamics.block.ModBlocks;
import com.voltaicdynamics.blockentity.ModBlockEntities;
import com.voltaicdynamics.energy.CableNetworkManager;
import com.voltaicdynamics.item.EnergyCellItem;
import com.voltaicdynamics.item.ModItems;
import com.voltaicdynamics.item.PocketCapacitorItem;
import com.voltaicdynamics.menu.ModMenus;
import net.minecraft.server.level.ServerLevel;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.config.ModConfig;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.capabilities.RegisterCapabilitiesEvent;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.energy.ComponentEnergyStorage;
import net.neoforged.neoforge.event.level.LevelEvent;
import net.neoforged.neoforge.event.server.ServerStoppedEvent;
import net.neoforged.neoforge.event.tick.LevelTickEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Mod(VoltaicDynamics.MOD_ID)
public class VoltaicDynamics {
    public static final String MOD_ID = "voltaicdynamics";
    public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

    public VoltaicDynamics(IEventBus modEventBus, ModContainer modContainer) {
        LOGGER.info("Initializing Voltaic Dynamics");

        modContainer.registerConfig(ModConfig.Type.SERVER, VoltaicConfig.SPEC);

        ModDataComponents.COMPONENTS.register(modEventBus);
        ModBlocks.BLOCKS.register(modEventBus);
        ModBlocks.registerBlockItems();
        ModItems.ITEMS.register(modEventBus);
        ModBlockEntities.BLOCK_ENTITIES.register(modEventBus);
        ModMenus.MENUS.register(modEventBus);
        ModCreativeModeTabs.CREATIVE_MODE_TABS.register(modEventBus);

        modEventBus.addListener(this::registerCapabilities);

        // Cable networks tick once per level tick and are discarded with their level.
        NeoForge.EVENT_BUS.addListener((LevelTickEvent.Post event) -> {
            if (event.getLevel() instanceof ServerLevel level) {
                CableNetworkManager.get(level).tick();
            }
        });
        NeoForge.EVENT_BUS.addListener((LevelEvent.Unload event) -> {
            if (event.getLevel() instanceof ServerLevel level) {
                CableNetworkManager.remove(level);
            }
        });
        NeoForge.EVENT_BUS.addListener((ServerStoppedEvent event) -> CableNetworkManager.clearAll());
    }

    private void registerCapabilities(RegisterCapabilitiesEvent event) {
        // Generators: energy out
        event.registerBlockEntity(Capabilities.EnergyStorage.BLOCK, ModBlockEntities.THERMOELECTRIC_GENERATOR.get(),
                (be, side) -> be.getEnergyStorage(side));
        event.registerBlockEntity(Capabilities.EnergyStorage.BLOCK, ModBlockEntities.COMBUSTION_GENERATOR.get(),
                (be, side) -> be.getEnergyStorage(side));
        event.registerBlockEntity(Capabilities.ItemHandler.BLOCK, ModBlockEntities.COMBUSTION_GENERATOR.get(),
                (be, side) -> be.getItemHandler(side));

        // Electric Furnace: energy in, items in/out
        event.registerBlockEntity(Capabilities.EnergyStorage.BLOCK, ModBlockEntities.ELECTRIC_FURNACE.get(),
                (be, side) -> be.getEnergyStorage(side));
        event.registerBlockEntity(Capabilities.ItemHandler.BLOCK, ModBlockEntities.ELECTRIC_FURNACE.get(),
                (be, side) -> be.getItemHandler(side));

        // Energy Cells
        event.registerBlockEntity(Capabilities.EnergyStorage.BLOCK, ModBlockEntities.BASIC_ENERGY_CELL.get(),
                (be, side) -> be.getEnergyStorage(side));
        event.registerBlockEntity(Capabilities.ItemHandler.BLOCK, ModBlockEntities.BASIC_ENERGY_CELL.get(),
                (be, side) -> be.getItemHandler(side));
        event.registerBlockEntity(Capabilities.EnergyStorage.BLOCK, ModBlockEntities.ADVANCED_ENERGY_CELL.get(),
                (be, side) -> be.getEnergyStorage(side));
        event.registerBlockEntity(Capabilities.ItemHandler.BLOCK, ModBlockEntities.ADVANCED_ENERGY_CELL.get(),
                (be, side) -> be.getItemHandler(side));

        // Energy Cable
        event.registerBlockEntity(Capabilities.EnergyStorage.BLOCK, ModBlockEntities.ENERGY_CABLE.get(),
                (be, side) -> be.getEnergyStorage(side));

        // Items that store FE in the energy data component
        event.registerItem(Capabilities.EnergyStorage.ITEM,
                (stack, context) -> new ComponentEnergyStorage(stack, ModDataComponents.ENERGY.get(),
                        PocketCapacitorItem.capacity(), PocketCapacitorItem.transfer()),
                ModItems.POCKET_CAPACITOR.get());
        event.registerItem(Capabilities.EnergyStorage.ITEM,
                (stack, context) -> {
                    EnergyCellItem item = (EnergyCellItem) stack.getItem();
                    return new ComponentEnergyStorage(stack, ModDataComponents.ENERGY.get(),
                            item.getCapacity(), item.getTransfer());
                },
                ModBlocks.BASIC_ENERGY_CELL.asItem(), ModBlocks.ADVANCED_ENERGY_CELL.asItem());
    }
}
