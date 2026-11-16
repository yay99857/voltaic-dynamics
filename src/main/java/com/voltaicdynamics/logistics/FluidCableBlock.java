package com.voltaicdynamics.logistics;

import com.mojang.serialization.MapCodec;
import net.minecraft.world.level.block.BaseEntityBlock;

public class FluidCableBlock extends LogisticsCableBlock {
    public static final MapCodec<FluidCableBlock> CODEC = simpleCodec(FluidCableBlock::new);

    public FluidCableBlock(Properties properties) {
        super(properties, LogisticsKind.FLUID);
    }

    @Override
    protected MapCodec<? extends BaseEntityBlock> codec() {
        return CODEC;
    }
}
