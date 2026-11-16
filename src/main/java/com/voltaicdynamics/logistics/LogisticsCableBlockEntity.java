package com.voltaicdynamics.logistics;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;
import net.neoforged.neoforge.items.IItemHandler;
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
    public IItemHandler getItemHandler(@Nullable Direction side) {
        if (kind() != LogisticsKind.ITEM || !accepts(side)) return null;
        Object face = faces[side.ordinal()];
        if (!(face instanceof ItemFace)) {
            face = new ItemFace(side);
            faces[side.ordinal()] = face;
        }
        return (IItemHandler) face;
    }

    @Nullable
    public IFluidHandler getFluidHandler(@Nullable Direction side) {
        if (kind() != LogisticsKind.FLUID || !accepts(side)) return null;
        Object face = faces[side.ordinal()];
        if (!(face instanceof FluidFace)) {
            face = new FluidFace(side);
            faces[side.ordinal()] = face;
        }
        return (IFluidHandler) face;
    }

    private final class ItemFace implements IItemHandler {
        private final Direction side;

        ItemFace(Direction side) {
            this.side = side;
        }

        public int getSlots() {
            return 1;
        }

        public ItemStack getStackInSlot(int slot) {
            return ItemStack.EMPTY;
        }

        public ItemStack insertItem(int slot, ItemStack stack, boolean simulate) {
            LogisticsNetwork network = network();
            if (network == null || stack.isEmpty()) return stack;
            return network.insertItem(worldPosition.relative(side), stack, simulate);
        }

        public ItemStack extractItem(int slot, int amount, boolean simulate) {
            return ItemStack.EMPTY;
        }

        public int getSlotLimit(int slot) {
            return 64;
        }

        public boolean isItemValid(int slot, ItemStack stack) {
            return true;
        }
    }

    private final class FluidFace implements IFluidHandler {
        private final Direction side;

        FluidFace(Direction side) {
            this.side = side;
        }

        public int getTanks() {
            return 1;
        }

        public FluidStack getFluidInTank(int tank) {
            return FluidStack.EMPTY;
        }

        public int getTankCapacity(int tank) {
            return LogisticsNetwork.fluidRate();
        }

        public boolean isFluidValid(int tank, FluidStack stack) {
            return true;
        }

        public int fill(FluidStack resource, FluidAction action) {
            LogisticsNetwork network = network();
            if (network == null || resource.isEmpty()) return 0;
            return network.insertFluid(worldPosition.relative(side), resource, action.simulate());
        }

        public FluidStack drain(FluidStack resource, FluidAction action) {
            return FluidStack.EMPTY;
        }

        public FluidStack drain(int maxDrain, FluidAction action) {
            return FluidStack.EMPTY;
        }
    }
}
