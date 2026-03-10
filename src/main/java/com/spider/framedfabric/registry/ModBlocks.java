package com.spider.framedfabric.registry;

import com.spider.framedfabric.FramedFabric;
import com.spider.framedfabric.block.*;
import net.minecraft.block.AbstractBlock;
import net.minecraft.block.Block;
import net.minecraft.block.BlockSetType;
import net.minecraft.block.Blocks;
import net.minecraft.block.DoorBlock;
import net.minecraft.block.FenceBlock;
import net.minecraft.block.FenceGateBlock;
import net.minecraft.block.PressurePlateBlock;
import net.minecraft.block.SlabBlock;
import net.minecraft.block.StairsBlock;
import net.minecraft.block.TrapdoorBlock;
import net.minecraft.block.WoodType;
import net.minecraft.item.Items;
import net.minecraft.registry.RegistryKey;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.sound.BlockSoundGroup;
import net.minecraft.util.Identifier;

import java.util.function.Function;

public final class ModBlocks {
    private ModBlocks() {}

    // A single “material identity” for the framed set.
    // Stone-like strength, but wood sound is also fine. Pick one; you can tweak later.
    private static final AbstractBlock.Settings BASE =
            AbstractBlock.Settings.create()
                    .strength(1.5f)
                    .sounds(BlockSoundGroup.WOOD);

    // You’ll likely want your framed set to behave like “wood” for door/trapdoor/button/plate logic
    // (sounds, interactions), but still be non-flammable if you prefer. We can tune later.
    private static final BlockSetType SET_TYPE = BlockSetType.OAK;
    private static final WoodType WOOD_TYPE = WoodType.OAK;

    public static final Block FRAMED_BLOCK =
            register("framed_block", FramedBlock::new, BASE.nonOpaque());

    public static final Block FRAMED_SLAB =
            register("framed_slab", FramedSlabBlock::new, BASE.nonOpaque());

    public static final Block FRAMED_STAIRS =
            register("framed_stairs",
                    s -> new FramedStairsBlock(Blocks.OAK_PLANKS.getDefaultState(), s.nonOpaque()),
                    BASE);

    public static final Block FRAMED_FENCE =
            register("framed_fence", FramedFenceBlock::new, BASE.nonOpaque());

    public static final Block FRAMED_FENCE_GATE =
            register("framed_fence_gate",
                    s -> new FramedFenceGateBlock(WOOD_TYPE, s.nonOpaque()),
                    BASE);

    public static final Block FRAMED_DOOR =
            register("framed_door",
                    s -> new FramedDoorBlock(SET_TYPE, s.nonOpaque()),
                    BASE.nonOpaque());

    public static final Block FRAMED_TRAPDOOR =
            register("framed_trapdoor",
                    s -> new FramedTrapdoorBlock(SET_TYPE, s.nonOpaque()),
                    BASE.nonOpaque());

    public static final Block FRAMED_BUTTON =
            register("framed_button",
                    s -> new FramedButtonBlock(SET_TYPE, 20, s.noCollision().nonOpaque()),
                    BASE.noCollision());

    public static final Block FRAMED_PRESSURE_PLATE =
            register("framed_pressure_plate", s -> new FramedPressurePlateBlock(SET_TYPE, s), BASE.nonOpaque());

    public static void init() {
        // force-load
    }

    private static Block register(String path, Function<AbstractBlock.Settings, Block> factory, AbstractBlock.Settings settings) {
        Identifier id = Identifier.of(FramedFabric.MOD_ID, path);
        RegistryKey<Block> key = RegistryKey.of(RegistryKeys.BLOCK, id);

        Block block = Blocks.register(key, factory, settings);
        Items.register(block); // registers BlockItem w/ same id
        return block;
    }
}
