package com.spider.framedfabric.client.render;

import net.minecraft.client.renderer.block.BlockModelRenderState;
import net.minecraft.client.renderer.block.MovingBlockRenderState;
import net.minecraft.client.renderer.block.dispatch.BlockStateModelPart;
import net.minecraft.client.renderer.blockentity.state.BlockEntityRenderState;
import org.jspecify.annotations.Nullable;

import java.util.List;

public final class FramedBERenderState extends BlockEntityRenderState {
    public boolean isFlowerPot;
    @Nullable
    public MovingBlockRenderState plant;
    @Nullable
    public MovingBlockRenderState baseView;
    @Nullable
    public List<BlockStateModelPart> baseParts;
    @Nullable
    public MovingBlockRenderState camoView;
    @Nullable
    public List<BlockStateModelPart> camoParts;
    public boolean camoHasTranslucency;
    public int[] camoTints = BlockModelRenderState.EMPTY_TINTS;
}
