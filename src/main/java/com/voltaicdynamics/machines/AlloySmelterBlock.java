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

/** Combines up to three ingredients into alloys with FE ({@code voltaicdynamics:alloying} recipes). */
public class AlloySmelterBlock extends ProcessingMachineBlock {
    public static final MapCodec<AlloySmelterBlock> CODEC = simpleCodec(AlloySmelterBlock::new);

    public AlloySmelterBlock(BlockBehaviour.Properties properties) {
        super(properties);
    }

    @Override
    protected MapCodec<? extends BaseEntityBlock> codec() {
        return CODEC;
    }

    @Override
    protected Supplier<BlockEntityType<AlloySmelterBlockEntity>> blockEntityType() {
        return VoltaicMachines.ALLOY_SMELTER_BE;
    }

    @Override
    public void appendHoverText(ItemStack stack, Item.TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        tooltip.add(Component.translatable("tooltip.voltaicdynamics.alloy_smelter.desc1",
                VoltaicConfig.get(VoltaicConfig.ALLOY_ENERGY_PER_TICK)).withStyle(ChatFormatting.AQUA));
        tooltip.add(Component.translatable("tooltip.voltaicdynamics.alloy_smelter.desc2").withStyle(ChatFormatting.GRAY));
        tooltip.add(Component.translatable("tooltip.voltaicdynamics.side_config").withStyle(ChatFormatting.DARK_GRAY));
    }
}
