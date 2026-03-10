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
import net.minecraft.world.BlockView;
import net.minecraft.world.World;
import net.minecraft.world.WorldView;
import net.minecraft.world.tick.ScheduledTickView;

import static com.spider.framedfabric.blockentity.FramedProperties.HAS_CAMO;

public class FramedCornerCubeBlock extends AbstractFramedEntityBlock implements Waterloggable {

    public static final MapCodec<FramedCornerCubeBlock> CODEC = createCodec(FramedCornerCubeBlock::new);

    public enum Corner implements StringIdentifiable {
        NW("nw"), NE("ne"), SW("sw"), SE("se");
        private final String id;
        Corner(String id) { this.id = id; }
        @Override public String asString() { return id; }
    }

    public static final EnumProperty<Corner> CORNER = EnumProperty.of("corner", Corner.class);
    public static final EnumProperty<BlockHalf> HALF = Properties.BLOCK_HALF;

    // Bottom shapes (8×8×8 cube in each corner)
    private static final VoxelShape BOTTOM_NW = Block.createCuboidShape(0, 0, 0, 8, 8, 8);
    private static final VoxelShape BOTTOM_NE = Block.createCuboidShape(8, 0, 0, 16, 8, 8);
    private static final VoxelShape BOTTOM_SW = Block.createCuboidShape(0, 0, 8, 8, 8, 16);
    private static final VoxelShape BOTTOM_SE = Block.createCuboidShape(8, 0, 8, 16, 8, 16);

    // Top shapes
    private static final VoxelShape TOP_NW = Block.createCuboidShape(0, 8, 0, 8, 16, 8);
    private static final VoxelShape TOP_NE = Block.createCuboidShape(8, 8, 0, 16, 16, 8);
    private static final VoxelShape TOP_SW = Block.createCuboidShape(0, 8, 8, 8, 16, 16);
    private static final VoxelShape TOP_SE = Block.createCuboidShape(8, 8, 8, 16, 16, 16);

    public FramedCornerCubeBlock(Settings settings) {
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

        // Clamp (prevents weirdness exactly on borders)
        double cx = MathHelper.clamp(lx, 0.0, 1.0 - 1e-6);
        double cz = MathHelper.clamp(lz, 0.0, 1.0 - 1e-6);

        Corner corner = pickCornerXZ(cx, cz);

        Direction side = ctx.getSide();
        BlockHalf half;
        if (side == Direction.DOWN) half = BlockHalf.TOP;
        else if (side == Direction.UP) half = BlockHalf.BOTTOM;
        else half = (ly >= 0.5) ? BlockHalf.TOP : BlockHalf.BOTTOM;

        return state
                .with(Properties.WATERLOGGED, waterlogged)
                .with(CORNER, corner)
                .with(HALF, half);
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
        boolean top = state.get(HALF) == BlockHalf.TOP;
        Corner c = state.get(CORNER);

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