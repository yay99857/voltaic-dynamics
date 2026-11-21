package com.voltaicdynamics.power;

import com.voltaicdynamics.VoltaicConfig;

import com.mojang.serialization.Codec;
import net.minecraft.ChatFormatting;
import net.minecraft.util.StringRepresentable;
import net.neoforged.neoforge.common.ModConfigSpec;
import org.jetbrains.annotations.Nullable;

public enum AccumulatorTier implements StringRepresentable {
    BASIC("basic", VoltaicConfig.BASIC_CELL_CAPACITY, VoltaicConfig.BASIC_CELL_TRANSFER, ChatFormatting.AQUA),
    ADVANCED("advanced", VoltaicConfig.ADVANCED_CELL_CAPACITY, VoltaicConfig.ADVANCED_CELL_TRANSFER, ChatFormatting.GOLD),
    ELITE("elite", VoltaicConfig.ELITE_CELL_CAPACITY, VoltaicConfig.ELITE_CELL_TRANSFER, ChatFormatting.LIGHT_PURPLE),
    ULTIMATE("ultimate", VoltaicConfig.ULTIMATE_CELL_CAPACITY, VoltaicConfig.ULTIMATE_CELL_TRANSFER, ChatFormatting.RED),
    /** Never runs out: always full, gives {@code creativeTransfer} FE/t and voids what it receives. */
    CREATIVE("creative", null, VoltaicConfig.CREATIVE_CELL_TRANSFER, ChatFormatting.DARK_PURPLE);

    public static final Codec<AccumulatorTier> CODEC = StringRepresentable.fromEnum(AccumulatorTier::values);

    private final String name;
    @Nullable
    private final ModConfigSpec.IntValue capacity;
    private final ModConfigSpec.IntValue transfer;
    private final ChatFormatting color;

    AccumulatorTier(String name, @Nullable ModConfigSpec.IntValue capacity, ModConfigSpec.IntValue transfer, ChatFormatting color) {
        this.name = name;
        this.capacity = capacity;
        this.transfer = transfer;
        this.color = color;
    }

    public int capacity() {
        return capacity == null ? Integer.MAX_VALUE : VoltaicConfig.get(capacity);
    }

    public int transfer() {
        return VoltaicConfig.get(transfer);
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
