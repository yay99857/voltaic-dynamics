package com.voltaicdynamics.blockentity;

import com.voltaicdynamics.VoltaicDynamics;
import com.voltaicdynamics.block.ModBlocks;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

public class ModBlockEntities {
    public static final DeferredRegister<BlockEntityType<?>> BLOCK_ENTITIES =
            DeferredRegister.create(Registries.BLOCK_ENTITY_TYPE, VoltaicDynamics.MOD_ID);

    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<ThermoelectricGeneratorBlockEntity>> THERMOELECTRIC_GENERATOR =
            BLOCK_ENTITIES.register("thermoelectric_generator", () ->
                    BlockEntityType.Builder.of(
                            ThermoelectricGeneratorBlockEntity::new,
                            ModBlocks.THERMOELECTRIC_GENERATOR.get()
                    ).build(null)
            );

    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<CombustionGeneratorBlockEntity>> COMBUSTION_GENERATOR =
            BLOCK_ENTITIES.register("combustion_generator", () ->
                    BlockEntityType.Builder.of(
                            CombustionGeneratorBlockEntity::new,
                            ModBlocks.COMBUSTION_GENERATOR.get()
                    ).build(null)
            );

    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<EnergyCellBlockEntity>> BASIC_ENERGY_CELL =
            BLOCK_ENTITIES.register("basic_energy_cell", () ->
                    BlockEntityType.Builder.of(
                            ModBlockEntities::createBasicEnergyCell,
                            ModBlocks.BASIC_ENERGY_CELL.get()
                    ).build(null)
            );

    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<EnergyCellBlockEntity>> ADVANCED_ENERGY_CELL =
            BLOCK_ENTITIES.register("advanced_energy_cell", () ->
                    BlockEntityType.Builder.of(
                            ModBlockEntities::createAdvancedEnergyCell,
                            ModBlocks.ADVANCED_ENERGY_CELL.get()
                    ).build(null)
            );

    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<EnergyCableBlockEntity>> ENERGY_CABLE =
            BLOCK_ENTITIES.register("energy_cable", () ->
                    BlockEntityType.Builder.of(
                            EnergyCableBlockEntity::new,
                            ModBlocks.ENERGY_CABLE.get()
                    ).build(null)
            );

    private static EnergyCellBlockEntity createBasicEnergyCell(BlockPos pos, BlockState state) {
        return new EnergyCellBlockEntity(BASIC_ENERGY_CELL.get(), pos, state, 1_000_000, 2_000, false);
    }

    private static EnergyCellBlockEntity createAdvancedEnergyCell(BlockPos pos, BlockState state) {
        return new EnergyCellBlockEntity(ADVANCED_ENERGY_CELL.get(), pos, state, 8_000_000, 10_000, true);
    }
}
