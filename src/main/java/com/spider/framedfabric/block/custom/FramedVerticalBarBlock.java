package com.spider.framedfabric.block.custom;

import com.mojang.serialization.MapCodec;
import com.spider.framedfabric.block.enums.VerticalBarShape;
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
import net.minecraft.util.StringIdentifiable;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.random.Random;
import net.minecraft.util.shape.VoxelShape;
import net.minecraft.util.shape.VoxelShapes;
import net.minecraft.world.BlockView;
import net.minecraft.world.World;
import net.minecraft.world.WorldAccess;
import net.minecraft.world.WorldView;
import net.minecraft.world.tick.ScheduledTickView;
import org.jetbrains.annotations.Nullable;

import static com.spider.framedfabric.blockentity.FramedProperties.HAS_CAMO;

public class FramedVerticalBarBlock extends AbstractFramedEntityBlock implements Waterloggable {
    public static final MapCodec<FramedVerticalBarBlock> CODEC = createCodec(FramedVerticalBarBlock::new);

    public enum Corner implements StringIdentifiable {
        NW("nw"),
        NE("ne"),
        SW("sw"),
        SE("se");

        private final String id;

        Corner(String id) {
            this.id = id;
        }

        @Override
        public String asString() {
            return id;
        }
    }

    public static final EnumProperty<Corner> CORNER = EnumProperty.of("corner", Corner.class);
    public static final EnumProperty<VerticalBarShape> SHAPE = EnumProperty.of("shape", VerticalBarShape.class);

    // thin selectable outline in each corner
    private static final VoxelShape SHAPE_NW = Block.createCuboidShape(0, 0, 0, 1, 16, 1);
    private static final VoxelShape SHAPE_NE = Block.createCuboidShape(15, 0, 0, 16, 16, 1);
    private static final VoxelShape SHAPE_SW = Block.createCuboidShape(0, 0, 15, 1, 16, 16);
    private static final VoxelShape SHAPE_SE = Block.createCuboidShape(15, 0, 15, 16, 16, 16);

    public FramedVerticalBarBlock(Settings settings) {
        super(settings);
        this.setDefaultState(
                this.getStateManager().getDefaultState()
                        .with(ROT, 1)
                        .with(HAS_CAMO, false)
                        .with(Properties.WATERLOGGED, false)
                        .with(CORNER, Corner.SE)
                        .with(SHAPE, VerticalBarShape.SINGLE)
        );
    }

    @Override
    protected MapCodec<? extends AbstractFramedEntityBlock> getCodec() {
        return CODEC;
    }

    @Override
    protected void appendProperties(StateManager.Builder<Block, BlockState> builder) {
        super.appendProperties(builder);
        builder.add(HAS_CAMO, Properties.WATERLOGGED, CORNER, SHAPE);
    }

    @Override
    public @Nullable BlockState getPlacementState(ItemPlacementContext ctx) {
        BlockPos pos = ctx.getBlockPos();
        var hit = ctx.getHitPos();

        double lx = hit.x - pos.getX();
        double lz = hit.z - pos.getZ();

        boolean waterlogged = ctx.getWorld().getFluidState(pos).getFluid() == Fluids.WATER;

        Corner corner = pickCornerForFace(ctx.getSide(), lx, lz);

        BlockState placed = this.getDefaultState()
                .with(Properties.WATERLOGGED, waterlogged)
                .with(CORNER, corner);

        return withComputedShape(placed, ctx.getWorld(), pos);
    }

    private static Corner pickCornerForFace(Direction side, double localX, double localZ) {
        return switch (side) {
            // clicked north face of existing block -> new block is north of it,
            // so bar should hug SOUTH side of the new block
            case NORTH -> (localX < 0.5) ? Corner.SW : Corner.SE;

            // clicked south face -> new block is south of it,
            // so bar should hug NORTH side of the new block
            case SOUTH -> (localX < 0.5) ? Corner.NW : Corner.NE;

            // clicked west face -> new block is west of it,
            // so bar should hug EAST side of the new block
            case WEST  -> (localZ < 0.5) ? Corner.NE : Corner.SE;

            // clicked east face -> new block is east of it,
            // so bar should hug WEST side of the new block
            case EAST  -> (localZ < 0.5) ? Corner.NW : Corner.SW;

            // top/bottom placement still uses direct quadrant pick
            case UP, DOWN -> pickCornerXZ(localX, localZ);

            default -> Corner.SE;
        };
    }

    private static Corner pickCornerXZ(double localX, double localZ) {
        boolean east = localX >= 0.5;
        boolean south = localZ >= 0.5;

        if (!east && !south) return Corner.NW;
        if ( east && !south) return Corner.NE;
        if (!east &&  south) return Corner.SW;
        return Corner.SE;
    }

    private BlockState withComputedShape(BlockState state, WorldAccess world, BlockPos pos) {
        Corner corner = state.get(CORNER);

        boolean up = connectsTo(world, pos.up(), corner);
        boolean down = connectsTo(world, pos.down(), corner);

        VerticalBarShape shape =
                (up && down) ? VerticalBarShape.MIDDLE :
                        up ? VerticalBarShape.BOTTOM :
                                down ? VerticalBarShape.TOP :
                                        VerticalBarShape.SINGLE;

        return state.with(SHAPE, shape);
    }

    private boolean connectsTo(WorldAccess world, BlockPos pos, Corner corner) {
        BlockState other = world.getBlockState(pos);
        if (!other.isOf(this)) return false;
        return other.get(CORNER) == corner;
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

        if (direction == Direction.UP || direction == Direction.DOWN) {
            return withComputedShape(state, (WorldAccess) world, pos);
        }

        return super.getStateForNeighborUpdate(
                state, world, tickView, pos, direction, neighborPos, neighborState, random
        );
    }

    private static VoxelShape outlineFor(BlockState state) {
        return switch (state.get(CORNER)) {
            case NW -> SHAPE_NW;
            case NE -> SHAPE_NE;
            case SW -> SHAPE_SW;
            case SE -> SHAPE_SE;
        };
    }

    @Override
    public VoxelShape getOutlineShape(BlockState state, BlockView world, BlockPos pos, ShapeContext context) {
        return outlineFor(state);
    }

    @Override
    public VoxelShape getCollisionShape(BlockState state, BlockView world, BlockPos pos, ShapeContext context) {
        return VoxelShapes.empty();
    }

    @Override
    public VoxelShape getRaycastShape(BlockState state, BlockView world, BlockPos pos) {
        return outlineFor(state);
    }

    @Override
    public FluidState getFluidState(BlockState state) {
        return state.get(Properties.WATERLOGGED)
                ? Fluids.WATER.getStill(false)
                : super.getFluidState(state);
    }

    @Override
    protected ActionResult onUse(BlockState state, World world, BlockPos pos, PlayerEntity player, BlockHitResult hit) {
        FramedBlockEntity be = (world.getBlockEntity(pos) instanceof FramedBlockEntity fbe) ? fbe : null;
        return FramedUseHandler.handleUse(state, world, pos, player, hit, be, 0);
    }
}