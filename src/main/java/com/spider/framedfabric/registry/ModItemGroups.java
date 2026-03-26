package com.spider.framedfabric.registry;

import com.spider.framedfabric.FramedFabric;
import net.fabricmc.fabric.api.creativetab.v1.FabricCreativeModeTab;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.ItemStack;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.Registry;
import net.minecraft.resources.ResourceKey;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;

public final class ModItemGroups {
    private ModItemGroups() {}

    public static final Identifier FRAMEDFABRIC_GROUP_ID =
            Identifier.fromNamespaceAndPath(FramedFabric.MOD_ID, "assets/framedfabric");

    public static final ResourceKey<CreativeModeTab> FRAMEDFABRIC_GROUP_KEY =
            ResourceKey.create(Registries.CREATIVE_MODE_TAB, FRAMEDFABRIC_GROUP_ID);

    public static final CreativeModeTab FRAMEDFABRIC_GROUP = Registry.register(
            BuiltInRegistries.CREATIVE_MODE_TAB,
            FRAMEDFABRIC_GROUP_ID,
            FabricCreativeModeTab.builder()
                    .title(Component.translatable("itemGroup.framedfabric"))
                    .icon(() -> new ItemStack(ModBlocks.FRAMED_BLOCK)) // or ModBlocks.FRAMED_BLOCK
                    .displayItems((displayContext, entries) -> {
                        // Tools / utility first
                        entries.accept(ModItems.HAMMER);
                        entries.accept(ModItems.WRENCH);
                        entries.accept(ModBlocks.WOOD_WORKBENCH);

                        // Basic shapes
                        // (These "add" calls work as long as the block has an associated BlockItem)
                        entries.accept(ModBlocks.FRAMED_BLOCK);
                        entries.accept(ModBlocks.FRAMED_MINI_CUBE);

                        // If you have these registered, add them here:
                        // Vanilla style
                        entries.accept(ModBlocks.FRAMED_SLAB);
                        entries.accept(ModBlocks.FRAMED_VERTICAL_SLAB);
                        entries.accept(ModBlocks.FRAMED_HALF_SLAB);
                        entries.accept(ModBlocks.FRAMED_STAIRS);
                        entries.accept(ModBlocks.FRAMED_VERTICAL_STAIRS);
                        entries.accept(ModBlocks.FRAMED_FENCE);
                        entries.accept(ModBlocks.FRAMED_FENCE_GATE);
                        entries.accept(ModBlocks.FRAMED_WALL);
                        entries.accept(ModBlocks.FRAMED_PANE);
                        entries.accept(ModBlocks.FRAMED_DOOR);
                        entries.accept(ModBlocks.FRAMED_TRAPDOOR);
                        entries.accept(ModBlocks.FRAMED_BUTTON);
                        entries.accept(ModBlocks.FRAMED_PRESSURE_PLATE);
                        entries.accept(ModBlocks.FRAMED_LIGHTNING_ROD);
                        //entries.add(ModBlocks.FRAMED_FLOWER_POT);
                        entries.accept(ModBlocks.FRAMED_LADDER);

                        //Custom Style
                        //entries.add(ModBlocks.FRAMED_SLOPE);
                        entries.accept(ModBlocks.FRAMED_CORNER_POST);
                        entries.accept(ModBlocks.FRAMED_CORNER_STEP);
                        entries.accept(ModBlocks.FRAMED_CORNER_CUBE);
                        entries.accept(ModBlocks.FRAMED_LARGE_POST);
                        entries.accept(ModBlocks.FRAMED_MEDIUM_POST);
                        entries.accept(ModBlocks.FRAMED_SMALL_POST);
                        entries.accept(ModBlocks.FRAMED_THIN_PLATE);
                        entries.accept(ModBlocks.FRAMED_CHECKERED);
                        entries.accept(ModBlocks.FRAMED_CHECKERED_SLAB);
                        entries.accept(ModBlocks.FRAMED_CHECKERED_VERTICAL_SLAB);
                        entries.accept(ModBlocks.FRAMED_BAR_BLOCK);
                        entries.accept(ModBlocks.FRAMED_VERTICAL_BAR);


                        // Add future shapes below in the order you want them to appear
                    })
                    .build()
    );

    public static void init() {
        // force-load
    }
}
