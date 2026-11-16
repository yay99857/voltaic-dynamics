package com.voltaicdynamics.core.sideconfig;

import net.minecraft.network.chat.Component;

/** What one face of a machine does for one transfer type. Colours follow Mekanism's side configuration. */
public enum IoMode {
    NONE(0xFF4A4A4A),
    INPUT(0xFFB83A3A),
    OUTPUT(0xFF3A5CC8),
    BOTH(0xFF8A44B0);

    private final int color;

    IoMode(int color) {
        this.color = color;
    }

    public int color() {
        return color;
    }

    public boolean canInput() {
        return this == INPUT || this == BOTH;
    }

    public boolean canOutput() {
        return this == OUTPUT || this == BOTH;
    }

    public Component label() {
        return Component.translatable("sideconfig.voltaicdynamics.mode." + name().toLowerCase());
    }

    public static IoMode byId(int id) {
        IoMode[] values = values();
        return id >= 0 && id < values.length ? values[id] : NONE;
    }
}
