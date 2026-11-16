package com.voltaicdynamics.core.sideconfig;

import com.voltaicdynamics.core.energy.EnergyBuffer;
import com.voltaicdynamics.core.energy.RecentInputFaces;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.capabilities.BlockCapabilityCache;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.energy.IEnergyStorage;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;
import net.neoforged.neoforge.items.IItemHandler;
import net.neoforged.neoforge.items.ItemHandlerHelper;
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
        if (level == null || level.isClientSide) return;
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
        private final BlockCapabilityCache<IItemHandler, Direction>[] items = new BlockCapabilityCache[6];
        @SuppressWarnings("unchecked")
        private final BlockCapabilityCache<IFluidHandler, Direction>[] fluids = new BlockCapabilityCache[6];
        @SuppressWarnings("unchecked")
        private final BlockCapabilityCache<IEnergyStorage, Direction>[] energy = new BlockCapabilityCache[6];

        public Neighbors(BlockEntity owner) {
            this.owner = owner;
        }

        @Nullable
        public IItemHandler items(Direction dir) {
            if (!(owner.getLevel() instanceof ServerLevel level)) return null;
            int i = dir.ordinal();
            if (items[i] == null) items[i] = BlockCapabilityCache.create(Capabilities.ItemHandler.BLOCK, level,
                    owner.getBlockPos().relative(dir), dir.getOpposite());
            return items[i].getCapability();
        }

        @Nullable
        public IFluidHandler fluids(Direction dir) {
            if (!(owner.getLevel() instanceof ServerLevel level)) return null;
            int i = dir.ordinal();
            if (fluids[i] == null) fluids[i] = BlockCapabilityCache.create(Capabilities.FluidHandler.BLOCK, level,
                    owner.getBlockPos().relative(dir), dir.getOpposite());
            return fluids[i].getCapability();
        }

        @Nullable
        public BlockEntity blockEntity(Direction dir) {
            Level level = owner.getLevel();
            return level == null ? null : level.getBlockEntity(owner.getBlockPos().relative(dir));
        }

        @Nullable
        public IEnergyStorage energy(Direction dir) {
            if (!(owner.getLevel() instanceof ServerLevel level)) return null;
            int i = dir.ordinal();
            if (energy[i] == null) energy[i] = BlockCapabilityCache.create(Capabilities.EnergyStorage.BLOCK, level,
                    owner.getBlockPos().relative(dir), dir.getOpposite());
            return energy[i].getCapability();
        }
    }

    /**
     * Pushes up to {@code maxItems} items from {@code slots} of {@code source} into the neighbours on faces whose
     * item mode can output. Returns the number of items moved.
     */
    public static int ejectItems(SideConfigurable machine, Neighbors neighbors, IItemHandler source, int[] slots, int maxItems) {
        int moved = 0;
        for (Direction dir : Direction.values()) {
            if (moved >= maxItems) break;
            if (!machine.getSideConfig().mode(TransferType.ITEM, dir, machine.getFacing()).canOutput()) continue;
            IItemHandler target = neighbors.items(dir);
            if (target == null) continue;
            for (int slot : slots) {
                if (moved >= maxItems) break;
                ItemStack stack = source.extractItem(slot, maxItems - moved, true);
                if (stack.isEmpty()) continue;
                ItemStack rest = ItemHandlerHelper.insertItemStacked(target, stack, true);
                int accepted = stack.getCount() - rest.getCount();
                if (accepted <= 0) continue;
                ItemStack taken = source.extractItem(slot, accepted, false);
                ItemStack leftover = ItemHandlerHelper.insertItemStacked(target, taken, false);
                if (!leftover.isEmpty()) source.insertItem(slot, leftover, false);
                moved += taken.getCount() - leftover.getCount();
            }
        }
        return moved;
    }

    /** Pushes fluid out of output faces, up to {@code maxAmount} mB in total. */
    public static int ejectFluid(SideConfigurable machine, Neighbors neighbors, IFluidHandler source, int maxAmount) {
        int moved = 0;
        for (Direction dir : Direction.values()) {
            if (moved >= maxAmount) break;
            if (!machine.getSideConfig().mode(TransferType.FLUID, dir, machine.getFacing()).canOutput()) continue;
            IFluidHandler target = neighbors.fluids(dir);
            if (target == null) continue;
            FluidStack available = source.drain(maxAmount - moved, IFluidHandler.FluidAction.SIMULATE);
            if (available.isEmpty()) break;
            int accepted = target.fill(available, IFluidHandler.FluidAction.SIMULATE);
            if (accepted <= 0) continue;
            FluidStack drained = source.drain(accepted, IFluidHandler.FluidAction.EXECUTE);
            moved += target.fill(drained, IFluidHandler.FluidAction.EXECUTE);
        }
        return moved;
    }

    /**
     * Pushes energy out of output faces.
     *
     * @param skipBuffers skips neighbouring {@link EnergyBuffer}s (other Accumulators), so batteries do not feed
     *                    each other. It does not look at {@code canExtract()}: other mods' cables report it too
     *                    (Mekanism's Universal Cable does), and they must still be fed.
     */
    public static int ejectEnergy(SideConfigurable machine, Neighbors neighbors, IEnergyStorage source, int maxTotal,
                                  boolean skipBuffers) {
        return ejectEnergy(machine, neighbors, source, maxTotal, skipBuffers, null);
    }

    /** As above; {@code inputFaces}, when given, keeps faces that just received energy from being pushed into. */
    public static int ejectEnergy(SideConfigurable machine, Neighbors neighbors, IEnergyStorage source, int maxTotal,
                                  boolean skipBuffers, @Nullable RecentInputFaces inputFaces) {
        int budget = source.extractEnergy(maxTotal, true);
        if (budget <= 0) return 0;
        int moved = 0;
        for (Direction dir : Direction.values()) {
            if (!machine.getSideConfig().mode(TransferType.ENERGY, dir, machine.getFacing()).canOutput()) continue;
            IEnergyStorage target = neighbors.energy(dir);
            if (target == null || target == source || !target.canReceive()) continue;
            if (inputFaces != null && inputFaces.receivedRecently(dir)) continue;
            if (skipBuffers && neighbors.blockEntity(dir) instanceof EnergyBuffer) continue;
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
