package com.voltaicdynamics.machines;

import com.voltaicdynamics.core.item.VoltaicBlockItem;
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
import net.neoforged.neoforge.registries.DeferredItem;
import net.minecraft.world.item.Item;
import net.neoforged.neoforge.registries.DeferredRegister;

/** Machines that turn FE into processing: Electric Furnace, Alloy Smelter, Pulverizer and their recipe types. */
public final class VoltaicMachines {
    public static final DeferredRegister.Blocks BLOCKS = DeferredRegister.createBlocks(VoltaicDynamics.MOD_ID);
    public static final DeferredRegister.Items ITEMS = DeferredRegister.createItems(VoltaicDynamics.MOD_ID);
    public static final DeferredRegister<BlockEntityType<?>> BLOCK_ENTITIES =
            DeferredRegister.create(Registries.BLOCK_ENTITY_TYPE, VoltaicDynamics.MOD_ID);
    public static final DeferredRegister<MenuType<?>> MENUS = DeferredRegister.create(Registries.MENU, VoltaicDynamics.MOD_ID);

    // Blocks
    public static final DeferredBlock<ElectricFurnaceBlock> ELECTRIC_FURNACE = BLOCKS.registerBlock("electric_furnace",
            p -> new ElectricFurnaceBlock(p),
            () -> VoltaicDynamics.machineProperties(3.5F, 6.0F)
                    .lightLevel(state -> state.getValue(ElectricFurnaceBlock.LIT) ? 10 : 0));

    public static final DeferredBlock<AlloySmelterBlock> ALLOY_SMELTER = BLOCKS.registerBlock("alloy_smelter",
            p -> new AlloySmelterBlock(p),
            () -> VoltaicDynamics.machineProperties(3.5F, 6.0F)
                    .lightLevel(state -> state.getValue(AlloySmelterBlock.LIT) ? 10 : 0));

    public static final DeferredBlock<PulverizerBlock> PULVERIZER = BLOCKS.registerBlock("pulverizer",
            p -> new PulverizerBlock(p),
            () -> VoltaicDynamics.machineProperties(3.5F, 6.0F)
                    .lightLevel(state -> state.getValue(PulverizerBlock.LIT) ? 6 : 0));

    // Upgrades
    public static final DeferredItem<UpgradeItem> SPEED_UPGRADE = ITEMS.registerItem("speed_upgrade", p -> new UpgradeItem(UpgradeItem.Type.SPEED, p), p -> p);
    public static final DeferredItem<UpgradeItem> EFFICIENCY_UPGRADE = ITEMS.registerItem("efficiency_upgrade", p -> new UpgradeItem(UpgradeItem.Type.EFFICIENCY, p), p -> p);

    // Tier Installers: raise a machine one tier in place
    public static final DeferredItem<TierInstallerItem> ADVANCED_TIER_INSTALLER = ITEMS.registerItem("advanced_tier_installer", p -> new TierInstallerItem(MachineTier.ADVANCED, p), p -> p);
    public static final DeferredItem<TierInstallerItem> ELITE_TIER_INSTALLER = ITEMS.registerItem("elite_tier_installer", p -> new TierInstallerItem(MachineTier.ELITE, p), p -> p);
    public static final DeferredItem<TierInstallerItem> ULTIMATE_TIER_INSTALLER = ITEMS.registerItem("ultimate_tier_installer", p -> new TierInstallerItem(MachineTier.ULTIMATE, p), p -> p.fireResistant());

    // Block entities
    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<ElectricFurnaceBlockEntity>> ELECTRIC_FURNACE_BE =
            BLOCK_ENTITIES.register("electric_furnace", () ->
                    new BlockEntityType<>(ElectricFurnaceBlockEntity::new, ELECTRIC_FURNACE.get()));

    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<AlloySmelterBlockEntity>> ALLOY_SMELTER_BE =
            BLOCK_ENTITIES.register("alloy_smelter", () ->
                    new BlockEntityType<>(AlloySmelterBlockEntity::new, ALLOY_SMELTER.get()));

    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<PulverizerBlockEntity>> PULVERIZER_BE =
            BLOCK_ENTITIES.register("pulverizer", () ->
                    new BlockEntityType<>(PulverizerBlockEntity::new, PULVERIZER.get()));

    // Menus
    public static final DeferredHolder<MenuType<?>, MenuType<ElectricFurnaceMenu>> ELECTRIC_FURNACE_MENU =
            MENUS.register("electric_furnace_menu", () -> IMenuTypeExtension.create(ElectricFurnaceMenu::new));

    public static final DeferredHolder<MenuType<?>, MenuType<AlloySmelterMenu>> ALLOY_SMELTER_MENU =
            MENUS.register("alloy_smelter_menu", () -> IMenuTypeExtension.create(AlloySmelterMenu::new));

    public static final DeferredHolder<MenuType<?>, MenuType<PulverizerMenu>> PULVERIZER_MENU =
            MENUS.register("pulverizer_menu", () -> IMenuTypeExtension.create(PulverizerMenu::new));

    private VoltaicMachines() {
    }

    public static void register(IEventBus modEventBus) {
        ITEMS.registerItem(ELECTRIC_FURNACE.getId().getPath(), p -> new VoltaicBlockItem(ELECTRIC_FURNACE.get(), p), Item.Properties::useBlockDescriptionPrefix);
        ITEMS.registerItem(ALLOY_SMELTER.getId().getPath(), p -> new VoltaicBlockItem(ALLOY_SMELTER.get(), p), Item.Properties::useBlockDescriptionPrefix);
        ITEMS.registerItem(PULVERIZER.getId().getPath(), p -> new VoltaicBlockItem(PULVERIZER.get(), p), Item.Properties::useBlockDescriptionPrefix);

        BLOCKS.register(modEventBus);
        ITEMS.register(modEventBus);
        BLOCK_ENTITIES.register(modEventBus);
        MENUS.register(modEventBus);
        ModRecipes.TYPES.register(modEventBus);
        ModRecipes.SERIALIZERS.register(modEventBus);
        modEventBus.addListener(VoltaicMachines::registerCapabilities);
        modEventBus.addListener(ModRecipes::registerPropertySets);
        net.neoforged.neoforge.common.NeoForge.EVENT_BUS.addListener(ModRecipes::syncToClients);
        modEventBus.addListener(VoltaicMachines::addToCreativeTab);
    }

    private static void registerCapabilities(RegisterCapabilitiesEvent event) {
        // Energy in, items in/out
        event.registerBlockEntity(Capabilities.Energy.BLOCK, ELECTRIC_FURNACE_BE.get(),
                (be, side) -> be.getEnergyStorage(side));
        event.registerBlockEntity(Capabilities.Item.BLOCK, ELECTRIC_FURNACE_BE.get(),
                (be, side) -> be.getItemHandler(side));
        event.registerBlockEntity(Capabilities.Energy.BLOCK, ALLOY_SMELTER_BE.get(),
                (be, side) -> be.getEnergyStorage(side));
        event.registerBlockEntity(Capabilities.Item.BLOCK, ALLOY_SMELTER_BE.get(),
                (be, side) -> be.getItemHandler(side));
        event.registerBlockEntity(Capabilities.Energy.BLOCK, PULVERIZER_BE.get(),
                (be, side) -> be.getEnergyStorage(side));
        event.registerBlockEntity(Capabilities.Item.BLOCK, PULVERIZER_BE.get(),
                (be, side) -> be.getItemHandler(side));
    }

    private static void addToCreativeTab(BuildCreativeModeTabContentsEvent event) {
        if (VoltaicCore.VOLTAIC_TAB.is(event.getTabKey())) {
            event.accept(ELECTRIC_FURNACE.get());
            event.accept(ALLOY_SMELTER.get());
            event.accept(PULVERIZER.get());
            event.accept(SPEED_UPGRADE.get());
            event.accept(EFFICIENCY_UPGRADE.get());
            event.accept(ADVANCED_TIER_INSTALLER.get());
            event.accept(ELITE_TIER_INSTALLER.get());
            event.accept(ULTIMATE_TIER_INSTALLER.get());
        }
    }
}
