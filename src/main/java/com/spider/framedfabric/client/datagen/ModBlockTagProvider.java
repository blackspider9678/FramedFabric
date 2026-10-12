package com.spider.framedfabric.client.datagen;

import com.spider.framedfabric.FramedFabric;
import com.spider.framedfabric.registry.ModBlocks;
import java.util.concurrent.CompletableFuture;
import net.fabricmc.fabric.api.datagen.v1.FabricPackOutput;
import net.fabricmc.fabric.api.datagen.v1.provider.FabricTagsProvider;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.tags.BlockTags;
import net.minecraft.tags.TagKey;
import net.minecraft.world.level.block.Block;

public class ModBlockTagProvider extends FabricTagsProvider.BlockTagsProvider {
    public static final TagKey<Block> FRAMED =
            TagKey.create(Registries.BLOCK, Identifier.fromNamespaceAndPath(FramedFabric.MOD_ID, "framed"));

    public ModBlockTagProvider(
            FabricPackOutput output,
            CompletableFuture<HolderLookup.Provider> registriesFuture
    ) {
        super(output, registriesFuture);
    }

    @Override
    protected void addTags(HolderLookup.Provider registries) {
        for (Block block : ModBlocks.FRAMED_ALL) {
            addBlock(BlockTags.MINEABLE_WITH_AXE, block);
            addBlock(FRAMED, block);
        }
        addBlock(BlockTags.MINEABLE_WITH_AXE, ModBlocks.WOOD_WORKBENCH);
        addBlock(BlockTags.WALLS, ModBlocks.FRAMED_WALL);
        addBlock(BlockTags.FENCES, ModBlocks.FRAMED_FENCE);
        addBlock(BlockTags.SLABS, ModBlocks.FRAMED_SLAB);
        addBlock(BlockTags.STAIRS, ModBlocks.FRAMED_STAIRS);
    }

    private void addBlock(TagKey<Block> tag, Block block) {
        builder(tag).add(BuiltInRegistries.BLOCK.getResourceKey(block).orElseThrow());
    }
}
