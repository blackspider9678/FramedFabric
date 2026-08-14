package com.spider.framedfabric.registry;

import com.spider.framedfabric.FramedFabric;
import com.spider.framedfabric.blockentity.FramedBlockEntity;
import com.spider.framedfabric.blockentity.FramedSignBlockEntity;
import net.fabricmc.fabric.api.object.builder.v1.block.entity.FabricBlockEntityType;
import net.fabricmc.fabric.api.object.builder.v1.block.entity.FabricBlockEntityTypeBuilder;
import net.minecraft.block.entity.BlockEntityType;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.util.Identifier;

public final class ModBlockEntities {
    private ModBlockEntities() {}

    public static BlockEntityType<FramedBlockEntity> FRAMED;
    public static BlockEntityType<FramedSignBlockEntity> FRAMED_SIGN;

    public static void init() {
        if (FRAMED != null) return;

        FRAMED = Registry.register(
                Registries.BLOCK_ENTITY_TYPE,
                Identifier.of(FramedFabric.MOD_ID, "framed"),
                FabricBlockEntityTypeBuilder
                        .create((pos, state) -> new FramedBlockEntity(FRAMED, pos, state),
                                ModBlocks.framedAllArray()
                        )
                        .build()
        );

        FRAMED_SIGN = Registry.register(
                Registries.BLOCK_ENTITY_TYPE,
                Identifier.of(FramedFabric.MOD_ID, "framed_sign"),
                FabricBlockEntityTypeBuilder
                        .create(FramedSignBlockEntity::new,
                                ModBlocks.FRAMED_SIGN,
                                ModBlocks.FRAMED_WALL_SIGN
                        )
                        .build()
        );

        ((FabricBlockEntityType) BlockEntityType.SHELF).addSupportedBlock(ModBlocks.FRAMED_SHELF);
    }
}
