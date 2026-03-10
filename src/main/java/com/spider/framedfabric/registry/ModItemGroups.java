package com.spider.framedfabric.registry;

import com.spider.framedfabric.FramedFabric;
import net.fabricmc.fabric.api.itemgroup.v1.FabricItemGroup;
import net.minecraft.item.ItemGroup;
import net.minecraft.item.ItemStack;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.registry.RegistryKey;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;

public final class ModItemGroups {
    private ModItemGroups() {}

    public static final Identifier FRAMEDFABRIC_GROUP_ID =
            Identifier.of(FramedFabric.MOD_ID, "framedfabric");

    public static final RegistryKey<ItemGroup> FRAMEDFABRIC_GROUP_KEY =
            RegistryKey.of(RegistryKeys.ITEM_GROUP, FRAMEDFABRIC_GROUP_ID);

    public static final ItemGroup FRAMEDFABRIC_GROUP = Registry.register(
            Registries.ITEM_GROUP,
            FRAMEDFABRIC_GROUP_ID,
            FabricItemGroup.builder()
                    .displayName(Text.translatable("itemGroup.framedfabric"))
                    .icon(() -> new ItemStack(ModItems.HAMMER)) // or ModBlocks.FRAMED_BLOCK
                    .entries((displayContext, entries) -> {
                        // Tools / utility first
                        entries.add(ModItems.HAMMER);
                        entries.add(ModItems.WRENCH);

                        // Basic shapes
                        // (These "add" calls work as long as the block has an associated BlockItem)
                        entries.add(ModBlocks.FRAMED_BLOCK);

                        // If you have these registered, add them here:
                        entries.add(ModBlocks.FRAMED_SLAB);
                        entries.add(ModBlocks.FRAMED_STAIRS);
                        entries.add(ModBlocks.FRAMED_FENCE);
                        entries.add(ModBlocks.FRAMED_FENCE_GATE);
                        entries.add(ModBlocks.FRAMED_DOOR);
                        entries.add(ModBlocks.FRAMED_TRAPDOOR);
                        entries.add(ModBlocks.FRAMED_BUTTON);
                        entries.add(ModBlocks.FRAMED_PRESSURE_PLATE);

                        // Add future shapes below in the order you want them to appear
                    })
                    .build()
    );

    public static void init() {
        // force-load
    }
}
