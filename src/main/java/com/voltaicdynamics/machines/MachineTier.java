package com.voltaicdynamics.machines;

import com.voltaicdynamics.VoltaicConfig;

import com.mojang.serialization.Codec;
import io.netty.buffer.ByteBuf;
import net.minecraft.ChatFormatting;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.util.StringRepresentable;
import net.neoforged.neoforge.common.ModConfigSpec;
import org.jetbrains.annotations.Nullable;

/**
 * Tier of an Electric Furnace, Alloy Smelter or Pulverizer, raised in place with a {@link TierInstallerItem}: how many
 * items it processes at once (like Mekanism's factories). Speed and efficiency stay with the {@link UpgradeItem}s.
 */
public enum MachineTier implements StringRepresentable {
    BASIC("basic", null, ChatFormatting.GRAY),
    ADVANCED("advanced", VoltaicConfig.ADVANCED_MACHINE_PARALLEL, ChatFormatting.GOLD),
    ELITE("elite", VoltaicConfig.ELITE_MACHINE_PARALLEL, ChatFormatting.LIGHT_PURPLE),
    ULTIMATE("ultimate", VoltaicConfig.ULTIMATE_MACHINE_PARALLEL, ChatFormatting.RED);

    public static final Codec<MachineTier> CODEC = StringRepresentable.fromEnum(MachineTier::values);
    public static final StreamCodec<ByteBuf, MachineTier> STREAM_CODEC =
            ByteBufCodecs.VAR_INT.map(MachineTier::byOrdinal, MachineTier::ordinal);

    private final String name;
    @Nullable
    private final ModConfigSpec.IntValue parallel;
    private final ChatFormatting color;

    MachineTier(String name, @Nullable ModConfigSpec.IntValue parallel, ChatFormatting color) {
        this.name = name;
        this.parallel = parallel;
        this.color = color;
    }

    /** Items processed at once. */
    public int parallel() {
        return parallel == null ? 1 : VoltaicConfig.get(parallel);
    }

    /** The tier one step up, or null at the top. */
    @Nullable
    public MachineTier next() {
        return ordinal() + 1 < values().length ? values()[ordinal() + 1] : null;
    }

    @Nullable
    public MachineTier previous() {
        return ordinal() > 0 ? values()[ordinal() - 1] : null;
    }

    public ChatFormatting color() {
        return color;
    }

    public static MachineTier byOrdinal(int ordinal) {
        MachineTier[] tiers = values();
        return tiers[Math.floorMod(ordinal, tiers.length)];
    }

    public static MachineTier byName(String name) {
        for (MachineTier tier : values()) if (tier.name.equals(name)) return tier;
        return BASIC;
    }

    @Override
    public String getSerializedName() {
        return name;
    }
}
