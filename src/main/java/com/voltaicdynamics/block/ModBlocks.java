package com.voltaicdynamics.block;

import com.voltaicdynamics.VoltaicDynamics;
import com.voltaicdynamics.item.ModItems;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.neoforged.neoforge.registries.DeferredBlock;
import net.neoforged.neoforge.registries.DeferredRegister;

public class ModBlocks {
    public static final DeferredRegister.Blocks BLOCKS = DeferredRegister.createBlocks(VoltaicDynamics.MOD_ID);

    // Generators
    public static final DeferredBlock<ThermoelectricGeneratorBlock> THERMOELECTRIC_GENERATOR = BLOCKS.register(
            "thermoelectric_generator",
            () -> new ThermoelectricGeneratorBlock(BlockBehaviour.Properties.of()
                    .strength(3.5F, 6.0F)
                    .sound(SoundType.METAL)
                    .requiresCorrectToolForDrops())
    );

    public static final DeferredBlock<CombustionGeneratorBlock> COMBUSTION_GENERATOR = BLOCKS.register(
            "combustion_generator",
            () -> new CombustionGeneratorBlock(BlockBehaviour.Properties.of()
                    .strength(3.5F, 6.0F)
                    .sound(SoundType.METAL)
                    .requiresCorrectToolForDrops()
                    .lightLevel(state -> state.getValue(CombustionGeneratorBlock.LIT) ? 13 : 0))
    );

    // Batteries / Energy Cells
    public static final DeferredBlock<EnergyCellBlock> BASIC_ENERGY_CELL = BLOCKS.register(
            "basic_energy_cell",
            () -> new EnergyCellBlock(BlockBehaviour.Properties.of()
                    .strength(4.0F, 8.0F)
                    .sound(SoundType.METAL)
                    .requiresCorrectToolForDrops(), false)
    );

    public static final DeferredBlock<EnergyCellBlock> ADVANCED_ENERGY_CELL = BLOCKS.register(
            "advanced_energy_cell",
            () -> new EnergyCellBlock(BlockBehaviour.Properties.of()
                    .strength(5.0F, 10.0F)
                    .sound(SoundType.METAL)
                    .requiresCorrectToolForDrops(), true)
    );

    // Energy Cables
    public static final DeferredBlock<EnergyCableBlock> ENERGY_CABLE = BLOCKS.register(
            "energy_cable",
            () -> new EnergyCableBlock(BlockBehaviour.Properties.of()
                    .strength(0.8F, 2.0F)
                    .sound(SoundType.METAL)
                    .noOcclusion())
    );

    public static void registerBlockItems() {
        ModItems.ITEMS.registerSimpleBlockItem(THERMOELECTRIC_GENERATOR);
        ModItems.ITEMS.registerSimpleBlockItem(COMBUSTION_GENERATOR);
        ModItems.ITEMS.registerSimpleBlockItem(BASIC_ENERGY_CELL);
        ModItems.ITEMS.registerSimpleBlockItem(ADVANCED_ENERGY_CELL);
        ModItems.ITEMS.registerSimpleBlockItem(ENERGY_CABLE);
    }
}
