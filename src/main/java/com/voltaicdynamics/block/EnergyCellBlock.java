package com.voltaicdynamics.block;

import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import com.voltaicdynamics.blockentity.EnergyCellBlockEntity;
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
    public static final MapCodec<EnergyCellBlock> CODEC = RecordCodecBuilder.mapCodec(instance -> instance.group(
            EnergyCellTier.CODEC.fieldOf("tier").forGetter(EnergyCellBlock::getTier),
            propertiesCodec()
    ).apply(instance, (tier, properties) -> new EnergyCellBlock(properties, tier)));

    private final EnergyCellTier tier;

    public EnergyCellBlock(BlockBehaviour.Properties properties, EnergyCellTier tier) {
        super(properties);
        this.tier = tier;
    }

    public EnergyCellTier getTier() {
        return tier;
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
        return new EnergyCellBlockEntity(pos, state);
    }

    @Nullable
    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> blockEntityType) {
        if (level.isClientSide) return null;
        return createTickerHelper(blockEntityType, EnergyCellBlockEntity.typeFor(tier), EnergyCellBlockEntity::tick);
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hitResult) {
        if (!level.isClientSide && level.getBlockEntity(pos) instanceof EnergyCellBlockEntity cell
                && player instanceof ServerPlayer serverPlayer) {
            serverPlayer.openMenu(cell, buf -> buf.writeBlockPos(pos));
        }
        return InteractionResult.sidedSuccess(level.isClientSide);
    }

    @Override
    protected void onRemove(BlockState state, Level level, BlockPos pos, BlockState newState, boolean isMoving) {
        if (!state.is(newState.getBlock()) && level.getBlockEntity(pos) instanceof EnergyCellBlockEntity cell) {
            cell.drops();
        }
        super.onRemove(state, level, pos, newState, isMoving);
    }

    @Override
    protected boolean hasAnalogOutputSignal(BlockState state) {
        return true;
    }

    @Override
    protected int getAnalogOutputSignal(BlockState state, Level level, BlockPos pos) {
        if (level.getBlockEntity(pos) instanceof EnergyCellBlockEntity cell) {
            return cell.getComparatorSignal();
        }
        return 0;
    }

    @Override
    public void appendHoverText(ItemStack stack, Item.TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        tooltip.add(Component.translatable("tooltip.voltaicdynamics.energy_cell.capacity",
                String.format("%,d", tier.capacity())).withStyle(tier.color()));
        tooltip.add(Component.translatable("tooltip.voltaicdynamics.energy_cell.transfer",
                String.format("%,d", tier.transfer())).withStyle(ChatFormatting.GRAY));
        tooltip.add(Component.translatable("tooltip.voltaicdynamics.energy_cell.keeps").withStyle(ChatFormatting.DARK_GRAY));
    }
}
