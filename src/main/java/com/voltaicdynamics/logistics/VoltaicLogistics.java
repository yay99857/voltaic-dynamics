package com.voltaicdynamics.logistics;

import net.neoforged.neoforge.transfer.fluid.ItemAccessFluidHandler;
import com.voltaicdynamics.core.item.VoltaicBlockItem;
import com.voltaicdynamics.VoltaicDynamics;
import com.voltaicdynamics.core.ModDataComponents;
import com.voltaicdynamics.core.VoltaicCore;

import net.minecraft.core.registries.Registries;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Rarity;
import net.minecraft.world.level.block.Block;
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

import java.util.List;

/** Voltaic Logistics: lag-free item and fluid cables, and the Fluid Tank. Depends only on Voltaic Core. */
public final class VoltaicLogistics {
    public static final DeferredRegister.Blocks BLOCKS = DeferredRegister.createBlocks(VoltaicDynamics.MOD_ID);
    public static final DeferredRegister.Items ITEMS = DeferredRegister.createItems(VoltaicDynamics.MOD_ID);
    public static final DeferredRegister<BlockEntityType<?>> BLOCK_ENTITIES =
            DeferredRegister.create(Registries.BLOCK_ENTITY_TYPE, VoltaicDynamics.MOD_ID);
    public static final DeferredRegister<MenuType<?>> MENUS = DeferredRegister.create(Registries.MENU, VoltaicDynamics.MOD_ID);

    // Blocks
    public static final DeferredBlock<ItemCableBlock> ITEM_CABLE = BLOCKS.registerBlock("item_cable",
            p -> new ItemCableBlock(p),
            () -> cableProperties());

    public static final DeferredBlock<FluidCableBlock> FLUID_CABLE = BLOCKS.registerBlock("fluid_cable",
            p -> new FluidCableBlock(p),
            () -> cableProperties());

    public static final DeferredBlock<FluidTankBlock> FLUID_TANK = BLOCKS.registerBlock("fluid_tank",
            p -> new FluidTankBlock(p, FluidTankTier.BASIC),
            () -> VoltaicDynamics.machineProperties(2.5F, 6.0F).noOcclusion());

    public static final DeferredBlock<FluidTankBlock> ADVANCED_FLUID_TANK = BLOCKS.registerBlock("advanced_fluid_tank",
            p -> new FluidTankBlock(p, FluidTankTier.ADVANCED),
            () -> VoltaicDynamics.machineProperties(3.0F, 8.0F).noOcclusion());

    public static final DeferredBlock<FluidTankBlock> ELITE_FLUID_TANK = BLOCKS.registerBlock("elite_fluid_tank",
            p -> new FluidTankBlock(p, FluidTankTier.ELITE),
            () -> VoltaicDynamics.machineProperties(3.5F, 12.0F).noOcclusion());

    public static final DeferredBlock<FluidTankBlock> ULTIMATE_FLUID_TANK = BLOCKS.registerBlock("ultimate_fluid_tank",
            p -> new FluidTankBlock(p, FluidTankTier.ULTIMATE),
            () -> VoltaicDynamics.machineProperties(4.0F, 15.0F).noOcclusion());

    public static final DeferredBlock<FluidTankBlock> CREATIVE_FLUID_TANK = BLOCKS.registerBlock("creative_fluid_tank",
            p -> new FluidTankBlock(p, FluidTankTier.CREATIVE),
            () -> VoltaicDynamics.machineProperties(4.0F, 3_600_000.0F).noOcclusion());

    /** Every tank block, basic first. */
    public static List<DeferredBlock<FluidTankBlock>> tanks() {
        return List.of(FLUID_TANK, ADVANCED_FLUID_TANK, ELITE_FLUID_TANK, ULTIMATE_FLUID_TANK, CREATIVE_FLUID_TANK);
    }

    // Block entities
    /** Shared by item and fluid cables; the block tells them apart. */
    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<LogisticsCableBlockEntity>> LOGISTICS_CABLE_BE =
            BLOCK_ENTITIES.register("logistics_cable", () ->
                    new BlockEntityType<>(LogisticsCableBlockEntity::new, ITEM_CABLE.get(), FLUID_CABLE.get()));

    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<FluidTankBlockEntity>> FLUID_TANK_BE =
            BLOCK_ENTITIES.register("fluid_tank", () ->
                    new BlockEntityType<>(FluidTankBlockEntity::new, tanks().stream().map(DeferredBlock::get)
                            .toArray(Block[]::new)));

    // Menus
    public static final DeferredHolder<MenuType<?>, MenuType<FluidTankMenu>> FLUID_TANK_MENU =
            MENUS.register("fluid_tank_menu", () -> IMenuTypeExtension.create(FluidTankMenu::new));

    private VoltaicLogistics() {
    }

    private static BlockBehaviour.Properties cableProperties() {
        return BlockBehaviour.Properties.of()
                .strength(0.8F, 2.0F)
                .sound(SoundType.METAL)
                .noOcclusion();
    }

    public static void register(IEventBus modEventBus) {
        for (DeferredBlock<FluidTankBlock> tank : tanks()) {
            Item.Properties props = new Item.Properties().stacksTo(1).useBlockDescriptionPrefix();
            if (tank == ULTIMATE_FLUID_TANK) props.fireResistant();
            if (tank == CREATIVE_FLUID_TANK) props.rarity(Rarity.EPIC);
            ITEMS.registerItem(tank.getId().getPath(), p -> new FluidTankItem(tank.get(), p), () -> props);
        }
        ITEMS.registerItem(ITEM_CABLE.getId().getPath(), p -> new VoltaicBlockItem(ITEM_CABLE.get(), p), Item.Properties::useBlockDescriptionPrefix);
        ITEMS.registerItem(FLUID_CABLE.getId().getPath(), p -> new VoltaicBlockItem(FLUID_CABLE.get(), p), Item.Properties::useBlockDescriptionPrefix);

        BLOCKS.register(modEventBus);
        ITEMS.register(modEventBus);
        BLOCK_ENTITIES.register(modEventBus);
        MENUS.register(modEventBus);
        modEventBus.addListener(VoltaicLogistics::registerCapabilities);
        modEventBus.addListener(VoltaicLogistics::addToCreativeTab);

        // Item and fluid networks tick once per level tick and are discarded with their level.
        NeoForge.EVENT_BUS.addListener((LevelTickEvent.Post event) -> {
            if (event.getLevel() instanceof ServerLevel level) {
                LogisticsNetworkManager.tickAll(level);
            }
        });
        NeoForge.EVENT_BUS.addListener((LevelEvent.Unload event) -> {
            if (event.getLevel() instanceof ServerLevel level) {
                LogisticsNetworkManager.remove(level);
            }
        });
        NeoForge.EVENT_BUS.addListener((ServerStoppedEvent event) -> LogisticsNetworkManager.clearAll());
    }

    private static void registerCapabilities(RegisterCapabilitiesEvent event) {
        // Connector faces accept pushes into the network
        event.registerBlockEntity(Capabilities.Item.BLOCK, LOGISTICS_CABLE_BE.get(),
                (be, side) -> be.getItemHandler(side));
        event.registerBlockEntity(Capabilities.Fluid.BLOCK, LOGISTICS_CABLE_BE.get(),
                (be, side) -> be.getFluidHandler(side));

        // Fluid Tank, as a block and as an item
        event.registerBlockEntity(Capabilities.Fluid.BLOCK, FLUID_TANK_BE.get(),
                (be, side) -> be.getFluidHandler(side));
        event.registerBlockEntity(Capabilities.Item.BLOCK, FLUID_TANK_BE.get(),
                (be, side) -> be.getItemHandler(side));
        // The Creative tank item has no item handler: a held infinite tank would be a free fluid source anywhere.
        event.registerItem(Capabilities.Fluid.ITEM,
                (stack, access) -> new ItemAccessFluidHandler(access, ModDataComponents.FLUID.get(),
                        ((FluidTankItem) stack.getItem()).getTier().capacity()),
                FLUID_TANK.asItem(), ADVANCED_FLUID_TANK.asItem(), ELITE_FLUID_TANK.asItem(), ULTIMATE_FLUID_TANK.asItem());
    }

    private static void addToCreativeTab(BuildCreativeModeTabContentsEvent event) {
        if (VoltaicCore.VOLTAIC_TAB.is(event.getTabKey())) {
            for (DeferredBlock<FluidTankBlock> tank : tanks()) event.accept(tank.get());
            event.accept(ITEM_CABLE.get());
            event.accept(FLUID_CABLE.get());
        }
    }
}
