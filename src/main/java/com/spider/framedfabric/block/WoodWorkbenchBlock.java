package com.spider.framedfabric.block;

import com.mojang.serialization.MapCodec;
import com.spider.framedfabric.screen.WoodWorkbenchScreenHandler;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.screen.NamedScreenHandlerFactory;
import net.minecraft.screen.ScreenHandlerContext;
import net.minecraft.screen.SimpleNamedScreenHandlerFactory;
import net.minecraft.text.Text;
import net.minecraft.util.ActionResult;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

public class WoodWorkbenchBlock extends Block {
    public static final MapCodec<WoodWorkbenchBlock> CODEC = createCodec(WoodWorkbenchBlock::new);

    public WoodWorkbenchBlock(Settings settings) {
        super(settings);
    }

    @Override
    protected MapCodec<? extends Block> getCodec() {
        return CODEC;
    }

    @Override
    protected ActionResult onUse(BlockState state, World world, BlockPos pos,
                                 PlayerEntity player, BlockHitResult hit) {
        if (!world.isClient()) {
            NamedScreenHandlerFactory factory = new SimpleNamedScreenHandlerFactory(
                    (syncId, playerInventory, playerEntity) ->
                            new WoodWorkbenchScreenHandler(syncId, playerInventory, ScreenHandlerContext.create(world, pos)),
                    Text.literal("Wood Workbench")
            );
            player.openHandledScreen(factory);
        }
        return ActionResult.SUCCESS;
    }
}