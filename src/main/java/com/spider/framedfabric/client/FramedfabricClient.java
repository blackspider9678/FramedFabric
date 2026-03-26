package com.spider.framedfabric.client;

import com.spider.framedfabric.client.render.FramedBlockEntityRenderer;
import com.spider.framedfabric.client.screen.WoodWorkbenchScreen;
import com.spider.framedfabric.net.payload.WoodWorkbenchRecipesPayload;
import com.spider.framedfabric.registry.ModBlockEntities;
import com.spider.framedfabric.registry.ModScreenHandlers;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.client.rendering.v1.BlockEntityRendererRegistry;
import net.minecraft.client.gui.screens.MenuScreens;

public class FramedfabricClient implements ClientModInitializer {
    @Override
    public void onInitializeClient() {
        BlockEntityRendererRegistry.register(ModBlockEntities.FRAMED, FramedBlockEntityRenderer::new);
        MenuScreens.register(ModScreenHandlers.WOOD_WORKBENCH, WoodWorkbenchScreen::new);

        ClientPlayNetworking.registerGlobalReceiver(WoodWorkbenchRecipesPayload.ID, (payload, context) -> {
            context.client().execute(() -> {
                if (context.client().player == null) return;
                if (!(context.client().player.containerMenu instanceof com.spider.framedfabric.screen.WoodWorkbenchScreenHandler handler)) return;
                if (handler.containerId != payload.syncId()) return;

                handler.setClientDisplayRecipes(payload.recipes());
            });
        });

        FramedModelPlugin.init();
        FramedColorProviders.init();
    }
}
