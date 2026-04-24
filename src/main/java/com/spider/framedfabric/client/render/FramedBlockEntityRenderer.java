package com.spider.framedfabric.client.render;

import com.mojang.blaze3d.vertex.PoseStack;
import com.spider.framedfabric.blockentity.FramedBlockEntity;
import com.spider.framedfabric.registry.ModBlocks;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.feature.ModelFeatureRenderer;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;

public final class FramedBlockEntityRenderer implements BlockEntityRenderer<FramedBlockEntity, FramedBERenderState> {
    public FramedBlockEntityRenderer(BlockEntityRendererProvider.Context ctx) {}

    @Override
    public FramedBERenderState createRenderState() {
        return new FramedBERenderState();
    }

    @Override
    public void extractRenderState(
            FramedBlockEntity be,
            FramedBERenderState rs,
            float tickProgress,
            Vec3 cameraPos,
            ModelFeatureRenderer.CrumblingOverlay crumblingOverlay
    ) {
        BlockEntityRenderer.super.extractRenderState(be, rs, tickProgress, cameraPos, crumblingOverlay);

        BlockState self = be.getBlockState();
        rs.isFlowerPot = self.getBlock() == ModBlocks.FRAMED_FLOWER_POT;

        if (!rs.isFlowerPot) {
            rs.plantState = Blocks.AIR.defaultBlockState();
            return;
        }

        ItemStack plantStack = be.getPotPlantStack();
        if (!plantStack.isEmpty() && plantStack.getItem() instanceof BlockItem blockItem) {
            rs.plantState = blockItem.getBlock().defaultBlockState();
        } else {
            rs.plantState = Blocks.AIR.defaultBlockState();
        }
    }

    @Override
    public void submit(
            FramedBERenderState rs,
            PoseStack matrices,
            SubmitNodeCollector queue,
            CameraRenderState cameraState
    ) {
        // The flower pot plant render path needs a 26.1.2 submit-node implementation.
    }
}
