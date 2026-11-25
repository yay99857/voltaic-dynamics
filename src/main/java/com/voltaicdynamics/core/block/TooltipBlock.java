package com.voltaicdynamics.core.block;

import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;

import java.util.function.Consumer;

/**
 * A block whose item shows extra tooltip lines. Blocks no longer get a tooltip hook of their own, so
 * {@link com.voltaicdynamics.core.item.VoltaicBlockItem} asks the block.
 */
public interface TooltipBlock {
    void appendTooltip(ItemStack stack, Item.TooltipContext context, Consumer<Component> tooltip, TooltipFlag flag);
}
