package com.spider.framedfabric.block.custom;

import com.mojang.serialization.MapCodec;
import com.spider.framedfabric.blockentity.AbstractFramedEntityBlock;
import com.spider.framedfabric.blockentity.FramedBlockEntity;
import com.spider.framedfabric.blockentity.FramedUseHandler;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.ShapeContext;
import net.minecraft.block.Waterloggable;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.fluid.FluidState;
import net.minecraft.fluid.Fluids;
import net.minecraft.item.ItemPlacementContext;
import net.minecraft.state.StateManager;
import net.minecraft.state.property.EnumProperty;
import net.minecraft.state.property.Properties;
import net.minecraft.util.ActionResult;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.random.Random;
import net.minecraft.util.shape.VoxelShape;
import net.minecraft.world.BlockView;
import net.minecraft.world.World;
import net.minecraft.world.WorldView;
import net.minecraft.world.tick.ScheduledTickView;

import static com.spider.framedfabric.blockentity.FramedProperties.HAS_CAMO;

public class FramedMediumPostBlock extends AbstractFramedEntityBlock implements Waterloggable {

    public static final MapCodec<FramedMediumPostBlock> CODEC = createCodec(FramedMediumPostBlock::new);

    public static final EnumProperty<Direction.Axis> AXIS = Properties.AXIS;

    // 10x16x10 centered: [3..13]
    private static final VoxelShape SHAPE_Y = Block.createCuboidShape(3, 0, 3, 13, 16, 13);
    private static final VoxelShape SHAPE_X = Block.createCuboidShape(0, 3, 3, 16, 13, 13);
    private static final VoxelShape SHAPE_Z = Block.createCuboidShape(3, 3, 0, 13, 13, 16);

    public FramedMediumPostBlock(Settings settings) {
        super(settings);
        this.setDefaultState(
                this.getStateManager().getDefaultState()
                        .with(ROT, 1)
                        .with(HAS_CAMO, false)
                        .with(Properties.WATERLOGGED, false)
                        .with(AXIS, Direction.Axis.Y)
        );
    }

    @Override
    protected void appendProperties(StateManager.Builder<Block, BlockState> builder) {
        super.appendProperties(builder);
        builder.add(HAS_CAMO, Properties.WATERLOGGED, AXIS);
    }

    @Override
    protected MapCodec<? extends AbstractFramedEntityBlock> getCodec() {
        return CODEC;
    }

    @Override
    public BlockState getPlacementState(ItemPlacementContext ctx) {
        BlockState state = super.getPlacementState(ctx);
        if (state == null) state = this.getDefaultState();

        BlockPos pos = ctx.getBlockPos();
        boolean waterlogged = ctx.getWorld().getFluidState(pos).getFluid() == Fluids.WATER;
        Direction.Axis axis = ctx.getSide().getAxis();

        return state.with(Properties.WATERLOGGED, waterlogged).with(AXIS, axis);
    }

    private static VoxelShape shapeFor(BlockState state) {
        return switch (state.get(AXIS)) {
            case X -> SHAPE_X;
            case Z -> SHAPE_Z;
            case Y -> SHAPE_Y;
        };
    }

    @Override public VoxelShape getOutlineShape(BlockState s, BlockView w, BlockPos p, ShapeContext c) { return shapeFor(s); }
    @Override public VoxelShape getCollisionShape(BlockState s, BlockView w, BlockPos p, ShapeContext c) { return shapeFor(s); }
    @Override public VoxelShape getRaycastShape(BlockState s, BlockView w, BlockPos p) { return shapeFor(s); }

    @Override
    public FluidState getFluidState(BlockState state) {
        return state.get(Properties.WATERLOGGED) ? Fluids.WATER.getStill(false) : super.getFluidState(state);
    }

    @Override
    protected BlockState getStateForNeighborUpdate(
            BlockState state, WorldView world, ScheduledTickView tickView,
            BlockPos pos, Direction direction, BlockPos neighborPos, BlockState neighborState, Random random
    ) {
        if (state.get(Properties.WATERLOGGED)) {
            tickView.scheduleFluidTick(pos, Fluids.WATER, Fluids.WATER.getTickRate(world));
        }
        return super.getStateForNeighborUpdate(state, world, tickView, pos, direction, neighborPos, neighborState, random);
    }

    @Override
    protected ActionResult onUse(BlockState state, World world, BlockPos pos, PlayerEntity player, BlockHitResult hit) {
        FramedBlockEntity be = (world.getBlockEntity(pos) instanceof FramedBlockEntity fbe) ? fbe : null;
        return FramedUseHandler.handleUse(state, world, pos, player, hit, be);
    }
}