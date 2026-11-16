package com.voltaicdynamics.machines.client;

import com.voltaicdynamics.VoltaicDynamics;
import com.voltaicdynamics.machines.ElectricFurnaceMenu;

import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;

public class ElectricFurnaceScreen extends ProcessingScreen<ElectricFurnaceMenu> {
    public static final int ARROW_X = 79;
    public static final int ARROW_Y = 34;

    public ElectricFurnaceScreen(ElectricFurnaceMenu menu, Inventory playerInventory, Component title) {
        super(menu, playerInventory, title,
                ResourceLocation.fromNamespaceAndPath(VoltaicDynamics.MOD_ID, "textures/gui/electric_furnace_gui.png"), ARROW_X, ARROW_Y);
    }
}
