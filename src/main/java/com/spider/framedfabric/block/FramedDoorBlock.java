package com.spider.framedfabric.block;

import com.mojang.serialization.MapCodec;
import net.minecraft.block.BlockSetType;
import net.minecraft.block.DoorBlock;

public class FramedDoorBlock extends DoorBlock {
    public static final MapCodec<FramedDoorBlock> CODEC = createCodec(s -> new FramedDoorBlock(BlockSetType.OAK, s));

    public FramedDoorBlock(BlockSetType setType, Settings settings) {
        super(setType, settings);
    }

    @Override
    public MapCodec<? extends DoorBlock> getCodec() {
        return CODEC;
    }
}
