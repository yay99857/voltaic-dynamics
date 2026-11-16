package com.voltaicdynamics.core.client;

import com.voltaicdynamics.VoltaicDynamics;
import com.voltaicdynamics.core.client.layout.ScreenLayout;
import com.voltaicdynamics.core.client.layout.ScreenLayouts;
import com.voltaicdynamics.core.menu.MachineMenu;

import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.renderer.Rect2i;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
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
    private static final Set<ResourceLocation> REPORTED_MISSING = new HashSet<>();

    private final ResourceLocation layoutId;
    @Nullable
    protected ScreenLayout layout;
    @Nullable
    protected SideConfigPanel sidePanel;

    /** @param layoutId the layout file {@code assets/<namespace>/screens/<path>.json} */
    protected MachineScreen(M menu, Inventory playerInventory, Component title, ResourceLocation layoutId) {
        super(menu, playerInventory, title);
        this.layoutId = layoutId;
        this.imageWidth = 176;
        this.imageHeight = 166;
        this.inventoryLabelY = 72;
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
        imageWidth = layout.width();
        imageHeight = layout.height();
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

    /** Draws the layout's background texture. */
    protected void drawBackground(GuiGraphics g) {
        if (layout != null) g.blit(layout.texture(), leftPos, topPos, 0, 0, imageWidth, imageHeight);
    }

    /** Draws a widget's texture part filled to {@code fraction}; does nothing if the layout lacks it. */
    protected void drawWidget(GuiGraphics g, String name, double fraction) {
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
    public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        super.render(guiGraphics, mouseX, mouseY, partialTick);
        if (sidePanel != null) {
            sidePanel.render(guiGraphics, font, mouseX, mouseY);
            List<Component> lines = sidePanel.tooltip(mouseX, mouseY);
            if (lines != null) {
                guiGraphics.renderComponentTooltip(font, lines, mouseX, mouseY);
                return;
            }
        }
        renderTooltip(guiGraphics, mouseX, mouseY);
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (sidePanel != null && sidePanel.mouseClicked(mouseX, mouseY, button)) return true;
        return super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    protected boolean hasClickedOutside(double mouseX, double mouseY, int guiLeft, int guiTop, int mouseButton) {
        if (sidePanel != null) {
            for (Rect2i area : sidePanel.getAreas()) {
                if (area.contains((int) mouseX, (int) mouseY)) return false;
            }
        }
        return super.hasClickedOutside(mouseX, mouseY, guiLeft, guiTop, mouseButton);
    }

    protected void energyTooltip(GuiGraphics g, int x, int y, int energy, int capacity, @Nullable Component extra) {
        Component first = Component.translatable("tooltip.voltaicdynamics.gui.energy",
                String.format("%,d", energy), String.format("%,d", capacity)).withStyle(ChatFormatting.AQUA);
        g.renderComponentTooltip(font, extra == null ? List.of(first) : List.of(first, extra), x, y);
    }
}
