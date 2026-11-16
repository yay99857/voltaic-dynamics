package com.voltaicdynamics.core.menu;

import net.minecraft.world.inventory.DataSlot;
import org.jetbrains.annotations.Nullable;

import java.util.function.IntSupplier;

/**
 * One int the server sends to an open menu, created with {@link MachineMenu#syncInt}.
 *
 * <p>Vanilla data slots travel as 16-bit shorts, so each value uses two of them (low and high half) and arrives
 * whole, energy above 32,767 FE included. Vanilla already sends a slot only when it changed. The menu registers
 * values in the same order on both sides, so there are no indices to keep in step by hand.
 */
public final class SyncedInt {
    /** Reads the live value; only set on the server. */
    @Nullable
    private final IntSupplier source;
    /** Last value received; only used on the client. */
    private int value;

    SyncedInt(@Nullable IntSupplier source) {
        this.source = source;
    }

    /** The live value on the server, the last synced value on the client. */
    public int get() {
        return source != null ? source.getAsInt() : value;
    }

    DataSlot lowHalf() {
        return new DataSlot() {
            @Override
            public int get() {
                return SyncedInt.this.get() & 0xFFFF;
            }

            @Override
            public void set(int half) {
                value = (value & 0xFFFF0000) | (half & 0xFFFF);
            }
        };
    }

    DataSlot highHalf() {
        return new DataSlot() {
            @Override
            public int get() {
                return SyncedInt.this.get() >>> 16;
            }

            @Override
            public void set(int half) {
                value = (value & 0xFFFF) | ((half & 0xFFFF) << 16);
            }
        };
    }
}
