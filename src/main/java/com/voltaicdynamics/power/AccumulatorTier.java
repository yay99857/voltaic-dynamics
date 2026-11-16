package com.voltaicdynamics.power;

import com.voltaicdynamics.VoltaicConfig;

import com.mojang.serialization.Codec;
import net.minecraft.ChatFormatting;
import net.minecraft.util.StringRepresentable;
import net.neoforged.neoforge.common.ModConfigSpec;

public enum AccumulatorTier implements StringRepresentable {
    BASIC("basic", VoltaicConfig.BASIC_CELL_CAPACITY, VoltaicConfig.BASIC_CELL_TRANSFER, ChatFormatting.AQUA),
    ADVANCED("advanced", VoltaicConfig.ADVANCED_CELL_CAPACITY, VoltaicConfig.ADVANCED_CELL_TRANSFER, ChatFormatting.GOLD);

    public static final Codec<AccumulatorTier> CODEC = StringRepresentable.fromEnum(AccumulatorTier::values);

    private final String name;
    private final ModConfigSpec.IntValue capacity;
    private final ModConfigSpec.IntValue transfer;
    private final ChatFormatting color;

    AccumulatorTier(String name, ModConfigSpec.IntValue capacity, ModConfigSpec.IntValue transfer, ChatFormatting color) {
        this.name = name;
        this.capacity = capacity;
        this.transfer = transfer;
        this.color = color;
    }

    public int capacity() {
        return VoltaicConfig.get(capacity);
    }

    public int transfer() {
        return VoltaicConfig.get(transfer);
    }

    public ChatFormatting color() {
        return color;
    }

    @Override
    public String getSerializedName() {
        return name;
    }
}
