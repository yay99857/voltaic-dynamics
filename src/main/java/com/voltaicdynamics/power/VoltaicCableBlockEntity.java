package com.voltaicdynamics.power;

import com.voltaicdynamics.VoltaicConfig;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.energy.IEnergyStorage;
import org.jetbrains.annotations.Nullable;

/**
 * The cable itself holds no energy and does not tick: it only exposes a face of its
 * {@link CableNetwork}, which routes energy in the same call (see {@link CableNetworkManager}).
 */
public class VoltaicCableBlockEntity extends BlockEntity {
    private final IEnergyStorage[] faces = new IEnergyStorage[7];

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

    public IEnergyStorage getEnergyStorage(@Nullable Direction side) {
        int index = side == null ? 6 : side.ordinal();
        IEnergyStorage face = faces[index];
        if (face == null) {
            face = new Face(side);
            faces[index] = face;
        }
        return face;
    }

    private final class Face implements IEnergyStorage {
        @Nullable
        private final Direction side;

        private Face(@Nullable Direction side) {
            this.side = side;
        }

        @Override
        public int receiveEnergy(int maxReceive, boolean simulate) {
            if (!(level instanceof ServerLevel serverLevel) || isRemoved()) return 0;
            CableNetwork network = CableNetworkManager.get(serverLevel).getNetwork(worldPosition);
            return network == null ? 0 : network.insert(worldPosition, side, maxReceive, simulate);
        }

        @Override
        public int extractEnergy(int maxExtract, boolean simulate) {
            return 0;
        }

        @Override
        public int getEnergyStored() {
            return 0;
        }

        @Override
        public int getMaxEnergyStored() {
            return VoltaicConfig.get(VoltaicConfig.CABLE_TRANSFER);
        }

        @Override
        public boolean canExtract() {
            return false;
        }

        @Override
        public boolean canReceive() {
            return true;
        }
    }
}
