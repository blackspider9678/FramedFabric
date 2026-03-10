package com.spider.framedfabric;

import com.spider.framedfabric.registry.ModBlockEntities;
import com.spider.framedfabric.registry.ModBlocks;
import com.spider.framedfabric.registry.ModItemGroups;
import com.spider.framedfabric.registry.ModItems;
import net.fabricmc.api.ModInitializer;
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

        LOGGER.info("FramedFabric initialized.");
    }
}
