package com.voltaicdynamics.logistics;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.Level;
import net.neoforged.neoforge.capabilities.BlockCapability;
import net.neoforged.neoforge.capabilities.Capabilities;

/** Item cables and fluid cables share all network code; only the capability differs. */
public enum LogisticsKind {
    ITEM(Capabilities.Item.BLOCK),
    FLUID(Capabilities.Fluid.BLOCK);

    private final BlockCapability<?, Direction> capability;

    LogisticsKind(BlockCapability<?, Direction> capability) {
        this.capability = capability;
    }

    public BlockCapability<?, Direction> capability() {
        return capability;
    }

    /** True when the block at {@code target} exposes this kind's capability on the face looking back at us. */
    public boolean hasTarget(Level level, BlockPos target, Direction faceTowardsCable) {
        return level.getCapability(capability, target, faceTowardsCable) != null;
    }
}
