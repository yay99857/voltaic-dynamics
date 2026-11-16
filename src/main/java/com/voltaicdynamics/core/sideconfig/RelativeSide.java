package com.voltaicdynamics.core.sideconfig;

import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;

/**
 * A face named from the point of view of a player looking at the machine's front, so the configuration
 * keeps its meaning whichever way the machine was placed. LEFT is the player's left.
 */
public enum RelativeSide {
    FRONT,
    BACK,
    LEFT,
    RIGHT,
    TOP,
    BOTTOM;

    public Direction toWorld(Direction facing) {
        return switch (this) {
            case FRONT -> facing;
            case BACK -> facing.getOpposite();
            case LEFT -> facing.getClockWise();
            case RIGHT -> facing.getCounterClockWise();
            case TOP -> Direction.UP;
            case BOTTOM -> Direction.DOWN;
        };
    }

    public static RelativeSide of(Direction facing, Direction world) {
        for (RelativeSide side : values()) {
            if (side.toWorld(facing) == world) return side;
        }
        return FRONT;
    }

    /** Machines without a front show compass directions instead (facing is taken as north). */
    public Component label(boolean absolute) {
        if (absolute) {
            return Component.translatable("sideconfig.voltaicdynamics.dir." + toWorld(Direction.NORTH).getName());
        }
        return Component.translatable("sideconfig.voltaicdynamics.side." + name().toLowerCase());
    }
}
