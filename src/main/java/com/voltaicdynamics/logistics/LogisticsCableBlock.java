package com.voltaicdynamics.logistics;

import com.voltaicdynamics.core.block.WrenchConfigurable;

import com.voltaicdynamics.VoltaicConfig;

import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.ItemTags;
import net.minecraft.tags.TagKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.ItemInteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Item and fluid cables. Looks and behaves like Mekanism transporters / AE2 cables: a thin core, arms to other
 * cables and a wider connector plate where it meets a machine. Sneak + right-click an arm with an empty hand,
 * or right-click it with any wrench (Voltaic Wrench or any item in {@code c:tools/wrench}), to cycle it: deliver -> pull (orange plate) -> off. Off between two cables cuts them apart.
 *
 * <p>No item ever travels as an entity and cables never tick: each network moves items directly from the
 * pulling connectors into the targets on a fixed interval (see {@link LogisticsNetwork}).
 */
public abstract class LogisticsCableBlock extends BaseEntityBlock implements WrenchConfigurable {
    public static final EnumProperty<ConnectionType> NORTH = EnumProperty.create("north", ConnectionType.class);
    public static final EnumProperty<ConnectionType> SOUTH = EnumProperty.create("south", ConnectionType.class);
    public static final EnumProperty<ConnectionType> EAST = EnumProperty.create("east", ConnectionType.class);
    public static final EnumProperty<ConnectionType> WEST = EnumProperty.create("west", ConnectionType.class);
    public static final EnumProperty<ConnectionType> UP = EnumProperty.create("up", ConnectionType.class);
    public static final EnumProperty<ConnectionType> DOWN = EnumProperty.create("down", ConnectionType.class);

    private static final TagKey<Item> WRENCHES = TagKey.create(net.minecraft.core.registries.Registries.ITEM,
            ResourceLocation.fromNamespaceAndPath("c", "tools/wrench"));

    private static final double CORE_MIN = 5.0 / 16.0;
    private static final double CORE_MAX = 11.0 / 16.0;
    private static final Map<Integer, VoxelShape> SHAPES = new ConcurrentHashMap<>();

    private final LogisticsKind kind;

    protected LogisticsCableBlock(BlockBehaviour.Properties properties, LogisticsKind kind) {
        super(properties);
        this.kind = kind;
        BlockState state = stateDefinition.any();
        for (Direction dir : Direction.values()) state = state.setValue(property(dir), ConnectionType.NONE);
        registerDefaultState(state);
    }

    public LogisticsKind kind() {
        return kind;
    }

    public static EnumProperty<ConnectionType> property(Direction direction) {
        return switch (direction) {
            case NORTH -> NORTH;
            case SOUTH -> SOUTH;
            case EAST -> EAST;
            case WEST -> WEST;
            case UP -> UP;
            case DOWN -> DOWN;
        };
    }

    public static ConnectionType connection(BlockState state, Direction direction) {
        return state.getValue(property(direction));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(NORTH, SOUTH, EAST, WEST, UP, DOWN);
    }

    @Override
    protected RenderShape getRenderShape(BlockState state) {
        return RenderShape.MODEL;
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new LogisticsCableBlockEntity(pos, state);
    }

    // ---- connections ----

    private boolean isSameCable(BlockState state) {
        return state.getBlock() instanceof LogisticsCableBlock cable && cable.kind == kind;
    }

    /** Works out one arm from the world, keeping the player's choice (pull / off) where it still applies. */
    private ConnectionType compute(LevelAccessor level, BlockPos pos, Direction dir, ConnectionType current) {
        if (current == ConnectionType.DISABLED) return ConnectionType.DISABLED;
        BlockPos neighborPos = pos.relative(dir);
        BlockState neighbor = level.getBlockState(neighborPos);
        if (isSameCable(neighbor)) {
            return connection(neighbor, dir.getOpposite()) == ConnectionType.DISABLED ? ConnectionType.NONE : ConnectionType.CABLE;
        }
        if (neighbor.getBlock() instanceof LogisticsCableBlock) return ConnectionType.NONE; // other kind of cable
        if (level instanceof Level fullLevel && kind.hasTarget(fullLevel, neighborPos, dir.getOpposite())) {
            return current == ConnectionType.PULL ? ConnectionType.PULL : ConnectionType.BLOCK;
        }
        return ConnectionType.NONE;
    }

    @Nullable
    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        BlockState state = defaultBlockState();
        for (Direction dir : Direction.values()) {
            state = state.setValue(property(dir), compute(context.getLevel(), context.getClickedPos(), dir, ConnectionType.NONE));
        }
        return state;
    }

    @Override
    protected BlockState updateShape(BlockState state, Direction direction, BlockState neighborState, LevelAccessor level,
                                     BlockPos pos, BlockPos neighborPos) {
        return state.setValue(property(direction), compute(level, pos, direction, connection(state, direction)));
    }

    @Override
    protected void onPlace(BlockState state, Level level, BlockPos pos, BlockState oldState, boolean movedByPiston) {
        super.onPlace(state, level, pos, oldState, movedByPiston);
        if (level instanceof ServerLevel serverLevel && oldState.is(this) && oldState != state) {
            LogisticsNetworkManager.get(serverLevel, kind).invalidateAt(pos);
        }
    }

    @Override
    protected void neighborChanged(BlockState state, Level level, BlockPos pos, Block neighborBlock, BlockPos neighborPos,
                                   boolean movedByPiston) {
        super.neighborChanged(state, level, pos, neighborBlock, neighborPos, movedByPiston);
        if (level instanceof ServerLevel serverLevel) {
            // A capability can appear without a shape update (a machine's face switched on in its GUI).
            Direction dir = Direction.fromDelta(neighborPos.getX() - pos.getX(), neighborPos.getY() - pos.getY(),
                    neighborPos.getZ() - pos.getZ());
            if (dir != null) {
                ConnectionType now = compute(level, pos, dir, connection(state, dir));
                if (now != connection(state, dir)) {
                    level.setBlock(pos, state.setValue(property(dir), now), Block.UPDATE_ALL);
                    return;
                }
            }
            LogisticsNetworkManager.get(serverLevel, kind).onNeighborChanged(pos, neighborPos);
        }
    }

    // ---- configuring arms ----

    @Override
    protected ItemInteractionResult useItemOn(ItemStack stack, BlockState state, Level level, BlockPos pos, Player player,
                                              InteractionHand hand, BlockHitResult hit) {
        if (stack.is(WRENCHES)) {
            configure(state, level, pos, player, hit);
            return ItemInteractionResult.sidedSuccess(level.isClientSide);
        }
        return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
        if (!player.isShiftKeyDown()) return InteractionResult.PASS;
        configure(state, level, pos, player, hit);
        return InteractionResult.sidedSuccess(level.isClientSide);
    }

    /** The arm under the cursor: the one sticking out of the core, or the face of the core that was clicked. */
    public static Direction hitSide(BlockPos pos, BlockHitResult hit) {
        Vec3 local = hit.getLocation().subtract(pos.getX(), pos.getY(), pos.getZ());
        double[] c = {local.x, local.y, local.z};
        Direction best = null;
        double bestDistance = 0;
        for (int axis = 0; axis < 3; axis++) {
            double v = c[axis];
            double outside = v < CORE_MIN - 1e-3 ? CORE_MIN - v : v > CORE_MAX + 1e-3 ? v - CORE_MAX : 0;
            if (outside > bestDistance) {
                bestDistance = outside;
                Direction.Axis a = Direction.Axis.values()[axis];
                best = Direction.fromAxisAndDirection(a, v > 0.5 ? Direction.AxisDirection.POSITIVE : Direction.AxisDirection.NEGATIVE);
            }
        }
        return best != null ? best : hit.getDirection();
    }

    private void configure(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
        if (level.isClientSide) return;
        Direction dir = hitSide(pos, hit);
        ConnectionType current = connection(state, dir);
        BlockPos neighborPos = pos.relative(dir);
        BlockState neighbor = level.getBlockState(neighborPos);
        ConnectionType next;
        switch (current) {
            case BLOCK -> next = ConnectionType.PULL;
            case PULL, CABLE -> next = ConnectionType.DISABLED;
            case DISABLED -> next = compute(level, pos, dir, ConnectionType.NONE);
            default -> {
                // Nothing on this arm: maybe it was cut from the other cable's side.
                if (isSameCable(neighbor) && connection(neighbor, dir.getOpposite()) == ConnectionType.DISABLED) {
                    next = ConnectionType.CABLE;
                } else {
                    player.displayClientMessage(Component.translatable("cable.voltaicdynamics.nothing"), true);
                    return;
                }
            }
        }
        level.setBlock(pos, state.setValue(property(dir), next), Block.UPDATE_ALL);
        // Cable-to-cable links are cut and rejoined on both sides.
        if (isSameCable(neighbor)) {
            ConnectionType other = next == ConnectionType.DISABLED ? ConnectionType.NONE : ConnectionType.CABLE;
            if (next == ConnectionType.CABLE || current == ConnectionType.CABLE) {
                ConnectionType theirs = next == ConnectionType.CABLE ? ConnectionType.CABLE : other;
                level.setBlock(neighborPos, neighbor.setValue(property(dir.getOpposite()), theirs), Block.UPDATE_ALL);
            }
        }
        level.playSound(null, pos, SoundEvents.ITEM_FRAME_ROTATE_ITEM, SoundSource.BLOCKS, 0.6F, next == ConnectionType.DISABLED ? 0.7F : 1.2F);
        player.displayClientMessage(Component.translatable("cable.voltaicdynamics.configured",
                Component.translatable("sideconfig.voltaicdynamics.dir." + dir.getName()), next.label()), true);
    }

    // ---- shape ----

    @Override
    protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        int key = 0;
        for (Direction dir : Direction.values()) {
            ConnectionType type = connection(state, dir);
            int code = type == ConnectionType.CABLE ? 1 : type.isEndpoint() ? 2 : 0;
            key = key * 3 + code;
        }
        return SHAPES.computeIfAbsent(key, LogisticsCableBlock::buildShape);
    }

    private static VoxelShape buildShape(int key) {
        VoxelShape shape = Block.box(5, 5, 5, 11, 11, 11);
        Direction[] dirs = Direction.values();
        for (int i = dirs.length - 1; i >= 0; i--) {
            int code = key % 3;
            key /= 3;
            if (code == 0) continue;
            shape = Shapes.or(shape, arm(dirs[i]));
            if (code == 2) shape = Shapes.or(shape, plate(dirs[i]));
        }
        return shape.optimize();
    }

    private static VoxelShape arm(Direction dir) {
        return switch (dir) {
            case DOWN -> Block.box(5, 0, 5, 11, 5, 11);
            case UP -> Block.box(5, 11, 5, 11, 16, 11);
            case NORTH -> Block.box(5, 5, 0, 11, 11, 5);
            case SOUTH -> Block.box(5, 5, 11, 11, 11, 16);
            case WEST -> Block.box(0, 5, 5, 5, 11, 11);
            case EAST -> Block.box(11, 5, 5, 16, 11, 11);
        };
    }

    private static VoxelShape plate(Direction dir) {
        return switch (dir) {
            case DOWN -> Block.box(3, 0, 3, 13, 2, 13);
            case UP -> Block.box(3, 14, 3, 13, 16, 13);
            case NORTH -> Block.box(3, 3, 0, 13, 13, 2);
            case SOUTH -> Block.box(3, 3, 14, 13, 13, 16);
            case WEST -> Block.box(0, 3, 3, 2, 13, 13);
            case EAST -> Block.box(14, 3, 3, 16, 13, 13);
        };
    }

    @Override
    public void appendHoverText(ItemStack stack, Item.TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        String name = kind == LogisticsKind.ITEM ? "item_cable" : "fluid_cable";
        Object rate = kind == LogisticsKind.ITEM
                ? VoltaicConfig.get(VoltaicConfig.ITEM_CABLE_ITEMS) + " / " + String.format("%.1f", VoltaicConfig.get(VoltaicConfig.ITEM_CABLE_INTERVAL) / 20.0) + " s"
                : String.format("%,d", VoltaicConfig.get(VoltaicConfig.FLUID_CABLE_RATE));
        tooltip.add(Component.translatable("tooltip.voltaicdynamics." + name + ".desc1", rate).withStyle(ChatFormatting.AQUA));
        tooltip.add(Component.translatable("tooltip.voltaicdynamics.logistics_cable.desc2").withStyle(ChatFormatting.GRAY));
        tooltip.add(Component.translatable("tooltip.voltaicdynamics.logistics_cable.desc3").withStyle(ChatFormatting.DARK_GRAY));
    }
}
