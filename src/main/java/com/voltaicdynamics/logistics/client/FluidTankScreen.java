package com.voltaicdynamics.logistics.client;

import com.voltaicdynamics.VoltaicDynamics;
import com.voltaicdynamics.core.client.FluidRenderHelper;
import com.voltaicdynamics.core.client.MachineScreen;
import com.voltaicdynamics.core.client.layout.ScreenLayout;
import com.voltaicdynamics.logistics.FluidTankMenu;

import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.neoforged.neoforge.fluids.FluidStack;

import java.util.ArrayList;
import java.util.List;

/**
 * Layout: {@code assets/voltaicdynamics/screens/fluid_tank.json}. The {@code gauge} widget is where the fluid is
 * drawn; its texture part (the measuring marks) is drawn over the fluid.
 */
public class FluidTankScreen extends MachineScreen<FluidTankMenu> {
    public FluidTankScreen(FluidTankMenu menu, Inventory playerInventory, Component title) {
        super(menu, playerInventory, title, VoltaicDynamics.id("fluid_tank"));
    }

    @Override
    protected void renderBg(GuiGraphics g, float partialTick, int mouseX, int mouseY) {
        drawBackground(g);
        ScreenLayout.Widget gauge = widget("gauge");
        if (gauge == null) return;
        FluidRenderHelper.drawGauge(g, menu.getFluid(), menu.getAmount(), menu.getCapacity(),
                leftPos + gauge.x(), topPos + gauge.y(), gauge.w(), gauge.h());
        drawWidget(g, "gauge", 1);
    }

    @Override
    protected void renderTooltip(GuiGraphics g, int x, int y) {
        super.renderTooltip(g, x, y);
        if (isHovering("gauge", x, y)) {
            FluidStack fluid = menu.getFluid();
            List<Component> lines = new ArrayList<>();
            lines.add(fluid.isEmpty() || menu.getAmount() == 0
                    ? Component.translatable("tooltip.voltaicdynamics.fluid_tank.empty")
                    : fluid.getHoverName().copy().withStyle(ChatFormatting.AQUA));
            lines.add(menu.isCreative()
                    ? Component.translatable("tooltip.voltaicdynamics.gui.fluid_infinite").withStyle(ChatFormatting.GRAY)
                    : Component.translatable("tooltip.voltaicdynamics.gui.fluid",
                    String.format("%,d", menu.getAmount()), String.format("%,d", menu.getCapacity())).withStyle(ChatFormatting.GRAY));
            g.renderComponentTooltip(font, lines, x, y);
        }
    }
}
