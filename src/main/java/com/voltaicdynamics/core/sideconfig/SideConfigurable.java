package com.voltaicdynamics.core.sideconfig;

import net.minecraft.core.Direction;

/** A block entity whose faces can be configured from its GUI. */
public interface SideConfigurable {
    SideConfig getSideConfig();

    /** The machine's front; blocks without one return NORTH and set {@link #hasFront()} to false. */
    Direction getFacing();

    default boolean hasFront() {
        return true;
    }

    /** Called on the server after the player changed a face or the auto-eject switch. */
    void onSideConfigChanged();
}
