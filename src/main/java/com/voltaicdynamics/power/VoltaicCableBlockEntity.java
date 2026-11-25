package com.voltaicdynamics.power;

import net.neoforged.neoforge.transfer.TransferPreconditions;
import net.neoforged.neoforge.transfer.energy.EnergyHandler;
import net.neoforged.neoforge.transfer.transaction.TransactionContext;
import com.voltaicdynamics.VoltaicConfig;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.Nullable;

/**
 * The cable itself holds no energy and does not tick: it only exposes a face of its
 * {@link CableNetwork}, which routes energy in the same call (see {@link CableNetworkManager}).
 */
public class VoltaicCableBlockEntity extends BlockEntity {
    private final EnergyHandler[] faces = new EnergyHandler[7];

    public VoltaicCableBlockEntity(BlockPos pos, BlockState blockState) {
        super(VoltaicPower.VOLTAIC_CABLE_BE.get(), pos, blockState);
    }

    @Override
    public void onLoad() {
        super.onLoad();
        if (level instanceof ServerLevel serverLevel) {
            CableNetworkManager.get(serverLevel).onCableAdded(worldPosition);
        }
    }

    @Override
    public void setRemoved() {
        super.setRemoved();
        if (level instanceof ServerLevel serverLevel) {
            CableNetworkManager.get(serverLevel).onCableRemoved(worldPosition);
        }
    }

    public EnergyHandler getEnergyStorage(@Nullable Direction side) {
        int index = side == null ? 6 : side.ordinal();
        EnergyHandler face = faces[index];
        if (face == null) {
            face = new Face(side);
            faces[index] = face;
        }
        return face;
    }

    /** A cable face: passes what it is given straight on to the network, stores nothing and gives nothing. */
    private final class Face implements EnergyHandler {
        @Nullable
        private final Direction side;

        private Face(@Nullable Direction side) {
            this.side = side;
        }

        @Override
        public int insert(int amount, TransactionContext transaction) {
            TransferPreconditions.checkNonNegative(amount);
            if (!(level instanceof ServerLevel serverLevel) || isRemoved()) return 0;
            CableNetwork network = CableNetworkManager.get(serverLevel).getNetwork(worldPosition);
            return network == null ? 0 : network.insert(worldPosition, side, amount, transaction);
        }

        @Override
        public int extract(int amount, TransactionContext transaction) {
            return 0;
        }

        @Override
        public long getAmountAsLong() {
            return 0;
        }

        @Override
        public long getCapacityAsLong() {
            return VoltaicConfig.get(VoltaicConfig.CABLE_TRANSFER);
        }
    }
}
