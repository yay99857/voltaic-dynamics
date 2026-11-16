package com.voltaicdynamics.logistics.client;

import com.voltaicdynamics.VoltaicDynamics;
import com.voltaicdynamics.core.client.FluidRenderHelper;
import com.voltaicdynamics.core.client.MachineScreen;
import com.voltaicdynamics.logistics.FluidTankMenu;

import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;
import net.neoforged.neoforge.fluids.FluidStack;

import java.util.ArrayList;
import java.util.List;

/** Tank GUI: big gauge with measuring marks (overlay at 176,0), container slots on the left. */
public class FluidTankScreen extends MachineScreen<FluidTankMenu> {
    private static final ResourceLocation TEXTURE =
            ResourceLocation.fromNamespaceAndPath(VoltaicDynamics.MOD_ID, "textures/gui/fluid_tank_gui.png");
    public static final int GAUGE_X = 80;
    public static final int GAUGE_Y = 17;
    public static final int GAUGE_W = 52;
    public static final int GAUGE_H = 54;

    public FluidTankScreen(FluidTankMenu menu, Inventory playerInventory, Component title) {
        super(menu, playerInventory, title);
    }

    @Override
    protected void renderBg(GuiGraphics g, float partialTick, int mouseX, int mouseY) {
        g.blit(TEXTURE, leftPos, topPos, 0, 0, imageWidth, imageHeight);
        FluidRenderHelper.drawGauge(g, menu.getFluid(), menu.getAmount(), menu.getCapacity(),
                leftPos + GAUGE_X, topPos + GAUGE_Y, GAUGE_W, GAUGE_H);
        g.blit(TEXTURE, leftPos + GAUGE_X, topPos + GAUGE_Y, 176, 0, GAUGE_W, GAUGE_H);
    }

    @Override
    protected void renderTooltip(GuiGraphics g, int x, int y) {
        super.renderTooltip(g, x, y);
        if (isHovering(GAUGE_X, GAUGE_Y, GAUGE_W, GAUGE_H, x, y)) {
            FluidStack fluid = menu.getFluid();
            List<Component> lines = new ArrayList<>();
            lines.add(fluid.isEmpty() || menu.getAmount() == 0
                    ? Component.translatable("tooltip.voltaicdynamics.fluid_tank.empty")
                    : fluid.getHoverName().copy().withStyle(ChatFormatting.AQUA));
            lines.add(Component.translatable("tooltip.voltaicdynamics.gui.fluid",
                    String.format("%,d", menu.getAmount()), String.format("%,d", menu.getCapacity())).withStyle(ChatFormatting.GRAY));
            g.renderComponentTooltip(font, lines, x, y);
        }
    }
}
