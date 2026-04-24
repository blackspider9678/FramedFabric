package com.spider.framedfabric.client.render;

import net.minecraft.client.renderer.blockentity.state.BlockEntityRenderState;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;

public final class FramedBERenderState extends BlockEntityRenderState {
    public boolean isFlowerPot;
    public BlockState plantState = Blocks.AIR.defaultBlockState();
}
