package com.voltaicdynamics.blockentity;

import com.voltaicdynamics.block.EnergyCableBlock;
import com.voltaicdynamics.energy.VoltaicEnergyStorage;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.energy.IEnergyStorage;

import java.util.ArrayList;
import java.util.List;

public class EnergyCableBlockEntity extends BlockEntity {
    public static final int BUFFER_CAPACITY = 4_000;
    public static final int MAX_TRANSFER = 2_000;

    private final VoltaicEnergyStorage energyStorage;

    public EnergyCableBlockEntity(BlockPos pos, BlockState blockState) {
        super(ModBlockEntities.ENERGY_CABLE.get(), pos, blockState);
        this.energyStorage = new VoltaicEnergyStorage(BUFFER_CAPACITY, MAX_TRANSFER, MAX_TRANSFER, 0, this::setChanged);
    }

    public static void tick(Level level, BlockPos pos, BlockState state, EnergyCableBlockEntity entity) {
        if (level == null || level.isClientSide) return;

        // =========================================================================
        // STEP 1: ACTIVE PULL (Suck energy from connected generators & pure producers)
        // =========================================================================
        int space = BUFFER_CAPACITY - entity.energyStorage.getEnergyStored();
        if (space > 0) {
            for (Direction direction : Direction.values()) {
                if (EnergyCableBlock.isConnected(state, direction)) {
                    BlockPos neighborPos = pos.relative(direction);
                    BlockState neighborState = level.getBlockState(neighborPos);

                    // Never pull from another cable
                    if (!(neighborState.getBlock() instanceof EnergyCableBlock)) {
                        IEnergyStorage source = level.getCapability(Capabilities.EnergyStorage.BLOCK, neighborPos, direction.getOpposite());
                        // Prioritize pulling from pure generators/sources (cannot receive) or full sources
                        if (source != null && source.canExtract() && source.getEnergyStored() > 0) {
                            if (!source.canReceive()) {
                                int toExtract = Math.min(space, MAX_TRANSFER);
                                int extracted = source.extractEnergy(toExtract, false);
                                if (extracted > 0) {
                                    entity.energyStorage.generateEnergy(extracted);
                                    space -= extracted;
                                    if (space <= 0) break;
                                }
                            }
                        }
                    }
                }
            }
        }

        // =========================================================================
        // STEP 2: ACTIVE PUSH (Deliver energy to connected batteries & machines)
        // =========================================================================
        int currentEnergy = entity.energyStorage.getEnergyStored();
        if (currentEnergy <= 0) return;

        List<IEnergyStorage> validReceivers = new ArrayList<>();

        // Priority 1: Deliver to external machines, batteries (Energy Cells), and receivers
        for (Direction direction : Direction.values()) {
            if (EnergyCableBlock.isConnected(state, direction)) {
                BlockPos neighborPos = pos.relative(direction);
                BlockState neighborState = level.getBlockState(neighborPos);

                if (!(neighborState.getBlock() instanceof EnergyCableBlock)) {
                    IEnergyStorage target = level.getCapability(Capabilities.EnergyStorage.BLOCK, neighborPos, direction.getOpposite());
                    if (target != null && target.canReceive()) {
                        validReceivers.add(target);
                    }
                }
            }
        }

        // Priority 2: If no machines connected to this cable, pass along to adjacent cables
        if (validReceivers.isEmpty()) {
            for (Direction direction : Direction.values()) {
                if (EnergyCableBlock.isConnected(state, direction)) {
                    BlockPos neighborPos = pos.relative(direction);
                    BlockState neighborState = level.getBlockState(neighborPos);

                    if (neighborState.getBlock() instanceof EnergyCableBlock) {
                        IEnergyStorage target = level.getCapability(Capabilities.EnergyStorage.BLOCK, neighborPos, direction.getOpposite());
                        if (target != null && target.canReceive() && target.getEnergyStored() < currentEnergy) {
                            validReceivers.add(target);
                        }
                    }
                }
            }
        }

        // Distribute evenly among all receivers
        if (!validReceivers.isEmpty()) {
            int toDistribute = Math.min(currentEnergy, MAX_TRANSFER);
            int perReceiver = Math.max(1, toDistribute / validReceivers.size());

            for (IEnergyStorage receiver : validReceivers) {
                int accepted = receiver.receiveEnergy(perReceiver, false);
                if (accepted > 0) {
                    entity.energyStorage.extractEnergy(accepted, false);
                }
                if (entity.energyStorage.getEnergyStored() <= 0) break;
            }
        }
    }

    public IEnergyStorage getEnergyStorage(Direction side) {
        return this.energyStorage;
    }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        tag.put("Energy", this.energyStorage.serializeNBT(registries));
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        if (tag.contains("Energy")) {
            this.energyStorage.deserializeNBT(registries, tag.getCompound("Energy"));
        }
    }
}
