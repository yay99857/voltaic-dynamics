package com.voltaicdynamics.core.sideconfig;

import com.voltaicdynamics.core.energy.EnergyBuffer;
import com.voltaicdynamics.core.energy.RecentInputFaces;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.capabilities.BlockCapabilityCache;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.transfer.ResourceHandler;
import net.neoforged.neoforge.transfer.ResourceHandlerUtil;
import net.neoforged.neoforge.transfer.energy.EnergyHandler;
import net.neoforged.neoforge.transfer.energy.EnergyHandlerUtil;
import net.neoforged.neoforge.transfer.fluid.FluidResource;
import net.neoforged.neoforge.transfer.item.ItemResource;
import net.neoforged.neoforge.transfer.transaction.Transaction;
import org.jetbrains.annotations.Nullable;

/** Shared server-side plumbing for side-configurable machines: applying changes and auto-eject. */
public final class SideConfigHelper {
    private SideConfigHelper() {
    }

    /**
     * After a face changed: drop the cached capability views, tell capability caches (cables, pipes, other mods)
     * and let neighbouring cables re-evaluate their connection shape.
     */
    public static void applyChange(BlockEntity be, SidedHandlers handlers) {
        handlers.invalidate();
        be.setChanged();
        Level level = be.getLevel();
        if (level == null || level.isClientSide()) return;
        BlockPos pos = be.getBlockPos();
        BlockState state = be.getBlockState();
        level.invalidateCapabilities(pos);
        state.updateNeighbourShapes(level, pos, Block.UPDATE_ALL);
        level.updateNeighborsAt(pos, state.getBlock());
    }

    /** Cached neighbour capabilities used for auto-eject. */
    public static final class Neighbors {
        private final BlockEntity owner;
        @SuppressWarnings("unchecked")
        private final BlockCapabilityCache<ResourceHandler<ItemResource>, Direction>[] items = new BlockCapabilityCache[6];
        @SuppressWarnings("unchecked")
        private final BlockCapabilityCache<ResourceHandler<FluidResource>, Direction>[] fluids = new BlockCapabilityCache[6];
        @SuppressWarnings("unchecked")
        private final BlockCapabilityCache<EnergyHandler, Direction>[] energy = new BlockCapabilityCache[6];

        public Neighbors(BlockEntity owner) {
            this.owner = owner;
        }

        @Nullable
        public ResourceHandler<ItemResource> items(Direction dir) {
            if (!(owner.getLevel() instanceof ServerLevel level)) return null;
            int i = dir.ordinal();
            if (items[i] == null) items[i] = BlockCapabilityCache.create(Capabilities.Item.BLOCK, level,
                    owner.getBlockPos().relative(dir), dir.getOpposite());
            return items[i].getCapability();
        }

        @Nullable
        public ResourceHandler<FluidResource> fluids(Direction dir) {
            if (!(owner.getLevel() instanceof ServerLevel level)) return null;
            int i = dir.ordinal();
            if (fluids[i] == null) fluids[i] = BlockCapabilityCache.create(Capabilities.Fluid.BLOCK, level,
                    owner.getBlockPos().relative(dir), dir.getOpposite());
            return fluids[i].getCapability();
        }

        @Nullable
        public BlockEntity blockEntity(Direction dir) {
            Level level = owner.getLevel();
            return level == null ? null : level.getBlockEntity(owner.getBlockPos().relative(dir));
        }

        @Nullable
        public EnergyHandler energy(Direction dir) {
            if (!(owner.getLevel() instanceof ServerLevel level)) return null;
            int i = dir.ordinal();
            if (energy[i] == null) energy[i] = BlockCapabilityCache.create(Capabilities.Energy.BLOCK, level,
                    owner.getBlockPos().relative(dir), dir.getOpposite());
            return energy[i].getCapability();
        }
    }

    /**
     * Pushes up to {@code maxItems} items from {@code slots} of {@code source} into the neighbours on faces whose
     * item mode can output. Returns the number of items moved.
     */
    public static int ejectItems(SideConfigurable machine, Neighbors neighbors, ResourceHandler<ItemResource> source,
                                 int[] slots, int maxItems) {
        int moved = 0;
        for (Direction dir : Direction.values()) {
            if (moved >= maxItems) break;
            if (!machine.getSideConfig().mode(TransferType.ITEM, dir, machine.getFacing()).canOutput()) continue;
            ResourceHandler<ItemResource> target = neighbors.items(dir);
            if (target == null) continue;
            for (int slot : slots) {
                if (moved >= maxItems) break;
                ItemResource resource = source.getResource(slot);
                int available = Math.min(source.getAmountAsInt(slot), maxItems - moved);
                if (resource.isEmpty() || available <= 0) continue;
                try (Transaction tx = Transaction.openRoot()) {
                    int inserted = ResourceHandlerUtil.insertStacking(target, resource, available, tx);
                    if (inserted <= 0) continue;
                    if (source.extract(slot, resource, inserted, tx) != inserted) continue;
                    tx.commit();
                    moved += inserted;
                }
            }
        }
        return moved;
    }

    /** Pushes fluid out of output faces, up to {@code maxAmount} mB in total. */
    public static int ejectFluid(SideConfigurable machine, Neighbors neighbors, ResourceHandler<FluidResource> source,
                                 int maxAmount) {
        int moved = 0;
        for (Direction dir : Direction.values()) {
            if (moved >= maxAmount) break;
            if (!machine.getSideConfig().mode(TransferType.FLUID, dir, machine.getFacing()).canOutput()) continue;
            ResourceHandler<FluidResource> target = neighbors.fluids(dir);
            if (target == null) continue;
            moved += ResourceHandlerUtil.move(source, target, r -> true, maxAmount - moved, null);
        }
        return moved;
    }

    /**
     * Pushes energy out of output faces.
     *
     * @param skipBuffers skips neighbouring {@link EnergyBuffer}s (other Accumulators), so batteries do not feed
     *                    each other. Other mods' cables can also give energy back, and must still be fed.
     */
    public static int ejectEnergy(SideConfigurable machine, Neighbors neighbors, EnergyHandler source, int maxTotal,
                                  boolean skipBuffers) {
        return ejectEnergy(machine, neighbors, source, maxTotal, skipBuffers, null);
    }

    /** As above; {@code inputFaces}, when given, keeps faces that just received energy from being pushed into. */
    public static int ejectEnergy(SideConfigurable machine, Neighbors neighbors, EnergyHandler source, int maxTotal,
                                  boolean skipBuffers, @Nullable RecentInputFaces inputFaces) {
        int moved = 0;
        for (Direction dir : Direction.values()) {
            if (moved >= maxTotal) break;
            if (!machine.getSideConfig().mode(TransferType.ENERGY, dir, machine.getFacing()).canOutput()) continue;
            EnergyHandler target = neighbors.energy(dir);
            if (target == null || target == source) continue;
            if (inputFaces != null && inputFaces.receivedRecently(dir)) continue;
            if (skipBuffers && neighbors.blockEntity(dir) instanceof EnergyBuffer) continue;
            moved += EnergyHandlerUtil.move(source, target, maxTotal - moved, null);
        }
        return moved;
    }
}
