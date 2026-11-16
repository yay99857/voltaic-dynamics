package com.voltaicdynamics.client;

import com.voltaicdynamics.VoltaicDynamics;
import com.voltaicdynamics.menu.ElectricFurnaceMenu;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;

import java.util.List;

public class ElectricFurnaceScreen extends AbstractContainerScreen<ElectricFurnaceMenu> {
    private static final ResourceLocation TEXTURE =
            ResourceLocation.fromNamespaceAndPath(VoltaicDynamics.MOD_ID, "textures/gui/electric_furnace_gui.png");

    public ElectricFurnaceScreen(ElectricFurnaceMenu menu, Inventory playerInventory, Component title) {
        super(menu, playerInventory, title);
        this.imageWidth = 176;
        this.imageHeight = 166;
        this.inventoryLabelY = 72;
    }

    @Override
    protected void renderBg(GuiGraphics guiGraphics, float partialTick, int mouseX, int mouseY) {
        int x = leftPos;
        int y = topPos;
        guiGraphics.blit(TEXTURE, x, y, 0, 0, imageWidth, imageHeight);

        // Progress arrow (24x17 at 79,34; filled sprite at 176,0)
        int progress = menu.getProgressScaled(24);
        if (progress > 0) {
            guiGraphics.blit(TEXTURE, x + 79, y + 34, 176, 0, progress, 17);
        }

        // Energy gauge (16x52 at 152,18; filled sprite at 176,18)
        int energyHeight = menu.getEnergyScaled(52);
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
        if (isHovering(152, 18, 16, 52, x, y)) {
            List<Component> tooltip = List.of(
                    Component.translatable("tooltip.voltaicdynamics.gui.energy",
                            String.format("%,d", menu.getEnergy()),
                            String.format("%,d", menu.getCapacity())).withStyle(ChatFormatting.AQUA),
                    Component.translatable("tooltip.voltaicdynamics.gui.usage", menu.getUsage()).withStyle(ChatFormatting.GOLD));
            guiGraphics.renderComponentTooltip(font, tooltip, x, y);
        }
    }
}
