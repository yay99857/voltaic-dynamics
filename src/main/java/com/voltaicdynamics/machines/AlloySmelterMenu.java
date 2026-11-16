package com.voltaicdynamics.machines;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.level.block.entity.BlockEntity;

public class AlloySmelterMenu extends ProcessingMenu {
    public AlloySmelterMenu(int containerId, Inventory inv, RegistryFriendlyByteBuf extraData) {
        this(containerId, inv, inv.player.level().getBlockEntity(extraData.readBlockPos()));
    }

    public AlloySmelterMenu(int containerId, Inventory inv, BlockEntity entity) {
        super(VoltaicMachines.ALLOY_SMELTER_MENU.get(), containerId, inv, entity);
    }
}
