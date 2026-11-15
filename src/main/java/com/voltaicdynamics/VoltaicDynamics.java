package com.voltaicdynamics;

import com.voltaicdynamics.block.ModBlocks;
import com.voltaicdynamics.blockentity.ModBlockEntities;
import com.voltaicdynamics.item.ModItems;
import com.voltaicdynamics.item.PocketCapacitorItem;
import com.voltaicdynamics.menu.ModMenus;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.capabilities.RegisterCapabilitiesEvent;
import net.neoforged.neoforge.energy.IEnergyStorage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Mod(VoltaicDynamics.MOD_ID)
public class VoltaicDynamics {
    public static final String MOD_ID = "voltaicdynamics";
    public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

    public VoltaicDynamics(IEventBus modEventBus) {
        LOGGER.info("Initializing Voltaic Dynamics - Powering ATM 11...");

        // Register Blocks and Items
        ModBlocks.BLOCKS.register(modEventBus);
        ModBlocks.registerBlockItems();
        ModItems.ITEMS.register(modEventBus);
        ModBlockEntities.BLOCK_ENTITIES.register(modEventBus);
        ModMenus.MENUS.register(modEventBus);
        ModCreativeModeTabs.CREATIVE_MODE_TABS.register(modEventBus);

        // Register Capabilities (FE Energy Storage and Item Handlers)
        modEventBus.addListener(this::registerCapabilities);
    }

    private void registerCapabilities(RegisterCapabilitiesEvent event) {
        // Register Energy Capability for Thermoelectric Generator
        event.registerBlockEntity(
                Capabilities.EnergyStorage.BLOCK,
                ModBlockEntities.THERMOELECTRIC_GENERATOR.get(),
                (be, side) -> be.getEnergyStorage(side)
        );

        // Register Energy Capability for Combustion Generator
        event.registerBlockEntity(
                Capabilities.EnergyStorage.BLOCK,
                ModBlockEntities.COMBUSTION_GENERATOR.get(),
                (be, side) -> be.getEnergyStorage(side)
        );

        // Register ItemHandler Capability for Combustion Generator (Fuel slot)
        event.registerBlockEntity(
                Capabilities.ItemHandler.BLOCK,
                ModBlockEntities.COMBUSTION_GENERATOR.get(),
                (be, side) -> be.getItemHandler(side)
        );

        // Register Energy Capability for Basic Energy Cell
        event.registerBlockEntity(
                Capabilities.EnergyStorage.BLOCK,
                ModBlockEntities.BASIC_ENERGY_CELL.get(),
                (be, side) -> be.getEnergyStorage(side)
        );

        // Register ItemHandler Capability for Basic Energy Cell (Charge/Discharge slots)
        event.registerBlockEntity(
                Capabilities.ItemHandler.BLOCK,
                ModBlockEntities.BASIC_ENERGY_CELL.get(),
                (be, side) -> be.getItemHandler(side)
        );

        // Register Energy Capability for Advanced Energy Cell
        event.registerBlockEntity(
                Capabilities.EnergyStorage.BLOCK,
                ModBlockEntities.ADVANCED_ENERGY_CELL.get(),
                (be, side) -> be.getEnergyStorage(side)
        );

        // Register ItemHandler Capability for Advanced Energy Cell (Charge/Discharge slots)
        event.registerBlockEntity(
                Capabilities.ItemHandler.BLOCK,
                ModBlockEntities.ADVANCED_ENERGY_CELL.get(),
                (be, side) -> be.getItemHandler(side)
        );

        // Register Energy Capability for Energy Cable
        event.registerBlockEntity(
                Capabilities.EnergyStorage.BLOCK,
                ModBlockEntities.ENERGY_CABLE.get(),
                (be, side) -> be.getEnergyStorage(side)
        );

        // Register Energy Capability for Pocket Capacitor item
        event.registerItem(
                Capabilities.EnergyStorage.ITEM,
                (stack, context) -> new IEnergyStorage() {
                    @Override
                    public int receiveEnergy(int maxReceive, boolean simulate) {
                        return PocketCapacitorItem.receiveEnergy(stack, maxReceive, simulate);
                    }

                    @Override
                    public int extractEnergy(int maxExtract, boolean simulate) {
                        return PocketCapacitorItem.extractEnergy(stack, maxExtract, simulate);
                    }

                    @Override
                    public int getEnergyStored() {
                        return PocketCapacitorItem.getEnergy(stack);
                    }

                    @Override
                    public int getMaxEnergyStored() {
                        return PocketCapacitorItem.CAPACITY;
                    }

                    @Override
                    public boolean canExtract() {
                        return true;
                    }

                    @Override
                    public boolean canReceive() {
                        return true;
                    }
                },
                ModItems.POCKET_CAPACITOR.get()
        );
    }
}
