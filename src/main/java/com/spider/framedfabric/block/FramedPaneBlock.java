package com.spider.framedfabric.block;

import com.mojang.serialization.MapCodec;
import com.spider.framedfabric.blockentity.FramedBlockEntity;
import com.spider.framedfabric.blockentity.FramedUseHandler;
import com.spider.framedfabric.registry.ModBlockEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.IronBarsBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.phys.BlockHitResult;
import org.jetbrains.annotations.Nullable;

import static com.spider.framedfabric.blockentity.FramedProperties.HAS_CAMO;

public class FramedPaneBlock extends IronBarsBlock implements net.minecraft.world.level.block.EntityBlock {

    public static final MapCodec<FramedPaneBlock> CODEC = simpleCodec(FramedPaneBlock::new);

    public FramedPaneBlock(Properties settings) {
        super(settings);
        registerDefaultState(defaultBlockState().setValue(HAS_CAMO, false));
    }

    @Override
    public MapCodec<? extends IronBarsBlock> codec() {
        return CODEC;
    }

    @Override
    public @Nullable BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new FramedBlockEntity(ModBlockEntities.FRAMED, pos, state);
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<net.minecraft.world.level.block.Block, BlockState> builder) {
        super.createBlockStateDefinition(builder);
        builder.add(HAS_CAMO);
    }

    @Override
    public InteractionResult useWithoutItem(BlockState state, Level world, BlockPos pos, Player player, BlockHitResult hit) {
        if (!(world.getBlockEntity(pos) instanceof FramedBlockEntity be)) return InteractionResult.PASS;
        return FramedUseHandler.handleUse(state, world, pos, player, hit, be);
    }
}
