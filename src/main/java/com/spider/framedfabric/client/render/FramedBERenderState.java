package com.spider.framedfabric.client.render;

import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.client.render.block.entity.state.BlockEntityRenderState;

public final class FramedBERenderState extends BlockEntityRenderState {
    public boolean isFlowerPot;
    public BlockState plantState = Blocks.AIR.getDefaultState();
}
