package com.voltaicdynamics.client;

import com.voltaicdynamics.VoltaicDynamics;
import com.voltaicdynamics.menu.CombustionGeneratorMenu;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;

import java.util.List;

public class CombustionGeneratorScreen extends AbstractContainerScreen<CombustionGeneratorMenu> {
    private static final ResourceLocation TEXTURE =
            ResourceLocation.fromNamespaceAndPath(VoltaicDynamics.MOD_ID, "textures/gui/combustion_generator_gui.png");

    public CombustionGeneratorScreen(CombustionGeneratorMenu menu, Inventory playerInventory, Component title) {
        super(menu, playerInventory, title);
        this.imageWidth = 176;
        this.imageHeight = 166;
        this.inventoryLabelY = 72;
    }

    @Override
    protected void renderBg(GuiGraphics guiGraphics, float partialTick, int mouseX, int mouseY) {
        int x = (this.width - this.imageWidth) / 2;
        int y = (this.height - this.imageHeight) / 2;

        // Fundo principal da GUI
        guiGraphics.blit(TEXTURE, x, y, 0, 0, this.imageWidth, this.imageHeight);

        // Ícone de Chama Dinâmica (se estiver queimando)
        if (this.menu.isBurning()) {
            int burnHeight = this.menu.getBurnScaled(14);
            guiGraphics.blit(TEXTURE, x + 80, y + 54 + 14 - burnHeight, 176, 14 - burnHeight, 14, burnHeight);
        }

        // Barra de Energia Dinâmica (Gauge)
        int energyHeight = this.menu.getEnergyScaled(52);
        if (energyHeight > 0) {
            guiGraphics.blit(TEXTURE, x + 152, y + 18 + 52 - energyHeight, 176, 18 + 52 - energyHeight, 16, energyHeight);
        }
    }

    @Override
    public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        super.render(guiGraphics, mouseX, mouseY, partialTick);
        renderTooltip(guiGraphics, mouseX, mouseY);
    }

    @Override
    protected void renderTooltip(GuiGraphics guiGraphics, int x, int y) {
        super.renderTooltip(guiGraphics, x, y);

        // Tooltip interativa na Barra de Energia
        if (isHovering(152, 18, 16, 52, x, y)) {
            List<Component> tooltip = List.of(
                    Component.translatable("tooltip.voltaicdynamics.gui.energy",
                            String.format("%,d", this.menu.getEnergy()),
                            String.format("%,d", this.menu.getMaxEnergy())).withStyle(ChatFormatting.AQUA),
                    Component.translatable("tooltip.voltaicdynamics.gui.rate",
                            this.menu.getGenerationRate()).withStyle(ChatFormatting.GOLD)
            );
            guiGraphics.renderComponentTooltip(this.font, tooltip, x, y);
        }
    }
}
