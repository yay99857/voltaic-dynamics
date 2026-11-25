package com.voltaicdynamics.gadgets;

import org.jetbrains.annotations.Nullable;
import net.neoforged.neoforge.transfer.energy.EnergyHandler;
import net.neoforged.neoforge.transfer.energy.EnergyHandlerUtil;
import com.voltaicdynamics.core.transfer.Transfers;
import net.minecraft.server.level.ServerLevel;
import java.util.function.Consumer;
import net.minecraft.world.item.component.TooltipDisplay;
import com.voltaicdynamics.VoltaicConfig;
import com.voltaicdynamics.core.item.EnergyBarHelper;

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

import java.util.List;

public class PocketAccumulatorItem extends Item {
    private static final EquipmentSlot[] CHARGED_SLOTS = {
            EquipmentSlot.MAINHAND, EquipmentSlot.OFFHAND,
            EquipmentSlot.HEAD, EquipmentSlot.CHEST, EquipmentSlot.LEGS, EquipmentSlot.FEET
    };

    public PocketAccumulatorItem(Properties properties) {
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
    public void inventoryTick(ItemStack stack, ServerLevel level, Entity entity, @Nullable EquipmentSlot heldIn) {
        if (!(entity instanceof Player player)) return;
        EnergyBarHelper.migrateLegacy(stack);

        EnergyHandler self = Transfers.itemEnergy(stack);
        if (self == null) return;
        int budget = transfer();

        for (EquipmentSlot slot : CHARGED_SLOTS) {
            ItemStack target = player.getItemBySlot(slot);
            if (target.isEmpty() || target == stack || target.getItem() instanceof PocketAccumulatorItem) continue;
            EnergyHandler targetEnergy = Transfers.itemEnergy(target);
            if (targetEnergy == null) continue;
            budget -= EnergyHandlerUtil.move(self, targetEnergy, budget, null);
            if (budget <= 0) return;
        }
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, TooltipDisplay display, Consumer<Component> tooltip, TooltipFlag flag) {
        EnergyBarHelper.addTooltip(stack, capacity(), tooltip, ChatFormatting.AQUA);
        tooltip.accept(Component.translatable("tooltip.voltaicdynamics.pocket_accumulator.desc").withStyle(ChatFormatting.GRAY));
    }
}
