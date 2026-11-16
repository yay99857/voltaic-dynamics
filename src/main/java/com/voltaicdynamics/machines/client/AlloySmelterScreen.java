package com.voltaicdynamics.machines.client;

import com.voltaicdynamics.VoltaicDynamics;
import com.voltaicdynamics.machines.AlloySmelterMenu;

import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;

public class AlloySmelterScreen extends ProcessingScreen<AlloySmelterMenu> {
    public static final ResourceLocation TEXTURE =
            ResourceLocation.fromNamespaceAndPath(VoltaicDynamics.MOD_ID, "textures/gui/alloy_smelter_gui.png");
    public static final int ARROW_X = 90;
    public static final int ARROW_Y = 34;

    public AlloySmelterScreen(AlloySmelterMenu menu, Inventory playerInventory, Component title) {
        super(menu, playerInventory, title, TEXTURE, ARROW_X, ARROW_Y);
    }
}
