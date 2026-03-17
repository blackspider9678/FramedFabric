package com.spider.framedfabric;

import com.spider.framedfabric.registry.*;
import com.spider.framedfabric.screen.WoodWorkbenchRecipes;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
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

        //WoodWorkbenchRecipes.init();
        ModRecipeTypes.init();

        LOGGER.info("FramedFabric initialized.");
    }
}
