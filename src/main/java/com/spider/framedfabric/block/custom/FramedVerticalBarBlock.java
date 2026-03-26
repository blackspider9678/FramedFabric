package com.spider.framedfabric.block.custom;

import com.mojang.serialization.MapCodec;
import com.spider.framedfabric.block.enums.VerticalBarShape;
import com.spider.framedfabric.blockentity.AbstractFramedEntityBlock;
import com.spider.framedfabric.blockentity.FramedBlockEntity;
import com.spider.framedfabric.blockentity.FramedUseHandler;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockBehaviour.Properties;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.level.block.SimpleWaterloggedBlock;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.level.material.Fluids;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.InteractionResult;
import net.minecraft.util.StringRepresentable;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.util.RandomSource;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.ScheduledTickAccess;
import org.jetbrains.annotations.Nullable;

import static com.spider.framedfabric.blockentity.FramedProperties.HAS_CAMO;

public class FramedVerticalBarBlock extends AbstractFramedEntityBlock implements SimpleWaterloggedBlock {
    public static final MapCodec<FramedVerticalBarBlock> CODEC = simpleCodec(FramedVerticalBarBlock::new);

    public enum Corner implements StringRepresentable {
        NW("nw"),
        NE("ne"),
        SW("sw"),
        SE("se");

        private final String id;

        Corner(String id) {
            this.id = id;
        }

        @Override
        public String getSerializedName() {
            return id;
        }
    }

    public static final EnumProperty<Corner> CORNER = EnumProperty.create("corner", Corner.class);
    public static final EnumProperty<VerticalBarShape> SHAPE = EnumProperty.create("shape", VerticalBarShape.class);

    // thin selectable outline in each corner
    private static final VoxelShape SHAPE_NW = Block.box(0, 0, 0, 1, 16, 1);
    private static final VoxelShape SHAPE_NE = Block.box(15, 0, 0, 16, 16, 1);
    private static final VoxelShape SHAPE_SW = Block.box(0, 0, 15, 1, 16, 16);
    private static final VoxelShape SHAPE_SE = Block.box(15, 0, 15, 16, 16, 16);

    public FramedVerticalBarBlock(Properties settings) {
        super(settings);
        this.registerDefaultState(
                this.getStateDefinition().any()
                        .setValue(ROT, 1)
                        .setValue(HAS_CAMO, false)
                        .setValue(BlockStateProperties.WATERLOGGED, false)
                        .setValue(CORNER, Corner.SE)
                        .setValue(SHAPE, VerticalBarShape.SINGLE)
        );
    }

    @Override
    protected MapCodec<? extends AbstractFramedEntityBlock> codec() {
        return CODEC;
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        super.createBlockStateDefinition(builder);
        builder.add(HAS_CAMO, BlockStateProperties.WATERLOGGED, CORNER, SHAPE);
    }

    @Override
    public @Nullable BlockState getStateForPlacement(BlockPlaceContext ctx) {
        BlockPos pos = ctx.getClickedPos();
        var hit = ctx.getClickLocation();

        double lx = hit.x - pos.getX();
        double lz = hit.z - pos.getZ();

        boolean waterlogged = ctx.getLevel().getFluidState(pos).getType() == Fluids.WATER;

        Corner corner = pickCornerForFace(ctx.getClickedFace(), lx, lz);

        BlockState placed = this.defaultBlockState()
                .setValue(BlockStateProperties.WATERLOGGED, waterlogged)
                .setValue(CORNER, corner);

        return withComputedShape(placed, ctx.getLevel(), pos);
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

    private BlockState withComputedShape(BlockState state, LevelAccessor world, BlockPos pos) {
        Corner corner = state.getValue(CORNER);

        boolean up = connectsTo(world, pos.above(), corner);
        boolean down = connectsTo(world, pos.below(), corner);

        VerticalBarShape shape =
                (up && down) ? VerticalBarShape.MIDDLE :
                        up ? VerticalBarShape.BOTTOM :
                                down ? VerticalBarShape.TOP :
                                        VerticalBarShape.SINGLE;

        return state.setValue(SHAPE, shape);
    }

    private boolean connectsTo(LevelAccessor world, BlockPos pos, Corner corner) {
        BlockState other = world.getBlockState(pos);
        if (!other.is(this)) return false;
        return other.getValue(CORNER) == corner;
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

        if (direction == Direction.UP || direction == Direction.DOWN) {
            return withComputedShape(state, (LevelAccessor) world, pos);
        }

        return super.updateShape(
                state, world, tickView, pos, direction, neighborPos, neighborState, random
        );
    }

    private static VoxelShape outlineFor(BlockState state) {
        return switch (state.getValue(CORNER)) {
            case NW -> SHAPE_NW;
            case NE -> SHAPE_NE;
            case SW -> SHAPE_SW;
            case SE -> SHAPE_SE;
        };
    }

    @Override
    public VoxelShape getShape(BlockState state, BlockGetter world, BlockPos pos, CollisionContext context) {
        return outlineFor(state);
    }

    @Override
    public VoxelShape getCollisionShape(BlockState state, BlockGetter world, BlockPos pos, CollisionContext context) {
        return Shapes.empty();
    }

    @Override
    public VoxelShape getInteractionShape(BlockState state, BlockGetter world, BlockPos pos) {
        return outlineFor(state);
    }

    @Override
    public FluidState getFluidState(BlockState state) {
        return state.getValue(BlockStateProperties.WATERLOGGED)
                ? Fluids.WATER.getSource(false)
                : super.getFluidState(state);
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level world, BlockPos pos, Player player, BlockHitResult hit) {
        FramedBlockEntity be = (world.getBlockEntity(pos) instanceof FramedBlockEntity fbe) ? fbe : null;
        return FramedUseHandler.handleUse(state, world, pos, player, hit, be, 0);
    }
}