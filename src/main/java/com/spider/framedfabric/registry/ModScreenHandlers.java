package com.spider.framedfabric.registry;

import com.spider.framedfabric.FramedFabric;
import com.spider.framedfabric.screen.WoodWorkbenchScreenHandler;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.world.flag.FeatureFlags;
import net.minecraft.world.inventory.MenuType;

public final class ModScreenHandlers {
    private ModScreenHandlers() {}

    public static MenuType<WoodWorkbenchScreenHandler> WOOD_WORKBENCH;

    public static void init() {
        WOOD_WORKBENCH = Registry.register(
                BuiltInRegistries.MENU,
                Identifier.fromNamespaceAndPath(FramedFabric.MOD_ID, "wood_workbench"),
                new MenuType<>(WoodWorkbenchScreenHandler::new, FeatureFlags.VANILLA_SET)
        );
    }
}