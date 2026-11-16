package com.voltaicdynamics.block;

import com.mojang.serialization.MapCodec;
import com.voltaicdynamics.VoltaicConfig;
import com.voltaicdynamics.blockentity.ModBlockEntities;
import com.voltaicdynamics.blockentity.ThermoelectricGeneratorBlockEntity;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import org.jetbrains.annotations.Nullable;

import java.util.List;

public class ThermoelectricGeneratorBlock extends BaseEntityBlock {
    public static final MapCodec<ThermoelectricGeneratorBlock> CODEC = simpleCodec(ThermoelectricGeneratorBlock::new);

    public ThermoelectricGeneratorBlock(BlockBehaviour.Properties properties) {
        super(properties);
    }

    @Override
    protected MapCodec<? extends BaseEntityBlock> codec() {
        return CODEC;
    }

    @Override
    protected RenderShape getRenderShape(BlockState state) {
        return RenderShape.MODEL;
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new ThermoelectricGeneratorBlockEntity(pos, state);
    }

    @Nullable
    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> blockEntityType) {
        if (level.isClientSide) return null;
        return createTickerHelper(blockEntityType, ModBlockEntities.THERMOELECTRIC_GENERATOR.get(), ThermoelectricGeneratorBlockEntity::tick);
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hitResult) {
        if (!level.isClientSide) {
            BlockEntity blockEntity = level.getBlockEntity(pos);
            if (blockEntity instanceof ThermoelectricGeneratorBlockEntity te) {
                player.displayClientMessage(
                        Component.translatable("chat.voltaicdynamics.thermoelectric.status",
                                te.getLastGenerated(),
                                String.format("%,d", te.getEnergyStored()),
                                String.format("%,d", te.getCapacity()))
                                .withStyle(ChatFormatting.GOLD),
                        true
                );
            }
        }
        return InteractionResult.sidedSuccess(level.isClientSide);
    }

    @Override
    public void appendHoverText(ItemStack stack, Item.TooltipContext context, List<Component> tooltipComponents, TooltipFlag tooltipFlag) {
        tooltipComponents.add(Component.translatable("tooltip.voltaicdynamics.thermoelectric.desc1").withStyle(ChatFormatting.AQUA));
        tooltipComponents.add(Component.translatable("tooltip.voltaicdynamics.thermoelectric.desc2").withStyle(ChatFormatting.GRAY));
        if (VoltaicConfig.get(VoltaicConfig.THERMO_WEAR_CHANCE) > 0) {
            tooltipComponents.add(Component.translatable("tooltip.voltaicdynamics.thermoelectric.desc3").withStyle(ChatFormatting.DARK_GRAY));
        }
    }
}
