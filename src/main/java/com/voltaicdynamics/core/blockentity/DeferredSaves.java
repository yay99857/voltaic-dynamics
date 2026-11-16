package com.voltaicdynamics.core.blockentity;

import net.minecraft.world.level.block.entity.BlockEntity;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.server.ServerStoppingEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;

import java.util.ArrayList;
import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Set;

/**
 * Runs the {@code setChanged()} calls queued by {@link VoltaicBlockEntity#saveChanges()}, once per block entity.
 *
 * <p>The queue is flushed when the server tick ends, and again when the next one starts, so changes made while
 * handling player packets between ticks are marked before chunks can unload or save. It is also flushed before
 * the server stops and saves the world. Server thread only.
 */
public final class DeferredSaves {
    private static final Set<BlockEntity> PENDING = Collections.newSetFromMap(new IdentityHashMap<>());

    private DeferredSaves() {
    }

    static void schedule(BlockEntity be) {
        PENDING.add(be);
    }

    /** Marks every queued block entity as changed now. Public so tests can check the result without waiting. */
    public static void flush() {
        if (PENDING.isEmpty()) return;
        List<BlockEntity> batch = new ArrayList<>(PENDING);
        PENDING.clear();
        for (BlockEntity be : batch) {
            if (!be.isRemoved() && be.getLevel() != null) {
                be.setChanged();
            }
        }
    }

    public static void register() {
        NeoForge.EVENT_BUS.addListener((ServerTickEvent.Pre event) -> flush());
        NeoForge.EVENT_BUS.addListener((ServerTickEvent.Post event) -> flush());
        NeoForge.EVENT_BUS.addListener((ServerStoppingEvent event) -> flush());
    }
}
