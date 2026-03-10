package com.spider.framedfabric.block.custom;

import com.mojang.serialization.MapCodec;
import com.spider.framedfabric.blockentity.AbstractFramedEntityBlock;
import com.spider.framedfabric.blockentity.FramedBlockEntity;
import com.spider.framedfabric.blockentity.FramedUseHandler;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.ShapeContext;
import net.minecraft.block.Waterloggable;
import net.minecraft.fluid.FluidState;
import net.minecraft.fluid.Fluids;
import net.minecraft.item.ItemPlacementContext;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.state.StateManager;
import net.minecraft.state.property.BooleanProperty;
import net.minecraft.state.property.EnumProperty;
import net.minecraft.state.property.Properties;
import net.minecraft.util.ActionResult;
import net.minecraft.util.StringIdentifiable;
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

public class FramedCornerPostBlock extends AbstractFramedEntityBlock implements Waterloggable {

    public static final MapCodec<FramedCornerPostBlock> CODEC = createCodec(FramedCornerPostBlock::new);

    /** false = normal, true = upside-down */
    public static final BooleanProperty UPSIDE_DOWN = BooleanProperty.of("upside_down");

    public enum Corner implements StringIdentifiable {
        NW("nw"),
        NE("ne"),
        SW("sw"),
        SE("se");

        private final String id;
        Corner(String id) { this.id = id; }
        @Override public String asString() { return id; }
    }

    public static final EnumProperty<Corner> CORNER = EnumProperty.of("corner", Corner.class);

    // This matches your model: from [8,0,8] to [16,16,16]
    private static final VoxelShape SHAPE_SE = Block.createCuboidShape(8, 0, 8, 16, 16, 16);
    private static final VoxelShape SHAPE_SW = Block.createCuboidShape(0, 0, 8, 8, 16, 16);
    private static final VoxelShape SHAPE_NE = Block.createCuboidShape(8, 0, 0, 16, 16, 8);
    private static final VoxelShape SHAPE_NW = Block.createCuboidShape(0, 0, 0, 8, 16, 8);

    public FramedCornerPostBlock(Settings settings) {
        super(settings);
        this.setDefaultState(
                this.getStateManager().getDefaultState()
                        .with(ROT, 1) // leave your framed ROT system intact
                        .with(HAS_CAMO, false)
                        .with(Properties.WATERLOGGED, false)
                        .with(UPSIDE_DOWN, false)
                        .with(CORNER, Corner.SE)
        );
    }

    @Override
    public BlockState getPlacementState(ItemPlacementContext ctx) {
        BlockState state = super.getPlacementState(ctx);
        if (state == null) state = this.getDefaultState();

        BlockPos pos = ctx.getBlockPos();
        var hit = ctx.getHitPos();

        // local coordinates inside the block [0..1)
        double lx = hit.x - pos.getX();
        double ly = hit.y - pos.getY();
        double lz = hit.z - pos.getZ();

        boolean waterlogged = ctx.getWorld().getFluidState(pos).getFluid() == Fluids.WATER;

        // default flip rule: placing on underside flips
        boolean upsideDown = ctx.getSide() == Direction.DOWN;

        // Smart corner pick: depends on which face you clicked
        Direction side = ctx.getSide();
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
                .with(Properties.WATERLOGGED, waterlogged)
                .with(UPSIDE_DOWN, upsideDown)
                .with(CORNER, corner);
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
        return switch (state.get(CORNER)) {
            case NW -> SHAPE_NW;
            case NE -> SHAPE_NE;
            case SW -> SHAPE_SW;
            case SE -> SHAPE_SE;
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

        return super.getStateForNeighborUpdate(
                state, world, tickView, pos, direction, neighborPos, neighborState, random
        );
    }

    @Override
    protected void appendProperties(StateManager.Builder<Block, BlockState> builder) {
        super.appendProperties(builder); // adds ROT
        builder.add(HAS_CAMO, Properties.WATERLOGGED, UPSIDE_DOWN, CORNER);
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