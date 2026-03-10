package com.spider.framedfabric.block;

import com.mojang.serialization.MapCodec;
import net.minecraft.block.BlockSetType;
import net.minecraft.block.PressurePlateBlock;

public class FramedPressurePlateBlock extends PressurePlateBlock {

    public static final MapCodec<PressurePlateBlock> CODEC =
            createCodec(s -> new FramedPressurePlateBlock(BlockSetType.OAK, s));

    public FramedPressurePlateBlock(BlockSetType setType, Settings settings) {
        super(setType, settings);
    }

    @Override
    public MapCodec<PressurePlateBlock> getCodec() {
        return CODEC;
    }
}
