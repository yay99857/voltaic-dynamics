package com.voltaicdynamics.logistics;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.HashSet;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.WeakHashMap;

/**
 * Item / fluid cable networks of one level, built lazily by flood fill and discarded whenever a cable, a
 * connector or an adjacent block entity changes. Nothing is saved: networks are derived from the world.
 */
public final class LogisticsNetworkManager {
    private static final Map<Level, EnumMap<LogisticsKind, LogisticsNetworkManager>> MANAGERS = new WeakHashMap<>();

    private final ServerLevel level;
    private final LogisticsKind kind;
    private final Map<BlockPos, LogisticsNetwork> byPos = new HashMap<>();
    private final Set<LogisticsNetwork> networks = Collections.newSetFromMap(new IdentityHashMap<>());
    private final Set<BlockPos> pending = new HashSet<>();

    private LogisticsNetworkManager(ServerLevel level, LogisticsKind kind) {
        this.level = level;
        this.kind = kind;
    }

    public static LogisticsNetworkManager get(ServerLevel level, LogisticsKind kind) {
        return MANAGERS.computeIfAbsent(level, l -> new EnumMap<>(LogisticsKind.class))
                .computeIfAbsent(kind, k -> new LogisticsNetworkManager(level, k));
    }

    public static void tickAll(ServerLevel level) {
        EnumMap<LogisticsKind, LogisticsNetworkManager> managers = MANAGERS.get(level);
        if (managers == null) return;
        for (LogisticsNetworkManager manager : managers.values()) manager.tick();
    }

    public static void remove(Level level) {
        MANAGERS.remove(level);
    }

    public static void clearAll() {
        MANAGERS.clear();
    }

    public void onCableAdded(BlockPos pos) {
        invalidateAround(pos);
        pending.add(pos.immutable());
    }

    public void onCableRemoved(BlockPos pos) {
        invalidateAround(pos);
    }

    /** A cable's block state changed (an arm was configured or connected). */
    public void invalidateAt(BlockPos pos) {
        invalidateAround(pos);
        pending.add(pos.immutable());
    }

    public void onNeighborChanged(BlockPos cablePos, BlockPos neighborPos) {
        LogisticsNetwork network = byPos.get(cablePos);
        if (network == null || isCable(neighborPos)) return;
        boolean hasBlockEntity = level.isLoaded(neighborPos) && level.getBlockEntity(neighborPos) != null;
        if (hasBlockEntity != network.hasEndpointAt(neighborPos)) invalidate(cablePos);
    }

    @Nullable
    public LogisticsNetwork getNetwork(BlockPos pos) {
        LogisticsNetwork network = byPos.get(pos);
        if (network == null && isCable(pos)) network = build(pos);
        return network;
    }

    void tick() {
        if (!pending.isEmpty()) {
            List<BlockPos> toBuild = new ArrayList<>(pending);
            pending.clear();
            for (BlockPos pos : toBuild) {
                if (!byPos.containsKey(pos) && isCable(pos)) build(pos);
            }
        }
        if (networks.isEmpty()) return;
        for (LogisticsNetwork network : List.copyOf(networks)) network.tick();
    }

    private void invalidateAround(BlockPos pos) {
        invalidate(pos);
        for (Direction direction : Direction.values()) invalidate(pos.relative(direction));
    }

    private void invalidate(BlockPos pos) {
        LogisticsNetwork network = byPos.get(pos);
        if (network == null) return;
        networks.remove(network);
        network.invalidate();
        for (BlockPos cable : network.cables()) {
            byPos.remove(cable);
            pending.add(cable);
        }
    }

    private boolean isCable(BlockPos pos) {
        return level.isLoaded(pos) && level.getBlockState(pos).getBlock() instanceof LogisticsCableBlock cable
                && cable.kind() == kind;
    }

    private LogisticsNetwork build(BlockPos start) {
        Set<BlockPos> cables = new HashSet<>();
        ArrayDeque<BlockPos> queue = new ArrayDeque<>();
        BlockPos origin = start.immutable();
        cables.add(origin);
        queue.add(origin);
        while (!queue.isEmpty()) {
            BlockPos current = queue.poll();
            BlockState state = level.getBlockState(current);
            for (Direction direction : Direction.values()) {
                if (LogisticsCableBlock.connection(state, direction) != ConnectionType.CABLE) continue;
                BlockPos next = current.relative(direction);
                if (!cables.contains(next) && isCable(next)) {
                    cables.add(next);
                    queue.add(next);
                }
            }
        }

        LogisticsNetwork network = new LogisticsNetwork(level, kind, cables);
        for (BlockPos cable : cables) {
            BlockState state = level.getBlockState(cable);
            for (Direction direction : Direction.values()) {
                ConnectionType type = LogisticsCableBlock.connection(state, direction);
                if (type.isEndpoint() && level.isLoaded(cable.relative(direction))) {
                    network.addEndpoint(cable, direction, type == ConnectionType.PULL);
                }
            }
        }
        for (BlockPos cable : cables) {
            LogisticsNetwork old = byPos.put(cable, network);
            if (old != null && old != network) {
                networks.remove(old);
                old.invalidate();
            }
            pending.remove(cable);
        }
        networks.add(network);
        return network;
    }
}
