package com.voltaicdynamics.machines;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;

import java.util.List;

/**
 * A machine upgrade. Goes in the two upgrade slots of the Electric Furnace, Alloy Smelter and Pulverizer; up to
 * {@link #MAX_PER_TYPE} of each type count.
 * <ul>
 *   <li>Speed: each one makes operations {@code 1 + 0.5 n} times faster; FE/t rises by the same factor, so the
 *       energy per operation stays the same.</li>
 *   <li>Efficiency: each one cuts the FE/t by 15% (60% less with four).</li>
 * </ul>
 */
public class UpgradeItem extends Item {
    public static final int MAX_PER_TYPE = 4;
    public static final double SPEED_PER_UPGRADE = 0.5;
    public static final double SAVING_PER_UPGRADE = 0.15;

    public enum Type {
        SPEED, EFFICIENCY
    }

    private final Type type;

    public UpgradeItem(Type type, Properties properties) {
        super(properties.stacksTo(MAX_PER_TYPE));
        this.type = type;
    }

    public Type type() {
        return type;
    }

    /** Speed multiplier for {@code count} speed upgrades. */
    public static double speedFactor(int count) {
        return 1 + SPEED_PER_UPGRADE * Math.min(count, MAX_PER_TYPE);
    }

    /** Energy multiplier for {@code count} efficiency upgrades. */
    public static double energyFactor(int count) {
        return 1 - SAVING_PER_UPGRADE * Math.min(count, MAX_PER_TYPE);
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        String key = "tooltip.voltaicdynamics." + (type == Type.SPEED ? "speed_upgrade" : "efficiency_upgrade");
        tooltip.add(Component.translatable(key).withStyle(ChatFormatting.AQUA));
        tooltip.add(Component.translatable("tooltip.voltaicdynamics.upgrade.where", MAX_PER_TYPE).withStyle(ChatFormatting.GRAY));
    }
}
