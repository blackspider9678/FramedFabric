package com.spider.framedfabric.block.custom;

import com.mojang.serialization.MapCodec;
import com.spider.framedfabric.blockentity.AbstractFramedEntityBlock;
import com.spider.framedfabric.blockentity.FramedBlockEntity;
import com.spider.framedfabric.blockentity.FramedUseHandler;
import net.minecraft.world.level.block.*;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.util.Mth;
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
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.level.block.state.properties.Half;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.level.material.Fluids;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;

import static com.spider.framedfabric.blockentity.FramedProperties.HAS_CAMO;

public class FramedCornerCubeBlock extends AbstractFramedEntityBlock implements SimpleWaterloggedBlock {

    public static final MapCodec<FramedCornerCubeBlock> CODEC = simpleCodec(FramedCornerCubeBlock::new);

    public enum Corner implements StringRepresentable {
        NW("nw"), NE("ne"), SW("sw"), SE("se");
        private final String id;
        Corner(String id) { this.id = id; }
        @Override public String getSerializedName() { return id; }
    }

    public static final EnumProperty<Corner> CORNER = EnumProperty.create("corner", Corner.class);
    public static final EnumProperty<Half> HALF = BlockStateProperties.HALF;

    // Bottom shapes (8×8×8 cube in each corner)
    private static final VoxelShape BOTTOM_NW = Block.box(0, 0, 0, 8, 8, 8);
    private static final VoxelShape BOTTOM_NE = Block.box(8, 0, 0, 16, 8, 8);
    private static final VoxelShape BOTTOM_SW = Block.box(0, 0, 8, 8, 8, 16);
    private static final VoxelShape BOTTOM_SE = Block.box(8, 0, 8, 16, 8, 16);

    // Top shapes
    private static final VoxelShape TOP_NW = Block.box(0, 8, 0, 8, 16, 8);
    private static final VoxelShape TOP_NE = Block.box(8, 8, 0, 16, 16, 8);
    private static final VoxelShape TOP_SW = Block.box(0, 8, 8, 8, 16, 16);
    private static final VoxelShape TOP_SE = Block.box(8, 8, 8, 16, 16, 16);

    public FramedCornerCubeBlock(Properties settings) {
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

        // Clamp (prevents weirdness exactly on borders)
        double cx = Mth.clamp(lx, 0.0, 1.0 - 1e-6);
        double cz = Mth.clamp(lz, 0.0, 1.0 - 1e-6);

        Corner corner = pickCornerXZ(cx, cz);

        Direction side = ctx.getClickedFace();
        Half half;
        if (side == Direction.DOWN) half = Half.TOP;
        else if (side == Direction.UP) half = Half.BOTTOM;
        else half = (ly >= 0.5) ? Half.TOP : Half.BOTTOM;

        return state
                .setValue(BlockStateProperties.WATERLOGGED, waterlogged)
                .setValue(CORNER, corner)
                .setValue(HALF, half);
    }

    private static Corner pickCornerXZ(double lx, double lz) {
        boolean east = lx >= 0.5;
        boolean south = lz >= 0.5;

        if (!east && !south) return Corner.NW;
        if ( east && !south) return Corner.NE;
        if (!east &&  south) return Corner.SW;
        return Corner.SE;
    }

    private static VoxelShape shapeFor(BlockState state) {
        boolean top = state.getValue(HALF) == Half.TOP;
        Corner c = state.getValue(CORNER);

        if (!top) {
            return switch (c) {
                case NW -> BOTTOM_NW;
                case NE -> BOTTOM_NE;
                case SW -> BOTTOM_SW;
                case SE -> BOTTOM_SE;
            };
        } else {
            return switch (c) {
                case NW -> TOP_NW;
                case NE -> TOP_NE;
                case SW -> TOP_SW;
                case SE -> TOP_SE;
            };
        }
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