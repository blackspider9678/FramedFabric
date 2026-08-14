package com.spider.framedfabric.client;

import com.spider.framedfabric.FramedFabric;
import com.spider.framedfabric.net.payload.WoodWorkbenchRecipesPayload;
import com.spider.framedfabric.registry.ModBlockEntities;
import com.spider.framedfabric.registry.ModBlocks;
import com.spider.framedfabric.client.render.FramedBlockEntityRenderer;
import com.spider.framedfabric.client.render.FramedSignTextBlockEntityRenderer;
import com.spider.framedfabric.client.screen.*;
import com.spider.framedfabric.registry.ModScreenHandlers;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.client.rendering.v1.BlockEntityRendererRegistry;
import net.fabricmc.fabric.api.client.rendering.v1.BlockRenderLayerMap;
import net.minecraft.client.gui.screen.ingame.HandledScreens;
import net.minecraft.client.render.BlockRenderLayer;
import net.minecraft.client.render.RenderLayers;

public class FramedfabricClient implements ClientModInitializer {
    @Override
    @SuppressWarnings({"unchecked", "rawtypes"})
    public void onInitializeClient() {
        FramedFabric.LOGGER.info("[FramedFabric] Client init");

        // Best “one size fits most”: leaves/foliage/glass textures won’t go gray/opaque.
        BlockRenderLayerMap.putBlocks(BlockRenderLayer.TRANSLUCENT, ModBlocks.framedAllArray());
        BlockEntityRendererRegistry.register(ModBlockEntities.FRAMED, FramedBlockEntityRenderer::new);
        BlockEntityRendererRegistry.register(ModBlockEntities.FRAMED_SIGN, ctx -> (net.minecraft.client.render.block.entity.BlockEntityRenderer) new FramedSignTextBlockEntityRenderer(ctx));
        HandledScreens.register(ModScreenHandlers.WOOD_WORKBENCH, WoodWorkbenchScreen::new);


        ClientPlayNetworking.registerGlobalReceiver(WoodWorkbenchRecipesPayload.ID, (payload, context) -> {
            context.client().execute(() -> {
                if (context.client().player == null) return;
                if (!(context.client().player.currentScreenHandler instanceof com.spider.framedfabric.screen.WoodWorkbenchScreenHandler handler)) return;
                if (handler.syncId != payload.syncId()) return;

                handler.setClientDisplayRecipes(payload.recipes());
            });
        });

        FramedModelPlugin.init();
        FramedColorProviders.init();
    }
}
