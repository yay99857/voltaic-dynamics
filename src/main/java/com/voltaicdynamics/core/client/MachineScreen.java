package com.voltaicdynamics.core.client;

import net.minecraft.client.renderer.RenderPipelines;
import com.voltaicdynamics.VoltaicDynamics;
import com.voltaicdynamics.core.client.layout.ScreenLayout;
import com.voltaicdynamics.core.client.layout.ScreenLayouts;
import com.voltaicdynamics.core.menu.MachineMenu;

import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.client.renderer.Rect2i;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.entity.player.Inventory;
import org.jetbrains.annotations.Nullable;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Base screen of the Voltaic machines. Its size, background, labels, slot positions and widgets (gauges, arrows,
 * tooltip areas) come from a {@link ScreenLayout} JSON file, reloaded with the resources (F3+T); it also adds the
 * side configuration tab and an energy tooltip helper.
 */
public abstract class MachineScreen<M extends MachineMenu> extends AbstractContainerScreen<M> {
    private static final Set<Identifier> REPORTED_MISSING = new HashSet<>();

    private final Identifier layoutId;
    @Nullable
    protected ScreenLayout layout;
    @Nullable
    protected SideConfigPanel sidePanel;

    /** @param layoutId the layout file {@code assets/<namespace>/screens/<path>.json} */
    protected MachineScreen(M menu, Inventory playerInventory, Component title, Identifier layoutId) {
        // The GUI size is fixed when the screen is made, so it comes from the layout already loaded.
        super(menu, playerInventory, title, widthOf(layoutId), heightOf(layoutId));
        this.layoutId = layoutId;
        this.inventoryLabelY = 72;
    }

    private static int widthOf(Identifier layoutId) {
        ScreenLayout layout = ScreenLayouts.get(layoutId);
        return layout == null ? 176 : layout.width();
    }

    private static int heightOf(Identifier layoutId) {
        ScreenLayout layout = ScreenLayouts.get(layoutId);
        return layout == null ? 166 : layout.height();
    }

    @Override
    protected void init() {
        layout = ScreenLayouts.get(layoutId);
        if (layout != null) {
            applyLayout(layout);
        } else if (REPORTED_MISSING.add(layoutId)) {
            VoltaicDynamics.LOGGER.error("Missing screen layout {} (assets/{}/screens/{}.json)",
                    layoutId, layoutId.getNamespace(), layoutId.getPath());
        }
        super.init();
        sidePanel = SideConfigPanel.create(menu);
        if (sidePanel != null) sidePanel.setPosition(leftPos + imageWidth, topPos);
    }

    private void applyLayout(ScreenLayout layout) {
        titleLabelX = layout.title()[0];
        titleLabelY = layout.title()[1];
        inventoryLabelX = layout.inventoryLabel()[0];
        inventoryLabelY = layout.inventoryLabel()[1];
        int[] player = layout.playerInventory();
        for (Slot slot : menu.slots) {
            if (slot.container instanceof Inventory) {
                int index = slot.getContainerSlot();
                boolean hotbar = index < 9;
                slot.x = player[0] + (index % 9) * 18;
                slot.y = hotbar ? player[1] + 58 : player[1] + (index / 9 - 1) * 18;
                continue;
            }
            String name = menu.slotName(slot);
            int[] pos = name == null ? null : layout.slot(name);
            if (pos != null) {
                slot.x = pos[0];
                slot.y = pos[1];
            } else if (name != null) {
                VoltaicDynamics.LOGGER.warn("Screen layout {} has no position for slot '{}'", layoutId, name);
            }
        }
    }

    @Nullable
    public ScreenLayout getLayout() {
        return layout;
    }

    /** A widget of the layout, or null when the layout or the widget is missing. */
    @Nullable
    public ScreenLayout.Widget widget(String name) {
        return layout == null ? null : layout.widget(name);
    }

    /** Draws the layout's background texture and the extra slot frames it asks for. */
    protected void drawBackground(GuiGraphicsExtractor g) {
        if (layout == null) return;
        g.blit(RenderPipelines.GUI_TEXTURED, layout.texture(), leftPos, topPos, 0, 0, imageWidth, imageHeight, 256, 256);
        for (String name : layout.slotFrames()) {
            int[] pos = layout.slot(name);
            if (pos != null) g.blit(RenderPipelines.GUI_TEXTURED, layout.slotFrameTexture(), leftPos + pos[0] - 1, topPos + pos[1] - 1, 0, 0, 18, 18, 18, 18);
        }
    }

    @Override
    protected void extractTooltip(GuiGraphicsExtractor g, int x, int y) {
        if (layout != null && hoveredSlot != null && !hoveredSlot.hasItem() && menu.getCarried().isEmpty()) {
            String name = menu.slotName(hoveredSlot);
            String hint = name == null ? null : layout.slotHints().get(name);
            if (hint != null) {
                g.setTooltipForNextFrame(font, Component.translatable(hint), x, y);
                return;
            }
        }
        if (sidePanel != null) {
            List<Component> lines = sidePanel.tooltip(x, y);
            if (lines != null) {
                g.setComponentTooltipForNextFrame(font, lines, x, y);
                return;
            }
        }
        super.extractTooltip(g, x, y);
    }

    /** Draws a widget's texture part filled to {@code fraction}; does nothing if the layout lacks it. */
    protected void drawWidget(GuiGraphicsExtractor g, String name, double fraction) {
        ScreenLayout.Widget w = widget(name);
        if (w != null && layout != null) w.draw(g, layout.texture(), leftPos, topPos, fraction);
    }

    protected boolean isHovering(@Nullable ScreenLayout.Widget w, double mouseX, double mouseY) {
        return w != null && w.contains(mouseX - leftPos, mouseY - topPos);
    }

    protected boolean isHovering(String widgetName, double mouseX, double mouseY) {
        return isHovering(widget(widgetName), mouseX, mouseY);
    }

    protected static double fraction(long value, long max) {
        return max <= 0 ? 0 : (double) value / max;
    }

    /** Areas outside the GUI texture that recipe viewers must not cover. */
    public List<Rect2i> getExtraAreas() {
        return sidePanel == null ? List.of() : sidePanel.getAreas();
    }

    @Override
    public void extractContents(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
        super.extractContents(graphics, mouseX, mouseY, partialTick);
        if (sidePanel != null) sidePanel.render(graphics, font, mouseX, mouseY);
    }

    @Override
    public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick) {
        if (sidePanel != null && sidePanel.mouseClicked(event.x(), event.y(), event.button())) return true;
        return super.mouseClicked(event, doubleClick);
    }

    @Override
    protected boolean hasClickedOutside(double mouseX, double mouseY, int guiLeft, int guiTop) {
        if (sidePanel != null) {
            for (Rect2i area : sidePanel.getAreas()) {
                if (area.contains((int) mouseX, (int) mouseY)) return false;
            }
        }
        return super.hasClickedOutside(mouseX, mouseY, guiLeft, guiTop);
    }

    protected void energyTooltip(GuiGraphicsExtractor g, int x, int y, int energy, int capacity, @Nullable Component extra) {
        Component first = Component.translatable("tooltip.voltaicdynamics.gui.energy",
                String.format("%,d", energy), String.format("%,d", capacity)).withStyle(ChatFormatting.AQUA);
        g.setComponentTooltipForNextFrame(font, extra == null ? List.of(first) : List.of(first, extra), x, y);
    }
}
