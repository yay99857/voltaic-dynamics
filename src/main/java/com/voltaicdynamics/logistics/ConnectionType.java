package com.voltaicdynamics.logistics;

import net.minecraft.network.chat.Component;
import net.minecraft.util.StringRepresentable;

/** What one arm of a logistics cable does. Stored in the block state so the model shows it. */
public enum ConnectionType implements StringRepresentable {
    /** Nothing there. */
    NONE,
    /** Linked to another cable of the same kind. */
    CABLE,
    /** Linked to an inventory or tank: the network delivers into it, and it may push into the network. */
    BLOCK,
    /** Linked to an inventory or tank the network pulls from (the orange connector). */
    PULL,
    /** Switched off by the player; kept until switched on again. */
    DISABLED;

    @Override
    public String getSerializedName() {
        return name().toLowerCase();
    }

    public boolean isEndpoint() {
        return this == BLOCK || this == PULL;
    }

    public Component label() {
        return Component.translatable("cable.voltaicdynamics.mode." + getSerializedName());
    }
}
