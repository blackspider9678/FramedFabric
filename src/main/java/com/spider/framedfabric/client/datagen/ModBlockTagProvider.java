package com.spider.framedfabric.client.datagen;

import com.spider.framedfabric.FramedFabric;
import com.spider.framedfabric.registry.ModBlocks;
import java.util.concurrent.CompletableFuture;
import net.fabricmc.fabric.api.datagen.v1.FabricPackOutput;
import net.fabricmc.fabric.api.datagen.v1.provider.FabricTagsProvider;
import net.minecraft.core.HolderLookup;
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
        valueLookupBuilder(BlockTags.MINEABLE_WITH_AXE)
                .addAll(ModBlocks.FRAMED_ALL)
                .add(ModBlocks.WOOD_WORKBENCH);

        valueLookupBuilder(FRAMED)
                .addAll(ModBlocks.FRAMED_ALL);

        valueLookupBuilder(BlockTags.WALLS).add(ModBlocks.FRAMED_WALL);
        valueLookupBuilder(BlockTags.FENCES).add(ModBlocks.FRAMED_FENCE);
        valueLookupBuilder(BlockTags.SLABS).add(ModBlocks.FRAMED_SLAB);
        valueLookupBuilder(BlockTags.STAIRS).add(ModBlocks.FRAMED_STAIRS);
    }
}
