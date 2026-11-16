package com.voltaicdynamics.energy;

import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.neoforged.neoforge.capabilities.BlockCapabilityCache;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.energy.IEnergyStorage;
import org.jetbrains.annotations.Nullable;

/**
 * Cached FE capabilities of the six neighbours of a block entity, so pushing energy every tick
 * does not repeat full capability lookups.
 */
public final class NeighborEnergyCache {
    private final BlockEntity owner;
    @SuppressWarnings("unchecked")
    private final BlockCapabilityCache<IEnergyStorage, Direction>[] caches = new BlockCapabilityCache[6];

    public NeighborEnergyCache(BlockEntity owner) {
        this.owner = owner;
    }

    @Nullable
    public IEnergyStorage get(Direction direction) {
        if (!(owner.getLevel() instanceof ServerLevel level)) return null;
        BlockCapabilityCache<IEnergyStorage, Direction> cache = caches[direction.ordinal()];
        if (cache == null) {
            cache = BlockCapabilityCache.create(Capabilities.EnergyStorage.BLOCK, level,
                    owner.getBlockPos().relative(direction), direction.getOpposite());
            caches[direction.ordinal()] = cache;
        }
        return cache.getCapability();
    }

    /**
     * Pushes up to {@code maxTotal} FE from {@code source} into neighbours.
     *
     * @param consumersOnly when true, skips neighbours that can also give energy back (other batteries),
     *                      which stops two storage blocks from bouncing the same energy every tick.
     * @return FE actually moved
     */
    public int push(IEnergyStorage source, int maxTotal, boolean consumersOnly) {
        int budget = source.extractEnergy(maxTotal, true);
        if (budget <= 0) return 0;
        int moved = 0;
        for (Direction direction : Direction.values()) {
            IEnergyStorage target = get(direction);
            if (target == null || target == source || !target.canReceive()) continue;
            if (consumersOnly && target.canExtract()) continue;
            int accepted = target.receiveEnergy(budget - moved, false);
            if (accepted > 0) {
                source.extractEnergy(accepted, false);
                moved += accepted;
                if (moved >= budget) break;
            }
        }
        return moved;
    }
}
