package com.voltaicdynamics.core.client.layout;

import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.voltaicdynamics.VoltaicDynamics;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.server.packs.resources.SimpleJsonResourceReloadListener;
import net.minecraft.util.GsonHelper;
import net.minecraft.util.profiling.ProfilerFiller;
import org.jetbrains.annotations.Nullable;

import java.util.HashMap;
import java.util.Map;

/**
 * Loads every {@code assets/voltaicdynamics/screens/*.json} into a {@link ScreenLayout} (other namespaces' screens
 * folders belong to other mods, such as AE2, and are skipped), on startup and on every
 * resource reload (F3+T), so a screen can be rearranged without recompiling. A file that fails to parse is
 * logged and skipped; its screen then keeps the slots where the menu put them.
 */
public final class ScreenLayouts extends SimpleJsonResourceReloadListener {
    public static final ScreenLayouts INSTANCE = new ScreenLayouts();
    private static volatile Map<ResourceLocation, ScreenLayout> layouts = Map.of();

    private ScreenLayouts() {
        super(new Gson(), "screens");
    }

    @Nullable
    public static ScreenLayout get(ResourceLocation id) {
        return layouts.get(id);
    }

    @Override
    protected void apply(Map<ResourceLocation, JsonElement> files, ResourceManager manager, ProfilerFiller profiler) {
        Map<ResourceLocation, ScreenLayout> loaded = new HashMap<>();
        files.forEach((id, json) -> {
            // Other mods use assets/<their namespace>/screens too (AE2 does, in its own format): only ours.
            if (!id.getNamespace().equals(VoltaicDynamics.MOD_ID)) return;
            try {
                loaded.put(id, ScreenLayout.parse(id, GsonHelper.convertToJsonObject(json, id.toString())));
            } catch (RuntimeException e) {
                VoltaicDynamics.LOGGER.error("Bad screen layout {}: {}", id, e.getMessage());
            }
        });
        layouts = Map.copyOf(loaded);
        VoltaicDynamics.LOGGER.info("Loaded {} screen layouts", loaded.size());
    }
}
