package com.voltaicdynamics.machines;

import java.util.function.Consumer;
import com.voltaicdynamics.core.block.TooltipBlock;
import com.voltaicdynamics.VoltaicConfig;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockBehaviour;

import java.util.List;
import java.util.function.Supplier;

/** First Voltaic machine that consumes FE: smelts any vanilla furnace recipe. */
public class ElectricFurnaceBlock extends ProcessingMachineBlock implements TooltipBlock {

    public ElectricFurnaceBlock(BlockBehaviour.Properties properties) {
        super(properties);
    }


    @Override
    protected Supplier<BlockEntityType<ElectricFurnaceBlockEntity>> blockEntityType() {
        return VoltaicMachines.ELECTRIC_FURNACE_BE;
    }

    @Override
    public void appendTooltip(ItemStack stack, Item.TooltipContext context, Consumer<Component> tooltip, TooltipFlag flag) {
        tooltip.accept(Component.translatable("tooltip.voltaicdynamics.electric_furnace.desc1",
                VoltaicConfig.get(VoltaicConfig.FURNACE_ENERGY_PER_TICK)).withStyle(ChatFormatting.AQUA));
        tooltip.accept(Component.translatable("tooltip.voltaicdynamics.electric_furnace.desc2",
                String.format("%.1f", VoltaicConfig.get(VoltaicConfig.FURNACE_TICKS_PER_ITEM) / 20.0)).withStyle(ChatFormatting.GRAY));
        tooltip.accept(Component.translatable("tooltip.voltaicdynamics.side_config").withStyle(ChatFormatting.DARK_GRAY));
    }
}
