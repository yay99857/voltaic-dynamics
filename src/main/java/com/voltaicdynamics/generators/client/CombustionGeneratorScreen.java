package com.voltaicdynamics.generators.client;

import com.voltaicdynamics.VoltaicDynamics;
import com.voltaicdynamics.core.client.MachineScreen;
import com.voltaicdynamics.generators.CombustionGeneratorMenu;

import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;

/** Layout: {@code assets/voltaicdynamics/screens/combustion_generator.json} ({@code burn} flame, {@code energy} gauge). */
public class CombustionGeneratorScreen extends MachineScreen<CombustionGeneratorMenu> {
    public CombustionGeneratorScreen(CombustionGeneratorMenu menu, Inventory playerInventory, Component title) {
        super(menu, playerInventory, title, VoltaicDynamics.id("combustion_generator"));
    }

    @Override
    protected void renderBg(GuiGraphics g, float partialTick, int mouseX, int mouseY) {
        drawBackground(g);
        if (menu.isBurning()) drawWidget(g, "burn", menu.getBurnFraction());
        drawWidget(g, "energy", fraction(menu.getEnergy(), menu.getMaxEnergy()));
    }

    @Override
    protected void renderTooltip(GuiGraphics g, int x, int y) {
        super.renderTooltip(g, x, y);
        if (isHovering("energy", x, y)) {
            energyTooltip(g, x, y, menu.getEnergy(), menu.getMaxEnergy(),
                    Component.translatable("tooltip.voltaicdynamics.gui.rate", menu.getGenerationRate()).withStyle(ChatFormatting.GOLD));
        }
    }
}
