package com.voltaicdynamics.power;

import com.voltaicdynamics.VoltaicDynamics;
import com.voltaicdynamics.core.ModDataComponents;
import com.voltaicdynamics.core.VoltaicCore;

import net.minecraft.core.registries.Registries;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.capabilities.RegisterCapabilitiesEvent;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.common.extensions.IMenuTypeExtension;
import net.neoforged.neoforge.energy.ComponentEnergyStorage;
import net.neoforged.neoforge.event.BuildCreativeModeTabContentsEvent;
import net.neoforged.neoforge.event.level.LevelEvent;
import net.neoforged.neoforge.event.server.ServerStoppedEvent;
import net.neoforged.neoforge.event.tick.LevelTickEvent;
import net.neoforged.neoforge.registries.DeferredBlock;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

/** Storing and moving FE: Accumulators and the Voltaic Cable network. */
public final class VoltaicPower {
    public static final DeferredRegister.Blocks BLOCKS = DeferredRegister.createBlocks(VoltaicDynamics.MOD_ID);
    public static final DeferredRegister.Items ITEMS = DeferredRegister.createItems(VoltaicDynamics.MOD_ID);
    public static final DeferredRegister<BlockEntityType<?>> BLOCK_ENTITIES =
            DeferredRegister.create(Registries.BLOCK_ENTITY_TYPE, VoltaicDynamics.MOD_ID);
    public static final DeferredRegister<MenuType<?>> MENUS = DeferredRegister.create(Registries.MENU, VoltaicDynamics.MOD_ID);

    // Blocks
    public static final DeferredBlock<AccumulatorBlock> BASIC_ACCUMULATOR = BLOCKS.register(
            "basic_accumulator",
            () -> new AccumulatorBlock(VoltaicDynamics.machineProperties(4.0F, 8.0F), AccumulatorTier.BASIC));

    public static final DeferredBlock<AccumulatorBlock> ADVANCED_ACCUMULATOR = BLOCKS.register(
            "advanced_accumulator",
            () -> new AccumulatorBlock(VoltaicDynamics.machineProperties(5.0F, 10.0F), AccumulatorTier.ADVANCED));

    public static final DeferredBlock<VoltaicCableBlock> VOLTAIC_CABLE = BLOCKS.register(
            "voltaic_cable",
            () -> new VoltaicCableBlock(BlockBehaviour.Properties.of()
                    .strength(0.8F, 2.0F)
                    .sound(SoundType.METAL)
                    .noOcclusion()));

    // Block entities
    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<AccumulatorBlockEntity>> BASIC_ACCUMULATOR_BE =
            BLOCK_ENTITIES.register("basic_accumulator", () ->
                    BlockEntityType.Builder.of(AccumulatorBlockEntity::new, BASIC_ACCUMULATOR.get()).build(null));

    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<AccumulatorBlockEntity>> ADVANCED_ACCUMULATOR_BE =
            BLOCK_ENTITIES.register("advanced_accumulator", () ->
                    BlockEntityType.Builder.of(AccumulatorBlockEntity::new, ADVANCED_ACCUMULATOR.get()).build(null));

    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<VoltaicCableBlockEntity>> VOLTAIC_CABLE_BE =
            BLOCK_ENTITIES.register("voltaic_cable", () ->
                    BlockEntityType.Builder.of(VoltaicCableBlockEntity::new, VOLTAIC_CABLE.get()).build(null));

    // Menus
    public static final DeferredHolder<MenuType<?>, MenuType<AccumulatorMenu>> ACCUMULATOR_MENU =
            MENUS.register("accumulator_menu", () -> IMenuTypeExtension.create(AccumulatorMenu::new));

    private VoltaicPower() {
    }

    public static void register(IEventBus modEventBus) {
        ITEMS.registerItem("basic_accumulator", props -> new AccumulatorItem(BASIC_ACCUMULATOR.get(), props), new Item.Properties());
        ITEMS.registerItem("advanced_accumulator", props -> new AccumulatorItem(ADVANCED_ACCUMULATOR.get(), props), new Item.Properties());
        ITEMS.registerSimpleBlockItem(VOLTAIC_CABLE);

        BLOCKS.register(modEventBus);
        ITEMS.register(modEventBus);
        BLOCK_ENTITIES.register(modEventBus);
        MENUS.register(modEventBus);
        addLegacyAliases();
        modEventBus.addListener(VoltaicPower::registerCapabilities);
        modEventBus.addListener(VoltaicPower::addToCreativeTab);

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

    /**
     * 1.2.0 renamed Energy Cells and Energy Cable so they no longer share names with Powah and AE2.
     * The aliases let 1.1.0 worlds load the old ids.
     */
    private static void addLegacyAliases() {
        String[][] renamed = {{"basic_energy_cell", "basic_accumulator"}, {"advanced_energy_cell", "advanced_accumulator"},
                {"energy_cable", "voltaic_cable"}};
        for (String[] pair : renamed) {
            BLOCKS.addAlias(VoltaicDynamics.id(pair[0]), VoltaicDynamics.id(pair[1]));
            ITEMS.addAlias(VoltaicDynamics.id(pair[0]), VoltaicDynamics.id(pair[1]));
            BLOCK_ENTITIES.addAlias(VoltaicDynamics.id(pair[0]), VoltaicDynamics.id(pair[1]));
        }
        MENUS.addAlias(VoltaicDynamics.id("energy_cell_menu"), VoltaicDynamics.id("accumulator_menu"));
    }

    private static void registerCapabilities(RegisterCapabilitiesEvent event) {
        event.registerBlockEntity(Capabilities.EnergyStorage.BLOCK, BASIC_ACCUMULATOR_BE.get(),
                (be, side) -> be.getEnergyStorage(side));
        event.registerBlockEntity(Capabilities.ItemHandler.BLOCK, BASIC_ACCUMULATOR_BE.get(),
                (be, side) -> be.getItemHandler(side));
        event.registerBlockEntity(Capabilities.EnergyStorage.BLOCK, ADVANCED_ACCUMULATOR_BE.get(),
                (be, side) -> be.getEnergyStorage(side));
        event.registerBlockEntity(Capabilities.ItemHandler.BLOCK, ADVANCED_ACCUMULATOR_BE.get(),
                (be, side) -> be.getItemHandler(side));
        event.registerBlockEntity(Capabilities.EnergyStorage.BLOCK, VOLTAIC_CABLE_BE.get(),
                (be, side) -> be.getEnergyStorage(side));

        // An Accumulator item keeps its FE in the energy data component.
        event.registerItem(Capabilities.EnergyStorage.ITEM,
                (stack, context) -> {
                    AccumulatorItem item = (AccumulatorItem) stack.getItem();
                    return new ComponentEnergyStorage(stack, ModDataComponents.ENERGY.get(),
                            item.getCapacity(), item.getTransfer());
                },
                BASIC_ACCUMULATOR.asItem(), ADVANCED_ACCUMULATOR.asItem());
    }

    private static void addToCreativeTab(BuildCreativeModeTabContentsEvent event) {
        if (VoltaicCore.VOLTAIC_TAB.is(event.getTabKey())) {
            event.accept(BASIC_ACCUMULATOR.get());
            event.accept(ADVANCED_ACCUMULATOR.get());
            event.accept(VOLTAIC_CABLE.get());
        }
    }
}
