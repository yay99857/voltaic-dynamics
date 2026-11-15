package com.voltaicdynamics.item;

import net.minecraft.ChatFormatting;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.level.Level;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.energy.IEnergyStorage;

import java.util.List;

public class PocketCapacitorItem extends Item {
    public static final int CAPACITY = 100_000;
    public static final int MAX_TRANSFER = 500;

    public PocketCapacitorItem(Properties properties) {
        super(properties.stacksTo(1));
    }

    public static int getEnergy(ItemStack stack) {
        CustomData customData = stack.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY);
        CompoundTag tag = customData.copyTag();
        return tag.getInt("Energy");
    }

    public static void setEnergy(ItemStack stack, int energy) {
        int clamped = Math.max(0, Math.min(energy, CAPACITY));
        CustomData.update(DataComponents.CUSTOM_DATA, stack, tag -> tag.putInt("Energy", clamped));
    }

    public static int receiveEnergy(ItemStack stack, int maxReceive, boolean simulate) {
        int current = getEnergy(stack);
        int received = Math.min(CAPACITY - current, Math.min(MAX_TRANSFER, maxReceive));
        if (!simulate && received > 0) {
            setEnergy(stack, current + received);
        }
        return received;
    }

    public static int extractEnergy(ItemStack stack, int maxExtract, boolean simulate) {
        int current = getEnergy(stack);
        int extracted = Math.min(current, Math.min(MAX_TRANSFER, maxExtract));
        if (!simulate && extracted > 0) {
            setEnergy(stack, current - extracted);
        }
        return extracted;
    }

    @Override
    public boolean isBarVisible(ItemStack stack) {
        return true;
    }

    @Override
    public int getBarWidth(ItemStack stack) {
        return Math.round(13.0F * (float) getEnergy(stack) / (float) CAPACITY);
    }

    @Override
    public int getBarColor(ItemStack stack) {
        return Mth.color(0.0F, 0.85F, 0.95F); // Electric Cyan
    }

    @Override
    public void inventoryTick(ItemStack stack, Level level, Entity entity, int slotId, boolean isSelected) {
        if (level.isClientSide) return;

        // Automatically charges electric tools held in main hand
        int currentEnergy = getEnergy(stack);
        if (currentEnergy <= 0) return;

        if (entity instanceof net.minecraft.world.entity.player.Player player) {
            ItemStack mainHand = player.getMainHandItem();
            if (!mainHand.isEmpty() && mainHand != stack) {
                IEnergyStorage targetEnergy = mainHand.getCapability(Capabilities.EnergyStorage.ITEM);
                if (targetEnergy != null && targetEnergy.canReceive()) {
                    int toTransfer = Math.min(currentEnergy, Math.min(MAX_TRANSFER, 250));
                    int accepted = targetEnergy.receiveEnergy(toTransfer, false);
                    if (accepted > 0) {
                        extractEnergy(stack, accepted, false);
                    }
                }
            }
        }
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltipComponents, TooltipFlag tooltipFlag) {
        int energy = getEnergy(stack);
        tooltipComponents.add(Component.translatable("tooltip.voltaicdynamics.energy",
                String.format("%,d", energy), String.format("%,d", CAPACITY)).withStyle(ChatFormatting.AQUA));
        tooltipComponents.add(Component.translatable("tooltip.voltaicdynamics.pocket_capacitor.desc")
                .withStyle(ChatFormatting.GRAY));
    }
}
