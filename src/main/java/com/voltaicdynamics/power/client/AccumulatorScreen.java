package com.voltaicdynamics.power.client;

import com.voltaicdynamics.VoltaicDynamics;
import com.voltaicdynamics.core.client.MachineScreen;
import com.voltaicdynamics.core.client.layout.ScreenLayout;
import com.voltaicdynamics.power.AccumulatorMenu;

import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;

import java.util.List;

/**
 * Layout: {@code assets/voltaicdynamics/screens/accumulator.json}. The Advanced Accumulator draws its gauge from
 * the {@code energy_advanced} widget (same place, gold texture).
 */
public class AccumulatorScreen extends MachineScreen<AccumulatorMenu> {
    public AccumulatorScreen(AccumulatorMenu menu, Inventory playerInventory, Component title) {
        super(menu, playerInventory, title, VoltaicDynamics.id("accumulator"));
    }

    private String gauge() {
        return menu.isAdvanced() && widget("energy_advanced") != null ? "energy_advanced" : "energy";
    }

    @Override
    protected void renderBg(GuiGraphics g, float partialTick, int mouseX, int mouseY) {
        drawBackground(g);
        drawWidget(g, gauge(), fraction(menu.getEnergy(), menu.getCapacity()));
    }

    @Override
    protected void renderTooltip(GuiGraphics g, int x, int y) {
        super.renderTooltip(g, x, y);
        ScreenLayout.Widget gauge = widget(gauge());
        if (isHovering(gauge, x, y)) {
            ChatFormatting color = menu.isAdvanced() ? ChatFormatting.GOLD : ChatFormatting.AQUA;
            g.renderComponentTooltip(font, List.of(
                    Component.translatable("tooltip.voltaicdynamics.gui.energy",
                            String.format("%,d", menu.getEnergy()), String.format("%,d", menu.getCapacity())).withStyle(color),
                    Component.translatable("tooltip.voltaicdynamics.gui.io_rate",
                            String.format("%,d", menu.getMaxTransfer())).withStyle(ChatFormatting.GRAY)), x, y);
        }
    }
}
