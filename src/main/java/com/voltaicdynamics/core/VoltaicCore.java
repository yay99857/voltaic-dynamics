package com.voltaicdynamics.core;

import com.voltaicdynamics.VoltaicDynamics;
import com.voltaicdynamics.core.blockentity.DeferredSaves;

import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.core.registries.BuiltInRegistries;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;

/**
 * Voltaic Core: what every module shares. Materials, data components and the creative tab live here;
 * the shared code lives in the sub-packages (energy storage, side configuration, base menu and screen).
 * If the modules ever ship as separate mods, this package becomes the library they all depend on.
 */
public final class VoltaicCore {
    public static final DeferredRegister.Items ITEMS = DeferredRegister.createItems(VoltaicDynamics.MOD_ID);
    public static final DeferredRegister<CreativeModeTab> CREATIVE_MODE_TABS =
            DeferredRegister.create(Registries.CREATIVE_MODE_TAB, VoltaicDynamics.MOD_ID);

    // Ingots
    public static final DeferredItem<Item> VOLTAIC_INGOT = ITEMS.registerSimpleItem("voltaic_ingot");
    public static final DeferredItem<Item> GALVANIC_ALLOY = ITEMS.registerSimpleItem("galvanic_alloy");

    // Dusts (for ore processing, crushing and blending)
    public static final DeferredItem<Item> VOLTAIC_DUST = ITEMS.registerSimpleItem("voltaic_dust");
    public static final DeferredItem<Item> GALVANIC_DUST = ITEMS.registerSimpleItem("galvanic_dust");
    /** Made by the Pulverizer (in c:dusts/*, so packs can unify them with other mods' dusts); smelt into ingots. */
    public static final DeferredItem<Item> IRON_DUST = ITEMS.registerSimpleItem("iron_dust");
    public static final DeferredItem<Item> COPPER_DUST = ITEMS.registerSimpleItem("copper_dust");
    public static final DeferredItem<Item> GOLD_DUST = ITEMS.registerSimpleItem("gold_dust");

    // Components
    public static final DeferredItem<Item> THERMOPILE_CORE = ITEMS.registerSimpleItem("thermopile_core");

    /**
     * One tab for every module. It lists the core materials; each module adds its own blocks and items
     * from {@code BuildCreativeModeTabContentsEvent}, in the order the modules are registered.
     */
    public static final DeferredHolder<CreativeModeTab, CreativeModeTab> VOLTAIC_TAB = CREATIVE_MODE_TABS.register("voltaic_tab",
            () -> CreativeModeTab.builder()
                    .title(Component.translatable("itemGroup.voltaicdynamics"))
                    .icon(VoltaicCore::tabIcon)
                    .displayItems((parameters, output) -> {
                        output.accept(VOLTAIC_INGOT.get());
                        output.accept(GALVANIC_ALLOY.get());
                        output.accept(VOLTAIC_DUST.get());
                        output.accept(GALVANIC_DUST.get());
                        output.accept(IRON_DUST.get());
                        output.accept(COPPER_DUST.get());
                        output.accept(GOLD_DUST.get());
                        output.accept(THERMOPILE_CORE.get());
                    })
                    .build());

    private VoltaicCore() {
    }

    public static void register(IEventBus modEventBus) {
        ModDataComponents.COMPONENTS.register(modEventBus);
        ITEMS.register(modEventBus);
        CREATIVE_MODE_TABS.register(modEventBus);
        DeferredSaves.register();

        // 1.2.0 renamed Conductive Alloy so it no longer shares a name or tags with Ender IO.
        ITEMS.addAlias(VoltaicDynamics.id("conductive_alloy"), VoltaicDynamics.id("galvanic_alloy"));
        ITEMS.addAlias(VoltaicDynamics.id("conductive_dust"), VoltaicDynamics.id("galvanic_dust"));
    }

    /** The Basic Accumulator lives in the power module, so the icon is looked up by id instead of imported. */
    private static ItemStack tabIcon() {
        Item icon = BuiltInRegistries.ITEM.get(VoltaicDynamics.id("basic_accumulator"));
        return new ItemStack(icon == Items.AIR ? VOLTAIC_INGOT.get() : icon);
    }
}
