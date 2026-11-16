package com.voltaicdynamics.logistics;

import com.voltaicdynamics.core.ModDataComponents;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.block.Block;
import net.neoforged.neoforge.fluids.SimpleFluidContent;

import java.util.List;

/** Tank item: shows and keeps the stored fluid (also exposed as an item fluid handler). */
public class FluidTankItem extends BlockItem {
    public FluidTankItem(Block block, Properties properties) {
        super(block, properties);
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        SimpleFluidContent fluid = stack.getOrDefault(ModDataComponents.FLUID.get(), SimpleFluidContent.EMPTY);
        if (fluid.isEmpty()) {
            tooltip.add(Component.translatable("tooltip.voltaicdynamics.fluid_tank.empty").withStyle(ChatFormatting.GRAY));
        } else {
            tooltip.add(Component.translatable("tooltip.voltaicdynamics.fluid_tank.contents", fluid.copy().getHoverName(),
                    String.format("%,d", fluid.getAmount()), String.format("%,d", FluidTankBlockEntity.capacity()))
                    .withStyle(ChatFormatting.AQUA));
        }
        tooltip.add(Component.translatable("tooltip.voltaicdynamics.fluid_tank.desc", String.format("%,d", FluidTankBlockEntity.capacity()))
                .withStyle(ChatFormatting.GRAY));
        tooltip.add(Component.translatable("tooltip.voltaicdynamics.side_config").withStyle(ChatFormatting.DARK_GRAY));
    }
}
