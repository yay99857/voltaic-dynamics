package com.voltaicdynamics.generators.client;

import com.voltaicdynamics.VoltaicDynamics;
import com.voltaicdynamics.core.client.FluidRenderHelper;
import com.voltaicdynamics.core.client.MachineScreen;
import com.voltaicdynamics.core.client.layout.ScreenLayout;
import com.voltaicdynamics.generators.FluidGeneratorMenu;

import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.neoforged.neoforge.fluids.FluidStack;

import java.util.ArrayList;
import java.util.List;

/** Layout: {@code assets/voltaicdynamics/screens/fluid_generator.json} ({@code gauge} for fuel, {@code energy}). */
public class FluidGeneratorScreen extends MachineScreen<FluidGeneratorMenu> {
    public FluidGeneratorScreen(FluidGeneratorMenu menu, Inventory playerInventory, Component title) {
        super(menu, playerInventory, title, VoltaicDynamics.id("fluid_generator"));
    }

    @Override
    protected void renderBg(GuiGraphics g, float partialTick, int mouseX, int mouseY) {
        drawBackground(g);
        ScreenLayout.Widget gauge = widget("gauge");
        if (gauge != null) {
            FluidStack fluid = menu.getFluid();
            FluidRenderHelper.drawGauge(g, fluid, fluid.getAmount(), menu.getTankCapacity(),
                    leftPos + gauge.x(), topPos + gauge.y(), gauge.w(), gauge.h());
            drawWidget(g, "gauge", 1);
        }
        drawWidget(g, "energy", fraction(menu.getEnergy(), menu.getCapacity()));
    }

    @Override
    protected void renderTooltip(GuiGraphics g, int x, int y) {
        super.renderTooltip(g, x, y);
        if (isHovering("gauge", x, y)) {
            FluidStack fluid = menu.getFluid();
            List<Component> lines = new ArrayList<>();
            lines.add(fluid.isEmpty()
                    ? Component.translatable("tooltip.voltaicdynamics.fluid_generator.no_fuel")
                    : fluid.getHoverName().copy().withStyle(ChatFormatting.LIGHT_PURPLE));
            lines.add(Component.translatable("tooltip.voltaicdynamics.gui.fluid",
                    String.format("%,d", fluid.getAmount()), String.format("%,d", menu.getTankCapacity())).withStyle(ChatFormatting.GRAY));
            g.renderComponentTooltip(font, lines, x, y);
        } else if (isHovering("energy", x, y)) {
            energyTooltip(g, x, y, menu.getEnergy(), menu.getCapacity(),
                    Component.translatable("tooltip.voltaicdynamics.gui.rate", menu.getRate()).withStyle(ChatFormatting.GOLD));
        }
    }
}
