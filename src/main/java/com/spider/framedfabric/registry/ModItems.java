package com.spider.framedfabric.registry;

import com.spider.framedfabric.FramedFabric;
import com.spider.framedfabric.item.FramedWrenchItem;
import java.util.function.Function;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.item.Item;

public final class ModItems {
    private ModItems() {}

    public static final Item HAMMER = register("hammer", settings -> new Item(settings.stacksTo(1)));
    public static final Item WRENCH = register("wrench", settings -> new FramedWrenchItem(settings.stacksTo(1)));

    public static void init() {
    }

    private static Item register(String path, Function<Item.Properties, Item> factory) {
        Identifier id = Identifier.fromNamespaceAndPath(FramedFabric.MOD_ID, path);
        ResourceKey<Item> key = ResourceKey.create(Registries.ITEM, id);
        return Registry.register(BuiltInRegistries.ITEM, key, factory.apply(new Item.Properties().setId(key)));
    }
}
