package com.voltaicdynamics.logistics;

import com.voltaicdynamics.VoltaicConfig;

import com.mojang.serialization.Codec;
import net.minecraft.ChatFormatting;
import net.minecraft.util.StringRepresentable;

/** Fluid Tank sizes: multiples of the configured basic capacity, plus the bottomless Creative tank. */
public enum FluidTankTier implements StringRepresentable {
    BASIC("basic", 1, ChatFormatting.AQUA),
    ADVANCED("advanced", 2, ChatFormatting.GOLD),
    ELITE("elite", 4, ChatFormatting.LIGHT_PURPLE),
    ULTIMATE("ultimate", 8, ChatFormatting.RED),
    /** Filled once with any fluid, then gives it forever and voids more of it. */
    CREATIVE("creative", 0, ChatFormatting.DARK_PURPLE);

    public static final Codec<FluidTankTier> CODEC = StringRepresentable.fromEnum(FluidTankTier::values);

    private final String name;
    private final int multiplier;
    private final ChatFormatting color;

    FluidTankTier(String name, int multiplier, ChatFormatting color) {
        this.name = name;
        this.multiplier = multiplier;
        this.color = color;
    }

    /** Capacity in mB. */
    public int capacity() {
        if (isCreative()) return Integer.MAX_VALUE;
        return (int) Math.min(Integer.MAX_VALUE, (long) VoltaicConfig.get(VoltaicConfig.TANK_CAPACITY) * multiplier);
    }

    public boolean isCreative() {
        return this == CREATIVE;
    }

    public ChatFormatting color() {
        return color;
    }

    @Override
    public String getSerializedName() {
        return name;
    }
}
