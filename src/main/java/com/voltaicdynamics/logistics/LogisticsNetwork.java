package com.voltaicdynamics.logistics;

import net.neoforged.neoforge.transfer.ResourceHandler;
import net.neoforged.neoforge.transfer.ResourceHandlerUtil;
import net.neoforged.neoforge.transfer.item.ItemResource;
import net.neoforged.neoforge.transfer.fluid.FluidResource;
import net.neoforged.neoforge.transfer.transaction.TransactionContext;
import net.neoforged.neoforge.transfer.transaction.Transaction;
import com.voltaicdynamics.VoltaicConfig;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.capabilities.BlockCapabilityCache;
import net.neoforged.neoforge.fluids.FluidStack;
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

    @SuppressWarnings("unchecked")
    private void pullItems(Endpoint source) {
        if (!(source.capability() instanceof ResourceHandler<?> raw)) return;
        ResourceHandler<ItemResource> from = (ResourceHandler<ItemResource>) raw;
        int budget = itemsPerOperation();
        int slots = from.size();
        if (slots <= 0) return;
        int scanned = 0;
        int slot = source.cursor % slots;
        while (budget > 0 && scanned < Math.min(slots, MAX_SLOTS_SCANNED)) {
            ItemResource resource = from.getResource(slot);
            int available = resource.isEmpty() ? 0 : Math.min(budget, from.getAmountAsInt(slot));
            if (available > 0) {
                int moved = 0;
                int[] last = {-1};
                try (Transaction tx = Transaction.openRoot()) {
                    int delivered = deliverItem(source.target, resource, available, tx, last);
                    // Only what the slot really gives up is delivered; otherwise the whole move is undone.
                    if (delivered > 0 && from.extract(slot, resource, delivered, tx) == delivered) {
                        tx.commit();
                        moved = delivered;
                    }
                }
                if (moved > 0) {
                    if (last[0] >= 0) roundRobin = last[0];
                    budget -= moved;
                    source.cursor = slot; // keep draining the same slot next time
                    if (budget <= 0) return;
                }
            }
            slot = (slot + 1) % slots;
            scanned++;
        }
        source.cursor = slot;
    }

    /**
     * Hands items to the targets, round-robin, skipping the block they came from. Returns how many were taken.
     * {@code last[0]} is set to the round-robin position after the last target that took some.
     */
    @SuppressWarnings("unchecked")
    private int deliverItem(@Nullable BlockPos from, ItemResource resource, int amount, TransactionContext transaction,
                            int[] last) {
        int remaining = amount;
        int count = targets.size();
        int start = roundRobin;
        for (int i = 0; i < count && remaining > 0; i++) {
            Endpoint target = targets.get((start + i) % count);
            if (target.target.equals(from)) continue;
            if (!(target.capability() instanceof ResourceHandler<?> raw)) continue;
            int inserted = ResourceHandlerUtil.insertStacking((ResourceHandler<ItemResource>) raw, resource, remaining, transaction);
            if (inserted > 0) {
                remaining -= inserted;
                last[0] = (start + i + 1) % count;
            }
        }
        return amount - remaining;
    }

    /** Items pushed into a connector by the block behind it. Returns how many were taken. */
    public int insertItem(BlockPos from, ItemResource resource, int amount, TransactionContext transaction) {
        if (!valid) return 0;
        int[] last = {-1};
        int inserted = deliverItem(from, resource, amount, transaction, last);
        if (last[0] >= 0) roundRobin = last[0];
        return inserted;
    }

    // ---- fluids ----

    @SuppressWarnings("unchecked")
    private void pullFluid(Endpoint source) {
        if (!(source.capability() instanceof ResourceHandler<?> raw)) return;
        ResourceHandler<FluidResource> from = (ResourceHandler<FluidResource>) raw;
        FluidResource resource = FluidResource.EMPTY;
        for (int i = 0; i < from.size() && resource.isEmpty(); i++) {
            if (from.getAmountAsLong(i) > 0) resource = from.getResource(i);
        }
        if (resource.isEmpty()) return;
        int[] last = {-1};
        boolean moved = false;
        try (Transaction tx = Transaction.openRoot()) {
            int available;
            try (Transaction probe = Transaction.open(tx)) {
                available = from.extract(resource, fluidRate(), probe);
            }
            if (available <= 0) return;
            int delivered = deliverFluid(source.target, resource, available, tx, last);
            if (delivered > 0 && from.extract(resource, delivered, tx) == delivered) {
                tx.commit();
                moved = true;
            }
        }
        if (moved && last[0] >= 0) roundRobin = last[0];
    }

    /** Splits a fluid between the targets that accept it. Returns the amount taken. */
    @SuppressWarnings("unchecked")
    private int deliverFluid(@Nullable BlockPos from, FluidResource resource, int amount, TransactionContext transaction,
                             int[] last) {
        int remaining = amount;
        int count = targets.size();
        int start = roundRobin;
        for (int i = 0; i < count && remaining > 0; i++) {
            Endpoint target = targets.get((start + i) % count);
            if (target.target.equals(from)) continue;
            if (!(target.capability() instanceof ResourceHandler<?> raw)) continue;
            int filled = ((ResourceHandler<FluidResource>) raw).insert(resource, remaining, transaction);
            if (filled > 0) {
                remaining -= filled;
                last[0] = (start + i + 1) % count;
            }
        }
        return amount - remaining;
    }

    /** Fluid pushed into a connector by the block behind it. */
    public int insertFluid(BlockPos from, FluidResource resource, int amount, TransactionContext transaction) {
        if (!valid) return 0;
        int[] last = {-1};
        int inserted = deliverFluid(from, resource, Math.min(amount, fluidRate()), transaction, last);
        if (last[0] >= 0) roundRobin = last[0];
        return inserted;
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
