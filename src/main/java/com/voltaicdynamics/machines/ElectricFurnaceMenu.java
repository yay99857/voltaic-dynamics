package com.voltaicdynamics.machines;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.level.block.entity.BlockEntity;

public class ElectricFurnaceMenu extends ProcessingMenu {
    public ElectricFurnaceMenu(int containerId, Inventory inv, RegistryFriendlyByteBuf extraData) {
        this(containerId, inv, inv.player.level().getBlockEntity(extraData.readBlockPos()));
    }

    public ElectricFurnaceMenu(int containerId, Inventory inv, BlockEntity entity) {
        super(VoltaicMachines.ELECTRIC_FURNACE_MENU.get(), containerId, inv, entity);
    }
}
