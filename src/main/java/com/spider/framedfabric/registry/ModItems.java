package com.spider.framedfabric.registry;

import com.spider.framedfabric.FramedFabric;
import com.spider.framedfabric.item.FramedWrenchItem;
import net.minecraft.core.registries.Registries;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.Registry;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.Item;

import java.util.function.Function;

public final class ModItems {
    private ModItems() {}

    public static final Item HAMMER = register("hammer", settings -> new Item(settings.stacksTo(1)));
    public static final Item WRENCH = register("wrench", settings -> new FramedWrenchItem(settings.stacksTo(1)));


    public static void init() {
        // force-load
    }

    private static Item register(String path, Function<Item.Properties, Item> factory) {
        Identifier id = Identifier.fromNamespaceAndPath(FramedFabric.MOD_ID, path);
        ResourceKey<Item> key = ResourceKey.create(Registries.ITEM, id);

        Item item = factory.apply(new Item.Properties().setId(key));
        return Registry.register(BuiltInRegistries.ITEM, key, item);
    }
}
