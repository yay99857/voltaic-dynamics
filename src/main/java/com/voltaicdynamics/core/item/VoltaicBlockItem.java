package com.voltaicdynamics.core.item;

import com.voltaicdynamics.core.block.TooltipBlock;

import net.minecraft.network.chat.Component;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.TooltipDisplay;
import net.minecraft.world.level.block.Block;

import java.util.function.Consumer;

/** The item of a Voltaic block; shows the block's {@link TooltipBlock} lines. */
public class VoltaicBlockItem extends BlockItem {
    public VoltaicBlockItem(Block block, Properties properties) {
        super(block, properties);
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, TooltipDisplay display, Consumer<Component> tooltip,
                                TooltipFlag flag) {
        super.appendHoverText(stack, context, display, tooltip, flag);
        if (getBlock() instanceof TooltipBlock block) block.appendTooltip(stack, context, tooltip, flag);
    }
}
