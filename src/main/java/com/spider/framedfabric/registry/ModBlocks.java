package com.spider.framedfabric.registry;

import com.spider.framedfabric.FramedFabric;
import com.spider.framedfabric.block.FramedBlock;
import com.spider.framedfabric.block.FramedButtonBlock;
import com.spider.framedfabric.block.FramedDoorBlock;
import com.spider.framedfabric.block.FramedFenceBlock;
import com.spider.framedfabric.block.FramedFenceGateBlock;
import com.spider.framedfabric.block.FramedFlowerPotBlock;
import com.spider.framedfabric.block.FramedLadderBlock;
import com.spider.framedfabric.block.FramedLightningRodBlock;
import com.spider.framedfabric.block.FramedMiniCubeBlock;
import com.spider.framedfabric.block.FramedPaneBlock;
import com.spider.framedfabric.block.FramedPressurePlateBlock;
import com.spider.framedfabric.block.FramedSlabBlock;
import com.spider.framedfabric.block.FramedSlopeBlock;
import com.spider.framedfabric.block.FramedStairsBlock;
import com.spider.framedfabric.block.FramedTrapdoorBlock;
import com.spider.framedfabric.block.FramedWallBlock;
import com.spider.framedfabric.block.WoodWorkbenchBlock;
import com.spider.framedfabric.block.custom.FramedBarBlock;
import com.spider.framedfabric.block.custom.FramedCheckeredBlock;
import com.spider.framedfabric.block.custom.FramedCheckeredSlabBlock;
import com.spider.framedfabric.block.custom.FramedCheckeredVerticalSlabBlock;
import com.spider.framedfabric.block.custom.FramedCornerCubeBlock;
import com.spider.framedfabric.block.custom.FramedCornerPostBlock;
import com.spider.framedfabric.block.custom.FramedCornerStepBlock;
import com.spider.framedfabric.block.custom.FramedHalfSlabBlock;
import com.spider.framedfabric.block.custom.FramedLargePostBlock;
import com.spider.framedfabric.block.custom.FramedMediumPostBlock;
import com.spider.framedfabric.block.custom.FramedSmallPostBlock;
import com.spider.framedfabric.block.custom.FramedThinPlateBlock;
import com.spider.framedfabric.block.custom.FramedVerticalBarBlock;
import com.spider.framedfabric.block.custom.FramedVerticalSlabBlock;
import com.spider.framedfabric.block.custom.FramedVerticalStairBlock;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockBehaviour;

public final class ModBlocks {
    private ModBlocks() {}

    private static final BlockBehaviour.Properties BASE =
            BlockBehaviour.Properties.of()
                    .strength(1.5f)
                    .sound(SoundType.WOOD)
                    .noOcclusion();

    public static final List<Block> FRAMED_ALL = new ArrayList<>();

    public static Block[] framedAllArray() {
        return FRAMED_ALL.toArray(Block[]::new);
    }

    public static final Block FRAMED_BLOCK = register("framed_block", FramedBlock::new, BASE);
    public static final Block FRAMED_MINI_CUBE = register("framed_mini_cube", FramedMiniCubeBlock::new, BASE);
    public static final Block FRAMED_SLAB = register("framed_slab", FramedSlabBlock::new, BASE);
    public static final Block FRAMED_VERTICAL_SLAB = register("framed_vertical_slab", FramedVerticalSlabBlock::new, BASE);
    public static final Block FRAMED_HALF_SLAB = register("framed_half_slab", FramedHalfSlabBlock::new, BASE);
    public static final Block FRAMED_STAIRS = register(
            "framed_stairs",
            settings -> new FramedStairsBlock(Blocks.OAK_PLANKS.defaultBlockState(), settings.noOcclusion()),
            BASE
    );
    public static final Block FRAMED_VERTICAL_STAIRS = register("framed_vertical_stairs", FramedVerticalStairBlock::new, BASE);
    public static final Block FRAMED_FENCE = register("framed_fence", FramedFenceBlock::new, BASE);
    public static final Block FRAMED_FENCE_GATE = register("framed_fence_gate", FramedFenceGateBlock::new, BASE);
    public static final Block FRAMED_WALL = register("framed_wall", FramedWallBlock::new, BASE);
    public static final Block FRAMED_PANE = register("framed_pane", FramedPaneBlock::new, BASE);
    public static final Block FRAMED_DOOR = register("framed_door", FramedDoorBlock::new, BASE);
    public static final Block FRAMED_TRAPDOOR = register("framed_trapdoor", FramedTrapdoorBlock::new, BASE);
    public static final Block FRAMED_BUTTON = register(
            "framed_button",
            settings -> new FramedButtonBlock(settings.noCollision()),
            BASE.noCollision()
    );
    public static final Block FRAMED_PRESSURE_PLATE = register("framed_pressure_plate", FramedPressurePlateBlock::new, BASE);
    public static final Block FRAMED_FLOWER_POT = register("framed_flower_pot", FramedFlowerPotBlock::new, BASE);
    public static final Block FRAMED_LIGHTNING_ROD = register("framed_lightning_rod", FramedLightningRodBlock::new, BASE);
    public static final Block FRAMED_LADDER = register("framed_ladder", FramedLadderBlock::new, BASE);
    public static final Block FRAMED_SLOPE = register(
            "framed_slope",
            FramedSlopeBlock::new,
            BlockBehaviour.Properties.ofFullCopy(Blocks.OAK_PLANKS).noOcclusion()
    );
    public static final Block FRAMED_CORNER_POST = register("framed_corner_post", FramedCornerPostBlock::new, BASE);
    public static final Block FRAMED_CORNER_STEP = register("framed_corner_step", FramedCornerStepBlock::new, BASE);
    public static final Block FRAMED_CORNER_CUBE = register("framed_corner_cube", FramedCornerCubeBlock::new, BASE);
    public static final Block FRAMED_SMALL_POST = register("framed_small_post", FramedSmallPostBlock::new, BASE);
    public static final Block FRAMED_MEDIUM_POST = register("framed_medium_post", FramedMediumPostBlock::new, BASE);
    public static final Block FRAMED_LARGE_POST = register("framed_large_post", FramedLargePostBlock::new, BASE);
    public static final Block FRAMED_THIN_PLATE = register("framed_thin_plate", FramedThinPlateBlock::new, BASE);
    public static final Block FRAMED_CHECKERED = register("framed_checkered", FramedCheckeredBlock::new, BASE);
    public static final Block FRAMED_CHECKERED_SLAB = register("framed_checkered_slab", FramedCheckeredSlabBlock::new, BASE);
    public static final Block FRAMED_CHECKERED_VERTICAL_SLAB = register("framed_checkered_vertical_slab", FramedCheckeredVerticalSlabBlock::new, BASE);
    public static final Block FRAMED_BAR_BLOCK = register("framed_bar", FramedBarBlock::new, BASE);
    public static final Block FRAMED_VERTICAL_BAR = register("framed_vertical_bar", FramedVerticalBarBlock::new, BASE);
    public static final Block WOOD_WORKBENCH = register(
            "wood_workbench",
            WoodWorkbenchBlock::new,
            BlockBehaviour.Properties.of()
                    .strength(2.5f)
                    .sound(SoundType.WOOD)
                    .noOcclusion(),
            false
    );

    public static void init() {}

    private static Block register(String path, Function<BlockBehaviour.Properties, Block> factory, BlockBehaviour.Properties settings) {
        return register(path, factory, settings, true);
    }

    private static Block register(
            String path,
            Function<BlockBehaviour.Properties, Block> factory,
            BlockBehaviour.Properties settings,
            boolean framed
    ) {
        Identifier id = Identifier.fromNamespaceAndPath(FramedFabric.MOD_ID, path);
        ResourceKey<Block> blockKey = ResourceKey.create(Registries.BLOCK, id);
        ResourceKey<Item> itemKey = ResourceKey.create(Registries.ITEM, id);

        Block block = Registry.register(BuiltInRegistries.BLOCK, blockKey, factory.apply(settings.setId(blockKey)));
        Registry.register(
                BuiltInRegistries.ITEM,
                itemKey,
                new BlockItem(block, new Item.Properties().useBlockDescriptionPrefix().setId(itemKey))
        );

        if (framed) {
            FRAMED_ALL.add(block);
        }

        return block;
    }
}
