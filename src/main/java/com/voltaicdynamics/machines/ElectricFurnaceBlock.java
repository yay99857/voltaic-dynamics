package com.voltaicdynamics.machines;

import com.voltaicdynamics.VoltaicConfig;

import com.mojang.serialization.MapCodec;
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
public class ElectricFurnaceBlock extends ProcessingMachineBlock {
    public static final MapCodec<ElectricFurnaceBlock> CODEC = simpleCodec(ElectricFurnaceBlock::new);

    public ElectricFurnaceBlock(BlockBehaviour.Properties properties) {
        super(properties);
    }

    @Override
    protected MapCodec<? extends BaseEntityBlock> codec() {
        return CODEC;
    }

    @Override
    protected Supplier<BlockEntityType<ElectricFurnaceBlockEntity>> blockEntityType() {
        return VoltaicMachines.ELECTRIC_FURNACE_BE;
    }

    @Override
    public void appendHoverText(ItemStack stack, Item.TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        tooltip.add(Component.translatable("tooltip.voltaicdynamics.electric_furnace.desc1",
                VoltaicConfig.get(VoltaicConfig.FURNACE_ENERGY_PER_TICK)).withStyle(ChatFormatting.AQUA));
        tooltip.add(Component.translatable("tooltip.voltaicdynamics.electric_furnace.desc2",
                String.format("%.1f", VoltaicConfig.get(VoltaicConfig.FURNACE_TICKS_PER_ITEM) / 20.0)).withStyle(ChatFormatting.GRAY));
        tooltip.add(Component.translatable("tooltip.voltaicdynamics.side_config").withStyle(ChatFormatting.DARK_GRAY));
    }
}
