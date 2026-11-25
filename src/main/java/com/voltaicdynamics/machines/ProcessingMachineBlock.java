package com.voltaicdynamics.machines;

import java.util.function.Consumer;
import com.voltaicdynamics.core.block.TooltipBlock;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.phys.BlockHitResult;
import org.jetbrains.annotations.Nullable;
import com.voltaicdynamics.core.ModDataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;

import java.util.List;

import java.util.function.Supplier;

/** Facing, lit state, GUI, drops, comparator and sparks shared by the FE processing machines. */
public abstract class ProcessingMachineBlock extends BaseEntityBlock implements TooltipBlock {
    public static final EnumProperty<Direction> FACING = HorizontalDirectionalBlock.FACING;
    public static final BooleanProperty LIT = BlockStateProperties.LIT;

    protected ProcessingMachineBlock(BlockBehaviour.Properties properties) {
        super(properties);
        registerDefaultState(stateDefinition.any().setValue(FACING, Direction.NORTH).setValue(LIT, false));
    }

    protected abstract Supplier<? extends BlockEntityType<? extends ProcessingMachineBlockEntity>> blockEntityType();

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(FACING, LIT);
    }

    @Nullable
    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        return defaultBlockState().setValue(FACING, context.getHorizontalDirection().getOpposite());
    }

    @Override
    protected RenderShape getRenderShape(BlockState state) {
        return RenderShape.MODEL;
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return blockEntityType().get().create(pos, state);
    }

    @Nullable
    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
        if (level.isClientSide() || type != blockEntityType().get()) return null;
        return (lvl, pos, st, be) -> ProcessingMachineBlockEntity.tick(lvl, pos, st, (ProcessingMachineBlockEntity) be);
    }

    /** A machine item raised with a Tier Installer says so. */
    @Override
    public void appendTooltip(ItemStack stack, Item.TooltipContext context, Consumer<Component> tooltip, TooltipFlag flag) {
        MachineTier tier = stack.get(ModDataComponents.MACHINE_TIER.get());
        if (tier != null && tier != MachineTier.BASIC) {
            tooltip.accept(Component.translatable("jade.voltaicdynamics.tier", TierInstallerItem.tierName(tier), tier.parallel())
                    .withStyle(tier.color()));
        }
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hitResult) {
        if (!level.isClientSide() && level.getBlockEntity(pos) instanceof ProcessingMachineBlockEntity machine
                && player instanceof ServerPlayer serverPlayer) {
            serverPlayer.openMenu(machine, buf -> buf.writeBlockPos(pos));
        }
        return InteractionResult.SUCCESS;
    }


    @Override
    protected boolean hasAnalogOutputSignal(BlockState state) {
        return true;
    }

    @Override
    protected int getAnalogOutputSignal(BlockState state, Level level, BlockPos pos, Direction direction) {
        return level.getBlockEntity(pos) instanceof ProcessingMachineBlockEntity machine ? machine.getComparatorSignal() : 0;
    }

    @Override
    public void animateTick(BlockState state, Level level, BlockPos pos, RandomSource random) {
        if (!state.getValue(LIT) || random.nextDouble() >= 0.3D) return;
        Direction direction = state.getValue(FACING);
        double offset = random.nextDouble() * 0.6D - 0.3D;
        double x = pos.getX() + 0.5D + (direction.getAxis() == Direction.Axis.X ? direction.getStepX() * 0.52D : offset);
        double y = pos.getY() + 0.3D + random.nextDouble() * 0.4D;
        double z = pos.getZ() + 0.5D + (direction.getAxis() == Direction.Axis.Z ? direction.getStepZ() * 0.52D : offset);
        level.addParticle(ParticleTypes.ELECTRIC_SPARK, x, y, z, 0.0D, 0.0D, 0.0D);
    }
}
