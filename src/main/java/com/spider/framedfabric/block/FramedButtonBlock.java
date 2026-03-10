package com.spider.framedfabric.block;

import com.mojang.serialization.MapCodec;
import net.minecraft.block.BlockSetType;
import net.minecraft.block.ButtonBlock;

public class FramedButtonBlock extends ButtonBlock {

    // Match the superclass return type EXACTLY
    public static final MapCodec<ButtonBlock> CODEC =
            createCodec(s -> new FramedButtonBlock(BlockSetType.OAK, 20, s));

    public FramedButtonBlock(BlockSetType setType, int pressTicks, Settings settings) {
        super(setType, pressTicks, settings);
    }

    @Override
    public MapCodec<ButtonBlock> getCodec() {
        return CODEC;
    }
}
