package com.spider.framedfabric.block;

import com.mojang.serialization.MapCodec;
import net.minecraft.block.FenceGateBlock;
import net.minecraft.block.WoodType;

public class FramedFenceGateBlock extends FenceGateBlock {

    public static final MapCodec<FenceGateBlock> CODEC =
            createCodec(s -> new FramedFenceGateBlock(WoodType.OAK, s));

    public FramedFenceGateBlock(WoodType type, Settings settings) {
        super(type, settings);
    }

    @Override
    public MapCodec<FenceGateBlock> getCodec() {
        return CODEC;
    }
}
