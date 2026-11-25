package com.voltaicdynamics.logistics;

import net.neoforged.neoforge.transfer.resource.Resource;
import net.neoforged.neoforge.transfer.TransferPreconditions;
import net.neoforged.neoforge.transfer.ResourceHandler;
import net.neoforged.neoforge.transfer.item.ItemResource;
import net.neoforged.neoforge.transfer.fluid.FluidResource;
import net.neoforged.neoforge.transfer.transaction.TransactionContext;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.fluids.FluidStack;
import org.jetbrains.annotations.Nullable;

/**
 * Holds nothing and never ticks. It registers the cable with its network and, on connector faces, lets the
 * machine behind push straight into the network (auto-eject, hoppers): whatever is accepted is delivered in
 * the same call.
 */
public class LogisticsCableBlockEntity extends BlockEntity {
    private final Object[] faces = new Object[6];

    public LogisticsCableBlockEntity(BlockPos pos, BlockState state) {
        super(VoltaicLogistics.LOGISTICS_CABLE_BE.get(), pos, state);
    }

    @Nullable
    private LogisticsKind kind() {
        return getBlockState().getBlock() instanceof LogisticsCableBlock cable ? cable.kind() : null;
    }

    @Override
    public void onLoad() {
        super.onLoad();
        LogisticsKind kind = kind();
        if (level instanceof ServerLevel serverLevel && kind != null) {
            LogisticsNetworkManager.get(serverLevel, kind).onCableAdded(worldPosition);
        }
    }

    @Override
    public void setRemoved() {
        super.setRemoved();
        LogisticsKind kind = kind();
        if (level instanceof ServerLevel serverLevel && kind != null) {
            LogisticsNetworkManager.get(serverLevel, kind).onCableRemoved(worldPosition);
        }
    }

    @Nullable
    private LogisticsNetwork network() {
        LogisticsKind kind = kind();
        if (!(level instanceof ServerLevel serverLevel) || kind == null || isRemoved()) return null;
        return LogisticsNetworkManager.get(serverLevel, kind).getNetwork(worldPosition);
    }

    private boolean accepts(@Nullable Direction side) {
        return side != null && LogisticsCableBlock.connection(getBlockState(), side) == ConnectionType.BLOCK;
    }

    @Nullable
    public ResourceHandler<ItemResource> getItemHandler(@Nullable Direction side) {
        if (kind() != LogisticsKind.ITEM || !accepts(side)) return null;
        Object face = faces[side.ordinal()];
        if (!(face instanceof ItemFace)) {
            face = new ItemFace(side);
            faces[side.ordinal()] = face;
        }
        return (ItemFace) face;
    }

    @Nullable
    public ResourceHandler<FluidResource> getFluidHandler(@Nullable Direction side) {
        if (kind() != LogisticsKind.FLUID || !accepts(side)) return null;
        Object face = faces[side.ordinal()];
        if (!(face instanceof FluidFace)) {
            face = new FluidFace(side);
            faces[side.ordinal()] = face;
        }
        return (FluidFace) face;
    }

    /** A connector face: what the block behind it pushes in goes straight on to the network. Holds nothing. */
    private abstract class Face<T extends Resource> implements ResourceHandler<T> {
        final Direction side;

        Face(Direction side) {
            this.side = side;
        }

        @Override
        public int size() {
            return 1;
        }

        @Override
        public long getAmountAsLong(int index) {
            return 0;
        }

        @Override
        public boolean isValid(int index, T resource) {
            return true;
        }

        @Override
        public int extract(int index, T resource, int amount, TransactionContext transaction) {
            return 0;
        }
    }

    private final class ItemFace extends Face<ItemResource> {
        ItemFace(Direction side) {
            super(side);
        }

        @Override
        public ItemResource getResource(int index) {
            return ItemResource.EMPTY;
        }

        @Override
        public long getCapacityAsLong(int index, ItemResource resource) {
            return resource.isEmpty() ? 64 : resource.getMaxStackSize();
        }

        @Override
        public int insert(int index, ItemResource resource, int amount, TransactionContext transaction) {
            TransferPreconditions.checkNonEmptyNonNegative(resource, amount);
            LogisticsNetwork network = network();
            if (network == null) return 0;
            return network.insertItem(worldPosition.relative(side), resource, amount, transaction);
        }
    }

    private final class FluidFace extends Face<FluidResource> {
        FluidFace(Direction side) {
            super(side);
        }

        @Override
        public FluidResource getResource(int index) {
            return FluidResource.EMPTY;
        }

        @Override
        public long getCapacityAsLong(int index, FluidResource resource) {
            return LogisticsNetwork.fluidRate();
        }

        @Override
        public int insert(int index, FluidResource resource, int amount, TransactionContext transaction) {
            TransferPreconditions.checkNonEmptyNonNegative(resource, amount);
            LogisticsNetwork network = network();
            if (network == null) return 0;
            return network.insertFluid(worldPosition.relative(side), resource, amount, transaction);
        }
    }
}
