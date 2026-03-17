package com.spider.framedfabric.registry;

import com.spider.framedfabric.FramedFabric;
import com.spider.framedfabric.screen.WoodWorkbenchScreenHandler;
import net.minecraft.resource.featuretoggle.FeatureFlags;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.screen.ScreenHandlerType;
import net.minecraft.util.Identifier;

public final class ModScreenHandlers {
    private ModScreenHandlers() {}

    public static ScreenHandlerType<WoodWorkbenchScreenHandler> WOOD_WORKBENCH;

    public static void init() {
        WOOD_WORKBENCH = Registry.register(
                Registries.SCREEN_HANDLER,
                Identifier.of(FramedFabric.MOD_ID, "wood_workbench"),
                new ScreenHandlerType<>(WoodWorkbenchScreenHandler::new, FeatureFlags.VANILLA_FEATURES)
        );
    }
}