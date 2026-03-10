package com.spider.framedfabric.client.render;

import com.spider.framedfabric.blockentity.FramedBlockEntity;
import com.spider.framedfabric.registry.ModBlocks;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.client.render.OverlayTexture;
import net.minecraft.client.render.block.entity.BlockEntityRenderer;
import net.minecraft.client.render.block.entity.BlockEntityRendererFactory;
import net.minecraft.client.render.command.ModelCommandRenderer;
import net.minecraft.client.render.command.OrderedRenderCommandQueue;
import net.minecraft.client.render.state.CameraRenderState;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.item.BlockItem;
import net.minecraft.item.ItemStack;
import net.minecraft.util.math.Vec3d;
import org.jspecify.annotations.Nullable;

public final class FramedBlockEntityRenderer implements BlockEntityRenderer<FramedBlockEntity, FramedBERenderState> {

    public FramedBlockEntityRenderer(BlockEntityRendererFactory.Context ctx) {}

    @Override
    public FramedBERenderState createRenderState() {
        return new FramedBERenderState();
    }

    @Override
    public void updateRenderState(FramedBlockEntity be, FramedBERenderState rs, float tickProgress, Vec3d cameraPos, ModelCommandRenderer.CrumblingOverlayCommand crumblingOverlay)
    {
        BlockState self = be.getCachedState();
        rs.isFlowerPot = (self.getBlock() == ModBlocks.FRAMED_FLOWER_POT);

        if (!rs.isFlowerPot) {
            rs.plantState = Blocks.AIR.getDefaultState();
            return;
        }

        ItemStack plantStack = be.getPotPlantStack();
        if (!plantStack.isEmpty() && plantStack.getItem() instanceof BlockItem bi) {
            rs.plantState = bi.getBlock().getDefaultState();
        } else {
            rs.plantState = Blocks.AIR.getDefaultState();
        }
    }

    @Override
    public void render(FramedBERenderState rs, MatrixStack matrices, OrderedRenderCommandQueue queue, CameraRenderState cameraState) {
        if (!rs.isFlowerPot || rs.plantState.isAir()) return;

        matrices.push();
        matrices.translate(0.5, 0.25, 0.5);
        matrices.scale(0.5f, 0.5f, 0.5f);
        matrices.translate(-0.5, 0.0, -0.5);

        int light = 0xF000F0;                 // fullbright for now
        int overlay = OverlayTexture.DEFAULT_UV;
        int outlineColor = 0;

        // ✅ Let Minecraft decide layers (cutout/translucent/etc)
        queue.submitBlock(matrices, rs.plantState, light, overlay, outlineColor);

        matrices.pop();
    }

}
