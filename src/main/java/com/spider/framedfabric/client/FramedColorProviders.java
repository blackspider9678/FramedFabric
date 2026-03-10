package com.spider.framedfabric.client;

import com.spider.framedfabric.block.custom.FramedCheckeredSlabBlock;
import com.spider.framedfabric.blockentity.FramedBlockEntity;
import com.spider.framedfabric.registry.ModBlocks;
import com.spider.framedfabric.registry.FramedTags;
import net.fabricmc.fabric.api.client.rendering.v1.ColorProviderRegistry;
import net.minecraft.block.BlockState;
import net.minecraft.block.SlabBlock;
import net.minecraft.block.enums.SlabType;
import net.minecraft.client.MinecraftClient;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.BlockRenderView;

import static com.spider.framedfabric.client.model.BakedCamoModel.TINT_BASE;

public final class FramedColorProviders {
    private FramedColorProviders() {}

    // Prevent infinite recursion if BlockColors routes back through our provider.
    private static final ThreadLocal<Boolean> IN_PROVIDER = ThreadLocal.withInitial(() -> false);

    public static void init() {
        ColorProviderRegistry.BLOCK.register((state, world, pos, tintIndex) -> {
            if (!(world instanceof BlockRenderView brv) || pos == null) return -1;

            if (IN_PROVIDER.get()) return -1;
            IN_PROVIDER.set(true);
            try {
                var client = MinecraftClient.getInstance();
                if (client == null || client.getBlockColors() == null) return -1;

                var be = brv.getBlockEntity(pos);
                if (!(be instanceof FramedBlockEntity fbe) || !fbe.hasAnyCamo()) return -1;

                int part;
                int camoTintIndex = tintIndex;

                if (tintIndex == 7) return -1;     // flower pot dirt layer
                if (tintIndex < 0) return -1;      // untinted quad => don't tint

                // 1) PACKED tint always wins (checkered, checkered slab, vslab double, slab double, etc.)
                if (tintIndex >= TINT_BASE) {
                    int packed = tintIndex - TINT_BASE;
                    part = packed / 8;
                    camoTintIndex = packed % 8;

                    if (part < 0 || part >= FramedBlockEntity.MAX_CAMO_PARTS) return -1;
                }
                // 2) UNPACKED: only vanilla-type blocks that *need* dynamic part choice
                else if (state.getBlock() instanceof SlabBlock) {
                    SlabType type = state.get(SlabBlock.TYPE);
                    part = (type == SlabType.TOP) ? 1 : 0;   // bottom/double use part 0 for tint purposes
                }
                // 3) UNPACKED vertical slab single
                else if (state.getBlock() instanceof com.spider.framedfabric.block.custom.FramedVerticalSlabBlock) {
                    part = 0;
                }
                // 4) Everything else is single-part
                else {
                    part = 0;
                }

                if (!fbe.hasCamoPart(part)) return -1;

                BlockState camo = fbe.getCamoPart(part);
                if (camo == null || camo.isAir()) return -1;
                if (camo.isIn(FramedTags.FRAMED_BLOCKS)) return -1;

                return client.getBlockColors().getColor(camo, brv, pos, camoTintIndex);
            } finally {
                IN_PROVIDER.set(false);
            }
        }, ModBlocks.framedAllArray());
    }
}
