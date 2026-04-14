package com.spider.framedfabric;

import com.spider.framedfabric.net.payload.WoodWorkbenchJeiRecipesPayload;
import com.spider.framedfabric.registry.*;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class FramedFabric implements ModInitializer {
    public static final String MOD_ID = "framedfabric";
    public static final Logger LOGGER = LoggerFactory.getLogger("FramedFabric");

    @Override
    public void onInitialize() {
        ModItems.init();
        ModBlocks.init();
        ModBlockEntities.init();
        ModItemGroups.init();
        ModScreenHandlers.init();
        ModPayloads.init();
        ModRecipeTypes.init();
        ServerPlayConnectionEvents.JOIN.register((handler, sender, server) ->
                sender.sendPacket(WoodWorkbenchJeiRecipesPayload.fromRecipeManager(server.getRecipeManager()))
        );

        LOGGER.info("FramedFabric initialized.");
    }
}
