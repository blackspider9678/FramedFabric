package com.spider.framedfabric.registry;

import com.spider.framedfabric.FramedFabric;
import com.spider.framedfabric.item.FramedWrenchItem;
import net.minecraft.item.Item;
import net.minecraft.item.Items;
import net.minecraft.registry.RegistryKey;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.util.Identifier;

import java.util.function.Function;

public final class ModItems {
    private ModItems() {}

    public static final Item HAMMER = register("hammer", settings -> new Item(settings.maxCount(1)));
    public static final Item WRENCH = register("wrench", settings -> new FramedWrenchItem(settings.maxCount(1)));


    public static void init() {
        // force-load
    }

    private static Item register(String path, Function<Item.Settings, Item> factory) {
        Identifier id = Identifier.of(FramedFabric.MOD_ID, path);
        RegistryKey<Item> key = RegistryKey.of(RegistryKeys.ITEM, id);

        // This injects the id into the Settings BEFORE the Item is constructed
        return Items.register(key, factory, new Item.Settings());
    }
}
