package com.voltaicdynamics.power;

import net.minecraft.core.Direction;
import java.util.function.Consumer;
import com.voltaicdynamics.core.block.TooltipBlock;
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

public class AccumulatorBlock extends BaseEntityBlock implements TooltipBlock {

    private final AccumulatorTier tier;

    public AccumulatorBlock(BlockBehaviour.Properties properties, AccumulatorTier tier) {
        super(properties);
        this.tier = tier;
    }

    public AccumulatorTier getTier() {
        return tier;
    }


    @Override
    protected RenderShape getRenderShape(BlockState state) {
        return RenderShape.MODEL;
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new AccumulatorBlockEntity(pos, state);
    }

    @Nullable
    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> blockEntityType) {
        if (level.isClientSide()) return null;
        return createTickerHelper(blockEntityType, AccumulatorBlockEntity.typeFor(tier), AccumulatorBlockEntity::tick);
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hitResult) {
        if (!level.isClientSide() && level.getBlockEntity(pos) instanceof AccumulatorBlockEntity cell
                && player instanceof ServerPlayer serverPlayer) {
            serverPlayer.openMenu(cell, buf -> buf.writeBlockPos(pos));
        }
        return InteractionResult.SUCCESS;
    }


    @Override
    protected boolean hasAnalogOutputSignal(BlockState state) {
        return true;
    }

    @Override
    protected int getAnalogOutputSignal(BlockState state, Level level, BlockPos pos, Direction direction) {
        if (level.getBlockEntity(pos) instanceof AccumulatorBlockEntity cell) {
            return cell.getComparatorSignal();
        }
        return 0;
    }

    @Override
    public void appendTooltip(ItemStack stack, Item.TooltipContext context, Consumer<Component> tooltip, TooltipFlag flag) {
        if (tier.isCreative()) {
            tooltip.accept(Component.translatable("tooltip.voltaicdynamics.accumulator.creative",
                    String.format("%,d", tier.transfer())).withStyle(tier.color()));
            return;
        }
        tooltip.accept(Component.translatable("tooltip.voltaicdynamics.accumulator.capacity",
                String.format("%,d", tier.capacity())).withStyle(tier.color()));
        tooltip.accept(Component.translatable("tooltip.voltaicdynamics.accumulator.transfer",
                String.format("%,d", tier.transfer())).withStyle(ChatFormatting.GRAY));
        tooltip.accept(Component.translatable("tooltip.voltaicdynamics.accumulator.keeps").withStyle(ChatFormatting.DARK_GRAY));
    }
}
