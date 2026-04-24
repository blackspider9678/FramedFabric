package com.spider.framedfabric.block.custom;

import com.mojang.serialization.MapCodec;
import com.spider.framedfabric.blockentity.AbstractFramedEntityBlock;
import com.spider.framedfabric.blockentity.FramedBlockEntity;
import com.spider.framedfabric.blockentity.FramedUseHandler;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.util.RandomSource;
import net.minecraft.util.StringRepresentable;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.ScheduledTickAccess;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SimpleWaterloggedBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.level.material.Fluids;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;

import static com.spider.framedfabric.blockentity.FramedProperties.HAS_CAMO;

public class FramedCornerPostBlock extends AbstractFramedEntityBlock implements SimpleWaterloggedBlock {

    public static final MapCodec<FramedCornerPostBlock> CODEC = simpleCodec(FramedCornerPostBlock::new);

    /** false = normal, true = upside-down */
    public static final BooleanProperty UPSIDE_DOWN = BooleanProperty.create("upside_down");

    public enum Corner implements StringRepresentable {
        NW("nw"),
        NE("ne"),
        SW("sw"),
        SE("se");

        private final String id;
        Corner(String id) { this.id = id; }
        @Override public String getSerializedName() { return id; }
    }

    public static final EnumProperty<Corner> CORNER = EnumProperty.create("corner", Corner.class);

    // This matches your model: from [8,0,8] to [16,16,16]
    private static final VoxelShape SHAPE_SE = Block.box(8, 0, 8, 16, 16, 16);
    private static final VoxelShape SHAPE_SW = Block.box(0, 0, 8, 8, 16, 16);
    private static final VoxelShape SHAPE_NE = Block.box(8, 0, 0, 16, 16, 8);
    private static final VoxelShape SHAPE_NW = Block.box(0, 0, 0, 8, 16, 8);

    public FramedCornerPostBlock(Properties settings) {
        super(settings);
        this.registerDefaultState(
                this.getStateDefinition().any()
                        .setValue(ROT, 1) // leave your framed ROT system intact
                        .setValue(HAS_CAMO, false)
                        .setValue(BlockStateProperties.WATERLOGGED, false)
                        .setValue(UPSIDE_DOWN, false)
                        .setValue(CORNER, Corner.SE)
        );
    }

    @Override
    public BlockState getStateForPlacement(BlockPlaceContext ctx) {
        BlockState state = super.getStateForPlacement(ctx);
        if (state == null) state = this.defaultBlockState();

        BlockPos pos = ctx.getClickedPos();
        var hit = ctx.getClickLocation();

        // local coordinates inside the block [0..1)
        double lx = hit.x - pos.getX();
        double ly = hit.y - pos.getY();
        double lz = hit.z - pos.getZ();

        boolean waterlogged = ctx.getLevel().getFluidState(pos).getType() == Fluids.WATER;

        // default flip rule: placing on underside flips
        boolean upsideDown = ctx.getClickedFace() == Direction.DOWN;

        // Smart corner pick: depends on which face you clicked
        Direction side = ctx.getClickedFace();
        Corner corner;

        switch (side) {
            case UP, DOWN -> {
                // choose corner by X/Z
                corner = pickCornerXZ(lx, lz);
            }
            case NORTH -> {
                // looking at the north face: use X for left/right, Y for up/down
                corner = pickCornerXZ(lx, 0.0); // z doesn't matter; we're selecting NW/NE based on x
                // optional: aim high to flip
                upsideDown = (ly >= 0.5);
            }
            case SOUTH -> {
                // south face: use X for left/right, Y for up/down
                corner = pickCornerXZ(lx, 1.0); // select SW/SE based on x, force "south"
                upsideDown = (ly >= 0.5);
            }
            case WEST -> {
                // west face: use Z for left/right along the face, Y for up/down
                corner = pickCornerXZ(0.0, lz); // select NW/SW based on z, force "west"
                upsideDown = (ly >= 0.5);
            }
            case EAST -> {
                // east face: use Z for left/right, Y for up/down
                corner = pickCornerXZ(1.0, lz); // select NE/SE based on z, force "east"
                upsideDown = (ly >= 0.5);
            }
            default -> corner = Corner.SE;
        }

        return state
                .setValue(BlockStateProperties.WATERLOGGED, waterlogged)
                .setValue(UPSIDE_DOWN, upsideDown)
                .setValue(CORNER, corner);
    }

    /**
     * Choose corner from local X/Z:
     * - X < 0.5 => west, X >= 0.5 => east
     * - Z < 0.5 => north, Z >= 0.5 => south
     */
    private static Corner pickCornerXZ(double localX, double localZ) {
        boolean east = localX >= 0.5;
        boolean south = localZ >= 0.5;

        if (!east && !south) return Corner.NW;
        if ( east && !south) return Corner.NE;
        if (!east &&  south) return Corner.SW;
        return Corner.SE;
    }

    private static Corner pickCorner(double localX, double localZ) {
        boolean east = localX >= 0.5;
        boolean south = localZ >= 0.5;

        if (!east && !south) return Corner.NW;
        if ( east && !south) return Corner.NE;
        if (!east &&  south) return Corner.SW;
        return Corner.SE;
    }

    private static VoxelShape shapeFor(BlockState state) {
        return switch (state.getValue(CORNER)) {
            case NW -> SHAPE_NW;
            case NE -> SHAPE_NE;
            case SW -> SHAPE_SW;
            case SE -> SHAPE_SE;
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

        return super.updateShape(
                state, world, tickView, pos, direction, neighborPos, neighborState, random
        );
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        super.createBlockStateDefinition(builder); // adds ROT
        builder.add(HAS_CAMO, BlockStateProperties.WATERLOGGED, UPSIDE_DOWN, CORNER);
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