package com.voltaicdynamics.block;

import com.voltaicdynamics.VoltaicDynamics;
import com.voltaicdynamics.item.EnergyCellItem;
import com.voltaicdynamics.item.ModItems;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.neoforged.neoforge.registries.DeferredBlock;
import net.neoforged.neoforge.registries.DeferredRegister;

public class ModBlocks {
    public static final DeferredRegister.Blocks BLOCKS = DeferredRegister.createBlocks(VoltaicDynamics.MOD_ID);

    // Generators
    public static final DeferredBlock<ThermoelectricGeneratorBlock> THERMOELECTRIC_GENERATOR = BLOCKS.register(
            "thermoelectric_generator",
            () -> new ThermoelectricGeneratorBlock(machineProperties(3.5F, 6.0F)));

    public static final DeferredBlock<CombustionGeneratorBlock> COMBUSTION_GENERATOR = BLOCKS.register(
            "combustion_generator",
            () -> new CombustionGeneratorBlock(machineProperties(3.5F, 6.0F)
                    .lightLevel(state -> state.getValue(CombustionGeneratorBlock.LIT) ? 13 : 0)));

    // Machines
    public static final DeferredBlock<ElectricFurnaceBlock> ELECTRIC_FURNACE = BLOCKS.register(
            "electric_furnace",
            () -> new ElectricFurnaceBlock(machineProperties(3.5F, 6.0F)
                    .lightLevel(state -> state.getValue(ElectricFurnaceBlock.LIT) ? 10 : 0)));

    // Batteries / Energy Cells
    public static final DeferredBlock<EnergyCellBlock> BASIC_ENERGY_CELL = BLOCKS.register(
            "basic_energy_cell",
            () -> new EnergyCellBlock(machineProperties(4.0F, 8.0F), EnergyCellTier.BASIC));

    public static final DeferredBlock<EnergyCellBlock> ADVANCED_ENERGY_CELL = BLOCKS.register(
            "advanced_energy_cell",
            () -> new EnergyCellBlock(machineProperties(5.0F, 10.0F), EnergyCellTier.ADVANCED));

    // Energy Cables
    public static final DeferredBlock<EnergyCableBlock> ENERGY_CABLE = BLOCKS.register(
            "energy_cable",
            () -> new EnergyCableBlock(BlockBehaviour.Properties.of()
                    .strength(0.8F, 2.0F)
                    .sound(SoundType.METAL)
                    .noOcclusion()));

    /** Needs a pickaxe (see the mineable/pickaxe tag), otherwise the block drops nothing. */
    private static BlockBehaviour.Properties machineProperties(float hardness, float resistance) {
        return BlockBehaviour.Properties.of()
                .strength(hardness, resistance)
                .sound(SoundType.METAL)
                .requiresCorrectToolForDrops();
    }

    public static void registerBlockItems() {
        ModItems.ITEMS.registerSimpleBlockItem(THERMOELECTRIC_GENERATOR);
        ModItems.ITEMS.registerSimpleBlockItem(COMBUSTION_GENERATOR);
        ModItems.ITEMS.registerSimpleBlockItem(ELECTRIC_FURNACE);
        ModItems.ITEMS.registerItem("basic_energy_cell", props -> new EnergyCellItem(BASIC_ENERGY_CELL.get(), props), new Item.Properties());
        ModItems.ITEMS.registerItem("advanced_energy_cell", props -> new EnergyCellItem(ADVANCED_ENERGY_CELL.get(), props), new Item.Properties());
        ModItems.ITEMS.registerSimpleBlockItem(ENERGY_CABLE);
    }
}
