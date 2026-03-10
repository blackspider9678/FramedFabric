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
import net.minecraft.util.StringIdentifiable;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.random.Random;
import net.minecraft.util.shape.VoxelShape;
import net.minecraft.util.shape.VoxelShapes;
import net.minecraft.world.BlockView;
import net.minecraft.world.World;
import net.minecraft.world.WorldView;
import net.minecraft.world.tick.ScheduledTickView;

import static com.spider.framedfabric.blockentity.FramedProperties.HAS_CAMO;

public class FramedCornerStepBlock extends AbstractFramedEntityBlock implements Waterloggable {

    public static final MapCodec<FramedCornerStepBlock> CODEC = createCodec(FramedCornerStepBlock::new);

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
    public static final EnumProperty<BlockHalf> HALF = Properties.BLOCK_HALF;

    // Base (as-authored) is SE corner. (Matches your model pieces)
    // A: [8..16, 0..8,  0..8]
    // B: [8..16, 0..8,  8..16]
    // C: [0..8,  0..8,  8..16]
    // D: [8..16, 8..16, 8..16]
    private static final VoxelShape BASE_BOTTOM =
            VoxelShapes.union(
                    Block.createCuboidShape(8, 0, 0, 16, 8, 8),
                    Block.createCuboidShape(8, 0, 8, 16, 8, 16),
                    Block.createCuboidShape(0, 0, 8, 8, 8, 16),
                    Block.createCuboidShape(8, 8, 8, 16, 16, 16)
            );

    private static final VoxelShape BASE_TOP =
            VoxelShapes.union(
                    Block.createCuboidShape(8, 8, 0, 16, 16, 8),
                    Block.createCuboidShape(8, 8, 8, 16, 16, 16),
                    Block.createCuboidShape(0, 8, 8, 8, 16, 16),
                    Block.createCuboidShape(8, 0, 8, 16, 8, 16)
            );

    public FramedCornerStepBlock(Settings settings) {
        super(settings);
        this.setDefaultState(
                this.getStateManager().getDefaultState()
                        .with(ROT, 1)
                        .with(HAS_CAMO, false)
                        .with(Properties.WATERLOGGED, false)
                        .with(CORNER, Corner.SE)
                        .with(HALF, BlockHalf.BOTTOM)
        );
    }

    @Override
    public BlockState getPlacementState(ItemPlacementContext ctx) {
        BlockState state = super.getPlacementState(ctx);
        if (state == null) state = this.getDefaultState();

        BlockPos pos = ctx.getBlockPos();
        var hit = ctx.getHitPos();

        double lx = hit.x - pos.getX();
        double ly = hit.y - pos.getY();
        double lz = hit.z - pos.getZ();

        boolean waterlogged = ctx.getWorld().getFluidState(pos).getFluid() == Fluids.WATER;

        Direction side = ctx.getSide();

        // “Like corner post”: aim upper half => TOP, lower half => BOTTOM
        // (and still respect UP/DOWN face if you prefer that behavior)
        BlockHalf half;
        if (side == Direction.DOWN) half = BlockHalf.TOP;
        else if (side == Direction.UP) half = BlockHalf.BOTTOM;
        else half = (ly >= 0.5) ? BlockHalf.TOP : BlockHalf.BOTTOM;

        // “Like corner post”: pick the corner you are looking at on that face
        Corner corner = pickCornerSmart(side, lx, lz);

        return state
                .with(Properties.WATERLOGGED, waterlogged)
                .with(HALF, half)
                .with(CORNER, corner);
    }

    /**
     * Corner selection that behaves well on any clicked face:
     * - top/bottom: use X/Z
     * - side faces: force the axis that is constant and still choose the intended corner
     */
    private static Corner pickCornerSmart(Direction side, double lx, double lz) {
        // When placing on a side face, lx or lz will be ~0 or ~1 (on the face).
        // DO NOT force it to the opposite edge; that causes mirroring.
        double cx = MathHelper.clamp(lx, 0.0, 1.0);
        double cz = MathHelper.clamp(lz, 0.0, 1.0);
        return pickCornerXZ(cx, cz);
    }

    private static Corner pickCornerXZ(double lx, double lz) {
        boolean east = lx >= 0.5;
        boolean south = lz >= 0.5;

        if (!east && !south) return Corner.NW;
        if ( east && !south) return Corner.NE;
        if (!east &&  south) return Corner.SW;
        return Corner.SE;
    }

    private static VoxelShape rotateCorner(VoxelShape shape, Corner corner) {
        // Base is SE. We rotate around Y to map to other corners.
        return switch (corner) {
            case SE -> shape;                    // y=0
            case SW -> rotateY90(shape);         // y=90
            case NW -> rotateY180(shape);        // y=180
            case NE -> rotateY270(shape);        // y=270
        };
    }

    private static VoxelShape rotateY90(VoxelShape s) {
        // (x,z) -> (16 - z, x)
        return VoxelShapes.combineAndSimplify(VoxelShapes.empty(), s, (a, b) -> b)
                .getBoundingBoxes()
                .stream()
                .map(box -> Block.createCuboidShape(
                        16 - box.maxZ * 16, box.minY * 16, box.minX * 16,
                        16 - box.minZ * 16, box.maxY * 16, box.maxX * 16
                ))
                .reduce(VoxelShapes.empty(), VoxelShapes::union);
    }
    private static VoxelShape rotateY180(VoxelShape s) { return rotateY90(rotateY90(s)); }
    private static VoxelShape rotateY270(VoxelShape s) { return rotateY90(rotateY180(s)); }

    private static VoxelShape shapeFor(BlockState state) {
        VoxelShape base = (state.get(HALF) == BlockHalf.TOP) ? BASE_TOP : BASE_BOTTOM;
        return rotateCorner(base, state.get(CORNER));
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
        builder.add(HAS_CAMO, Properties.WATERLOGGED, CORNER, HALF);
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