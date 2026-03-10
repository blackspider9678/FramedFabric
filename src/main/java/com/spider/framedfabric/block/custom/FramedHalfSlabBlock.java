package com.spider.framedfabric.block.custom;

import com.mojang.serialization.MapCodec;
import com.spider.framedfabric.blockentity.AbstractFramedEntityBlock;
import com.spider.framedfabric.blockentity.FramedBlockEntity;
import com.spider.framedfabric.blockentity.FramedUseHandler;
import net.minecraft.block.*;
import net.minecraft.block.enums.BlockHalf;
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

public class FramedHalfSlabBlock extends AbstractFramedEntityBlock implements Waterloggable {

    public static final MapCodec<FramedHalfSlabBlock> CODEC = createCodec(FramedHalfSlabBlock::new);

    public static final EnumProperty<Direction> FACING = Properties.HORIZONTAL_FACING;
    public static final EnumProperty<BlockHalf> HALF = Properties.BLOCK_HALF;

    // Base volume is SOUTH + BOTTOM: [0..16, 0..8, 8..16]
    private static final VoxelShape SOUTH_BOTTOM = Block.createCuboidShape(0, 0, 8, 16, 8, 16);
    private static final VoxelShape NORTH_BOTTOM = Block.createCuboidShape(0, 0, 0, 16, 8, 8);
    private static final VoxelShape EAST_BOTTOM  = Block.createCuboidShape(8, 0, 0, 16, 8, 16);
    private static final VoxelShape WEST_BOTTOM  = Block.createCuboidShape(0, 0, 0, 8, 8, 16);

    private static final VoxelShape SOUTH_TOP = Block.createCuboidShape(0, 8, 8, 16, 16, 16);
    private static final VoxelShape NORTH_TOP = Block.createCuboidShape(0, 8, 0, 16, 16, 8);
    private static final VoxelShape EAST_TOP  = Block.createCuboidShape(8, 8, 0, 16, 16, 16);
    private static final VoxelShape WEST_TOP  = Block.createCuboidShape(0, 8, 0, 8, 16, 16);

    public FramedHalfSlabBlock(Settings settings) {
        super(settings);
        this.setDefaultState(
                this.getStateManager().getDefaultState()
                        .with(ROT, 1)
                        .with(HAS_CAMO, false)
                        .with(Properties.WATERLOGGED, false)
                        .with(FACING, Direction.SOUTH)
                        .with(HALF, BlockHalf.BOTTOM)
        );
    }

    @Override
    public BlockState getPlacementState(ItemPlacementContext ctx) {
        BlockState state = super.getPlacementState(ctx);
        if (state == null) state = this.getDefaultState();

        BlockPos pos = ctx.getBlockPos();
        var hit = ctx.getHitPos();

        double lx = hit.x - pos.getX(); // 0..1
        double ly = hit.y - pos.getY(); // 0..1
        double lz = hit.z - pos.getZ(); // 0..1

        boolean waterlogged = ctx.getWorld().getFluidState(pos).getFluid() == Fluids.WATER;

        // TOP/BOTTOM selection:
        // - click underside => TOP (since you're placing "against" the underside)
        // - click top face => BOTTOM
        // - click side face => decide by aiming high/low
        BlockHalf half;
        Direction side = ctx.getSide();

        if (side == Direction.DOWN) half = BlockHalf.TOP;
        else if (side == Direction.UP) half = BlockHalf.BOTTOM;
        else half = (ly >= 0.5) ? BlockHalf.TOP : BlockHalf.BOTTOM;

        // FACING selection:
        // - if you click a side face, the panel goes on that side of the block space
        // - if you click top/bottom, choose the nearest side based on which axis you're closer to
        Direction facing;
        if (side.getAxis().isHorizontal()) {
            // IMPORTANT: placing against a block side -> the half should be on the side that touches the clicked block,
            // which is opposite of ctx.getSide() (because ctx.getSide() points from clicked block -> placement position).
            facing = side.getOpposite();
        } else {
            facing = pickNearestHorizontalSide(lx, lz);
        }

        return state
                .with(Properties.WATERLOGGED, waterlogged)
                .with(HALF, half)
                .with(FACING, facing);
    }

    private static Direction pickNearestHorizontalSide(double lx, double lz) {
        // Compare distance to the 4 sides (0 or 1 on each axis)
        double dNorth = lz;
        double dSouth = 1.0 - lz;
        double dWest  = lx;
        double dEast  = 1.0 - lx;

        double min = dNorth;
        Direction best = Direction.NORTH;

        if (dSouth < min) { min = dSouth; best = Direction.SOUTH; }
        if (dWest  < min) { min = dWest;  best = Direction.WEST;  }
        if (dEast  < min) { /*min = dEast;*/ best = Direction.EAST;  }

        return best;
    }

    private static VoxelShape shapeFor(BlockState state) {
        boolean top = state.get(HALF) == BlockHalf.TOP;
        return switch (state.get(FACING)) {
            case NORTH -> top ? NORTH_TOP : NORTH_BOTTOM;
            case SOUTH -> top ? SOUTH_TOP : SOUTH_BOTTOM;
            case EAST  -> top ? EAST_TOP  : EAST_BOTTOM;
            case WEST  -> top ? WEST_TOP  : WEST_BOTTOM;
            default -> top ? SOUTH_TOP : SOUTH_BOTTOM;
        };
    }

    @Override
    public VoxelShape getOutlineShape(BlockState state, BlockView world, BlockPos pos, ShapeContext context) {
        return shapeFor(state);
    }

    @Override
    public VoxelShape getCollisionShape(BlockState state, BlockView world, BlockPos pos, ShapeContext context) {
        return shapeFor(state);
    }

    @Override
    public VoxelShape getRaycastShape(BlockState state, BlockView world, BlockPos pos) {
        return shapeFor(state);
    }

    @Override
    public FluidState getFluidState(BlockState state) {
        return state.get(Properties.WATERLOGGED)
                ? Fluids.WATER.getStill(false)
                : super.getFluidState(state);
    }

    @Override
    protected BlockState getStateForNeighborUpdate(
            BlockState state,
            WorldView world,
            ScheduledTickView tickView,
            BlockPos pos,
            Direction direction,
            BlockPos neighborPos,
            BlockState neighborState,
            Random random
    ) {
        if (state.get(Properties.WATERLOGGED)) {
            tickView.scheduleFluidTick(pos, Fluids.WATER, Fluids.WATER.getTickRate(world));
        }
        return super.getStateForNeighborUpdate(state, world, tickView, pos, direction, neighborPos, neighborState, random);
    }

    @Override
    protected void appendProperties(StateManager.Builder<Block, BlockState> builder) {
        super.appendProperties(builder);
        builder.add(HAS_CAMO, Properties.WATERLOGGED, FACING, HALF);
    }

    @Override
    protected MapCodec<? extends AbstractFramedEntityBlock> getCodec() {
        return CODEC;
    }

    @Override
    protected ActionResult onUse(BlockState state, World world, BlockPos pos, PlayerEntity player, BlockHitResult hit) {
        FramedBlockEntity be = (world.getBlockEntity(pos) instanceof FramedBlockEntity fbe) ? fbe : null;
        return FramedUseHandler.handleUse(state, world, pos, player, hit, be);
    }
}