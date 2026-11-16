package com.voltaicdynamics;

import com.voltaicdynamics.client.ClientModEvents;
import com.voltaicdynamics.core.VoltaicCore;
import com.voltaicdynamics.gadgets.VoltaicGadgets;
import com.voltaicdynamics.generators.VoltaicGenerators;
import com.voltaicdynamics.logistics.VoltaicLogistics;
import com.voltaicdynamics.machines.VoltaicMachines;
import com.voltaicdynamics.power.VoltaicPower;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.config.ModConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Entry point. The mod is one jar, but its content is split into modules that only depend on
 * {@link VoltaicCore}, so any of them can later become its own mod:
 * <ul>
 *   <li>{@code core}: materials, data components, creative tab, energy storage, side configuration, base GUI</li>
 *   <li>{@code generators}: Thermoelectric and Combustion Generators</li>
 *   <li>{@code machines}: Electric Furnace, Alloy Smelter and the alloying recipe type</li>
 *   <li>{@code power}: Accumulators and Voltaic Cables</li>
 *   <li>{@code logistics}: item and fluid cables, Fluid Tank</li>
 *   <li>{@code gadgets}: Pocket Accumulator</li>
 * </ul>
 * {@code client.ClientModEvents}, {@code compat}, {@code datagen} and {@code gametest} still cover every module.
 */
@Mod(VoltaicDynamics.MOD_ID)
public class VoltaicDynamics {
    public static final String MOD_ID = "voltaicdynamics";
    public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

    public VoltaicDynamics(IEventBus modEventBus, ModContainer modContainer) {
        LOGGER.info("Initializing Voltaic Dynamics");

        modContainer.registerConfig(ModConfig.Type.SERVER, VoltaicConfig.SPEC);

        // The order here is also the order of the creative tab.
        VoltaicCore.register(modEventBus);
        VoltaicGadgets.register(modEventBus);
        VoltaicGenerators.register(modEventBus);
        VoltaicMachines.register(modEventBus);
        VoltaicPower.register(modEventBus);
        VoltaicLogistics.register(modEventBus);
    }

    public static ResourceLocation id(String path) {
        return ResourceLocation.fromNamespaceAndPath(MOD_ID, path);
    }

    /** Machine blocks need a pickaxe (see the mineable/pickaxe tag), otherwise they drop nothing. */
    public static BlockBehaviour.Properties machineProperties(float hardness, float resistance) {
        return BlockBehaviour.Properties.of()
                .strength(hardness, resistance)
                .sound(SoundType.METAL)
                .requiresCorrectToolForDrops();
    }
}
