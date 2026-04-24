package com.spider.framedfabric.client;

import com.spider.framedfabric.block.FramedMiniCubeBlock;
import com.spider.framedfabric.client.model.BakedCamoModel;
import com.spider.framedfabric.client.model.FramedSlopeStateModel;
import com.spider.framedfabric.client.model.MiniCubeRotatingModel;
import com.spider.framedfabric.registry.ModBlocks;
import net.fabricmc.fabric.api.client.model.loading.v1.ModelLoadingPlugin;
import net.minecraft.client.renderer.block.dispatch.BlockStateModel;
import net.minecraft.world.level.block.Block;

public final class FramedModelPlugin {
    private FramedModelPlugin() {}

    public static void init() {
        ModelLoadingPlugin.register(ctx -> {
            ctx.modifyBlockModelAfterBake().register((model, bakeCtx) -> {
                Block block = bakeCtx.state().getBlock();

                if (!ModBlocks.FRAMED_ALL.contains(block)) {
                    return model;
                }

                if (block == ModBlocks.FRAMED_SLOPE) {
                    return new BakedCamoModel(new FramedSlopeStateModel());
                }

                if (model instanceof BlockStateModel blockModel) {
                    BlockStateModel wrapped = new BakedCamoModel(blockModel);

                    if (block instanceof FramedMiniCubeBlock) {
                        wrapped = new MiniCubeRotatingModel(wrapped);
                    }

                    return wrapped;
                }

                return model;
            });
        });
    }
}
