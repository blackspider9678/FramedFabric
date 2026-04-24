package com.spider.framedfabric.block;

import com.mojang.serialization.MapCodec;
import com.spider.framedfabric.blockentity.AbstractFramedEntityBlock;
import com.spider.framedfabric.blockentity.FramedBlockEntity;
import com.spider.framedfabric.registry.ModBlockEntities;
import net.minecraft.world.level.block.*;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Mirror;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.SimpleWaterloggedBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.level.block.state.properties.Half;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.level.material.Fluids;
import org.jetbrains.annotations.Nullable;

public class FramedSlopeBlock extends AbstractFramedEntityBlock implements SimpleWaterloggedBlock {

    // Don’t type this as DirectionProperty in your mappings; just keep it untyped.
    public static final EnumProperty<Direction> FACING = BlockStateProperties.HORIZONTAL_FACING;
    public static final EnumProperty<Half> HALF = BlockStateProperties.HALF;

    public FramedSlopeBlock(Properties settings) {
        super(settings);
        registerDefaultState(defaultBlockState()
                .setValue(FACING, Direction.NORTH)
                .setValue(HALF, Half.BOTTOM)
                .setValue(BlockStateProperties.WATERLOGGED, false)
        );
    }

    @Override
    protected MapCodec<? extends BaseEntityBlock> codec() {
        return null;
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        super.createBlockStateDefinition(builder);
        builder.add(FACING, HALF, BlockStateProperties.WATERLOGGED);
    }

    @Override
    public @Nullable BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        // Match your constructor: (BlockEntityType, pos, state)
        return new FramedBlockEntity(ModBlockEntities.FRAMED, pos, state);
    }

    @Override
    public @Nullable BlockState getStateForPlacement(BlockPlaceContext ctx) {
        Direction facing = ctx.getHorizontalDirection();

        double hitY = ctx.getClickLocation().y - ctx.getClickedPos().getY();
        Half half = (hitY > 0.5) ? Half.TOP : Half.BOTTOM;

        FluidState fs = ctx.getLevel().getFluidState(ctx.getClickedPos());
        boolean waterlogged = fs.getType() == Fluids.WATER;

        return defaultBlockState()
                .setValue(FACING, facing)
                .setValue(HALF, half)
                .setValue(BlockStateProperties.WATERLOGGED, waterlogged);
    }

    @Override
    public BlockState rotate(BlockState state, Rotation rotation) {
        return state.setValue(FACING, rotation.rotate(state.getValue(FACING)));
    }

    @Override
    public BlockState mirror(BlockState state, Mirror mirror) {
        return state.rotate(mirror.getRotation(state.getValue(FACING)));
    }

    @Override
    public FluidState getFluidState(BlockState state) {
        return state.getValue(BlockStateProperties.WATERLOGGED)
                ? Fluids.WATER.getSource(false)
                : super.getFluidState(state);
    }
}
