package com.voltaicdynamics.core.sideconfig;

import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

/** The three things a face can move. Each has its own tab in the side configuration panel. */
public enum TransferType {
    ITEM,
    FLUID,
    ENERGY;

    public Component label() {
        return Component.translatable("sideconfig.voltaicdynamics.type." + name().toLowerCase());
    }

    /** Tab icon. */
    public ItemStack icon() {
        return switch (this) {
            case ITEM -> new ItemStack(Items.CHEST);
            case FLUID -> new ItemStack(Items.WATER_BUCKET);
            case ENERGY -> new ItemStack(Items.REDSTONE);
        };
    }
}
