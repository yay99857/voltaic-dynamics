package com.voltaicdynamics.logistics;

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
import net.neoforged.neoforge.event.BuildCreativeModeTabContentsEvent;
import net.neoforged.neoforge.event.level.LevelEvent;
import net.neoforged.neoforge.event.server.ServerStoppedEvent;
import net.neoforged.neoforge.event.tick.LevelTickEvent;
import net.neoforged.neoforge.fluids.capability.templates.FluidHandlerItemStack;
import net.neoforged.neoforge.registries.DeferredBlock;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

/** Voltaic Logistics: lag-free item and fluid cables, and the Fluid Tank. Depends only on Voltaic Core. */
public final class VoltaicLogistics {
    public static final DeferredRegister.Blocks BLOCKS = DeferredRegister.createBlocks(VoltaicDynamics.MOD_ID);
    public static final DeferredRegister.Items ITEMS = DeferredRegister.createItems(VoltaicDynamics.MOD_ID);
    public static final DeferredRegister<BlockEntityType<?>> BLOCK_ENTITIES =
            DeferredRegister.create(Registries.BLOCK_ENTITY_TYPE, VoltaicDynamics.MOD_ID);
    public static final DeferredRegister<MenuType<?>> MENUS = DeferredRegister.create(Registries.MENU, VoltaicDynamics.MOD_ID);

    // Blocks
    public static final DeferredBlock<ItemCableBlock> ITEM_CABLE = BLOCKS.register(
            "item_cable",
            () -> new ItemCableBlock(cableProperties()));

    public static final DeferredBlock<FluidCableBlock> FLUID_CABLE = BLOCKS.register(
            "fluid_cable",
            () -> new FluidCableBlock(cableProperties()));

    public static final DeferredBlock<FluidTankBlock> FLUID_TANK = BLOCKS.register(
            "fluid_tank",
            () -> new FluidTankBlock(VoltaicDynamics.machineProperties(2.5F, 6.0F).noOcclusion()));

    // Block entities
    /** Shared by item and fluid cables; the block tells them apart. */
    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<LogisticsCableBlockEntity>> LOGISTICS_CABLE_BE =
            BLOCK_ENTITIES.register("logistics_cable", () ->
                    BlockEntityType.Builder.of(LogisticsCableBlockEntity::new, ITEM_CABLE.get(), FLUID_CABLE.get()).build(null));

    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<FluidTankBlockEntity>> FLUID_TANK_BE =
            BLOCK_ENTITIES.register("fluid_tank", () ->
                    BlockEntityType.Builder.of(FluidTankBlockEntity::new, FLUID_TANK.get()).build(null));

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
        ITEMS.registerItem("fluid_tank", props -> new FluidTankItem(FLUID_TANK.get(), props), new Item.Properties().stacksTo(1));
        ITEMS.registerSimpleBlockItem(ITEM_CABLE);
        ITEMS.registerSimpleBlockItem(FLUID_CABLE);

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
        event.registerBlockEntity(Capabilities.ItemHandler.BLOCK, LOGISTICS_CABLE_BE.get(),
                (be, side) -> be.getItemHandler(side));
        event.registerBlockEntity(Capabilities.FluidHandler.BLOCK, LOGISTICS_CABLE_BE.get(),
                (be, side) -> be.getFluidHandler(side));

        // Fluid Tank, as a block and as an item
        event.registerBlockEntity(Capabilities.FluidHandler.BLOCK, FLUID_TANK_BE.get(),
                (be, side) -> be.getFluidHandler(side));
        event.registerBlockEntity(Capabilities.ItemHandler.BLOCK, FLUID_TANK_BE.get(),
                (be, side) -> be.getItemHandler(side));
        event.registerItem(Capabilities.FluidHandler.ITEM,
                (stack, context) -> new FluidHandlerItemStack(ModDataComponents.FLUID, stack, FluidTankBlockEntity.capacity()),
                FLUID_TANK.asItem());
    }

    private static void addToCreativeTab(BuildCreativeModeTabContentsEvent event) {
        if (VoltaicCore.VOLTAIC_TAB.is(event.getTabKey())) {
            event.accept(FLUID_TANK.get());
            event.accept(ITEM_CABLE.get());
            event.accept(FLUID_CABLE.get());
        }
    }
}
