package com.spider.framedfabric.client;

import com.spider.framedfabric.FramedFabric;
import com.spider.framedfabric.registry.ModBlockEntities;
import com.spider.framedfabric.registry.ModBlocks;
import com.spider.framedfabric.client.render.FramedBlockEntityRenderer;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.rendering.v1.BlockEntityRendererRegistry;
import net.fabricmc.fabric.api.client.rendering.v1.BlockRenderLayerMap;
import net.minecraft.client.render.BlockRenderLayer;
import net.minecraft.client.render.RenderLayers;

public class FramedfabricClient implements ClientModInitializer {
    @Override
    public void onInitializeClient() {
        FramedFabric.LOGGER.info("[FramedFabric] Client init");

        // Best “one size fits most”: leaves/foliage/glass textures won’t go gray/opaque.
        BlockRenderLayerMap.putBlocks(BlockRenderLayer.TRANSLUCENT, ModBlocks.framedAllArray());
        BlockEntityRendererRegistry.register(ModBlockEntities.FRAMED, FramedBlockEntityRenderer::new);

        FramedModelPlugin.init();
        FramedColorProviders.init();
    }
}
