package com.voltaicdynamics.logistics;

import com.voltaicdynamics.core.transfer.Transfers;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.neoforged.neoforge.transfer.fluid.FluidUtil;
import org.jetbrains.annotations.Nullable;

public class FluidTankBlock extends BaseEntityBlock {
    private static final VoxelShape SHAPE = Block.box(1, 0, 1, 15, 16, 15);

    private final FluidTankTier tier;

    public FluidTankBlock(BlockBehaviour.Properties properties, FluidTankTier tier) {
        super(properties);
        this.tier = tier;
    }

    public FluidTankTier getTier() {
        return tier;
    }


    @Override
    protected RenderShape getRenderShape(BlockState state) {
        return RenderShape.MODEL;
    }

    @Override
    protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return SHAPE;
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new FluidTankBlockEntity(pos, state);
    }

    @Nullable
    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
        if (level.isClientSide()) return null;
        return createTickerHelper(type, VoltaicLogistics.FLUID_TANK_BE.get(), FluidTankBlockEntity::tick);
    }

    /** Buckets and other fluid containers fill or empty the tank directly. */
    @Override
    protected InteractionResult useItemOn(ItemStack stack, BlockState state, Level level, BlockPos pos, Player player,
                                              InteractionHand hand, BlockHitResult hit) {
        if (!Transfers.isFluidContainer(stack)) {
            return InteractionResult.TRY_WITH_EMPTY_HAND;
        }
        if (FluidUtil.interactWithFluidHandler(player, hand, level, pos, hit.getDirection(), null)) {
            return InteractionResult.SUCCESS;
        }
        return InteractionResult.TRY_WITH_EMPTY_HAND;
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hitResult) {
        // Sneak + empty hand clears a Creative tank.
        if (tier.isCreative() && player.isShiftKeyDown()) {
            if (!level.isClientSide() && level.getBlockEntity(pos) instanceof FluidTankBlockEntity tank && tank.clearCreative()) {
                player.sendOverlayMessage(Component.translatable("message.voltaicdynamics.creative_tank_cleared"));
            }
            return InteractionResult.SUCCESS;
        }
        if (!level.isClientSide() && level.getBlockEntity(pos) instanceof FluidTankBlockEntity tank
                && player instanceof ServerPlayer serverPlayer) {
            serverPlayer.openMenu(tank, buf -> buf.writeBlockPos(pos));
        }
        return InteractionResult.SUCCESS;
    }


    @Override
    protected boolean hasAnalogOutputSignal(BlockState state) {
        return true;
    }

    @Override
    protected int getAnalogOutputSignal(BlockState state, Level level, BlockPos pos, Direction direction) {
        return level.getBlockEntity(pos) instanceof FluidTankBlockEntity tank ? tank.getComparatorSignal() : 0;
    }
}
