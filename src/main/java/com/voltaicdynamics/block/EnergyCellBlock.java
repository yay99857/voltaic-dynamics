package com.voltaicdynamics.block;

import com.mojang.serialization.MapCodec;
import com.voltaicdynamics.blockentity.EnergyCellBlockEntity;
import com.voltaicdynamics.blockentity.ModBlockEntities;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
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

public class EnergyCellBlock extends BaseEntityBlock {
    public static final MapCodec<EnergyCellBlock> CODEC = simpleCodec(EnergyCellBlock::new);
    private final boolean isAdvanced;

    public EnergyCellBlock(BlockBehaviour.Properties properties, boolean isAdvanced) {
        super(properties);
        this.isAdvanced = isAdvanced;
    }

    public EnergyCellBlock(BlockBehaviour.Properties properties) {
        this(properties, false);
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
        if (isAdvanced) {
            return new EnergyCellBlockEntity(ModBlockEntities.ADVANCED_ENERGY_CELL.get(), pos, state, 8_000_000, 10_000, true);
        } else {
            return new EnergyCellBlockEntity(ModBlockEntities.BASIC_ENERGY_CELL.get(), pos, state, 1_000_000, 2_000, false);
        }
    }

    @Nullable
    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> blockEntityType) {
        BlockEntityType<EnergyCellBlockEntity> targetType = isAdvanced
                ? ModBlockEntities.ADVANCED_ENERGY_CELL.get()
                : ModBlockEntities.BASIC_ENERGY_CELL.get();

        return createTickerHelper(blockEntityType, targetType, EnergyCellBlockEntity::tick);
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hitResult) {
        if (!level.isClientSide) {
            BlockEntity blockEntity = level.getBlockEntity(pos);
            if (blockEntity instanceof EnergyCellBlockEntity cell && player instanceof ServerPlayer serverPlayer) {
                serverPlayer.openMenu(cell, buf -> buf.writeBlockPos(pos));
            }
        }
        return InteractionResult.sidedSuccess(level.isClientSide);
    }

    @Override
    public void onRemove(BlockState state, Level level, BlockPos pos, BlockState newState, boolean isMoving) {
        if (!state.is(newState.getBlock())) {
            BlockEntity blockEntity = level.getBlockEntity(pos);
            if (blockEntity instanceof EnergyCellBlockEntity cell) {
                cell.drops();
            }
        }
        super.onRemove(state, level, pos, newState, isMoving);
    }

    @Override
    public void appendHoverText(ItemStack stack, Item.TooltipContext context, List<Component> tooltipComponents, TooltipFlag tooltipFlag) {
        if (isAdvanced) {
            tooltipComponents.add(Component.translatable("tooltip.voltaicdynamics.advanced_energy_cell.desc1").withStyle(ChatFormatting.GOLD));
            tooltipComponents.add(Component.translatable("tooltip.voltaicdynamics.advanced_energy_cell.desc2").withStyle(ChatFormatting.GRAY));
        } else {
            tooltipComponents.add(Component.translatable("tooltip.voltaicdynamics.basic_energy_cell.desc1").withStyle(ChatFormatting.AQUA));
            tooltipComponents.add(Component.translatable("tooltip.voltaicdynamics.basic_energy_cell.desc2").withStyle(ChatFormatting.GRAY));
        }
    }
}
