package com.spider.framedfabric.block.custom;

import com.mojang.serialization.MapCodec;
import com.spider.framedfabric.blockentity.AbstractFramedEntityBlock;
import com.spider.framedfabric.blockentity.FramedBlockEntity;
import com.spider.framedfabric.blockentity.FramedUseHandler;
import net.minecraft.block.*;
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
import net.minecraft.util.shape.VoxelShapes;
import net.minecraft.world.BlockView;
import net.minecraft.world.World;
import net.minecraft.world.WorldView;
import net.minecraft.world.tick.ScheduledTickView;

import static com.spider.framedfabric.blockentity.FramedProperties.HAS_CAMO;

public class FramedVerticalStairBlock extends AbstractFramedEntityBlock implements Waterloggable {

    public static final MapCodec<FramedVerticalStairBlock> CODEC = createCodec(FramedVerticalStairBlock::new);

    public static final EnumProperty<Direction> FACING = Properties.HORIZONTAL_FACING;

    // Matches your model JSON:
    // 1) [8..16, 0..16, 0..16]
    // 2) [0..8,  0..16, 8..16]
    private static final VoxelShape BASE_NORTH =
            VoxelShapes.union(
                    Block.createCuboidShape(8, 0, 0, 16, 16, 16),
                    Block.createCuboidShape(0, 0, 8, 8, 16, 16)
            );

    public FramedVerticalStairBlock(Settings settings) {
        super(settings);
        this.setDefaultState(
                this.getStateManager().getDefaultState()
                        .with(ROT, 1)
                        .with(HAS_CAMO, false)
                        .with(Properties.WATERLOGGED, false)
                        .with(FACING, Direction.NORTH)
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

    // Add inside FramedVerticalStairBlock (near properties)

    private enum Corner { NW, NE, SW, SE; }

    @Override
    public BlockState getPlacementState(ItemPlacementContext ctx) {
        BlockState state = super.getPlacementState(ctx);
        if (state == null) state = this.getDefaultState();

        BlockPos pos = ctx.getBlockPos();
        var hit = ctx.getHitPos();

        double lx = hit.x - pos.getX(); // 0..1
        double lz = hit.z - pos.getZ(); // 0..1

        boolean waterlogged = ctx.getWorld().getFluidState(pos).getFluid() == Fluids.WATER;
        Direction side = ctx.getSide();

        Direction facing = facingFromCornerSmart(side, lx, lz);

        return state
                .with(Properties.WATERLOGGED, waterlogged)
                .with(FACING, facing);
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

    // --- Shapes ---

    private static VoxelShape shapeFor(BlockState state) {
        return rotateY(BASE_NORTH, state.get(FACING)).simplify();
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
        final VoxelShape[] out = new VoxelShape[]{VoxelShapes.empty()};
        shape.forEachBox((minX, minY, minZ, maxX, maxY, maxZ) -> {
            double[] b = op.apply(minX, minY, minZ, maxX, maxY, maxZ);
            out[0] = VoxelShapes.union(out[0], VoxelShapes.cuboid(b[0], b[1], b[2], b[3], b[4], b[5]));
        });
        return out[0];
    }

    // --- Use handler ---

    @Override
    protected ActionResult onUse(BlockState state, World world, BlockPos pos, PlayerEntity player, BlockHitResult hit) {
        FramedBlockEntity be = (world.getBlockEntity(pos) instanceof FramedBlockEntity fbe) ? fbe : null;
        return FramedUseHandler.handleUse(state, world, pos, player, hit, be);
    }
}