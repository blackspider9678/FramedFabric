package com.spider.framedfabric.registry;

import com.spider.framedfabric.FramedFabric;
import com.spider.framedfabric.block.*;
import com.spider.framedfabric.block.custom.*;
import com.spider.framedfabric.block.FramedLadderBlock;
import net.minecraft.block.*;
import net.minecraft.item.Items;
import net.minecraft.registry.RegistryKey;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.sound.BlockSoundGroup;
import net.minecraft.util.Identifier;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;

public final class ModBlocks {
    private ModBlocks() {}

    private static final AbstractBlock.Settings BASE =
            AbstractBlock.Settings.create()
                    .strength(1.5f)
                    .sounds(BlockSoundGroup.WOOD)
                    .nonOpaque()
            ;

    /** All framed blocks, auto-filled as we register. */
    public static final List<Block> FRAMED_ALL = new ArrayList<>();

    /** Convenience for APIs that require varargs Blocks. */
    public static Block[] framedAllArray() {
        return FRAMED_ALL.toArray(Block[]::new);
    }

    public static final Block FRAMED_BLOCK =
            register("framed_block", FramedBlock::new, BASE);

    public static final Block FRAMED_MINI_CUBE =
            register("framed_mini_cube", FramedMiniCubeBlock::new, BASE);

    public static final Block FRAMED_SLAB =
            register("framed_slab", FramedSlabBlock::new, BASE);

    public static final Block FRAMED_VERTICAL_SLAB =
            register("framed_vertical_slab", FramedVerticalSlabBlock::new, BASE);

    public static final Block FRAMED_HALF_SLAB =
            register("framed_half_slab", FramedHalfSlabBlock::new, BASE);

    public static final Block FRAMED_STAIRS =
            register("framed_stairs",
                    s -> new FramedStairsBlock(Blocks.OAK_PLANKS.getDefaultState(), s.nonOpaque()),
                    BASE);

    public static final Block FRAMED_VERTICAL_STAIRS =
            register("framed_vertical_stairs", FramedVerticalStairBlock::new, BASE);

    public static final Block FRAMED_FENCE =
            register("framed_fence", FramedFenceBlock::new, BASE);

    public static final Block FRAMED_FENCE_GATE =
            register("framed_fence_gate", FramedFenceGateBlock::new, BASE);

    public static final Block FRAMED_WALL =
            register("framed_wall",
                    FramedWallBlock::new,
                    BASE
            );
    public static final Block FRAMED_PANE =
            register("framed_pane", FramedPaneBlock::new, BASE);

    public static final Block FRAMED_DOOR =
            register("framed_door", FramedDoorBlock::new, BASE);

    public static final Block FRAMED_TRAPDOOR =
            register("framed_trapdoor", FramedTrapdoorBlock::new, BASE);

    public static final Block FRAMED_BUTTON =
            register("framed_button",
                    s -> new FramedButtonBlock(s.noCollision()),
                    BASE.noCollision());

    public static final Block FRAMED_PRESSURE_PLATE =
            register("framed_pressure_plate", FramedPressurePlateBlock::new, BASE);

    public static final Block FRAMED_FLOWER_POT =
            register("framed_flower_pot", FramedFlowerPotBlock::new, BASE);

    public static final Block FRAMED_LIGHTNING_ROD =
            register("framed_lightning_rod", FramedLightningRodBlock::new, BASE);

    public static final Block FRAMED_LADDER =
            register("framed_ladder", FramedLadderBlock::new, BASE);

    //Custom Blocks
    public static final Block FRAMED_SLOPE =
            register("framed_slope", FramedSlopeBlock::new,
                    AbstractBlock.Settings.copy(Blocks.OAK_PLANKS).nonOpaque());

    public static final Block FRAMED_CORNER_POST =
            register("framed_corner_post", FramedCornerPostBlock::new, BASE);

    public static final Block FRAMED_CORNER_STEP =
            register("framed_corner_step", FramedCornerStepBlock::new, BASE);

    public static final Block FRAMED_CORNER_CUBE =
            register("framed_corner_cube", FramedCornerCubeBlock::new, BASE);

    public static final Block FRAMED_SMALL_POST =
            register("framed_small_post", FramedSmallPostBlock::new, BASE);

    public static final Block FRAMED_MEDIUM_POST =
            register("framed_medium_post", FramedMediumPostBlock::new, BASE);

    public static final Block FRAMED_LARGE_POST =
            register("framed_large_post", FramedLargePostBlock::new, BASE);

    public static final Block FRAMED_THIN_PLATE =
            register("framed_thin_plate", FramedThinPlateBlock::new, BASE);

    public static final Block FRAMED_CHECKERED =
            register("framed_checkered", FramedCheckeredBlock::new, BASE);

    public static final Block FRAMED_CHECKERED_SLAB =
            register("framed_checkered_slab", FramedCheckeredSlabBlock::new, BASE);

    public static final Block FRAMED_CHECKERED_VERTICAL_SLAB =
            register("framed_checkered_vertical_slab", FramedCheckeredVerticalSlabBlock::new, BASE);

    public static final Block FRAMED_BAR_BLOCK =
            register("framed_bar", FramedBarBlock::new, BASE);

    public static final Block FRAMED_VERTICAL_BAR =
            register("framed_vertical_bar", FramedVerticalBarBlock::new, BASE);

    public static final Block WOOD_WORKBENCH =
            register("wood_workbench", WoodWorkbenchBlock::new,
                    AbstractBlock.Settings.create()
                            .strength(2.5f)
                            .sounds(BlockSoundGroup.WOOD)
                            .nonOpaque());

    public static void init() {}

    private static Block register(String path, Function<AbstractBlock.Settings, Block> factory, AbstractBlock.Settings settings) {
        Identifier id = Identifier.of(FramedFabric.MOD_ID, path);
        RegistryKey<Block> key = RegistryKey.of(RegistryKeys.BLOCK, id);

        Block block = Blocks.register(key, factory, settings);
        Items.register(block);

        FRAMED_ALL.add(block); // <-- auto add here
        return block;
    }
}
