package com.voltaicdynamics.power;

import com.voltaicdynamics.core.item.EnergyBarHelper;

import net.minecraft.network.chat.Component;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;

import java.util.List;

/** Accumulator as an item: shows the charge it carries after being broken. */
public class AccumulatorItem extends BlockItem {
    private final AccumulatorBlock cell;

    public AccumulatorItem(AccumulatorBlock block, Properties properties) {
        super(block, properties.stacksTo(1));
        this.cell = block;
    }

    public int getCapacity() {
        return cell.getTier().capacity();
    }

    public int getTransfer() {
        return cell.getTier().transfer();
    }

    public AccumulatorTier getTier() {
        return cell.getTier();
    }

    @Override
    public boolean isBarVisible(ItemStack stack) {
        return getTier().isCreative() || EnergyBarHelper.getEnergy(stack) > 0;
    }

    @Override
    public int getBarWidth(ItemStack stack) {
        return getTier().isCreative() ? 13 : EnergyBarHelper.barWidth(stack, getCapacity());
    }

    @Override
    public int getBarColor(ItemStack stack) {
        return EnergyBarHelper.BAR_COLOR;
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        if (!getTier().isCreative()) EnergyBarHelper.addTooltip(stack, getCapacity(), tooltip, cell.getTier().color());
        super.appendHoverText(stack, context, tooltip, flag);
    }
}
