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
            Identifier.of(FramedFabric.MOD_ID, "assets/framedfabric");

    public static final RegistryKey<ItemGroup> FRAMEDFABRIC_GROUP_KEY =
            RegistryKey.of(RegistryKeys.ITEM_GROUP, FRAMEDFABRIC_GROUP_ID);

    public static final ItemGroup FRAMEDFABRIC_GROUP = Registry.register(
            Registries.ITEM_GROUP,
            FRAMEDFABRIC_GROUP_ID,
            FabricItemGroup.builder()
                    .displayName(Text.translatable("itemGroup.framedfabric"))
                    .icon(() -> new ItemStack(ModBlocks.FRAMED_BLOCK)) // or ModBlocks.FRAMED_BLOCK
                    .entries((displayContext, entries) -> {
                        // Tools / utility first
                        entries.add(ModItems.HAMMER);
                        entries.add(ModItems.WRENCH);
                        entries.add(ModBlocks.WOOD_WORKBENCH);

                        // Basic shapes
                        // (These "add" calls work as long as the block has an associated BlockItem)
                        entries.add(ModBlocks.FRAMED_BLOCK);
                        entries.add(ModBlocks.FRAMED_MINI_CUBE);

                        // If you have these registered, add them here:
                        // Vanilla style
                        entries.add(ModBlocks.FRAMED_SLAB);
                        entries.add(ModBlocks.FRAMED_VERTICAL_SLAB);
                        entries.add(ModBlocks.FRAMED_HALF_SLAB);
                        entries.add(ModBlocks.FRAMED_STAIRS);
                        entries.add(ModBlocks.FRAMED_VERTICAL_STAIRS);
                        entries.add(ModBlocks.FRAMED_FENCE);
                        entries.add(ModBlocks.FRAMED_FENCE_GATE);
                        entries.add(ModBlocks.FRAMED_WALL);
                        entries.add(ModBlocks.FRAMED_PANE);
                        entries.add(ModBlocks.FRAMED_DOOR);
                        entries.add(ModBlocks.FRAMED_TRAPDOOR);
                        entries.add(ModBlocks.FRAMED_BUTTON);
                        entries.add(ModBlocks.FRAMED_PRESSURE_PLATE);
                        entries.add(ModBlocks.FRAMED_LIGHTNING_ROD);
                        entries.add(ModBlocks.FRAMED_FLOWER_POT);
                        entries.add(ModBlocks.FRAMED_LADDER);

                        //Custom Style
                        //entries.add(ModBlocks.FRAMED_SLOPE);
                        entries.add(ModBlocks.FRAMED_CORNER_POST);
                        entries.add(ModBlocks.FRAMED_CORNER_STEP);
                        entries.add(ModBlocks.FRAMED_CORNER_CUBE);
                        entries.add(ModBlocks.FRAMED_LARGE_POST);
                        entries.add(ModBlocks.FRAMED_MEDIUM_POST);
                        entries.add(ModBlocks.FRAMED_SMALL_POST);
                        entries.add(ModBlocks.FRAMED_THIN_PLATE);
                        entries.add(ModBlocks.FRAMED_CHECKERED);
                        entries.add(ModBlocks.FRAMED_CHECKERED_SLAB);
                        entries.add(ModBlocks.FRAMED_CHECKERED_VERTICAL_SLAB);
                        entries.add(ModBlocks.FRAMED_BAR_BLOCK);
                        entries.add(ModBlocks.FRAMED_VERTICAL_BAR);


                        // Add future shapes below in the order you want them to appear
                    })
                    .build()
    );

    public static void init() {
        // force-load
    }
}
