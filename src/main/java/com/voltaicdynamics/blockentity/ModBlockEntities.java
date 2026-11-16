package com.voltaicdynamics.blockentity;

import com.voltaicdynamics.VoltaicDynamics;
import com.voltaicdynamics.block.ModBlocks;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.level.block.entity.BlockEntityType;
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

    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<ElectricFurnaceBlockEntity>> ELECTRIC_FURNACE =
            BLOCK_ENTITIES.register("electric_furnace", () ->
                    BlockEntityType.Builder.of(
                            ElectricFurnaceBlockEntity::new,
                            ModBlocks.ELECTRIC_FURNACE.get()
                    ).build(null)
            );

    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<EnergyCellBlockEntity>> BASIC_ENERGY_CELL =
            BLOCK_ENTITIES.register("basic_energy_cell", () ->
                    BlockEntityType.Builder.of(
                            EnergyCellBlockEntity::new,
                            ModBlocks.BASIC_ENERGY_CELL.get()
                    ).build(null)
            );

    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<EnergyCellBlockEntity>> ADVANCED_ENERGY_CELL =
            BLOCK_ENTITIES.register("advanced_energy_cell", () ->
                    BlockEntityType.Builder.of(
                            EnergyCellBlockEntity::new,
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
}
