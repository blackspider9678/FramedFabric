package com.spider.framedfabric.client;

import com.spider.framedfabric.block.FramedMiniCubeBlock;
import com.spider.framedfabric.client.model.BakedCamoModel;
import com.spider.framedfabric.client.model.FramedSlopeStateModel;
import com.spider.framedfabric.client.model.MiniCubeRotatingModel;
import com.spider.framedfabric.registry.ModBlocks;
import net.fabricmc.fabric.api.client.model.loading.v1.ModelLoadingPlugin;
import net.minecraft.block.Block;
import net.minecraft.client.render.model.BlockStateModel;

public final class FramedModelPlugin {
    private FramedModelPlugin() {}

    public static void init() {
        ModelLoadingPlugin.register(ctx -> {
            ctx.modifyBlockModelAfterBake().register((model, bakeCtx) -> {
                Block b = bakeCtx.state().getBlock();

                if (!ModBlocks.FRAMED_ALL.contains(b)) {
                    return model;
                }

                // slope: custom geometry parent
                if (b == ModBlocks.FRAMED_SLOPE) {
                    return new BakedCamoModel(new FramedSlopeStateModel());
                }

                if (model instanceof BlockStateModel bsm) {
                    // Base: camo wrapper
                    BlockStateModel wrapped = new BakedCamoModel(bsm);

                    // Mini cube: add 16-step yaw rotation wrapper AFTER camo
                    if (b instanceof FramedMiniCubeBlock) {
                        wrapped = new MiniCubeRotatingModel(wrapped);
                    }

                    return wrapped;
                }

                return model;
            });
        });
    }
}
