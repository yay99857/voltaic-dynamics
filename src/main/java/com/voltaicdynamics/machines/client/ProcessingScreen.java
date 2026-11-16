package com.voltaicdynamics.machines.client;

import com.voltaicdynamics.core.client.MachineScreen;
import com.voltaicdynamics.machines.ProcessingMenu;

import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;

/**
 * GUI of an FE processing machine. The texture holds the background (176x166), the filled arrow at (176,0)
 * and the filled energy gauge at (176,18).
 */
public abstract class ProcessingScreen<M extends ProcessingMenu> extends MachineScreen<M> {
    private final ResourceLocation texture;
    private final int arrowX;
    private final int arrowY;

    protected ProcessingScreen(M menu, Inventory playerInventory, Component title, ResourceLocation texture, int arrowX, int arrowY) {
        super(menu, playerInventory, title);
        this.texture = texture;
        this.arrowX = arrowX;
        this.arrowY = arrowY;
    }

    public int arrowX() {
        return arrowX;
    }

    public int arrowY() {
        return arrowY;
    }

    @Override
    protected void renderBg(GuiGraphics guiGraphics, float partialTick, int mouseX, int mouseY) {
        int x = leftPos;
        int y = topPos;
        guiGraphics.blit(texture, x, y, 0, 0, imageWidth, imageHeight);

        int progress = menu.getProgressScaled(24);
        if (progress > 0) {
            guiGraphics.blit(texture, x + arrowX, y + arrowY, 176, 0, progress, 17);
        }

        int energyHeight = menu.getEnergyScaled(52);
        if (energyHeight > 0) {
            guiGraphics.blit(texture, x + 152, y + 18 + 52 - energyHeight, 176, 18 + 52 - energyHeight, 16, energyHeight);
        }
    }

    @Override
    protected void renderTooltip(GuiGraphics guiGraphics, int x, int y) {
        super.renderTooltip(guiGraphics, x, y);
        if (isHovering(152, 18, 16, 52, x, y)) {
            energyTooltip(guiGraphics, x, y, menu.getEnergy(), menu.getCapacity(),
                    Component.translatable("tooltip.voltaicdynamics.gui.usage", menu.getUsage()).withStyle(ChatFormatting.GOLD));
        }
    }
}
