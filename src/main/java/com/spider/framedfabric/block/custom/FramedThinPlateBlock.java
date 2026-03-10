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

public class FramedThinPlateBlock extends AbstractFramedEntityBlock implements Waterloggable {

    public static final MapCodec<FramedThinPlateBlock> CODEC = createCodec(FramedThinPlateBlock::new);

    public static final EnumProperty<Direction> FACING = Properties.FACING;

    private static final VoxelShape SHAPE_DOWN  = Block.createCuboidShape(0, 0, 0, 16, 1, 16);
    private static final VoxelShape SHAPE_UP    = Block.createCuboidShape(0, 15, 0, 16, 16, 16);
    private static final VoxelShape SHAPE_NORTH = Block.createCuboidShape(0, 0, 0, 16, 16, 1);
    private static final VoxelShape SHAPE_SOUTH = Block.createCuboidShape(0, 0, 15, 16, 16, 16);
    private static final VoxelShape SHAPE_WEST  = Block.createCuboidShape(0, 0, 0, 1, 16, 16);
    private static final VoxelShape SHAPE_EAST  = Block.createCuboidShape(15, 0, 0, 16, 16, 16);

    public FramedThinPlateBlock(Settings settings) {
        super(settings);
        this.setDefaultState(
                this.getStateManager().getDefaultState()
                        .with(ROT, 1)
                        .with(HAS_CAMO, false)
                        .with(Properties.WATERLOGGED, false)
                        .with(FACING, Direction.DOWN)
        );
    }

    @Override
    protected void appendProperties(StateManager.Builder<Block, BlockState> builder) {
        super.appendProperties(builder);
        builder.add(HAS_CAMO, Properties.WATERLOGGED, FACING);
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

        // exactly like the posts: orient from the side you clicked
        Direction facing = ctx.getSide().getOpposite();

        return state.with(Properties.WATERLOGGED, waterlogged).with(FACING, facing);
    }

    private static VoxelShape shapeFor(BlockState state) {
        return switch (state.get(FACING)) {
            case DOWN  -> SHAPE_DOWN;
            case UP    -> SHAPE_UP;
            case NORTH -> SHAPE_NORTH;
            case SOUTH -> SHAPE_SOUTH;
            case WEST  -> SHAPE_WEST;
            case EAST  -> SHAPE_EAST;
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