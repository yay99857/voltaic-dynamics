package com.voltaicdynamics.generators;

import com.voltaicdynamics.VoltaicDynamics;
import com.voltaicdynamics.core.VoltaicCore;

import net.minecraft.core.registries.Registries;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.capabilities.RegisterCapabilitiesEvent;
import net.neoforged.neoforge.common.extensions.IMenuTypeExtension;
import net.neoforged.neoforge.event.BuildCreativeModeTabContentsEvent;
import net.neoforged.neoforge.registries.DeferredBlock;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.neoforged.neoforge.registries.datamaps.RegisterDataMapTypesEvent;

/** Voltaic Generators: the blocks that make FE. Depends only on Voltaic Core. */
public final class VoltaicGenerators {
    public static final DeferredRegister.Blocks BLOCKS = DeferredRegister.createBlocks(VoltaicDynamics.MOD_ID);
    public static final DeferredRegister.Items ITEMS = DeferredRegister.createItems(VoltaicDynamics.MOD_ID);
    public static final DeferredRegister<BlockEntityType<?>> BLOCK_ENTITIES =
            DeferredRegister.create(Registries.BLOCK_ENTITY_TYPE, VoltaicDynamics.MOD_ID);
    public static final DeferredRegister<MenuType<?>> MENUS = DeferredRegister.create(Registries.MENU, VoltaicDynamics.MOD_ID);

    // Blocks
    public static final DeferredBlock<ThermoelectricGeneratorBlock> THERMOELECTRIC_GENERATOR = BLOCKS.register(
            "thermoelectric_generator",
            () -> new ThermoelectricGeneratorBlock(VoltaicDynamics.machineProperties(3.5F, 6.0F)));

    public static final DeferredBlock<CombustionGeneratorBlock> COMBUSTION_GENERATOR = BLOCKS.register(
            "combustion_generator",
            () -> new CombustionGeneratorBlock(VoltaicDynamics.machineProperties(3.5F, 6.0F)
                    .lightLevel(state -> state.getValue(CombustionGeneratorBlock.LIT) ? 13 : 0)));

    public static final DeferredBlock<FluidGeneratorBlock> FLUID_GENERATOR = BLOCKS.register(
            "fluid_generator",
            () -> new FluidGeneratorBlock(VoltaicDynamics.machineProperties(3.5F, 6.0F)
                    .lightLevel(state -> state.getValue(FluidGeneratorBlock.LIT) ? 10 : 0)));

    // Block entities
    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<ThermoelectricGeneratorBlockEntity>> THERMOELECTRIC_GENERATOR_BE =
            BLOCK_ENTITIES.register("thermoelectric_generator", () ->
                    BlockEntityType.Builder.of(ThermoelectricGeneratorBlockEntity::new, THERMOELECTRIC_GENERATOR.get()).build(null));

    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<CombustionGeneratorBlockEntity>> COMBUSTION_GENERATOR_BE =
            BLOCK_ENTITIES.register("combustion_generator", () ->
                    BlockEntityType.Builder.of(CombustionGeneratorBlockEntity::new, COMBUSTION_GENERATOR.get()).build(null));

    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<FluidGeneratorBlockEntity>> FLUID_GENERATOR_BE =
            BLOCK_ENTITIES.register("fluid_generator", () ->
                    BlockEntityType.Builder.of(FluidGeneratorBlockEntity::new, FLUID_GENERATOR.get()).build(null));

    // Menus
    public static final DeferredHolder<MenuType<?>, MenuType<CombustionGeneratorMenu>> COMBUSTION_GENERATOR_MENU =
            MENUS.register("combustion_generator_menu", () -> IMenuTypeExtension.create(CombustionGeneratorMenu::new));

    public static final DeferredHolder<MenuType<?>, MenuType<FluidGeneratorMenu>> FLUID_GENERATOR_MENU =
            MENUS.register("fluid_generator_menu", () -> IMenuTypeExtension.create(FluidGeneratorMenu::new));

    private VoltaicGenerators() {
    }

    public static void register(IEventBus modEventBus) {
        ITEMS.registerSimpleBlockItem(THERMOELECTRIC_GENERATOR);
        ITEMS.registerSimpleBlockItem(COMBUSTION_GENERATOR);
        ITEMS.registerSimpleBlockItem(FLUID_GENERATOR);

        BLOCKS.register(modEventBus);
        ITEMS.register(modEventBus);
        BLOCK_ENTITIES.register(modEventBus);
        MENUS.register(modEventBus);
        modEventBus.addListener(VoltaicGenerators::registerCapabilities);
        modEventBus.addListener(VoltaicGenerators::addToCreativeTab);
        modEventBus.addListener((RegisterDataMapTypesEvent event) -> event.register(FluidFuel.DATA_MAP));
    }

    private static void registerCapabilities(RegisterCapabilitiesEvent event) {
        event.registerBlockEntity(Capabilities.EnergyStorage.BLOCK, THERMOELECTRIC_GENERATOR_BE.get(),
                (be, side) -> be.getEnergyStorage(side));
        event.registerBlockEntity(Capabilities.EnergyStorage.BLOCK, COMBUSTION_GENERATOR_BE.get(),
                (be, side) -> be.getEnergyStorage(side));
        event.registerBlockEntity(Capabilities.ItemHandler.BLOCK, COMBUSTION_GENERATOR_BE.get(),
                (be, side) -> be.getItemHandler(side));
        event.registerBlockEntity(Capabilities.EnergyStorage.BLOCK, FLUID_GENERATOR_BE.get(),
                (be, side) -> be.getEnergyStorage(side));
        event.registerBlockEntity(Capabilities.FluidHandler.BLOCK, FLUID_GENERATOR_BE.get(),
                (be, side) -> be.getFluidHandler(side));
        event.registerBlockEntity(Capabilities.ItemHandler.BLOCK, FLUID_GENERATOR_BE.get(),
                (be, side) -> be.getItemHandler(side));
    }

    private static void addToCreativeTab(BuildCreativeModeTabContentsEvent event) {
        if (VoltaicCore.VOLTAIC_TAB.is(event.getTabKey())) {
            event.accept(THERMOELECTRIC_GENERATOR.get());
            event.accept(COMBUSTION_GENERATOR.get());
            event.accept(FLUID_GENERATOR.get());
        }
    }
}
