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
import net.minecraft.util.StringRepresentable;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.ScheduledTickAccess;

import static com.spider.framedfabric.blockentity.FramedProperties.HAS_CAMO;

public class FramedCornerStepBlock extends AbstractFramedEntityBlock implements SimpleWaterloggedBlock {

    public static final MapCodec<FramedCornerStepBlock> CODEC = simpleCodec(FramedCornerStepBlock::new);

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
    public static final EnumProperty<Half> HALF = BlockStateProperties.HALF;

    // Base (as-authored) is SE corner. (Matches your model pieces)
    // A: [8..16, 0..8,  0..8]
    // B: [8..16, 0..8,  8..16]
    // C: [0..8,  0..8,  8..16]
    // D: [8..16, 8..16, 8..16]
    private static final VoxelShape BASE_BOTTOM =
            Shapes.or(
                    Block.box(8, 0, 0, 16, 8, 8),
                    Block.box(8, 0, 8, 16, 8, 16),
                    Block.box(0, 0, 8, 8, 8, 16),
                    Block.box(8, 8, 8, 16, 16, 16)
            );

    private static final VoxelShape BASE_TOP =
            Shapes.or(
                    Block.box(8, 8, 0, 16, 16, 8),
                    Block.box(8, 8, 8, 16, 16, 16),
                    Block.box(0, 8, 8, 8, 16, 16),
                    Block.box(8, 0, 8, 16, 8, 16)
            );

    public FramedCornerStepBlock(Properties settings) {
        super(settings);
        this.registerDefaultState(
                this.getStateDefinition().any()
                        .setValue(ROT, 1)
                        .setValue(HAS_CAMO, false)
                        .setValue(BlockStateProperties.WATERLOGGED, false)
                        .setValue(CORNER, Corner.SE)
                        .setValue(HALF, Half.BOTTOM)
        );
    }

    @Override
    public BlockState getStateForPlacement(BlockPlaceContext ctx) {
        BlockState state = super.getStateForPlacement(ctx);
        if (state == null) state = this.defaultBlockState();

        BlockPos pos = ctx.getClickedPos();
        var hit = ctx.getClickLocation();

        double lx = hit.x - pos.getX();
        double ly = hit.y - pos.getY();
        double lz = hit.z - pos.getZ();

        boolean waterlogged = ctx.getLevel().getFluidState(pos).getType() == Fluids.WATER;

        Direction side = ctx.getClickedFace();

        // “Like corner post”: aim upper half => TOP, lower half => BOTTOM
        // (and still respect UP/DOWN face if you prefer that behavior)
        Half half;
        if (side == Direction.DOWN) half = Half.TOP;
        else if (side == Direction.UP) half = Half.BOTTOM;
        else half = (ly >= 0.5) ? Half.TOP : Half.BOTTOM;

        // “Like corner post”: pick the corner you are looking at on that face
        Corner corner = pickCornerSmart(side, lx, lz);

        return state
                .setValue(BlockStateProperties.WATERLOGGED, waterlogged)
                .setValue(HALF, half)
                .setValue(CORNER, corner);
    }

    /**
     * Corner selection that behaves well on any clicked face:
     * - top/bottom: use X/Z
     * - side faces: force the axis that is constant and still choose the intended corner
     */
    private static Corner pickCornerSmart(Direction side, double lx, double lz) {
        // When placing on a side face, lx or lz will be ~0 or ~1 (on the face).
        // DO NOT force it to the opposite edge; that causes mirroring.
        double cx = Mth.clamp(lx, 0.0, 1.0);
        double cz = Mth.clamp(lz, 0.0, 1.0);
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
        return Shapes.join(Shapes.empty(), s, (a, b) -> b)
                .toAabbs()
                .stream()
                .map(box -> Block.box(
                        16 - box.maxZ * 16, box.minY * 16, box.minX * 16,
                        16 - box.minZ * 16, box.maxY * 16, box.maxX * 16
                ))
                .reduce(Shapes.empty(), Shapes::or);
    }
    private static VoxelShape rotateY180(VoxelShape s) { return rotateY90(rotateY90(s)); }
    private static VoxelShape rotateY270(VoxelShape s) { return rotateY90(rotateY180(s)); }

    private static VoxelShape shapeFor(BlockState state) {
        VoxelShape base = (state.getValue(HALF) == Half.TOP) ? BASE_TOP : BASE_BOTTOM;
        return rotateCorner(base, state.getValue(CORNER));
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
        builder.add(HAS_CAMO, BlockStateProperties.WATERLOGGED, CORNER, HALF);
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