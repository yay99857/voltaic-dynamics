package com.voltaicdynamics.core.client.layout;

import net.minecraft.resources.FileToIdConverter;
import net.minecraft.util.ExtraCodecs;
import com.google.gson.JsonElement;
import com.voltaicdynamics.VoltaicDynamics;
import net.minecraft.resources.Identifier;
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
public final class ScreenLayouts extends SimpleJsonResourceReloadListener<JsonElement> {
    public static final ScreenLayouts INSTANCE = new ScreenLayouts();
    private static volatile Map<Identifier, ScreenLayout> layouts = Map.of();

    private ScreenLayouts() {
        super(ExtraCodecs.JSON, FileToIdConverter.json("screens"));
    }

    @Nullable
    public static ScreenLayout get(Identifier id) {
        return layouts.get(id);
    }

    @Override
    protected void apply(Map<Identifier, JsonElement> files, ResourceManager manager, ProfilerFiller profiler) {
        Map<Identifier, ScreenLayout> loaded = new HashMap<>();
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
