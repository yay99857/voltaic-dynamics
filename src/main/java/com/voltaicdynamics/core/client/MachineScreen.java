package com.voltaicdynamics.core.client;

import com.voltaicdynamics.core.menu.MachineMenu;

import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.renderer.Rect2i;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import org.jetbrains.annotations.Nullable;

import java.util.List;

/** Base screen of the Voltaic machines: adds the side configuration tab and an energy gauge tooltip helper. */
public abstract class MachineScreen<M extends MachineMenu> extends AbstractContainerScreen<M> {
    @Nullable
    protected SideConfigPanel sidePanel;

    protected MachineScreen(M menu, Inventory playerInventory, Component title) {
        super(menu, playerInventory, title);
        this.imageWidth = 176;
        this.imageHeight = 166;
        this.inventoryLabelY = 72;
    }

    @Override
    protected void init() {
        super.init();
        sidePanel = SideConfigPanel.create(menu);
        if (sidePanel != null) sidePanel.setPosition(leftPos + imageWidth, topPos);
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
