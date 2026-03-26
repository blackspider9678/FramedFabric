package com.spider.framedfabric.block.custom;

import com.mojang.serialization.MapCodec;
import com.spider.framedfabric.blockentity.AbstractFramedEntityBlock;
import com.spider.framedfabric.blockentity.FramedBlockEntity;
import com.spider.framedfabric.blockentity.FramedUseHandler;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SimpleWaterloggedBlock;
import net.minecraft.world.level.block.state.BlockBehaviour.Properties;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.Half;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.level.material.Fluids;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.util.RandomSource;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.ScheduledTickAccess;

import static com.spider.framedfabric.blockentity.FramedProperties.HAS_CAMO;

public class FramedHalfSlabBlock extends AbstractFramedEntityBlock implements SimpleWaterloggedBlock {

    public static final MapCodec<FramedHalfSlabBlock> CODEC = simpleCodec(FramedHalfSlabBlock::new);

    public static final EnumProperty<Direction> FACING = BlockStateProperties.HORIZONTAL_FACING;
    public static final EnumProperty<Half> HALF = BlockStateProperties.HALF;

    // Base volume is SOUTH + BOTTOM: [0..16, 0..8, 8..16]
    private static final VoxelShape SOUTH_BOTTOM = Block.box(0, 0, 8, 16, 8, 16);
    private static final VoxelShape NORTH_BOTTOM = Block.box(0, 0, 0, 16, 8, 8);
    private static final VoxelShape EAST_BOTTOM  = Block.box(8, 0, 0, 16, 8, 16);
    private static final VoxelShape WEST_BOTTOM  = Block.box(0, 0, 0, 8, 8, 16);

    private static final VoxelShape SOUTH_TOP = Block.box(0, 8, 8, 16, 16, 16);
    private static final VoxelShape NORTH_TOP = Block.box(0, 8, 0, 16, 16, 8);
    private static final VoxelShape EAST_TOP  = Block.box(8, 8, 0, 16, 16, 16);
    private static final VoxelShape WEST_TOP  = Block.box(0, 8, 0, 8, 16, 16);

    public FramedHalfSlabBlock(Properties settings) {
        super(settings);
        this.registerDefaultState(
                this.getStateDefinition().any()
                        .setValue(ROT, 1)
                        .setValue(HAS_CAMO, false)
                        .setValue(BlockStateProperties.WATERLOGGED, false)
                        .setValue(FACING, Direction.SOUTH)
                        .setValue(HALF, Half.BOTTOM)
        );
    }

    @Override
    public BlockState getStateForPlacement(BlockPlaceContext ctx) {
        BlockState state = super.getStateForPlacement(ctx);
        if (state == null) state = this.defaultBlockState();

        BlockPos pos = ctx.getClickedPos();
        var hit = ctx.getClickLocation();

        double lx = hit.x - pos.getX(); // 0..1
        double ly = hit.y - pos.getY(); // 0..1
        double lz = hit.z - pos.getZ(); // 0..1

        boolean waterlogged = ctx.getLevel().getFluidState(pos).getType() == Fluids.WATER;

        // TOP/BOTTOM selection:
        // - click underside => TOP (since you're placing "against" the underside)
        // - click top face => BOTTOM
        // - click side face => decide by aiming high/low
        Half half;
        Direction side = ctx.getClickedFace();

        if (side == Direction.DOWN) half = Half.TOP;
        else if (side == Direction.UP) half = Half.BOTTOM;
        else half = (ly >= 0.5) ? Half.TOP : Half.BOTTOM;

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
                .setValue(BlockStateProperties.WATERLOGGED, waterlogged)
                .setValue(HALF, half)
                .setValue(FACING, facing);
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
        boolean top = state.getValue(HALF) == Half.TOP;
        return switch (state.getValue(FACING)) {
            case NORTH -> top ? NORTH_TOP : NORTH_BOTTOM;
            case SOUTH -> top ? SOUTH_TOP : SOUTH_BOTTOM;
            case EAST  -> top ? EAST_TOP  : EAST_BOTTOM;
            case WEST  -> top ? WEST_TOP  : WEST_BOTTOM;
            default -> top ? SOUTH_TOP : SOUTH_BOTTOM;
        };
    }

    @Override
    public VoxelShape getShape(BlockState state, BlockGetter world, BlockPos pos, CollisionContext context) {
        return shapeFor(state);
    }

    @Override
    public VoxelShape getCollisionShape(BlockState state, BlockGetter world, BlockPos pos, CollisionContext context) {
        return shapeFor(state);
    }

    @Override
    public VoxelShape getInteractionShape(BlockState state, BlockGetter world, BlockPos pos) {
        return shapeFor(state);
    }

    @Override
    public FluidState getFluidState(BlockState state) {
        return state.getValue(BlockStateProperties.WATERLOGGED)
                ? Fluids.WATER.getSource(false)
                : super.getFluidState(state);
    }

    @Override
    protected BlockState updateShape(
            BlockState state,
            LevelReader world,
            ScheduledTickAccess tickView,
            BlockPos pos,
            Direction direction,
            BlockPos neighborPos,
            BlockState neighborState,
            RandomSource random
    ) {
        if (state.getValue(BlockStateProperties.WATERLOGGED)) {
            tickView.scheduleTick(pos, Fluids.WATER, Fluids.WATER.getTickDelay(world));
        }
        return super.updateShape(state, world, tickView, pos, direction, neighborPos, neighborState, random);
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        super.createBlockStateDefinition(builder);
        builder.add(HAS_CAMO, BlockStateProperties.WATERLOGGED, FACING, HALF);
    }

    @Override
    protected MapCodec<? extends AbstractFramedEntityBlock> codec() {
        return CODEC;
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level world, BlockPos pos, Player player, BlockHitResult hit) {
        FramedBlockEntity be = (world.getBlockEntity(pos) instanceof FramedBlockEntity fbe) ? fbe : null;
        return FramedUseHandler.handleUse(state, world, pos, player, hit, be);
    }
}