package com.spider.framedfabric.client.datagen;

import com.spider.framedfabric.FramedFabric;
import com.spider.framedfabric.registry.ModBlocks;
import net.fabricmc.fabric.api.datagen.v1.FabricDataOutput;
import net.fabricmc.fabric.api.datagen.v1.provider.FabricTagProvider;
import net.minecraft.block.Block;
import net.minecraft.registry.Registries;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.registry.RegistryWrapper;
import net.minecraft.registry.tag.BlockTags;
import net.minecraft.registry.tag.TagKey;
import net.minecraft.util.Identifier;

import java.util.concurrent.CompletableFuture;

public class ModBlockTagProvider extends FabricTagProvider.BlockTagProvider {

    public ModBlockTagProvider(FabricDataOutput output,
                               CompletableFuture<RegistryWrapper.WrapperLookup> registriesFuture) {
        super(output, registriesFuture);
    }

    public static final TagKey<Block> FRAMED =
            TagKey.of(RegistryKeys.BLOCK, Identifier.of(FramedFabric.MOD_ID, "framed"));

    @Override
    protected void configure(RegistryWrapper.WrapperLookup arg) {

        valueLookupBuilder(BlockTags.AXE_MINEABLE)
                .add(ModBlocks.FRAMED_ALL);

        valueLookupBuilder(FRAMED)
                .add(ModBlocks.FRAMED_ALL);

        valueLookupBuilder(BlockTags.WALLS).add(ModBlocks.FRAMED_WALL);
        valueLookupBuilder(BlockTags.FENCES).add(ModBlocks.FRAMED_FENCE);
        valueLookupBuilder(BlockTags.SLABS).add(ModBlocks.FRAMED_SLAB);
        valueLookupBuilder(BlockTags.STAIRS).add(ModBlocks.FRAMED_STAIRS);
    }
}