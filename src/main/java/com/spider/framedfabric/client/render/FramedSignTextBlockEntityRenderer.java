package com.spider.framedfabric.client.render;

import net.minecraft.block.WoodType;
import net.minecraft.client.model.Model;
import net.minecraft.client.render.block.entity.BlockEntityRendererFactory;
import net.minecraft.client.render.block.entity.SignBlockEntityRenderer;
import net.minecraft.client.render.command.ModelCommandRenderer;
import net.minecraft.client.render.command.OrderedRenderCommandQueue;
import net.minecraft.client.util.math.MatrixStack;

public class FramedSignTextBlockEntityRenderer extends SignBlockEntityRenderer {
    public FramedSignTextBlockEntityRenderer(BlockEntityRendererFactory.Context context) {
        super(context);
    }

    @Override
    protected void renderSign(
            MatrixStack matrices,
            int light,
            WoodType woodType,
            Model.SinglePartModel model,
            ModelCommandRenderer.CrumblingOverlayCommand overlay,
            OrderedRenderCommandQueue queue
    ) {
        // The framed sign body is rendered as the block model so camo can replace its quads.
    }
}
