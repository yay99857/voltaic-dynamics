package com.voltaicdynamics.core.energy;

import net.minecraft.core.Direction;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.neoforged.neoforge.transfer.energy.EnergyHandler;
import net.neoforged.neoforge.transfer.transaction.SnapshotJournal;
import net.neoforged.neoforge.transfer.transaction.TransactionContext;
import org.jetbrains.annotations.Nullable;

import java.util.Arrays;

/**
 * Remembers which faces of a battery received energy in the last couple of ticks, so the battery does not push
 * energy straight back out of them. Without this, an Accumulator on another mod's cable network (Mekanism's
 * Universal Cable, for example) would hand every FE the network gives it back to the network, and never charge.
 * Once the network stops supplying, the face opens again and the battery discharges into it.
 */
public final class RecentInputFaces {
    /** Ticks a face stays closed after its last input; covers networks that tick before or after the battery. */
    private static final int WINDOW = 2;

    private final BlockEntity owner;
    private final long[] lastInput = new long[6];
    private final Face[] faces = new Face[6];

    public RecentInputFaces(BlockEntity owner) {
        this.owner = owner;
        Arrays.fill(lastInput, Long.MIN_VALUE / 2);
    }

    /** The capability for {@code side}, recording every committed insert that goes through it. */
    @Nullable
    public EnergyHandler wrap(@Nullable Direction side, @Nullable EnergyHandler base) {
        if (base == null || side == null) return base;
        Face face = faces[side.ordinal()];
        if (face == null || face.base != base) {
            face = new Face(base, side.ordinal());
            faces[side.ordinal()] = face;
        }
        return face;
    }

    public boolean receivedRecently(Direction side) {
        return owner.getLevel() != null && owner.getLevel().getGameTime() - lastInput[side.ordinal()] <= WINDOW;
    }

    private final class Face implements EnergyHandler {
        private final EnergyHandler base;
        private final int index;
        /** Rolls the timestamp back when the insert that set it is aborted (a simulation). */
        private final SnapshotJournal<Long> journal = new SnapshotJournal<>() {
            @Override
            protected Long createSnapshot() {
                return lastInput[index];
            }

            @Override
            protected void revertToSnapshot(Long snapshot) {
                lastInput[index] = snapshot;
            }
        };

        private Face(EnergyHandler base, int index) {
            this.base = base;
            this.index = index;
        }

        @Override
        public int insert(int amount, TransactionContext transaction) {
            int received = base.insert(amount, transaction);
            if (received > 0 && owner.getLevel() != null) {
                journal.updateSnapshots(transaction);
                lastInput[index] = owner.getLevel().getGameTime();
            }
            return received;
        }

        @Override
        public int extract(int amount, TransactionContext transaction) {
            return base.extract(amount, transaction);
        }

        @Override
        public long getAmountAsLong() {
            return base.getAmountAsLong();
        }

        @Override
        public long getCapacityAsLong() {
            return base.getCapacityAsLong();
        }
    }
}
