package com.spider.framedfabric.block;

import com.mojang.serialization.MapCodec;
import net.minecraft.block.BlockSetType;
import net.minecraft.block.TrapdoorBlock;

public class FramedTrapdoorBlock extends TrapdoorBlock {
    public static final MapCodec<FramedTrapdoorBlock> CODEC = createCodec(s -> new FramedTrapdoorBlock(BlockSetType.OAK, s));

    public FramedTrapdoorBlock(BlockSetType setType, Settings settings) {
        super(setType, settings);
    }

    @Override
    public MapCodec<? extends TrapdoorBlock> getCodec() {
        return CODEC;
    }
}
