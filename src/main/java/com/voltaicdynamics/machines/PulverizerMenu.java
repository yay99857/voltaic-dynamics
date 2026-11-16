package com.voltaicdynamics.machines;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.level.block.entity.BlockEntity;

public class PulverizerMenu extends ProcessingMenu {
    public PulverizerMenu(int containerId, Inventory inv, RegistryFriendlyByteBuf extraData) {
        this(containerId, inv, inv.player.level().getBlockEntity(extraData.readBlockPos()));
    }

    public PulverizerMenu(int containerId, Inventory inv, BlockEntity entity) {
        super(VoltaicMachines.PULVERIZER_MENU.get(), containerId, inv, entity);
    }
}
