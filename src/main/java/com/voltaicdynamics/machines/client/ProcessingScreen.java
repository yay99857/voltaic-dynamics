package com.voltaicdynamics.machines.client;

import com.voltaicdynamics.core.client.MachineScreen;
import com.voltaicdynamics.machines.ProcessingMenu;

import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;

/** GUI of an FE processing machine; the layout has a {@code progress} arrow and an {@code energy} gauge. */
public abstract class ProcessingScreen<M extends ProcessingMenu> extends MachineScreen<M> {
    protected ProcessingScreen(M menu, Inventory playerInventory, Component title, ResourceLocation layoutId) {
        super(menu, playerInventory, title, layoutId);
    }

    @Override
    protected void renderBg(GuiGraphics g, float partialTick, int mouseX, int mouseY) {
        drawBackground(g);
        drawWidget(g, "progress", menu.getProgressFraction());
        drawWidget(g, "energy", fraction(menu.getEnergy(), menu.getCapacity()));
    }

    @Override
    protected void renderTooltip(GuiGraphics g, int x, int y) {
        super.renderTooltip(g, x, y);
        if (isHovering("energy", x, y)) {
            energyTooltip(g, x, y, menu.getEnergy(), menu.getCapacity(),
                    Component.translatable("tooltip.voltaicdynamics.gui.usage", menu.getUsage()).withStyle(ChatFormatting.GOLD));
        }
    }
}
