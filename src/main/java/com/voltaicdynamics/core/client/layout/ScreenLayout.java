package com.voltaicdynamics.core.client.layout;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.GsonHelper;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Where everything sits in one machine screen, read from {@code assets/<namespace>/screens/<name>.json}
 * (see {@link ScreenLayouts}). Positions are in GUI pixels from the top-left of the background texture.
 *
 * <pre>{@code
 * {
 *   "texture": "voltaicdynamics:textures/gui/electric_furnace_gui.png",
 *   "size": [176, 166],
 *   "title": [8, 6],
 *   "inventory_label": [8, 72],
 *   "player_inventory": [8, 84],
 *   "slots": { "input_0": [56, 35], "output": [116, 35] },
 *   "widgets": {
 *     "progress": { "x": 79, "y": 34, "w": 24, "h": 17, "u": 176, "v": 0, "fill": "right" },
 *     "energy":   { "x": 152, "y": 18, "w": 16, "h": 52, "u": 176, "v": 18, "fill": "up" }
 *   }
 * }
 * }</pre>
 *
 * {@code "slot_frames": ["upgrade_0"]} draws an 18x18 slot frame ({@code "slot_frame_texture"}, default
 * {@code voltaicdynamics:textures/gui/slot.png}) behind slots the background texture has no frame for, and
 * {@code "slot_hints": {"upgrade_0": "<translation key>"}} is the tooltip of such a slot while it is empty.
 *
 * <p>Slot names come from the menu ({@code MachineMenu.addMachineSlot}). A widget is a rectangle on the background
 * plus, when it has {@code u}/{@code v}, the part of the texture drawn over it ("fill": up, down, right, left
 * for gauges and arrows, "none" for a fixed overlay). Screens also use widgets for their tooltip areas.
 */
public record ScreenLayout(ResourceLocation texture, int width, int height, int[] title, int[] inventoryLabel,
                           int[] playerInventory, Map<String, int[]> slots, Map<String, Widget> widgets,
                           List<String> slotFrames, ResourceLocation slotFrameTexture, Map<String, String> slotHints) {
    public static final ResourceLocation DEFAULT_SLOT_FRAME =
            ResourceLocation.fromNamespaceAndPath("voltaicdynamics", "textures/gui/slot.png");


    @Nullable
    public Widget widget(String name) {
        return widgets.get(name);
    }

    @Nullable
    public int[] slot(String name) {
        return slots.get(name);
    }

    public enum Fill {
        UP, DOWN, RIGHT, LEFT, NONE
    }

    public record Widget(int x, int y, int w, int h, int u, int v, Fill fill) {
        /** True when the GUI-relative point is inside the widget. */
        public boolean contains(double relX, double relY) {
            return relX >= x && relX < x + w && relY >= y && relY < y + h;
        }

        /** Draws the texture part over the widget, showing {@code fraction} (0..1) of it in the fill direction. */
        public void draw(GuiGraphics g, ResourceLocation texture, int left, int top, double fraction) {
            if (u < 0) return;
            double f = Math.max(0, Math.min(1, fraction));
            switch (fill) {
                case NONE -> g.blit(texture, left + x, top + y, u, v, w, h);
                case RIGHT -> {
                    int shown = (int) (w * f);
                    if (shown > 0) g.blit(texture, left + x, top + y, u, v, shown, h);
                }
                case LEFT -> {
                    int shown = (int) (w * f);
                    if (shown > 0) g.blit(texture, left + x + w - shown, top + y, u + w - shown, v, shown, h);
                }
                case UP -> {
                    int shown = (int) (h * f);
                    if (shown > 0) g.blit(texture, left + x, top + y + h - shown, u, v + h - shown, w, shown);
                }
                case DOWN -> {
                    int shown = (int) (h * f);
                    if (shown > 0) g.blit(texture, left + x, top + y, u, v, w, shown);
                }
            }
        }

        /** The same widget drawn from another part of the texture (a variant, such as the Advanced Accumulator's gauge). */
        public Widget withUv(int newU, int newV) {
            return new Widget(x, y, w, h, newU, newV, fill);
        }
    }

    static ScreenLayout parse(ResourceLocation id, JsonObject json) {
        ResourceLocation texture = ResourceLocation.parse(GsonHelper.getAsString(json, "texture"));
        int[] size = pair(json, "size", new int[]{176, 166});
        Map<String, int[]> slots = new HashMap<>();
        JsonObject slotsJson = GsonHelper.getAsJsonObject(json, "slots", new JsonObject());
        for (Map.Entry<String, JsonElement> e : slotsJson.entrySet()) {
            slots.put(e.getKey(), pair(e.getValue(), id + " slot " + e.getKey()));
        }
        Map<String, Widget> widgets = new HashMap<>();
        JsonObject widgetsJson = GsonHelper.getAsJsonObject(json, "widgets", new JsonObject());
        for (Map.Entry<String, JsonElement> e : widgetsJson.entrySet()) {
            JsonObject w = GsonHelper.convertToJsonObject(e.getValue(), id + " widget " + e.getKey());
            widgets.put(e.getKey(), new Widget(
                    GsonHelper.getAsInt(w, "x"), GsonHelper.getAsInt(w, "y"),
                    GsonHelper.getAsInt(w, "w"), GsonHelper.getAsInt(w, "h"),
                    GsonHelper.getAsInt(w, "u", -1), GsonHelper.getAsInt(w, "v", -1),
                    Fill.valueOf(GsonHelper.getAsString(w, "fill", "none").toUpperCase())));
        }
        List<String> frames = new ArrayList<>();
        for (JsonElement e : GsonHelper.getAsJsonArray(json, "slot_frames", new JsonArray())) frames.add(e.getAsString());
        Map<String, String> hints = new HashMap<>();
        for (Map.Entry<String, JsonElement> e : GsonHelper.getAsJsonObject(json, "slot_hints", new JsonObject()).entrySet()) {
            hints.put(e.getKey(), e.getValue().getAsString());
        }
        ResourceLocation frameTexture = json.has("slot_frame_texture")
                ? ResourceLocation.parse(GsonHelper.getAsString(json, "slot_frame_texture")) : DEFAULT_SLOT_FRAME;
        return new ScreenLayout(texture, size[0], size[1],
                pair(json, "title", new int[]{8, 6}),
                pair(json, "inventory_label", new int[]{8, size[1] - 94}),
                pair(json, "player_inventory", new int[]{8, size[1] - 82}),
                Map.copyOf(slots), Map.copyOf(widgets), List.copyOf(frames), frameTexture, Map.copyOf(hints));
    }

    private static int[] pair(JsonObject json, String key, int[] fallback) {
        return json.has(key) ? pair(json.get(key), key) : fallback;
    }

    private static int[] pair(JsonElement element, String what) {
        JsonArray array = GsonHelper.convertToJsonArray(element, what);
        if (array.size() != 2) throw new IllegalArgumentException(what + ": expected [x, y]");
        return new int[]{array.get(0).getAsInt(), array.get(1).getAsInt()};
    }
}
