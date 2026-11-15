package com.voltaicdynamics.client;

import com.voltaicdynamics.VoltaicDynamics;
import com.voltaicdynamics.menu.ModMenus;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RegisterMenuScreensEvent;

@EventBusSubscriber(modid = VoltaicDynamics.MOD_ID, value = Dist.CLIENT)
public class ClientModEvents {

    @SubscribeEvent
    public static void registerScreens(RegisterMenuScreensEvent event) {
        event.register(ModMenus.COMBUSTION_GENERATOR_MENU.get(), CombustionGeneratorScreen::new);
        event.register(ModMenus.ENERGY_CELL_MENU.get(), EnergyCellScreen::new);
    }
}
