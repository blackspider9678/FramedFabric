package com.spider.framedfabric.registry;

import com.spider.framedfabric.FramedFabric;
import com.spider.framedfabric.blockentity.FramedBlockEntity;
import net.fabricmc.fabric.api.object.builder.v1.block.entity.FabricBlockEntityTypeBuilder;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.Registry;
import net.minecraft.resources.Identifier;

public final class ModBlockEntities {
    private ModBlockEntities() {}

    public static BlockEntityType<FramedBlockEntity> FRAMED;

    public static void init() {
        if (FRAMED != null) return;

        FRAMED = Registry.register(
                BuiltInRegistries.BLOCK_ENTITY_TYPE,
                Identifier.fromNamespaceAndPath(FramedFabric.MOD_ID, "framed"),
                FabricBlockEntityTypeBuilder
                        .create((pos, state) -> new FramedBlockEntity(FRAMED, pos, state),
                                ModBlocks.framedAllArray()
                        )
                        .build()
        );
    }
}
