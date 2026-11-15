package com.voltaicdynamics.blockentity;

import com.voltaicdynamics.energy.VoltaicEnergyStorage;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.Fluids;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.energy.IEnergyStorage;

public class ThermoelectricGeneratorBlockEntity extends BlockEntity {
    public static final int CAPACITY = 50_000;
    public static final int MAX_OUTPUT = 1_000;

    private final VoltaicEnergyStorage energyStorage;
    private int lastGenerated = 0;
    private int tickCount = 0;

    public ThermoelectricGeneratorBlockEntity(BlockPos pos, BlockState blockState) {
        super(ModBlockEntities.THERMOELECTRIC_GENERATOR.get(), pos, blockState);
        this.energyStorage = new VoltaicEnergyStorage(CAPACITY, 0, MAX_OUTPUT, 0, this::setChanged);
    }

    public static void tick(Level level, BlockPos pos, BlockState state, ThermoelectricGeneratorBlockEntity entity) {
        if (level == null || level.isClientSide) return;

        entity.tickCount++;

        // Recalculates thermal gradient once per second (20 ticks) for high performance
        if (entity.tickCount % 20 == 0) {
            entity.lastGenerated = entity.calculateGenerationRate(level, pos);
        }

        if (entity.lastGenerated > 0) {
            entity.energyStorage.generateEnergy(entity.lastGenerated);
        }

        // Push energy to adjacent blocks and cables
        entity.pushEnergyToNeighbors(level, pos);
    }

    private int calculateGenerationRate(Level level, BlockPos pos) {
        int hotPoints = 0;
        int coldPoints = 0;

        for (Direction direction : Direction.values()) {
            BlockPos neighborPos = pos.relative(direction);
            BlockState neighborState = level.getBlockState(neighborPos);

            // Heat sources
            if (neighborState.is(Blocks.LAVA) || level.getFluidState(neighborPos).is(Fluids.LAVA)) {
                hotPoints += 20;
            } else if (neighborState.is(Blocks.MAGMA_BLOCK)) {
                hotPoints += 12;
            } else if (neighborState.is(Blocks.CAMPFIRE) || neighborState.is(Blocks.SOUL_CAMPFIRE)
                    || neighborState.is(Blocks.FIRE) || neighborState.is(Blocks.SOUL_FIRE)) {
                hotPoints += 15;
            }

            // Cold sources
            if (neighborState.is(Blocks.BLUE_ICE)) {
                coldPoints += 25;
            } else if (neighborState.is(Blocks.PACKED_ICE)) {
                coldPoints += 16;
            } else if (neighborState.is(Blocks.POWDER_SNOW)) {
                coldPoints += 14;
            } else if (neighborState.is(Blocks.ICE)) {
                coldPoints += 10;
            } else if (neighborState.is(Blocks.SNOW_BLOCK)) {
                coldPoints += 8;
            } else if (neighborState.is(Blocks.SNOW)) {
                coldPoints += 6;
            } else if (neighborState.is(Blocks.WATER) || level.getFluidState(neighborPos).is(Fluids.WATER)) {
                coldPoints += 5;
            }
        }

        // Ambient Cold: if placed in a snowy/sub-zero biome (like in snowy plains/mountains)
        if (level.getBiome(pos).value().coldEnoughToSnow(pos)) {
            coldPoints += 8;
        }

        if (hotPoints > 0 && coldPoints > 0) {
            // Seebeck thermal gradient equation
            int generation = (hotPoints * coldPoints) / 15;
            return Math.min(generation, 100); // Early-game balanced cap at 100 FE/t
        }

        return 0;
    }

    private void pushEnergyToNeighbors(Level level, BlockPos pos) {
        if (this.energyStorage.getEnergyStored() <= 0) return;

        for (Direction direction : Direction.values()) {
            BlockPos targetPos = pos.relative(direction);
            IEnergyStorage target = level.getCapability(Capabilities.EnergyStorage.BLOCK, targetPos, direction.getOpposite());
            if (target != null && target.canReceive()) {
                int toSend = Math.min(this.energyStorage.getEnergyStored(), MAX_OUTPUT);
                int accepted = target.receiveEnergy(toSend, false);
                if (accepted > 0) {
                    this.energyStorage.extractEnergy(accepted, false);
                }
                if (this.energyStorage.getEnergyStored() <= 0) break;
            }
        }
    }

    public IEnergyStorage getEnergyStorage(Direction side) {
        return this.energyStorage;
    }

    public int getLastGenerated() {
        return this.lastGenerated;
    }

    public int getEnergyStored() {
        return this.energyStorage.getEnergyStored();
    }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        tag.put("Energy", this.energyStorage.serializeNBT(registries));
        tag.putInt("LastGenerated", this.lastGenerated);
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        if (tag.contains("Energy")) {
            this.energyStorage.deserializeNBT(registries, tag.getCompound("Energy"));
        }
        this.lastGenerated = tag.getInt("LastGenerated");
    }
}
