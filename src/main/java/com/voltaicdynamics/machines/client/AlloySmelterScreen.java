package com.voltaicdynamics.machines.client;

import com.voltaicdynamics.VoltaicDynamics;
import com.voltaicdynamics.machines.AlloySmelterMenu;

import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;

/** Layout: {@code assets/voltaicdynamics/screens/alloy_smelter.json}. */
public class AlloySmelterScreen extends ProcessingScreen<AlloySmelterMenu> {
    public AlloySmelterScreen(AlloySmelterMenu menu, Inventory playerInventory, Component title) {
        super(menu, playerInventory, title, VoltaicDynamics.id("alloy_smelter"));
    }
}
