package com.spider.framedfabric.registry;

import com.spider.framedfabric.net.payload.WoodWorkbenchRecipesPayload;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;

public final class ModPayloads {
    private ModPayloads() {}

    public static void init() {
        PayloadTypeRegistry.playS2C().register(WoodWorkbenchRecipesPayload.ID, WoodWorkbenchRecipesPayload.CODEC);
    }
}
