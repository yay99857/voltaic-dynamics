package com.voltaicdynamics.machines;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.level.block.entity.BlockEntity;

public class AlloySmelterMenu extends ProcessingMenu {
    /** Slot positions shared with the screen, JEI and EMI. */
    public static final int[][] INPUTS = {{30, 35}, {48, 35}, {66, 35}};
    public static final int OUTPUT_X = 124;
    public static final int OUTPUT_Y = 35;

    public AlloySmelterMenu(int containerId, Inventory inv, RegistryFriendlyByteBuf extraData) {
        this(containerId, inv, inv.player.level().getBlockEntity(extraData.readBlockPos()));
    }

    public AlloySmelterMenu(int containerId, Inventory inv, BlockEntity entity) {
        super(VoltaicMachines.ALLOY_SMELTER_MENU.get(), containerId, inv, entity, INPUTS, OUTPUT_X, OUTPUT_Y);
    }
}
