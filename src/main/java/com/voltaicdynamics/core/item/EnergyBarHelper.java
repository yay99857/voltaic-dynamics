package com.voltaicdynamics.core.item;

import com.voltaicdynamics.core.ModDataComponents;

import net.minecraft.ChatFormatting;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;
import net.minecraft.world.item.ItemStack;
import net.minecraft.core.component.DataComponents;
import net.minecraft.world.item.component.CustomData;

import java.util.List;

/** Shared energy bar and tooltip for items that store FE in {@link ModDataComponents#ENERGY}. */
public final class EnergyBarHelper {
    public static final int BAR_COLOR = Mth.color(0.0F, 0.85F, 0.95F); // electric cyan

    private EnergyBarHelper() {
    }

    public static int getEnergy(ItemStack stack) {
        Integer energy = stack.get(ModDataComponents.ENERGY.get());
        if (energy != null) return energy;
        // Items made by 1.0.0 kept their charge in custom_data.
        CompoundTag legacy = stack.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag();
        return legacy.getInt("Energy");
    }

    /** Moves a 1.0.0 custom_data charge into the energy component. */
    public static void migrateLegacy(ItemStack stack) {
        CustomData data = stack.get(DataComponents.CUSTOM_DATA);
        if (data == null || !data.contains("Energy")) return;
        int energy = data.copyTag().getInt("Energy");
        CustomData.update(DataComponents.CUSTOM_DATA, stack, tag -> tag.remove("Energy"));
        CustomData remaining = stack.get(DataComponents.CUSTOM_DATA);
        if (remaining != null && remaining.isEmpty()) stack.remove(DataComponents.CUSTOM_DATA);
        if (!stack.has(ModDataComponents.ENERGY.get())) stack.set(ModDataComponents.ENERGY.get(), energy);
    }

    public static int barWidth(ItemStack stack, int capacity) {
        if (capacity <= 0) return 0;
        return Math.round(13.0F * Math.min(getEnergy(stack), capacity) / (float) capacity);
    }

    public static void addTooltip(ItemStack stack, int capacity, List<Component> tooltip, ChatFormatting color) {
        tooltip.add(Component.translatable("tooltip.voltaicdynamics.energy",
                String.format("%,d", getEnergy(stack)), String.format("%,d", capacity)).withStyle(color));
    }
}
