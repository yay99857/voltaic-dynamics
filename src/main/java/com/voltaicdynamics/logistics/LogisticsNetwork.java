package com.voltaicdynamics.logistics;

import com.voltaicdynamics.VoltaicConfig;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.capabilities.BlockCapabilityCache;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;
import net.neoforged.neoforge.items.IItemHandler;
import net.neoforged.neoforge.items.ItemHandlerHelper;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

/**
 * One connected group of item or fluid cables. Built once by flood fill and kept until a cable or connector
 * changes, so routing never walks the cables again.
 *
 * <p>Why it does not lag:
 * <ul>
 *   <li>No item entities or "items in transit" to tick, sync or render: stacks move straight from the pulling
 *       inventory into the target inventory.</li>
 *   <li>Cables have no ticker; one network tick handles all its connectors, and item networks only act every
 *       {@code itemCable.interval} ticks (staggered by position so networks do not all fire on the same tick).</li>
 *   <li>Neighbour capabilities are cached ({@link BlockCapabilityCache}) and slot scans are bounded per operation.</li>
 * </ul>
 * Targets are served round-robin, so several machines on one line share the flow.
 */
public final class LogisticsNetwork {
    private static final int MAX_SLOTS_SCANNED = 54;

    private final ServerLevel level;
    private final LogisticsKind kind;
    private final Set<BlockPos> cables;
    private final List<Endpoint> targets = new ArrayList<>();
    private final List<Endpoint> pulls = new ArrayList<>();
    private final long phase;
    private int roundRobin;
    private boolean valid = true;

    LogisticsNetwork(ServerLevel level, LogisticsKind kind, Set<BlockPos> cables) {
        this.level = level;
        this.kind = kind;
        this.cables = cables;
        this.phase = cables.isEmpty() ? 0 : Math.floorMod(cables.iterator().next().asLong(), 997);
    }

    void addEndpoint(BlockPos cable, Direction side, boolean pull) {
        BlockPos target = cable.relative(side);
        Endpoint endpoint = new Endpoint(cable, side, target, BlockCapabilityCache.create(kind.capability(), level, target,
                side.getOpposite(), () -> valid, () -> {
                }));
        if (pull) {
            pulls.add(endpoint);
        } else {
            targets.add(endpoint);
        }
    }

    Set<BlockPos> cables() {
        return cables;
    }

    boolean hasEndpointAt(BlockPos pos) {
        for (Endpoint e : targets) if (e.target.equals(pos)) return true;
        for (Endpoint e : pulls) if (e.target.equals(pos)) return true;
        return false;
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

    public int targetCount() {
        return targets.size();
    }

    public int pullCount() {
        return pulls.size();
    }

    static int itemsPerOperation() {
        return VoltaicConfig.get(VoltaicConfig.ITEM_CABLE_ITEMS);
    }

    static int itemInterval() {
        return VoltaicConfig.get(VoltaicConfig.ITEM_CABLE_INTERVAL);
    }

    static int fluidRate() {
        return VoltaicConfig.get(VoltaicConfig.FLUID_CABLE_RATE);
    }

    void tick() {
        if (!valid || pulls.isEmpty() || targets.isEmpty()) return;
        if (kind == LogisticsKind.ITEM) {
            if ((level.getGameTime() + phase) % itemInterval() != 0) return;
            for (Endpoint source : pulls) pullItems(source);
        } else {
            for (Endpoint source : pulls) pullFluid(source);
        }
    }

    // ---- items ----

    private void pullItems(Endpoint source) {
        if (!(source.capability() instanceof IItemHandler from)) return;
        int budget = itemsPerOperation();
        int slots = from.getSlots();
        if (slots <= 0) return;
        int scanned = 0;
        int slot = source.cursor % slots;
        while (budget > 0 && scanned < Math.min(slots, MAX_SLOTS_SCANNED)) {
            ItemStack available = from.extractItem(slot, budget, true);
            if (!available.isEmpty()) {
                ItemStack rest = deliverItem(source.target, available, true);
                int movable = available.getCount() - rest.getCount();
                if (movable > 0) {
                    ItemStack taken = from.extractItem(slot, movable, false);
                    ItemStack leftover = deliverItem(source.target, taken, false);
                    if (!leftover.isEmpty()) {
                        // A target changed its mind between simulate and execute: give the rest back.
                        ItemStack back = from.insertItem(slot, leftover, false);
                        if (!back.isEmpty()) net.minecraft.world.Containers.dropItemStack(level,
                                source.target.getX() + 0.5, source.target.getY() + 0.5, source.target.getZ() + 0.5, back);
                    }
                    budget -= taken.getCount() - leftover.getCount();
                    source.cursor = slot; // keep draining the same slot next time
                    if (budget <= 0) return;
                }
            }
            slot = (slot + 1) % slots;
            scanned++;
        }
        source.cursor = slot;
    }

    /** Hands a stack to the targets, round-robin, skipping the block it came from. Returns what nobody took. */
    private ItemStack deliverItem(@Nullable BlockPos from, ItemStack stack, boolean simulate) {
        ItemStack remaining = stack.copy();
        int count = targets.size();
        int start = roundRobin;
        for (int i = 0; i < count && !remaining.isEmpty(); i++) {
            Endpoint target = targets.get((start + i) % count);
            if (target.target.equals(from)) continue;
            if (!(target.capability() instanceof IItemHandler to)) continue;
            int before = remaining.getCount();
            remaining = ItemHandlerHelper.insertItemStacked(to, remaining, simulate);
            if (!simulate && remaining.getCount() < before) roundRobin = (start + i + 1) % count;
        }
        return remaining;
    }

    /** Items pushed into a connector by the block behind it. */
    public ItemStack insertItem(BlockPos from, ItemStack stack, boolean simulate) {
        if (!valid) return stack;
        return deliverItem(from, stack, simulate);
    }

    // ---- fluids ----

    private void pullFluid(Endpoint source) {
        if (!(source.capability() instanceof IFluidHandler from)) return;
        FluidStack available = from.drain(fluidRate(), IFluidHandler.FluidAction.SIMULATE);
        if (available.isEmpty()) return;
        int accepted = deliverFluid(source.target, available, true);
        if (accepted <= 0) return;
        FluidStack drained = from.drain(available.copyWithAmount(accepted), IFluidHandler.FluidAction.EXECUTE);
        if (drained.isEmpty()) return;
        int delivered = deliverFluid(source.target, drained, false);
        if (delivered < drained.getAmount()) {
            from.fill(drained.copyWithAmount(drained.getAmount() - delivered), IFluidHandler.FluidAction.EXECUTE);
        }
    }

    /** Splits a fluid between the targets that accept it. Returns the amount taken. */
    private int deliverFluid(@Nullable BlockPos from, FluidStack fluid, boolean simulate) {
        int remaining = fluid.getAmount();
        int count = targets.size();
        int start = roundRobin;
        IFluidHandler.FluidAction action = simulate ? IFluidHandler.FluidAction.SIMULATE : IFluidHandler.FluidAction.EXECUTE;
        for (int i = 0; i < count && remaining > 0; i++) {
            Endpoint target = targets.get((start + i) % count);
            if (target.target.equals(from)) continue;
            if (!(target.capability() instanceof IFluidHandler to)) continue;
            int filled = to.fill(fluid.copyWithAmount(remaining), action);
            remaining -= filled;
            if (!simulate && filled > 0) roundRobin = (start + i + 1) % count;
        }
        return fluid.getAmount() - remaining;
    }

    /** Fluid pushed into a connector by the block behind it. */
    public int insertFluid(BlockPos from, FluidStack fluid, boolean simulate) {
        if (!valid) return 0;
        FluidStack limited = fluid.getAmount() > fluidRate() ? fluid.copyWithAmount(fluidRate()) : fluid;
        return deliverFluid(from, limited, simulate);
    }

    private final class Endpoint {
        final BlockPos cable;
        final Direction side;
        final BlockPos target;
        final BlockCapabilityCache<?, Direction> cache;
        int cursor;

        Endpoint(BlockPos cable, Direction side, BlockPos target, BlockCapabilityCache<?, Direction> cache) {
            this.cable = cable;
            this.side = side;
            this.target = target;
            this.cache = cache;
        }

        @Nullable
        Object capability() {
            return valid ? cache.getCapability() : null;
        }
    }
}
