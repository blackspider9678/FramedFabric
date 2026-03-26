package com.spider.framedfabric.block;

import com.mojang.serialization.MapCodec;
import com.spider.framedfabric.screen.WoodWorkbenchScreenHandler;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockBehaviour.Properties;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.inventory.ContainerLevelAccess;
import net.minecraft.world.SimpleMenuProvider;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;

public class WoodWorkbenchBlock extends Block {
    public static final MapCodec<WoodWorkbenchBlock> CODEC = simpleCodec(WoodWorkbenchBlock::new);

    public WoodWorkbenchBlock(Properties settings) {
        super(settings);
    }

    @Override
    protected MapCodec<? extends Block> codec() {
        return CODEC;
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level world, BlockPos pos,
                                               Player player, BlockHitResult hit) {
        if (!world.isClientSide()) {
            MenuProvider factory = new SimpleMenuProvider(
                    (syncId, playerInventory, playerEntity) ->
                            new WoodWorkbenchScreenHandler(syncId, playerInventory, ContainerLevelAccess.create(world, pos)),
                    Component.literal("Wood Workbench")
            );
            player.openMenu(factory);
        }
        return InteractionResult.SUCCESS;
    }
}