package com.voltaicdynamics.client;

import com.voltaicdynamics.VoltaicDynamics;
import com.voltaicdynamics.core.client.layout.ScreenLayouts;
import com.voltaicdynamics.generators.VoltaicGenerators;
import com.voltaicdynamics.generators.client.CombustionGeneratorScreen;
import com.voltaicdynamics.logistics.VoltaicLogistics;
import com.voltaicdynamics.logistics.client.FluidTankItemRendering;
import com.voltaicdynamics.logistics.client.FluidTankRenderer;
import com.voltaicdynamics.logistics.client.FluidTankScreen;
import com.voltaicdynamics.machines.VoltaicMachines;
import com.voltaicdynamics.machines.client.AlloySmelterScreen;
import com.voltaicdynamics.machines.client.ElectricFurnaceScreen;
import com.voltaicdynamics.machines.client.PulverizerScreen;
import com.voltaicdynamics.power.VoltaicPower;
import com.voltaicdynamics.power.client.AccumulatorScreen;

import net.neoforged.neoforge.client.event.EntityRenderersEvent;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.fml.event.lifecycle.FMLClientSetupEvent;
import net.neoforged.neoforge.client.event.RegisterClientReloadListenersEvent;
import net.neoforged.neoforge.client.event.RegisterColorHandlersEvent;
import net.neoforged.neoforge.client.event.RegisterMenuScreensEvent;

@EventBusSubscriber(modid = VoltaicDynamics.MOD_ID, value = Dist.CLIENT)
public class ClientModEvents {

    @SubscribeEvent
    public static void registerScreens(RegisterMenuScreensEvent event) {
        event.register(VoltaicGenerators.COMBUSTION_GENERATOR_MENU.get(), CombustionGeneratorScreen::new);
        event.register(VoltaicPower.ACCUMULATOR_MENU.get(), AccumulatorScreen::new);
        event.register(VoltaicMachines.ELECTRIC_FURNACE_MENU.get(), ElectricFurnaceScreen::new);
        event.register(VoltaicMachines.ALLOY_SMELTER_MENU.get(), AlloySmelterScreen::new);
        event.register(VoltaicMachines.PULVERIZER_MENU.get(), PulverizerScreen::new);
        event.register(VoltaicLogistics.FLUID_TANK_MENU.get(), FluidTankScreen::new);
    }

    @SubscribeEvent
    public static void registerRenderers(EntityRenderersEvent.RegisterRenderers event) {
        event.registerBlockEntityRenderer(VoltaicLogistics.FLUID_TANK_BE.get(), FluidTankRenderer::new);
    }

    @SubscribeEvent
    public static void clientSetup(FMLClientSetupEvent event) {
        event.enqueueWork(FluidTankItemRendering::registerFillProperty);
    }

    /** Screen layouts (assets/<mod>/screens/*.json), reloaded with F3+T. */
    @SubscribeEvent
    public static void registerReloadListeners(RegisterClientReloadListenersEvent event) {
        event.registerReloadListener(ScreenLayouts.INSTANCE);
    }

    @SubscribeEvent
    public static void registerItemColors(RegisterColorHandlersEvent.Item event) {
        FluidTankItemRendering.registerColors(event);
    }
}
