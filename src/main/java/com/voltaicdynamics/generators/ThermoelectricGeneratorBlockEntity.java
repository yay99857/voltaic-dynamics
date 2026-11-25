package com.voltaicdynamics.generators;

import net.neoforged.neoforge.transfer.energy.EnergyHandler;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import com.voltaicdynamics.core.blockentity.VoltaicBlockEntity;
import com.voltaicdynamics.VoltaicConfig;
import com.voltaicdynamics.core.energy.NeighborEnergyCache;
import com.voltaicdynamics.core.energy.VoltaicEnergyStorage;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.level.material.Fluids;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

public class ThermoelectricGeneratorBlockEntity extends VoltaicBlockEntity {
    public static final int MAX_OUTPUT = 1_000;

    private final VoltaicEnergyStorage energyStorage;
    private final NeighborEnergyCache neighbors = new NeighborEnergyCache(this);
    private int lastGenerated = 0;
    private int tickCount = 0;

    public ThermoelectricGeneratorBlockEntity(BlockPos pos, BlockState blockState) {
        super(VoltaicGenerators.THERMOELECTRIC_GENERATOR_BE.get(), pos, blockState);
        this.energyStorage = new VoltaicEnergyStorage(VoltaicConfig.get(VoltaicConfig.THERMO_CAPACITY),
                0, MAX_OUTPUT, this::saveChanges);
    }

    public static void tick(Level level, BlockPos pos, BlockState state, ThermoelectricGeneratorBlockEntity entity) {
        // Recalculates the thermal gradient once per second; the offset spreads generators over different ticks.
        if ((entity.tickCount++ + pos.hashCode()) % 20 == 0) {
            entity.lastGenerated = entity.calculateGenerationRate(level, pos, true);
        }
        if (entity.lastGenerated > 0) {
            entity.energyStorage.generateEnergy(entity.lastGenerated);
        }
        entity.neighbors.push(entity.energyStorage, MAX_OUTPUT, false);
    }

    private enum Kind { HOT, COLD }

    private record Source(Kind kind, int points, @Nullable BlockState wornInto) {
    }

    @Nullable
    private static Source classify(Level level, BlockPos pos, BlockState state) {
        FluidState fluid = level.getFluidState(pos);
        if (fluid.is(Fluids.LAVA)) {
            return new Source(Kind.HOT, 20, Blocks.OBSIDIAN.defaultBlockState());
        }
        if (fluid.is(Fluids.FLOWING_LAVA)) {
            return new Source(Kind.HOT, 20, null);
        }
        if (state.is(Blocks.MAGMA_BLOCK)) return new Source(Kind.HOT, 12, Blocks.NETHERRACK.defaultBlockState());
        if (state.is(Blocks.CAMPFIRE) || state.is(Blocks.SOUL_CAMPFIRE) || state.is(Blocks.FIRE) || state.is(Blocks.SOUL_FIRE)) {
            return new Source(Kind.HOT, 15, null);
        }

        if (state.is(Blocks.BLUE_ICE)) return new Source(Kind.COLD, 25, Blocks.PACKED_ICE.defaultBlockState());
        if (state.is(Blocks.PACKED_ICE)) return new Source(Kind.COLD, 16, Blocks.ICE.defaultBlockState());
        if (state.is(Blocks.POWDER_SNOW)) return new Source(Kind.COLD, 14, Blocks.AIR.defaultBlockState());
        if (state.is(Blocks.ICE)) {
            BlockState melted = level.environmentAttributes().getValue(net.minecraft.world.attribute.EnvironmentAttributes.WATER_EVAPORATES, pos) ? Blocks.AIR.defaultBlockState() : Blocks.WATER.defaultBlockState();
            return new Source(Kind.COLD, 10, melted);
        }
        if (state.is(Blocks.SNOW_BLOCK)) return new Source(Kind.COLD, 8, Blocks.AIR.defaultBlockState());
        if (state.is(Blocks.SNOW)) return new Source(Kind.COLD, 6, Blocks.AIR.defaultBlockState());
        if (fluid.is(Fluids.WATER) || fluid.is(Fluids.FLOWING_WATER)) return new Source(Kind.COLD, 5, null);
        return null;
    }

    /**
     * Seebeck gradient: (heat x cold) / 15, capped by config. When {@code applyWear} is set the
     * contributing blocks may slowly degrade (lava cools, ice melts) so the power is not free forever.
     */
    public int calculateGenerationRate(Level level, BlockPos pos, boolean applyWear) {
        int hotPoints = 0;
        int coldPoints = 0;
        List<BlockPos> wearable = new ArrayList<>();
        List<BlockState> wornStates = new ArrayList<>();

        for (Direction direction : Direction.values()) {
            BlockPos neighborPos = pos.relative(direction);
            BlockState neighborState = level.getBlockState(neighborPos);
            Source source = classify(level, neighborPos, neighborState);
            if (source == null) continue;
            if (source.kind() == Kind.HOT) {
                hotPoints += source.points();
            } else {
                coldPoints += source.points();
            }
            if (source.wornInto() != null) {
                wearable.add(neighborPos);
                wornStates.add(source.wornInto());
            }
        }

        // Ambient cold: snowy biomes help the cold side.
        if (level.getBiome(pos).value().coldEnoughToSnow(pos, level.getSeaLevel())) {
            coldPoints += 8;
        }

        if (hotPoints <= 0 || coldPoints <= 0) return 0;

        if (applyWear) {
            double chance = VoltaicConfig.get(VoltaicConfig.THERMO_WEAR_CHANCE);
            if (chance > 0) {
                for (int i = 0; i < wearable.size(); i++) {
                    if (level.getRandom().nextDouble() < chance) {
                        BlockPos wornPos = wearable.get(i);
                        level.setBlock(wornPos, wornStates.get(i), Block.UPDATE_ALL);
                        level.playSound(null, wornPos, SoundEvents.LAVA_EXTINGUISH, SoundSource.BLOCKS, 0.4F, 1.6F);
                    }
                }
            }
        }

        int generation = (hotPoints * coldPoints) / 15;
        return Math.min(generation, VoltaicConfig.get(VoltaicConfig.THERMO_MAX_GENERATION));
    }

    public EnergyHandler getEnergyStorage(@Nullable Direction side) {
        return energyStorage;
    }

    public int getLastGenerated() {
        return lastGenerated;
    }

    public int getEnergyStored() {
        return energyStorage.getEnergyStored();
    }

    public int getCapacity() {
        return energyStorage.getMaxEnergyStored();
    }

    @Override
    protected void saveAdditional(ValueOutput output) {
        super.saveAdditional(output);
        energyStorage.serialize(output);
        output.putInt("LastGenerated", lastGenerated);
    }

    @Override
    protected void loadAdditional(ValueInput input) {
        super.loadAdditional(input);
        energyStorage.deserialize(input);
        lastGenerated = input.getIntOr("LastGenerated", 0);
    }
}
