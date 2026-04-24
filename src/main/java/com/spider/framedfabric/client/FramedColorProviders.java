package com.spider.framedfabric.client;

import com.spider.framedfabric.block.custom.FramedVerticalSlabBlock;
import com.spider.framedfabric.blockentity.FramedBlockEntity;
import com.spider.framedfabric.registry.ModBlocks;
import java.util.List;
import net.fabricmc.fabric.api.client.rendering.v1.BlockColorRegistry;
import net.minecraft.client.Minecraft;
import net.minecraft.client.color.block.BlockTintSource;
import net.minecraft.client.renderer.block.BlockAndTintGetter;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.SlabBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.SlabType;

import static com.spider.framedfabric.client.model.BakedCamoModel.TINT_BASE;

public final class FramedColorProviders {
    private FramedColorProviders() {}

    public static void init() {
        BlockColorRegistry.register(
                createLayers(),
                ModBlocks.framedAllArray()
        );
    }

    private static List<BlockTintSource> createLayers() {
        int layerCount = TINT_BASE + (FramedBlockEntity.MAX_CAMO_PARTS * 8);

        return java.util.stream.IntStream.range(0, layerCount)
                .mapToObj(TintLayer::new)
                .map(BlockTintSource.class::cast)
                .toList();
    }

    private record TintLayer(int layerIndex) implements BlockTintSource {
        @Override
        public int color(BlockState state) {
            return -1;
        }

        @Override
        public int colorInWorld(BlockState state, BlockAndTintGetter world, BlockPos pos) {
            var be = world.getBlockEntity(pos);
            if (!(be instanceof FramedBlockEntity framed) || !framed.hasAnyCamo()) {
                return -1;
            }

            if (layerIndex == 7 || layerIndex < 0) {
                return -1;
            }

            int part;
            int camoTintIndex = layerIndex;

            if (layerIndex >= TINT_BASE) {
                int packed = layerIndex - TINT_BASE;
                part = packed / 8;
                camoTintIndex = packed % 8;

                if (part < 0 || part >= FramedBlockEntity.MAX_CAMO_PARTS) {
                    return -1;
                }
            } else {
                part = unpackedPartIndex(state);
            }

            if (!framed.hasCamoPart(part)) {
                return -1;
            }

            BlockState camo = framed.getCamoPart(part);
            if (camo == null || camo.isAir() || ModBlocks.FRAMED_ALL.contains(camo.getBlock())) {
                return -1;
            }

            var client = Minecraft.getInstance();
            if (client == null) {
                return -1;
            }

            List<BlockTintSource> tintSources = client.getBlockColors().getTintSources(camo);
            if (camoTintIndex < 0 || camoTintIndex >= tintSources.size()) {
                return -1;
            }

            return tintSources.get(camoTintIndex).colorInWorld(camo, world, pos);
        }

        private static int unpackedPartIndex(BlockState state) {
            if (state.getBlock() instanceof SlabBlock) {
                SlabType type = state.getValue(SlabBlock.TYPE);
                return (type == SlabType.TOP) ? 1 : 0;
            }

            if (state.getBlock() instanceof FramedVerticalSlabBlock) {
                return 0;
            }

            return 0;
        }
    }
}
