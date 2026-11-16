package com.voltaicdynamics.machines.client;

import com.voltaicdynamics.VoltaicDynamics;
import com.voltaicdynamics.machines.PulverizerMenu;

import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;

/** Layout: {@code assets/voltaicdynamics/screens/pulverizer.json}. */
public class PulverizerScreen extends ProcessingScreen<PulverizerMenu> {
    public PulverizerScreen(PulverizerMenu menu, Inventory playerInventory, Component title) {
        super(menu, playerInventory, title, VoltaicDynamics.id("pulverizer"));
    }
}
