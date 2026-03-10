package com.spider.framedfabric.block;

import com.mojang.serialization.MapCodec;
import net.minecraft.block.FenceBlock;

public class FramedFenceBlock extends FenceBlock {

    public static final MapCodec<FenceBlock> CODEC =
            createCodec(FramedFenceBlock::new);

    public FramedFenceBlock(Settings settings) {
        super(settings);
    }

    @Override
    public MapCodec<FenceBlock> getCodec() {
        return CODEC;
    }
}
