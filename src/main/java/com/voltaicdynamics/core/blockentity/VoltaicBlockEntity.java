package com.voltaicdynamics.core.blockentity;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;

/** Base of the Voltaic block entities that store data. */
public abstract class VoltaicBlockEntity extends BlockEntity {
    protected VoltaicBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state) {
        super(type, pos, state);
    }

    /**
     * Marks this block entity as changed, once per tick at most. Use it instead of {@link #setChanged()} on hot
     * paths (energy received or extracted, inventory and tank changes): on the server the real
     * {@code setChanged()} runs once when the tick ends, however many times this was called during it. That
     * saves a chunk lookup and a comparator update per call. On the client it calls {@code setChanged()} directly.
     */
    public void saveChanges() {
        if (level == null || level.isClientSide()) {
            setChanged();
        } else {
            DeferredSaves.schedule(this);
        }
    }
}
