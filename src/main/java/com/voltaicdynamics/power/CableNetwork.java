package com.voltaicdynamics.power;

import net.neoforged.neoforge.transfer.transaction.SnapshotJournal;
import com.voltaicdynamics.core.energy.EnergyBuffer;
import net.neoforged.neoforge.transfer.energy.EnergyHandler;
import net.neoforged.neoforge.transfer.transaction.TransactionContext;
import net.neoforged.neoforge.transfer.transaction.Transaction;
import com.voltaicdynamics.core.transfer.Transfers;
import com.voltaicdynamics.VoltaicConfig;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.capabilities.BlockCapabilityCache;
import net.neoforged.neoforge.capabilities.Capabilities;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * One connected group of Voltaic Cables. The network holds no energy: whatever is inserted through a
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
    /** Ticks the cables keep glowing after the last energy went through, so a pulsing source does not flicker. */
    private static final int GLOW_HOLD = 20;
    private long lastFlow = Long.MIN_VALUE / 2;
    /** Rolls {@link #lastFlow} back when the insert that set it is aborted (a simulation). */
    private final SnapshotJournal<Long> flowJournal = new SnapshotJournal<>() {
        @Override
        protected Long createSnapshot() {
            return lastFlow;
        }

        @Override
        protected void revertToSnapshot(Long snapshot) {
            lastFlow = snapshot;
        }
    };
    /** Whether the cables were last set glowing; null until the first {@link #updateGlow()} syncs them. */
    @Nullable
    private Boolean lit;

    CableNetwork(ServerLevel level, Set<BlockPos> cables) {
        this.level = level;
        this.cables = cables;
    }

    void addAcceptor(BlockPos cablePos, Direction side) {
        BlockPos target = cablePos.relative(side);
        Acceptor acceptor = new Acceptor(target, BlockCapabilityCache.create(
                Capabilities.Energy.BLOCK, level, target, side.getOpposite(), () -> valid, () -> {
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
    public int insert(BlockPos cablePos, @Nullable Direction side, int amount, TransactionContext transaction) {
        if (!valid || amount <= 0) return 0;
        amount = Math.min(amount, transferLimit());
        BlockPos sourcePos = side == null ? null : cablePos.relative(side);
        boolean fromStorage = false;
        if (sourcePos != null) {
            List<Acceptor> source = acceptorsByPos.get(sourcePos);
            if (source != null) {
                for (Acceptor acceptor : source) {
                    EnergyHandler storage = acceptor.storage();
                    if (storage != null && isStorage(acceptor, storage)) {
                        fromStorage = true;
                        break;
                    }
                }
            }
        }
        return route(sourcePos, fromStorage, amount, transaction);
    }

    /**
     * Whether a neighbour stores energy (it can give and take), as opposed to a consumer or a generator. Our
     * Accumulators always count, even empty or full; other blocks are judged by what they would do right now.
     */
    private boolean isStorage(Acceptor acceptor, EnergyHandler storage) {
        if (level.getBlockEntity(acceptor.pos) instanceof EnergyBuffer) return true;
        return Transfers.canExtractEnergy(storage) && Transfers.canInsertEnergy(storage);
    }

    private int route(@Nullable BlockPos sourcePos, boolean fromStorage, int amount, TransactionContext transaction) {
        List<Target> consumers = new ArrayList<>();
        List<Target> storages = new ArrayList<>();
        Set<BlockPos> seen = new HashSet<>();
        long time = level.getGameTime();
        for (Acceptor acceptor : acceptors) {
            if (acceptor.pos.equals(sourcePos)) continue;
            // A block touching several cables is one destination, not several.
            if (!seen.add(acceptor.pos)) continue;
            EnergyHandler storage = acceptor.storage();
            if (storage == null) continue;
            int room = acceptor.room(time);
            if (room <= 0) continue;
            boolean isStorage = isStorage(acceptor, storage);
            if (!isStorage && !Transfers.canInsertEnergy(storage)) continue;
            Target target = new Target(acceptor, storage, room);
            if (isStorage) {
                storages.add(target);
            } else {
                consumers.add(target);
            }
        }
        int sent = distribute(consumers, amount, transaction, time);
        if (!fromStorage && sent < amount) {
            sent += distribute(storages, amount - sent, transaction, time);
        }
        if (sent > 0) {
            flowJournal.updateSnapshots(transaction);
            lastFlow = time;
        }
        return sent;
    }

    /** Splits {@code amount} evenly; a few rounds hand what one target refused to the others. */
    private static int distribute(List<Target> targets, int amount, TransactionContext transaction, long time) {
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
                int got = target.storage.insert(want, transaction);
                target.given += got;
                remaining -= got;
                if (got >= want && target.given < target.room) next.add(target);
            }
            active = next;
        }
        for (Target target : targets) {
            if (target.given > 0) target.acceptor.record(time, target.given, transaction);
        }
        return amount - remaining;
    }

    /** Pulls from neighbours that only produce (and might not push by themselves). */
    void tick() {
        if (!valid) return;
        int limit = transferLimit();
        for (Acceptor acceptor : List.copyOf(acceptors)) {
            EnergyHandler storage = acceptor.storage();
            if (storage == null || isStorage(acceptor, storage) || Transfers.canInsertEnergy(storage)) continue;
            int available = Transfers.extractEnergy(storage, limit, true);
            if (available <= 0) continue;
            try (Transaction tx = Transaction.openRoot()) {
                int sent = route(acceptor.pos, false, available, tx);
                if (sent > 0 && storage.extract(sent, tx) == sent) tx.commit();
            }
        }
    }

    /**
     * Lights the cables while energy flows and dims them {@link #GLOW_HOLD} ticks after it stops. Only changes the
     * block state when the network switches, and only tells clients (no neighbour or shape updates, so the network
     * is not rebuilt). The first call after a rebuild brings every cable to the same state.
     */
    void updateGlow() {
        if (!valid) return;
        boolean flowing = level.getGameTime() - lastFlow <= GLOW_HOLD;
        if (lit != null && lit == flowing) return;
        lit = flowing;
        for (BlockPos pos : cables) {
            if (!level.isLoaded(pos)) continue;
            BlockState state = level.getBlockState(pos);
            if (state.getBlock() instanceof VoltaicCableBlock && state.getValue(VoltaicCableBlock.POWERED) != flowing) {
                level.setBlock(pos, state.setValue(VoltaicCableBlock.POWERED, flowing),
                        Block.UPDATE_CLIENTS | Block.UPDATE_KNOWN_SHAPE);
            }
        }
    }

    private final class Acceptor {
        final BlockPos pos;
        final BlockCapabilityCache<EnergyHandler, Direction> cache;
        long tick = -1;
        int receivedThisTick;
        private final SnapshotJournal<long[]> journal = new SnapshotJournal<>() {
            @Override
            protected long[] createSnapshot() {
                return new long[]{tick, receivedThisTick};
            }

            @Override
            protected void revertToSnapshot(long[] snapshot) {
                tick = snapshot[0];
                receivedThisTick = (int) snapshot[1];
            }
        };

        Acceptor(BlockPos pos, BlockCapabilityCache<EnergyHandler, Direction> cache) {
            this.pos = pos;
            this.cache = cache;
        }

        @Nullable
        EnergyHandler storage() {
            // An invalidated network's caches may refuse queries; it has been replaced anyway.
            return valid ? cache.getCapability() : null;
        }

        int room(long time) {
            return transferLimit() - (tick == time ? receivedThisTick : 0);
        }

        void record(long time, int amount, TransactionContext transaction) {
            journal.updateSnapshots(transaction);
            if (tick != time) {
                tick = time;
                receivedThisTick = 0;
            }
            receivedThisTick += amount;
        }
    }

    private static final class Target {
        final Acceptor acceptor;
        final EnergyHandler storage;
        final int room;
        int given;

        Target(Acceptor acceptor, EnergyHandler storage, int room) {
            this.acceptor = acceptor;
            this.storage = storage;
            this.room = room;
        }
    }
}
