package com.voltaicdynamics.item;

import com.voltaicdynamics.block.EnergyCellBlock;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;

import java.util.List;

/** Energy Cell as an item: shows the charge it carries after being broken. */
public class EnergyCellItem extends BlockItem {
    private final EnergyCellBlock cell;

    public EnergyCellItem(EnergyCellBlock block, Properties properties) {
        super(block, properties.stacksTo(1));
        this.cell = block;
    }

    public int getCapacity() {
        return cell.getTier().capacity();
    }

    public int getTransfer() {
        return cell.getTier().transfer();
    }

    @Override
    public boolean isBarVisible(ItemStack stack) {
        return EnergyBarHelper.getEnergy(stack) > 0;
    }

    @Override
    public int getBarWidth(ItemStack stack) {
        return EnergyBarHelper.barWidth(stack, getCapacity());
    }

    @Override
    public int getBarColor(ItemStack stack) {
        return EnergyBarHelper.BAR_COLOR;
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        EnergyBarHelper.addTooltip(stack, getCapacity(), tooltip, cell.getTier().color());
        super.appendHoverText(stack, context, tooltip, flag);
    }
}
