package com.voltaicdynamics.power;

import net.neoforged.neoforge.transfer.energy.ItemAccessEnergyHandler;
import com.voltaicdynamics.core.item.VoltaicBlockItem;
import com.voltaicdynamics.VoltaicDynamics;
import com.voltaicdynamics.core.ModDataComponents;
import com.voltaicdynamics.core.VoltaicCore;
import com.voltaicdynamics.core.energy.CreativeEnergyStorage;

import net.minecraft.core.registries.Registries;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Rarity;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.capabilities.RegisterCapabilitiesEvent;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.common.extensions.IMenuTypeExtension;
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
    public static final DeferredBlock<AccumulatorBlock> BASIC_ACCUMULATOR = BLOCKS.registerBlock("basic_accumulator",
            p -> new AccumulatorBlock(p, AccumulatorTier.BASIC),
            () -> VoltaicDynamics.machineProperties(4.0F, 8.0F));

    public static final DeferredBlock<AccumulatorBlock> ADVANCED_ACCUMULATOR = BLOCKS.registerBlock("advanced_accumulator",
            p -> new AccumulatorBlock(p, AccumulatorTier.ADVANCED),
            () -> VoltaicDynamics.machineProperties(5.0F, 10.0F));

    public static final DeferredBlock<AccumulatorBlock> ELITE_ACCUMULATOR = BLOCKS.registerBlock("elite_accumulator",
            p -> new AccumulatorBlock(p, AccumulatorTier.ELITE),
            () -> VoltaicDynamics.machineProperties(5.0F, 12.0F));

    public static final DeferredBlock<AccumulatorBlock> ULTIMATE_ACCUMULATOR = BLOCKS.registerBlock("ultimate_accumulator",
            p -> new AccumulatorBlock(p, AccumulatorTier.ULTIMATE),
            () -> VoltaicDynamics.machineProperties(5.0F, 15.0F));

    public static final DeferredBlock<AccumulatorBlock> CREATIVE_ACCUMULATOR = BLOCKS.registerBlock("creative_accumulator",
            p -> new AccumulatorBlock(p, AccumulatorTier.CREATIVE),
            () -> VoltaicDynamics.machineProperties(5.0F, 3_600_000.0F));

    public static final DeferredBlock<VoltaicCableBlock> VOLTAIC_CABLE = BLOCKS.registerBlock("voltaic_cable",
            p -> new VoltaicCableBlock(p),
            () -> BlockBehaviour.Properties.of()
                    .strength(0.8F, 2.0F)
                    .sound(SoundType.METAL)
                    .noOcclusion());

    // Block entities
    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<AccumulatorBlockEntity>> BASIC_ACCUMULATOR_BE =
            BLOCK_ENTITIES.register("basic_accumulator", () ->
                    new BlockEntityType<>(AccumulatorBlockEntity::new, BASIC_ACCUMULATOR.get()));

    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<AccumulatorBlockEntity>> ADVANCED_ACCUMULATOR_BE =
            BLOCK_ENTITIES.register("advanced_accumulator", () ->
                    new BlockEntityType<>(AccumulatorBlockEntity::new, ADVANCED_ACCUMULATOR.get()));

    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<AccumulatorBlockEntity>> ELITE_ACCUMULATOR_BE =
            BLOCK_ENTITIES.register("elite_accumulator", () ->
                    new BlockEntityType<>(AccumulatorBlockEntity::new, ELITE_ACCUMULATOR.get()));

    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<AccumulatorBlockEntity>> ULTIMATE_ACCUMULATOR_BE =
            BLOCK_ENTITIES.register("ultimate_accumulator", () ->
                    new BlockEntityType<>(AccumulatorBlockEntity::new, ULTIMATE_ACCUMULATOR.get()));

    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<AccumulatorBlockEntity>> CREATIVE_ACCUMULATOR_BE =
            BLOCK_ENTITIES.register("creative_accumulator", () ->
                    new BlockEntityType<>(AccumulatorBlockEntity::new, CREATIVE_ACCUMULATOR.get()));

    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<VoltaicCableBlockEntity>> VOLTAIC_CABLE_BE =
            BLOCK_ENTITIES.register("voltaic_cable", () ->
                    new BlockEntityType<>(VoltaicCableBlockEntity::new, VOLTAIC_CABLE.get()));

    // Menus
    public static final DeferredHolder<MenuType<?>, MenuType<AccumulatorMenu>> ACCUMULATOR_MENU =
            MENUS.register("accumulator_menu", () -> IMenuTypeExtension.create(AccumulatorMenu::new));

    private VoltaicPower() {
    }

    public static void register(IEventBus modEventBus) {
        ITEMS.registerItem("basic_accumulator", props -> new AccumulatorItem(BASIC_ACCUMULATOR.get(), props), () -> new Item.Properties().useBlockDescriptionPrefix());
        ITEMS.registerItem("advanced_accumulator", props -> new AccumulatorItem(ADVANCED_ACCUMULATOR.get(), props), () -> new Item.Properties().useBlockDescriptionPrefix());
        ITEMS.registerItem("elite_accumulator", props -> new AccumulatorItem(ELITE_ACCUMULATOR.get(), props), () -> new Item.Properties().useBlockDescriptionPrefix());
        ITEMS.registerItem("ultimate_accumulator", props -> new AccumulatorItem(ULTIMATE_ACCUMULATOR.get(), props),
                () -> new Item.Properties().useBlockDescriptionPrefix().fireResistant());
        ITEMS.registerItem("creative_accumulator", props -> new AccumulatorItem(CREATIVE_ACCUMULATOR.get(), props),
                () -> new Item.Properties().useBlockDescriptionPrefix().rarity(Rarity.EPIC));
        ITEMS.registerItem(VOLTAIC_CABLE.getId().getPath(), p -> new VoltaicBlockItem(VOLTAIC_CABLE.get(), p), Item.Properties::useBlockDescriptionPrefix);

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
        for (AccumulatorTier tier : AccumulatorTier.values()) {
            event.registerBlockEntity(Capabilities.Energy.BLOCK, AccumulatorBlockEntity.typeFor(tier),
                    (be, side) -> be.getEnergyStorage(side));
            event.registerBlockEntity(Capabilities.Item.BLOCK, AccumulatorBlockEntity.typeFor(tier),
                    (be, side) -> be.getItemHandler(side));
        }
        event.registerBlockEntity(Capabilities.Energy.BLOCK, VOLTAIC_CABLE_BE.get(),
                (be, side) -> be.getEnergyStorage(side));

        // An Accumulator item keeps its FE in the energy data component.
        event.registerItem(Capabilities.Energy.ITEM,
                (stack, access) -> {
                    AccumulatorItem item = (AccumulatorItem) stack.getItem();
                    if (item.getTier().isCreative()) return new CreativeEnergyStorage(item.getTransfer());
                    return new ItemAccessEnergyHandler(access, ModDataComponents.ENERGY.get(),
                            item.getCapacity(), item.getTransfer());
                },
                BASIC_ACCUMULATOR.asItem(), ADVANCED_ACCUMULATOR.asItem(), ELITE_ACCUMULATOR.asItem(),
                ULTIMATE_ACCUMULATOR.asItem(), CREATIVE_ACCUMULATOR.asItem());
    }

    private static void addToCreativeTab(BuildCreativeModeTabContentsEvent event) {
        if (VoltaicCore.VOLTAIC_TAB.is(event.getTabKey())) {
            event.accept(BASIC_ACCUMULATOR.get());
            event.accept(ADVANCED_ACCUMULATOR.get());
            event.accept(ELITE_ACCUMULATOR.get());
            event.accept(ULTIMATE_ACCUMULATOR.get());
            event.accept(CREATIVE_ACCUMULATOR.get());
            event.accept(VOLTAIC_CABLE.get());
        }
    }
}
