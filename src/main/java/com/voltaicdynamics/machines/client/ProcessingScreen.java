package com.voltaicdynamics.machines.client;

import com.voltaicdynamics.core.client.MachineScreen;
import com.voltaicdynamics.machines.MachineTier;
import com.voltaicdynamics.machines.ProcessingMenu;
import com.voltaicdynamics.machines.TierInstallerItem;

import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.player.Inventory;

/** GUI of an FE processing machine; the layout has a {@code progress} arrow and an {@code energy} gauge. */
public abstract class ProcessingScreen<M extends ProcessingMenu> extends MachineScreen<M> {
    protected ProcessingScreen(M menu, Inventory playerInventory, Component title, Identifier layoutId) {
        super(menu, playerInventory, title, layoutId);
    }

    @Override
    public void extractBackground(GuiGraphicsExtractor g, int mouseX, int mouseY, float partialTick) {
        super.extractBackground(g, mouseX, mouseY, partialTick);
        drawBackground(g);
        drawWidget(g, "progress", menu.getProgressFraction());
        drawWidget(g, "energy", fraction(menu.getEnergy(), menu.getCapacity()));
        // Tier badge in the top right corner: how many items it processes at once.
        MachineTier tier = menu.getTier();
        if (tier != MachineTier.BASIC) {
            Component badge = Component.literal("\u00d7" + tier.parallel()).withStyle(tier.color());
            g.text(font, badge, leftPos + imageWidth - 8 - font.width(badge), topPos + 6, 0xFFFFFFFF, false);
        }
    }

    @Override
    protected void extractTooltip(GuiGraphicsExtractor g, int x, int y) {
        super.extractTooltip(g, x, y);
        Component badge = Component.literal("\u00d7" + menu.getTier().parallel());
        int bx = leftPos + imageWidth - 8 - font.width(badge);
        if (menu.getTier() != MachineTier.BASIC && x >= bx && x < leftPos + imageWidth - 8 && y >= topPos + 5 && y < topPos + 15) {
            g.setComponentTooltipForNextFrame(font, java.util.List.of(
                    TierInstallerItem.tierName(menu.getTier()).copy().withStyle(menu.getTier().color()),
                    Component.translatable("tooltip.voltaicdynamics.tier_installer", menu.getTier().parallel())
                            .withStyle(ChatFormatting.GRAY)), x, y);
        }
        if (isHovering("energy", x, y)) {
            energyTooltip(g, x, y, menu.getEnergy(), menu.getCapacity(),
                    Component.translatable("tooltip.voltaicdynamics.gui.usage", menu.getUsage()).withStyle(ChatFormatting.GOLD));
        }
    }
}
