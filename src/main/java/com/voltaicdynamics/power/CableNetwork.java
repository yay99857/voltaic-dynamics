package com.voltaicdynamics.energy;

import com.voltaicdynamics.VoltaicConfig;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.neoforged.neoforge.capabilities.BlockCapabilityCache;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.energy.IEnergyStorage;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * One connected group of Energy Cables. The network holds no energy: whatever is inserted through a
 * cable face is delivered in the same call to the blocks touching the network.
 *
 * <p>Routing rules:
 * <ul>
 *   <li>Consumers (can receive, cannot extract: machines) are served first, split evenly.</li>
 *   <li>Storage (can receive and extract: batteries) gets what is left, but only when the energy
 *       did not itself come from storage. Batteries feed machines, never each other, so no loops.</li>
 *   <li>Energy never goes back into the block that inserted it.</li>
 *   <li>Each connection accepts at most {@code transferPerConnection} FE per tick.</li>
 *   <li>Pure producers that do not push on their own (can extract, cannot receive) are pulled from once per tick.</li>
 * </ul>
 */
public final class CableNetwork {
    private final ServerLevel level;
    private final Set<BlockPos> cables;
    private final List<Acceptor> acceptors = new ArrayList<>();
    private final Map<BlockPos, List<Acceptor>> acceptorsByPos = new HashMap<>();
    private boolean valid = true;

    CableNetwork(ServerLevel level, Set<BlockPos> cables) {
        this.level = level;
        this.cables = cables;
    }

    void addAcceptor(BlockPos cablePos, Direction side) {
        BlockPos target = cablePos.relative(side);
        Acceptor acceptor = new Acceptor(target, BlockCapabilityCache.create(
                Capabilities.EnergyStorage.BLOCK, level, target, side.getOpposite(), () -> valid, () -> {
                }));
        acceptors.add(acceptor);
        acceptorsByPos.computeIfAbsent(target, p -> new ArrayList<>()).add(acceptor);
    }

    Set<BlockPos> cables() {
        return cables;
    }

    boolean hasAcceptorAt(BlockPos pos) {
        return acceptorsByPos.containsKey(pos);
    }

    void invalidate() {
        valid = false;
    }

    public boolean isValid() {
        return valid;
    }

    public int size() {
        return cables.size();
    }

    public int acceptorCount() {
        return acceptors.size();
    }

    private static int transferLimit() {
        return VoltaicConfig.get(VoltaicConfig.CABLE_TRANSFER);
    }

    /**
     * Inserts energy through the face {@code side} of the cable at {@code cablePos}.
     */
    public int insert(BlockPos cablePos, @Nullable Direction side, int amount, boolean simulate) {
        if (!valid || amount <= 0) return 0;
        amount = Math.min(amount, transferLimit());
        BlockPos sourcePos = side == null ? null : cablePos.relative(side);
        boolean fromStorage = false;
        if (sourcePos != null) {
            List<Acceptor> source = acceptorsByPos.get(sourcePos);
            if (source != null) {
                for (Acceptor acceptor : source) {
                    IEnergyStorage storage = acceptor.storage();
                    if (storage != null && storage.canExtract() && storage.canReceive()) {
                        fromStorage = true;
                        break;
                    }
                }
            }
        }
        return route(sourcePos, fromStorage, amount, simulate);
    }

    private int route(@Nullable BlockPos sourcePos, boolean fromStorage, int amount, boolean simulate) {
        List<Target> consumers = new ArrayList<>();
        List<Target> storages = new ArrayList<>();
        Set<BlockPos> seen = new HashSet<>();
        long time = level.getGameTime();
        for (Acceptor acceptor : acceptors) {
            if (acceptor.pos.equals(sourcePos)) continue;
            // A block touching several cables is one destination, not several.
            if (!seen.add(acceptor.pos)) continue;
            IEnergyStorage storage = acceptor.storage();
            if (storage == null || !storage.canReceive()) continue;
            int room = acceptor.room(time);
            if (room <= 0) continue;
            Target target = new Target(acceptor, storage, room);
            if (storage.canExtract()) {
                storages.add(target);
            } else {
                consumers.add(target);
            }
        }
        int sent = distribute(consumers, amount, simulate, time);
        if (!fromStorage && sent < amount) {
            sent += distribute(storages, amount - sent, simulate, time);
        }
        return sent;
    }

    /** Splits {@code amount} evenly; a few rounds hand what one target refused to the others. */
    private static int distribute(List<Target> targets, int amount, boolean simulate, long time) {
        if (targets.isEmpty() || amount <= 0) return 0;
        int remaining = amount;
        List<Target> active = new ArrayList<>(targets);
        for (int round = 0; round < 4 && remaining > 0 && !active.isEmpty(); round++) {
            int share = Math.max(1, remaining / active.size());
            List<Target> next = new ArrayList<>();
            for (Target target : active) {
                if (remaining <= 0) break;
                int want = Math.min(Math.min(share, remaining), target.room - target.given);
                if (want <= 0) continue;
                int got;
                if (simulate) {
                    got = Math.max(0, target.storage.receiveEnergy(target.given + want, true) - target.given);
                } else {
                    got = target.storage.receiveEnergy(want, false);
                }
                target.given += got;
                remaining -= got;
                if (got >= want && target.given < target.room) next.add(target);
            }
            active = next;
        }
        if (!simulate) {
            for (Target target : targets) {
                if (target.given > 0) target.acceptor.record(time, target.given);
            }
        }
        return amount - remaining;
    }

    /** Pulls from neighbours that only produce (and might not push by themselves). */
    void tick() {
        if (!valid) return;
        int limit = transferLimit();
        for (Acceptor acceptor : List.copyOf(acceptors)) {
            IEnergyStorage storage = acceptor.storage();
            if (storage == null || !storage.canExtract() || storage.canReceive()) continue;
            int available = storage.extractEnergy(limit, true);
            if (available <= 0) continue;
            int sent = route(acceptor.pos, false, available, false);
            if (sent > 0) storage.extractEnergy(sent, false);
        }
    }

    private final class Acceptor {
        final BlockPos pos;
        final BlockCapabilityCache<IEnergyStorage, Direction> cache;
        long tick = -1;
        int receivedThisTick;

        Acceptor(BlockPos pos, BlockCapabilityCache<IEnergyStorage, Direction> cache) {
            this.pos = pos;
            this.cache = cache;
        }

        @Nullable
        IEnergyStorage storage() {
            // An invalidated network's caches may refuse queries; it has been replaced anyway.
            return valid ? cache.getCapability() : null;
        }

        int room(long time) {
            return transferLimit() - (tick == time ? receivedThisTick : 0);
        }

        void record(long time, int amount) {
            if (tick != time) {
                tick = time;
                receivedThisTick = 0;
            }
            receivedThisTick += amount;
        }
    }

    private static final class Target {
        final Acceptor acceptor;
        final IEnergyStorage storage;
        final int room;
        int given;

        Target(Acceptor acceptor, IEnergyStorage storage, int room) {
            this.acceptor = acceptor;
            this.storage = storage;
            this.room = room;
        }
    }
}
