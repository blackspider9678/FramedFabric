package com.spider.framedfabric.client.datagen;

import com.spider.framedfabric.block.custom.FramedCheckeredVerticalSlabBlock;
import com.spider.framedfabric.block.custom.FramedVerticalSlabBlock;
import com.spider.framedfabric.block.enums.VerticalSlabType;
import com.spider.framedfabric.registry.ModBlocks;
import net.fabricmc.fabric.api.datagen.v1.FabricDataOutput;
import net.fabricmc.fabric.api.datagen.v1.provider.FabricBlockLootTableProvider;
import net.minecraft.block.Block;
import net.minecraft.loot.LootPool;
import net.minecraft.loot.LootTable;
import net.minecraft.loot.condition.BlockStatePropertyLootCondition;
import net.minecraft.loot.entry.ItemEntry;
import net.minecraft.loot.function.SetCountLootFunction;
import net.minecraft.loot.provider.number.ConstantLootNumberProvider;
import net.minecraft.predicate.StatePredicate;
import net.minecraft.registry.RegistryWrapper;
import net.minecraft.state.property.Property;
import net.minecraft.util.StringIdentifiable;

import java.util.concurrent.CompletableFuture;

public class ModBlockLootTableProvider extends FabricBlockLootTableProvider {

    public ModBlockLootTableProvider(FabricDataOutput output,
                                     CompletableFuture<RegistryWrapper.WrapperLookup> registriesFuture) {
        super(output, registriesFuture);
    }

    @Override
    public void generate() {
        for (Block block : ModBlocks.FRAMED_ALL) {
            addDrop(block, this::createFramedDrop);
        }

        addDrop(ModBlocks.WOOD_WORKBENCH);
    }

    private LootTable.Builder createFramedDrop(Block block) {
        if (block == ModBlocks.FRAMED_DOOR) {
            return doorDrops(block);
        }

        if (block == ModBlocks.FRAMED_WALL_SIGN) {
            return dropItem(block, ModBlocks.FRAMED_SIGN);
        }

        if (block == ModBlocks.FRAMED_SLAB || block == ModBlocks.FRAMED_CHECKERED_SLAB) {
            return slabDrops(block);
        }

        if (block == ModBlocks.FRAMED_VERTICAL_SLAB) {
            return doubledDropOnProperty(block, FramedVerticalSlabBlock.TYPE, VerticalSlabType.DOUBLE);
        }

        if (block == ModBlocks.FRAMED_CHECKERED_VERTICAL_SLAB) {
            return doubledDropOnProperty(block, FramedCheckeredVerticalSlabBlock.TYPE, VerticalSlabType.DOUBLE);
        }

        return drops(block);
    }

    private LootTable.Builder dropItem(Block brokenBlock, Block droppedBlock) {
        return LootTable.builder()
                .pool(LootPool.builder()
                        .rolls(ConstantLootNumberProvider.create(1.0F))
                        .with(applyExplosionDecay(brokenBlock, ItemEntry.builder(droppedBlock))));
    }

    private <T extends Comparable<T> & StringIdentifiable> LootTable.Builder doubledDropOnProperty(
            Block block,
            Property<T> property,
            T doubledValue
    ) {
        return LootTable.builder()
                .pool(LootPool.builder()
                        .rolls(ConstantLootNumberProvider.create(1.0F))
                        .with(applyExplosionDecay(block,
                                ItemEntry.builder(block)
                                        .apply(SetCountLootFunction.builder(ConstantLootNumberProvider.create(2.0F))
                                                .conditionally(BlockStatePropertyLootCondition.builder(block)
                                                        .properties(StatePredicate.Builder.create()
                                                                .exactMatch(property, doubledValue)))))));
    }
}
