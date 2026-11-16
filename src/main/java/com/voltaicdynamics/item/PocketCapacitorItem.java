package com.voltaicdynamics.item;

import com.voltaicdynamics.VoltaicConfig;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.energy.IEnergyStorage;

import java.util.List;

public class PocketCapacitorItem extends Item {
    private static final EquipmentSlot[] CHARGED_SLOTS = {
            EquipmentSlot.MAINHAND, EquipmentSlot.OFFHAND,
            EquipmentSlot.HEAD, EquipmentSlot.CHEST, EquipmentSlot.LEGS, EquipmentSlot.FEET
    };

    public PocketCapacitorItem(Properties properties) {
        super(properties.stacksTo(1));
    }

    public static int capacity() {
        return VoltaicConfig.get(VoltaicConfig.CAPACITOR_CAPACITY);
    }

    public static int transfer() {
        return VoltaicConfig.get(VoltaicConfig.CAPACITOR_TRANSFER);
    }

    @Override
    public boolean isBarVisible(ItemStack stack) {
        return true;
    }

    @Override
    public int getBarWidth(ItemStack stack) {
        return EnergyBarHelper.barWidth(stack, capacity());
    }

    @Override
    public int getBarColor(ItemStack stack) {
        return EnergyBarHelper.BAR_COLOR;
    }

    /** Charges the held items and worn armor; the per-tick transfer limit is shared between them. */
    @Override
    public void inventoryTick(ItemStack stack, Level level, Entity entity, int slotId, boolean isSelected) {
        if (level.isClientSide || !(entity instanceof Player player)) return;
        EnergyBarHelper.migrateLegacy(stack);

        IEnergyStorage self = stack.getCapability(Capabilities.EnergyStorage.ITEM);
        if (self == null) return;
        int budget = self.extractEnergy(transfer(), true);
        if (budget <= 0) return;

        for (EquipmentSlot slot : CHARGED_SLOTS) {
            ItemStack target = player.getItemBySlot(slot);
            if (target.isEmpty() || target == stack || target.getItem() instanceof PocketCapacitorItem) continue;
            IEnergyStorage targetEnergy = target.getCapability(Capabilities.EnergyStorage.ITEM);
            if (targetEnergy == null || !targetEnergy.canReceive()) continue;
            int accepted = targetEnergy.receiveEnergy(budget, false);
            if (accepted > 0) {
                self.extractEnergy(accepted, false);
                budget -= accepted;
                if (budget <= 0) return;
            }
        }
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        EnergyBarHelper.addTooltip(stack, capacity(), tooltip, ChatFormatting.AQUA);
        tooltip.add(Component.translatable("tooltip.voltaicdynamics.pocket_capacitor.desc").withStyle(ChatFormatting.GRAY));
    }
}
