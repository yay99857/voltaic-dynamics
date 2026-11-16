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

/** Crushes ores, raw metals and ingots into dusts with FE ({@code voltaicdynamics:crushing} recipes). */
public class PulverizerBlock extends ProcessingMachineBlock {
    public static final MapCodec<PulverizerBlock> CODEC = simpleCodec(PulverizerBlock::new);

    public PulverizerBlock(BlockBehaviour.Properties properties) {
        super(properties);
    }

    @Override
    protected MapCodec<? extends BaseEntityBlock> codec() {
        return CODEC;
    }

    @Override
    protected Supplier<BlockEntityType<PulverizerBlockEntity>> blockEntityType() {
        return VoltaicMachines.PULVERIZER_BE;
    }

    @Override
    public void appendHoverText(ItemStack stack, Item.TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        tooltip.add(Component.translatable("tooltip.voltaicdynamics.pulverizer.desc1",
                VoltaicConfig.get(VoltaicConfig.PULVERIZER_ENERGY_PER_TICK)).withStyle(ChatFormatting.AQUA));
        tooltip.add(Component.translatable("tooltip.voltaicdynamics.pulverizer.desc2").withStyle(ChatFormatting.GRAY));
        tooltip.add(Component.translatable("tooltip.voltaicdynamics.side_config").withStyle(ChatFormatting.DARK_GRAY));
    }
}
