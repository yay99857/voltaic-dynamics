package com.voltaicdynamics.datagen;

import com.voltaicdynamics.VoltaicDynamics;

import net.minecraft.data.loot.LootTableProvider;
import net.minecraft.world.level.storage.loot.parameters.LootContextParamSets;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.data.event.GatherDataEvent;
import net.neoforged.neoforge.data.event.GatherDataRegistryEntriesEvent;

/**
 * {@code ./gradlew runData}: tags, the fluid fuel data map, block loot tables and recipes into src/generated/resources.
 * Loot tables and recipes are reloadable registries, so they are gathered as registry entries.
 */
@EventBusSubscriber(modid = VoltaicDynamics.MOD_ID)
public final class ModDataGenerators {
    private ModDataGenerators() {
    }

    @SubscribeEvent
    public static void gatherRegistryEntries(GatherDataRegistryEntriesEvent event) {
        event.lootTable(new LootTableProvider.SubProviderEntry(ModBlockLootProvider::new, LootContextParamSets.BLOCK))
                .recipe(ModRecipeProvider::new);
    }

    @SubscribeEvent
    public static void gatherData(GatherDataEvent.Client event) {
        event.createBlockAndItemTags(ModBlockTagsProvider::new, (output, lookup, blockTags) -> new ModItemTagsProvider(output, lookup));
        event.createProvider(ModDataMapProvider::new);
    }
}
