package com.voltaicdynamics.logistics;

import com.mojang.serialization.MapCodec;
import net.minecraft.world.level.block.BaseEntityBlock;

public class ItemCableBlock extends LogisticsCableBlock {
    public static final MapCodec<ItemCableBlock> CODEC = simpleCodec(ItemCableBlock::new);

    public ItemCableBlock(Properties properties) {
        super(properties, LogisticsKind.ITEM);
    }

    @Override
    protected MapCodec<? extends BaseEntityBlock> codec() {
        return CODEC;
    }
}
