package com.voltaicdynamics.logistics;

import net.minecraft.world.level.block.BaseEntityBlock;

public class FluidCableBlock extends LogisticsCableBlock {

    public FluidCableBlock(Properties properties) {
        super(properties, LogisticsKind.FLUID);
    }

}
