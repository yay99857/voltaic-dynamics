package com.voltaicdynamics.machines;

import com.voltaicdynamics.VoltaicDynamics;
import com.voltaicdynamics.core.VoltaicCore;
import com.voltaicdynamics.machines.recipe.ModRecipes;

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

/** Machines that turn FE into processing: Electric Furnace, Alloy Smelter and the alloying recipe type. */
public final class VoltaicMachines {
    public static final DeferredRegister.Blocks BLOCKS = DeferredRegister.createBlocks(VoltaicDynamics.MOD_ID);
    public static final DeferredRegister.Items ITEMS = DeferredRegister.createItems(VoltaicDynamics.MOD_ID);
    public static final DeferredRegister<BlockEntityType<?>> BLOCK_ENTITIES =
            DeferredRegister.create(Registries.BLOCK_ENTITY_TYPE, VoltaicDynamics.MOD_ID);
    public static final DeferredRegister<MenuType<?>> MENUS = DeferredRegister.create(Registries.MENU, VoltaicDynamics.MOD_ID);

    // Blocks
    public static final DeferredBlock<ElectricFurnaceBlock> ELECTRIC_FURNACE = BLOCKS.register(
            "electric_furnace",
            () -> new ElectricFurnaceBlock(VoltaicDynamics.machineProperties(3.5F, 6.0F)
                    .lightLevel(state -> state.getValue(ElectricFurnaceBlock.LIT) ? 10 : 0)));

    public static final DeferredBlock<AlloySmelterBlock> ALLOY_SMELTER = BLOCKS.register(
            "alloy_smelter",
            () -> new AlloySmelterBlock(VoltaicDynamics.machineProperties(3.5F, 6.0F)
                    .lightLevel(state -> state.getValue(AlloySmelterBlock.LIT) ? 10 : 0)));

    // Block entities
    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<ElectricFurnaceBlockEntity>> ELECTRIC_FURNACE_BE =
            BLOCK_ENTITIES.register("electric_furnace", () ->
                    BlockEntityType.Builder.of(ElectricFurnaceBlockEntity::new, ELECTRIC_FURNACE.get()).build(null));

    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<AlloySmelterBlockEntity>> ALLOY_SMELTER_BE =
            BLOCK_ENTITIES.register("alloy_smelter", () ->
                    BlockEntityType.Builder.of(AlloySmelterBlockEntity::new, ALLOY_SMELTER.get()).build(null));

    // Menus
    public static final DeferredHolder<MenuType<?>, MenuType<ElectricFurnaceMenu>> ELECTRIC_FURNACE_MENU =
            MENUS.register("electric_furnace_menu", () -> IMenuTypeExtension.create(ElectricFurnaceMenu::new));

    public static final DeferredHolder<MenuType<?>, MenuType<AlloySmelterMenu>> ALLOY_SMELTER_MENU =
            MENUS.register("alloy_smelter_menu", () -> IMenuTypeExtension.create(AlloySmelterMenu::new));

    private VoltaicMachines() {
    }

    public static void register(IEventBus modEventBus) {
        ITEMS.registerSimpleBlockItem(ELECTRIC_FURNACE);
        ITEMS.registerSimpleBlockItem(ALLOY_SMELTER);

        BLOCKS.register(modEventBus);
        ITEMS.register(modEventBus);
        BLOCK_ENTITIES.register(modEventBus);
        MENUS.register(modEventBus);
        ModRecipes.TYPES.register(modEventBus);
        ModRecipes.SERIALIZERS.register(modEventBus);
        modEventBus.addListener(VoltaicMachines::registerCapabilities);
        modEventBus.addListener(VoltaicMachines::addToCreativeTab);
    }

    private static void registerCapabilities(RegisterCapabilitiesEvent event) {
        // Energy in, items in/out
        event.registerBlockEntity(Capabilities.EnergyStorage.BLOCK, ELECTRIC_FURNACE_BE.get(),
                (be, side) -> be.getEnergyStorage(side));
        event.registerBlockEntity(Capabilities.ItemHandler.BLOCK, ELECTRIC_FURNACE_BE.get(),
                (be, side) -> be.getItemHandler(side));
        event.registerBlockEntity(Capabilities.EnergyStorage.BLOCK, ALLOY_SMELTER_BE.get(),
                (be, side) -> be.getEnergyStorage(side));
        event.registerBlockEntity(Capabilities.ItemHandler.BLOCK, ALLOY_SMELTER_BE.get(),
                (be, side) -> be.getItemHandler(side));
    }

    private static void addToCreativeTab(BuildCreativeModeTabContentsEvent event) {
        if (VoltaicCore.VOLTAIC_TAB.is(event.getTabKey())) {
            event.accept(ELECTRIC_FURNACE.get());
            event.accept(ALLOY_SMELTER.get());
        }
    }
}
