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

/** Combines up to three ingredients into alloys with FE ({@code voltaicdynamics:alloying} recipes). */
public class AlloySmelterBlock extends ProcessingMachineBlock implements TooltipBlock {

    public AlloySmelterBlock(BlockBehaviour.Properties properties) {
        super(properties);
    }


    @Override
    protected Supplier<BlockEntityType<AlloySmelterBlockEntity>> blockEntityType() {
        return VoltaicMachines.ALLOY_SMELTER_BE;
    }

    @Override
    public void appendTooltip(ItemStack stack, Item.TooltipContext context, Consumer<Component> tooltip, TooltipFlag flag) {
        tooltip.accept(Component.translatable("tooltip.voltaicdynamics.alloy_smelter.desc1",
                VoltaicConfig.get(VoltaicConfig.ALLOY_ENERGY_PER_TICK)).withStyle(ChatFormatting.AQUA));
        tooltip.accept(Component.translatable("tooltip.voltaicdynamics.alloy_smelter.desc2").withStyle(ChatFormatting.GRAY));
        tooltip.accept(Component.translatable("tooltip.voltaicdynamics.side_config").withStyle(ChatFormatting.DARK_GRAY));
    }
}
