package com.voltaicdynamics.gadgets;

import com.voltaicdynamics.VoltaicDynamics;
import com.voltaicdynamics.core.ModDataComponents;
import com.voltaicdynamics.core.VoltaicCore;

import net.minecraft.world.item.Item;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.capabilities.RegisterCapabilitiesEvent;
import net.neoforged.neoforge.energy.ComponentEnergyStorage;
import net.neoforged.neoforge.event.BuildCreativeModeTabContentsEvent;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;

/** Voltaic Gadgets: powered tools you carry. Depends only on Voltaic Core. */
public final class VoltaicGadgets {
    public static final DeferredRegister.Items ITEMS = DeferredRegister.createItems(VoltaicDynamics.MOD_ID);

    public static final DeferredItem<PocketAccumulatorItem> POCKET_ACCUMULATOR = ITEMS.register("pocket_accumulator",
            () -> new PocketAccumulatorItem(new Item.Properties()));

    private VoltaicGadgets() {
    }

    public static void register(IEventBus modEventBus) {
        ITEMS.register(modEventBus);
        // 1.2.0 renamed the Pocket Capacitor so it no longer shares a name with other mods.
        ITEMS.addAlias(VoltaicDynamics.id("pocket_capacitor"), VoltaicDynamics.id("pocket_accumulator"));
        modEventBus.addListener(VoltaicGadgets::registerCapabilities);
        modEventBus.addListener(VoltaicGadgets::addToCreativeTab);
    }

    private static void registerCapabilities(RegisterCapabilitiesEvent event) {
        event.registerItem(Capabilities.EnergyStorage.ITEM,
                (stack, context) -> new ComponentEnergyStorage(stack, ModDataComponents.ENERGY.get(),
                        PocketAccumulatorItem.capacity(), PocketAccumulatorItem.transfer()),
                POCKET_ACCUMULATOR.get());
    }

    private static void addToCreativeTab(BuildCreativeModeTabContentsEvent event) {
        if (VoltaicCore.VOLTAIC_TAB.is(event.getTabKey())) {
            event.accept(POCKET_ACCUMULATOR.get());
        }
    }
}
