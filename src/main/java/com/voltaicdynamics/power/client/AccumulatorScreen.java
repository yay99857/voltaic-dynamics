package com.voltaicdynamics.power.client;

import com.voltaicdynamics.VoltaicDynamics;
import com.voltaicdynamics.core.client.MachineScreen;
import com.voltaicdynamics.power.AccumulatorMenu;

import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;

import java.util.List;

public class AccumulatorScreen extends MachineScreen<AccumulatorMenu> {
    private static final ResourceLocation TEXTURE =
            ResourceLocation.fromNamespaceAndPath(VoltaicDynamics.MOD_ID, "textures/gui/accumulator_gui.png");

    public AccumulatorScreen(AccumulatorMenu menu, Inventory playerInventory, Component title) {
        super(menu, playerInventory, title);
        this.imageWidth = 176;
        this.imageHeight = 166;
        this.inventoryLabelY = 72;
    }

    @Override
    protected void renderBg(GuiGraphics guiGraphics, float partialTick, int mouseX, int mouseY) {
        int x = (this.width - this.imageWidth) / 2;
        int y = (this.height - this.imageHeight) / 2;

        guiGraphics.blit(TEXTURE, x, y, 0, 0, this.imageWidth, this.imageHeight);

        // Barra de Energia Dinâmica
        int energyHeight = this.menu.getEnergyScaled(52);
        if (energyHeight > 0) {
            int uOffset = this.menu.isAdvanced() ? 196 : 176;
            guiGraphics.blit(TEXTURE, x + 40, y + 18 + 52 - energyHeight, uOffset, 52 - energyHeight, 16, energyHeight);
        }
    }

    @Override
    protected void renderTooltip(GuiGraphics guiGraphics, int x, int y) {
        super.renderTooltip(guiGraphics, x, y);

        // Tooltip na Barra de Energia
        if (isHovering(40, 18, 16, 52, x, y)) {
            ChatFormatting color = this.menu.isAdvanced() ? ChatFormatting.GOLD : ChatFormatting.AQUA;
            List<Component> tooltip = List.of(
                    Component.translatable("tooltip.voltaicdynamics.gui.energy",
                            String.format("%,d", this.menu.getEnergy()),
                            String.format("%,d", this.menu.getCapacity())).withStyle(color),
                    Component.translatable("tooltip.voltaicdynamics.gui.io_rate",
                            String.format("%,d", this.menu.getMaxTransfer())).withStyle(ChatFormatting.GRAY)
            );
            guiGraphics.renderComponentTooltip(this.font, tooltip, x, y);
        }
    }
}
