package com.voltaicdynamics.item;

import com.voltaicdynamics.VoltaicDynamics;
import net.minecraft.world.item.Item;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;

public class ModItems {
    public static final DeferredRegister.Items ITEMS = DeferredRegister.createItems(VoltaicDynamics.MOD_ID);

    // Canonical Ingots
    public static final DeferredItem<Item> VOLTAIC_INGOT = ITEMS.registerSimpleItem("voltaic_ingot");
    public static final DeferredItem<Item> CONDUCTIVE_ALLOY = ITEMS.registerSimpleItem("conductive_alloy");

    // Metal Dusts (Standard for ore processing, crushing and blending)
    public static final DeferredItem<Item> VOLTAIC_DUST = ITEMS.registerSimpleItem("voltaic_dust");
    public static final DeferredItem<Item> CONDUCTIVE_DUST = ITEMS.registerSimpleItem("conductive_dust");

    // Technology Components
    public static final DeferredItem<Item> THERMOPILE_CORE = ITEMS.registerSimpleItem("thermopile_core");
    public static final DeferredItem<PocketCapacitorItem> POCKET_CAPACITOR = ITEMS.register("pocket_capacitor",
            () -> new PocketCapacitorItem(new Item.Properties()));
}
