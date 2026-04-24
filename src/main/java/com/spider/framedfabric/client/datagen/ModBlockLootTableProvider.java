package com.spider.framedfabric.client.datagen;

import com.spider.framedfabric.block.custom.FramedCheckeredVerticalSlabBlock;
import com.spider.framedfabric.block.custom.FramedVerticalSlabBlock;
import com.spider.framedfabric.block.enums.VerticalSlabType;
import com.spider.framedfabric.registry.ModBlocks;
import java.util.concurrent.CompletableFuture;
import net.fabricmc.fabric.api.datagen.v1.FabricPackOutput;
import net.fabricmc.fabric.api.datagen.v1.provider.FabricBlockLootSubProvider;
import net.minecraft.advancements.criterion.StatePropertiesPredicate;
import net.minecraft.core.HolderLookup;
import net.minecraft.util.StringRepresentable;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.properties.Property;
import net.minecraft.world.level.storage.loot.LootPool;
import net.minecraft.world.level.storage.loot.LootTable;
import net.minecraft.world.level.storage.loot.entries.LootItem;
import net.minecraft.world.level.storage.loot.functions.SetItemCountFunction;
import net.minecraft.world.level.storage.loot.predicates.LootItemBlockStatePropertyCondition;
import net.minecraft.world.level.storage.loot.providers.number.ConstantValue;

public class ModBlockLootTableProvider extends FabricBlockLootSubProvider {
    public ModBlockLootTableProvider(
            FabricPackOutput output,
            CompletableFuture<HolderLookup.Provider> registriesFuture
    ) {
        super(output, registriesFuture);
    }

    @Override
    public void generate() {
        for (Block block : ModBlocks.FRAMED_ALL) {
            add(block, this::createFramedDrop);
        }

        dropSelf(ModBlocks.WOOD_WORKBENCH);
    }

    private LootTable.Builder createFramedDrop(Block block) {
        if (block == ModBlocks.FRAMED_DOOR) {
            return createDoorTable(block);
        }

        if (block == ModBlocks.FRAMED_SLAB || block == ModBlocks.FRAMED_CHECKERED_SLAB) {
            return createSlabItemTable(block);
        }

        if (block == ModBlocks.FRAMED_VERTICAL_SLAB) {
            return doubledDropOnProperty(block, FramedVerticalSlabBlock.TYPE, VerticalSlabType.DOUBLE);
        }

        if (block == ModBlocks.FRAMED_CHECKERED_VERTICAL_SLAB) {
            return doubledDropOnProperty(block, FramedCheckeredVerticalSlabBlock.TYPE, VerticalSlabType.DOUBLE);
        }

        return createSingleItemTable(block);
    }

    private <T extends Comparable<T> & StringRepresentable> LootTable.Builder doubledDropOnProperty(
            Block block,
            Property<T> property,
            T doubledValue
    ) {
        return LootTable.lootTable()
                .withPool(LootPool.lootPool()
                        .setRolls(ConstantValue.exactly(1.0F))
                        .add(applyExplosionDecay(block,
                                LootItem.lootTableItem(block)
                                        .apply(SetItemCountFunction.setCount(ConstantValue.exactly(2.0F))
                                                .when(LootItemBlockStatePropertyCondition.hasBlockStateProperties(block)
                                                        .setProperties(StatePropertiesPredicate.Builder.properties()
                                                                .hasProperty(property, doubledValue)))))));
    }
}
