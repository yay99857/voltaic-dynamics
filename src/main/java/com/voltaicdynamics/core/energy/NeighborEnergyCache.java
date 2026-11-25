package com.voltaicdynamics.core.energy;

import com.voltaicdynamics.core.transfer.Transfers;

import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.neoforged.neoforge.capabilities.BlockCapabilityCache;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.transfer.energy.EnergyHandler;
import net.neoforged.neoforge.transfer.energy.EnergyHandlerUtil;
import org.jetbrains.annotations.Nullable;

/**
 * Cached FE capabilities of the six neighbours of a block entity, so pushing energy every tick
 * does not repeat full capability lookups.
 */
public final class NeighborEnergyCache {
    private final BlockEntity owner;
    @SuppressWarnings("unchecked")
    private final BlockCapabilityCache<EnergyHandler, Direction>[] caches = new BlockCapabilityCache[6];

    public NeighborEnergyCache(BlockEntity owner) {
        this.owner = owner;
    }

    @Nullable
    public EnergyHandler get(Direction direction) {
        if (!(owner.getLevel() instanceof ServerLevel level)) return null;
        BlockCapabilityCache<EnergyHandler, Direction> cache = caches[direction.ordinal()];
        if (cache == null) {
            cache = BlockCapabilityCache.create(Capabilities.Energy.BLOCK, level,
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
    public int push(EnergyHandler source, int maxTotal, boolean consumersOnly) {
        int moved = 0;
        for (Direction direction : Direction.values()) {
            if (moved >= maxTotal) break;
            EnergyHandler target = get(direction);
            if (target == null || target == source) continue;
            if (consumersOnly && Transfers.canExtractEnergy(target)) continue;
            moved += EnergyHandlerUtil.move(source, target, maxTotal - moved, null);
        }
        return moved;
    }
}
