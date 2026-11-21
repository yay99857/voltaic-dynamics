package com.voltaicdynamics.logistics;

import com.voltaicdynamics.core.ModDataComponents;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.neoforged.neoforge.fluids.SimpleFluidContent;

import java.util.List;

/** Tank item: shows and keeps the stored fluid (also exposed as an item fluid handler). */
public class FluidTankItem extends BlockItem {
    private final FluidTankTier tier;

    public FluidTankItem(FluidTankBlock block, Properties properties) {
        super(block, properties);
        this.tier = block.getTier();
    }

    public FluidTankTier getTier() {
        return tier;
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        SimpleFluidContent fluid = stack.getOrDefault(ModDataComponents.FLUID.get(), SimpleFluidContent.EMPTY);
        String capacity = String.format("%,d", tier.capacity());
        if (tier.isCreative()) {
            tooltip.add(fluid.isEmpty()
                    ? Component.translatable("tooltip.voltaicdynamics.fluid_tank.empty").withStyle(ChatFormatting.GRAY)
                    : Component.translatable("tooltip.voltaicdynamics.creative_tank.contents", fluid.copy().getHoverName())
                    .withStyle(tier.color()));
            tooltip.add(Component.translatable("tooltip.voltaicdynamics.creative_tank.desc").withStyle(ChatFormatting.GRAY));
            tooltip.add(Component.translatable("tooltip.voltaicdynamics.side_config").withStyle(ChatFormatting.DARK_GRAY));
            return;
        }
        if (fluid.isEmpty()) {
            tooltip.add(Component.translatable("tooltip.voltaicdynamics.fluid_tank.empty").withStyle(ChatFormatting.GRAY));
        } else {
            tooltip.add(Component.translatable("tooltip.voltaicdynamics.fluid_tank.contents", fluid.copy().getHoverName(),
                    String.format("%,d", fluid.getAmount()), capacity)
                    .withStyle(tier.color()));
        }
        tooltip.add(Component.translatable("tooltip.voltaicdynamics.fluid_tank.desc", capacity)
                .withStyle(ChatFormatting.GRAY));
        tooltip.add(Component.translatable("tooltip.voltaicdynamics.side_config").withStyle(ChatFormatting.DARK_GRAY));
    }
}
