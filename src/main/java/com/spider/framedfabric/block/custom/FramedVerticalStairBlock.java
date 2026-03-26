package com.spider.framedfabric.block.custom;

import com.mojang.serialization.MapCodec;
import com.spider.framedfabric.blockentity.AbstractFramedEntityBlock;
import com.spider.framedfabric.blockentity.FramedBlockEntity;
import com.spider.framedfabric.blockentity.FramedUseHandler;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SimpleWaterloggedBlock;
import net.minecraft.world.level.block.state.BlockBehaviour.Properties;
import net.minecraft.world.level.block.state.BlockState;
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
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.ScheduledTickAccess;

import static com.spider.framedfabric.blockentity.FramedProperties.HAS_CAMO;

public class FramedVerticalStairBlock extends AbstractFramedEntityBlock implements SimpleWaterloggedBlock {

    public static final MapCodec<FramedVerticalStairBlock> CODEC = simpleCodec(FramedVerticalStairBlock::new);

    public static final EnumProperty<Direction> FACING = BlockStateProperties.HORIZONTAL_FACING;

    // Matches your model JSON:
    // 1) [8..16, 0..16, 0..16]
    // 2) [0..8,  0..16, 8..16]
    private static final VoxelShape BASE_NORTH =
            Shapes.or(
                    Block.box(8, 0, 0, 16, 16, 16),
                    Block.box(0, 0, 8, 8, 16, 16)
            );

    public FramedVerticalStairBlock(Properties settings) {
        super(settings);
        this.registerDefaultState(
                this.getStateDefinition().any()
                        .setValue(ROT, 1)
                        .setValue(HAS_CAMO, false)
                        .setValue(BlockStateProperties.WATERLOGGED, false)
                        .setValue(FACING, Direction.NORTH)
        );
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        super.createBlockStateDefinition(builder);
        builder.add(HAS_CAMO, BlockStateProperties.WATERLOGGED, FACING);
    }

    @Override
    protected MapCodec<? extends AbstractFramedEntityBlock> codec() {
        return CODEC;
    }

    // Add inside FramedVerticalStairBlock (near properties)

    private enum Corner { NW, NE, SW, SE; }

    @Override
    public BlockState getStateForPlacement(BlockPlaceContext ctx) {
        BlockState state = super.getStateForPlacement(ctx);
        if (state == null) state = this.defaultBlockState();

        BlockPos pos = ctx.getClickedPos();
        var hit = ctx.getClickLocation();

        double lx = hit.x - pos.getX(); // 0..1
        double lz = hit.z - pos.getZ(); // 0..1

        boolean waterlogged = ctx.getLevel().getFluidState(pos).getType() == Fluids.WATER;
        Direction side = ctx.getClickedFace();

        Direction facing = facingFromCornerSmart(side, lx, lz);

        return state
                .setValue(BlockStateProperties.WATERLOGGED, waterlogged)
                .setValue(FACING, facing);
    }

    /**
     * Corner-post style corner picking:
     * - UP/DOWN: use X/Z quadrant
     * - Side faces: force the constant axis so left/right on that face decides the corner
     */
    private static Direction facingFromCornerSmart(Direction side, double lx, double lz) {
        return switch (side) {
            case UP, DOWN -> facingFromCorner(lx, lz);

            case NORTH -> facingFromCorner(lx, 1.0); // NW/NE
            case SOUTH -> facingFromCorner(lx, 0.0); // SW/SE
            case WEST  -> facingFromCorner(1.0, lz); // NW/SW
            case EAST  -> facingFromCorner(0.0, lz); // NE/SE

            default -> Direction.NORTH;
        };
    }

    private static Direction facingFromCorner(double lx, double lz) {
        boolean east = lx >= 0.5;
        boolean south = lz >= 0.5;

        if (!east && !south) return Direction.SOUTH;
        if ( east && !south) return Direction.WEST;
        if (!east &&  south) return Direction.EAST;
        return Direction.NORTH;
    }

    // --- Waterlogging ---

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

    // --- Shapes ---

    private static VoxelShape shapeFor(BlockState state) {
        return rotateY(BASE_NORTH, state.getValue(FACING)).optimize();
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

    private static VoxelShape rotateY(VoxelShape shape, Direction facing) {
        return switch (facing) {
            case NORTH -> shape;
            case EAST  -> rotateY90(shape);
            case SOUTH -> rotateY180(shape);
            case WEST  -> rotateY270(shape);
            default -> shape;
        };
    }

    private static VoxelShape rotateY90(VoxelShape shape) {
        return transform(shape, (minX, minY, minZ, maxX, maxY, maxZ) ->
                new double[]{1 - maxZ, minY, minX, 1 - minZ, maxY, maxX});
    }

    private static VoxelShape rotateY180(VoxelShape shape) {
        return transform(shape, (minX, minY, minZ, maxX, maxY, maxZ) ->
                new double[]{1 - maxX, minY, 1 - maxZ, 1 - minX, maxY, 1 - minZ});
    }

    private static VoxelShape rotateY270(VoxelShape shape) {
        return transform(shape, (minX, minY, minZ, maxX, maxY, maxZ) ->
                new double[]{minZ, minY, 1 - maxX, maxZ, maxY, 1 - minX});
    }

    @FunctionalInterface
    private interface BoxOp {
        double[] apply(double minX, double minY, double minZ, double maxX, double maxY, double maxZ);
    }

    private static VoxelShape transform(VoxelShape shape, BoxOp op) {
        final VoxelShape[] out = new VoxelShape[]{Shapes.empty()};
        shape.forAllBoxes((minX, minY, minZ, maxX, maxY, maxZ) -> {
            double[] b = op.apply(minX, minY, minZ, maxX, maxY, maxZ);
            out[0] = Shapes.or(out[0], Shapes.box(b[0], b[1], b[2], b[3], b[4], b[5]));
        });
        return out[0];
    }

    // --- Use handler ---

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level world, BlockPos pos, Player player, BlockHitResult hit) {
        FramedBlockEntity be = (world.getBlockEntity(pos) instanceof FramedBlockEntity fbe) ? fbe : null;
        return FramedUseHandler.handleUse(state, world, pos, player, hit, be);
    }
}