package com.voltaicdynamics.power;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.WeakHashMap;

/**
 * Keeps the cable networks of one level. Networks are built lazily by flood fill over loaded cables
 * and thrown away whenever a cable or an adjacent block entity appears or disappears; the next
 * access (or the next tick) rebuilds them. Nothing is saved: networks are derived from the world.
 */
public final class CableNetworkManager {
    private static final Map<Level, CableNetworkManager> MANAGERS = new WeakHashMap<>();

    private final ServerLevel level;
    private final Map<BlockPos, CableNetwork> byPos = new HashMap<>();
    private final Set<CableNetwork> networks = Collections.newSetFromMap(new IdentityHashMap<>());
    private final Set<BlockPos> pending = new HashSet<>();

    private CableNetworkManager(ServerLevel level) {
        this.level = level;
    }

    public static CableNetworkManager get(ServerLevel level) {
        return MANAGERS.computeIfAbsent(level, l -> new CableNetworkManager((ServerLevel) l));
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

    /** Called when a block next to the cable at {@code cablePos} changes. */
    public void onNeighborChanged(BlockPos cablePos, BlockPos neighborPos) {
        CableNetwork network = byPos.get(cablePos);
        if (network == null) return;
        if (isCable(neighborPos)) return; // cables report themselves when loaded/removed
        boolean hasBlockEntity = level.isLoaded(neighborPos) && level.getBlockEntity(neighborPos) != null;
        if (hasBlockEntity != network.hasAcceptorAt(neighborPos)) {
            invalidate(cablePos);
        }
    }

    @Nullable
    public CableNetwork getNetwork(BlockPos pos) {
        CableNetwork network = byPos.get(pos);
        if (network == null && isCable(pos)) {
            network = build(pos);
        }
        return network;
    }

    public void tick() {
        if (!pending.isEmpty()) {
            List<BlockPos> toBuild = new ArrayList<>(pending);
            pending.clear();
            for (BlockPos pos : toBuild) {
                if (!byPos.containsKey(pos) && isCable(pos)) build(pos);
            }
        }
        if (networks.isEmpty()) return;
        for (CableNetwork network : List.copyOf(networks)) {
            network.tick();
        }
    }

    private void invalidateAround(BlockPos pos) {
        invalidate(pos);
        for (Direction direction : Direction.values()) {
            invalidate(pos.relative(direction));
        }
    }

    private void invalidate(BlockPos pos) {
        CableNetwork network = byPos.get(pos);
        if (network == null) return;
        networks.remove(network);
        network.invalidate();
        for (BlockPos cable : network.cables()) {
            byPos.remove(cable);
            pending.add(cable);
        }
    }

    private boolean isCable(BlockPos pos) {
        return level.isLoaded(pos) && level.getBlockState(pos).getBlock() instanceof VoltaicCableBlock;
    }

    private CableNetwork build(BlockPos start) {
        Set<BlockPos> cables = new HashSet<>();
        ArrayDeque<BlockPos> queue = new ArrayDeque<>();
        BlockPos origin = start.immutable();
        cables.add(origin);
        queue.add(origin);
        while (!queue.isEmpty()) {
            BlockPos current = queue.poll();
            for (Direction direction : Direction.values()) {
                BlockPos next = current.relative(direction);
                if (!cables.contains(next) && isCable(next)) {
                    cables.add(next);
                    queue.add(next);
                }
            }
        }

        CableNetwork network = new CableNetwork(level, cables);
        for (BlockPos cable : cables) {
            for (Direction direction : Direction.values()) {
                BlockPos neighbor = cable.relative(direction);
                if (cables.contains(neighbor) || !level.isLoaded(neighbor)) continue;
                if (level.getBlockEntity(neighbor) != null) {
                    network.addAcceptor(cable, direction);
                }
            }
        }
        for (BlockPos cable : cables) {
            CableNetwork old = byPos.put(cable, network);
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
