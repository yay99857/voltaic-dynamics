package com.voltaicdynamics.machines.client;

import com.voltaicdynamics.VoltaicDynamics;
import com.voltaicdynamics.machines.ElectricFurnaceMenu;

import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;

/** Layout: {@code assets/voltaicdynamics/screens/electric_furnace.json}. */
public class ElectricFurnaceScreen extends ProcessingScreen<ElectricFurnaceMenu> {
    public ElectricFurnaceScreen(ElectricFurnaceMenu menu, Inventory playerInventory, Component title) {
        super(menu, playerInventory, title, VoltaicDynamics.id("electric_furnace"));
    }
}
