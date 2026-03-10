package com.spider.framedfabric.block.custom;

import com.mojang.serialization.MapCodec;
import com.spider.framedfabric.block.enums.VerticalSlabType;
import com.spider.framedfabric.blockentity.FramedBlockEntity;
import com.spider.framedfabric.blockentity.FramedUseHandler;
import com.spider.framedfabric.camo.FramedCamoLogic;
import com.spider.framedfabric.registry.ModBlockEntities;
import net.minecraft.block.Block;
import net.minecraft.block.BlockEntityProvider;
import net.minecraft.block.BlockState;
import net.minecraft.block.ShapeContext;
import net.minecraft.block.Waterloggable;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.fluid.FluidState;
import net.minecraft.fluid.Fluids;
import net.minecraft.item.ItemPlacementContext;
import net.minecraft.item.ItemStack;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.state.StateManager;
import net.minecraft.state.property.BooleanProperty;
import net.minecraft.state.property.EnumProperty;
import net.minecraft.state.property.Properties;
import net.minecraft.state.property.Property;
import net.minecraft.util.ActionResult;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.Vec3d;
import net.minecraft.util.math.random.Random;
import net.minecraft.util.shape.VoxelShape;
import net.minecraft.util.shape.VoxelShapes;
import net.minecraft.world.BlockView;
import net.minecraft.world.World;
import net.minecraft.world.WorldView;
import net.minecraft.world.tick.ScheduledTickView;
import org.jetbrains.annotations.Nullable;

import static com.spider.framedfabric.blockentity.FramedProperties.ROT;

public class FramedCheckeredVerticalSlabBlock extends Block implements BlockEntityProvider, Waterloggable {
    public static final MapCodec<FramedCheckeredVerticalSlabBlock> CODEC =
            createCodec(FramedCheckeredVerticalSlabBlock::new);

    public static final BooleanProperty HAS_CAMO = BooleanProperty.of("has_camo");

    public static final EnumProperty<VerticalSlabType> TYPE =
            EnumProperty.of("type", VerticalSlabType.class);

    public static final EnumProperty<Direction> FACING = Properties.HORIZONTAL_FACING;

    public static final BooleanProperty WATERLOGGED = Properties.WATERLOGGED;

    // SINGLE occupies the half indicated by FACING (same shapes as your FramedVerticalSlabBlock)
    private static final VoxelShape NORTH_SHAPE = Block.createCuboidShape(0, 0, 0, 16, 16, 8);
    private static final VoxelShape SOUTH_SHAPE = Block.createCuboidShape(0, 0, 8, 16, 16, 16);
    private static final VoxelShape WEST_SHAPE  = Block.createCuboidShape(0, 0, 0, 8, 16, 16);
    private static final VoxelShape EAST_SHAPE  = Block.createCuboidShape(8, 0, 0, 16, 16, 16);

    public FramedCheckeredVerticalSlabBlock(Settings settings) {
        super(settings);
        this.setDefaultState(this.getStateManager().getDefaultState()
                .with(HAS_CAMO, false)
                .with(TYPE, VerticalSlabType.SINGLE)
                .with(FACING, Direction.NORTH)
                .with(WATERLOGGED, false)
                .with(ROT, 1)
        );
    }

    @Override
    @SuppressWarnings({"unchecked", "rawtypes"})
    public MapCodec<? extends Block> getCodec() {
        return (MapCodec) CODEC;
    }

    @Override
    protected void appendProperties(StateManager.Builder<Block, BlockState> builder) {
        builder.add(new Property[]{HAS_CAMO, TYPE, FACING, WATERLOGGED, ROT});
    }

    @Override
    public @Nullable BlockEntity createBlockEntity(BlockPos pos, BlockState state) {
        return new FramedBlockEntity(ModBlockEntities.FRAMED, pos, state);
    }

    @Override
    protected ActionResult onUse(BlockState state, World world, BlockPos pos, PlayerEntity player, BlockHitResult hit) {
        FramedBlockEntity be = (world.getBlockEntity(pos) instanceof FramedBlockEntity fbe) ? fbe : null;
        return FramedUseHandler.handleUse(state, world, pos, player, hit, be);
    }

    // -------------------------
    // Placement / merge
    // -------------------------

    @Override
    public @Nullable BlockState getPlacementState(ItemPlacementContext ctx) {
        BlockPos pos = ctx.getBlockPos();
        World world = ctx.getWorld();
        BlockState existing = world.getBlockState(pos);

        if (existing.isOf(this) && shouldMerge(existing, ctx)) {
            BlockState placed = existing
                    .with(TYPE, VerticalSlabType.DOUBLE)
                    .with(WATERLOGGED, false);

            boolean has = existing.contains(HAS_CAMO) && existing.get(HAS_CAMO);
            int rot = existing.contains(ROT) ? existing.get(ROT) : 1;

            if (world.getBlockEntity(pos) instanceof FramedBlockEntity fbe) {
                has = fbe.hasAnyCamo();
            }

            placed = placed.with(HAS_CAMO, has).with(ROT, rot);

            if (existing.contains(FACING)) placed = placed.with(FACING, existing.get(FACING));
            return placed;
        }

        FluidState fluidState = world.getFluidState(pos);
        boolean water = fluidState.getFluid() == Fluids.WATER;

        Direction facing = pickFacing(ctx);

        return this.getDefaultState()
                .with(TYPE, VerticalSlabType.SINGLE)
                .with(FACING, facing)
                .with(WATERLOGGED, water);
    }

    @Override
    protected boolean canReplace(BlockState state, ItemPlacementContext ctx) {
        if (state.get(TYPE) == VerticalSlabType.DOUBLE) return false;
        if (!ctx.getStack().isOf(this.asItem())) return false;
        return shouldMerge(state, ctx);
    }

    private static Direction pickFacing(ItemPlacementContext ctx) {
        Direction side = ctx.getSide();

        if (side.getAxis().isHorizontal()) return side.getOpposite();

        Vec3d hit = ctx.getHitPos();
        BlockPos pos = ctx.getBlockPos();

        double lx = hit.x - pos.getX(); // 0..1
        double lz = hit.z - pos.getZ(); // 0..1

        double dx = lx - 0.5;
        double dz = lz - 0.5;

        if (Math.abs(dx) >= Math.abs(dz)) return dx >= 0 ? Direction.EAST : Direction.WEST;
        return dz >= 0 ? Direction.SOUTH : Direction.NORTH;
    }

    // -------------------------
    // Shapes
    // -------------------------

    @Override
    protected VoxelShape getOutlineShape(BlockState state, BlockView world, BlockPos pos, ShapeContext context) {
        if (state.get(TYPE) == VerticalSlabType.DOUBLE) return VoxelShapes.fullCube();

        return switch (state.get(FACING)) {
            case NORTH -> NORTH_SHAPE;
            case SOUTH -> SOUTH_SHAPE;
            case EAST  -> EAST_SHAPE;
            case WEST  -> WEST_SHAPE;
            default    -> VoxelShapes.fullCube();
        };
    }

    // -------------------------
    // Waterlogging
    // -------------------------

    @Override
    protected FluidState getFluidState(BlockState state) {
        return state.get(WATERLOGGED) ? Fluids.WATER.getStill(false) : super.getFluidState(state);
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
        if (state.get(WATERLOGGED)) {
            tickView.scheduleFluidTick(pos, Fluids.WATER, Fluids.WATER.getTickRate(world));
        }
        return super.getStateForNeighborUpdate(state, world, tickView, pos, direction, neighborPos, neighborState, random);
    }

    // -------------------------
    // Drops
    // -------------------------

    @Override
    public BlockState onBreak(World world, BlockPos pos, BlockState state, PlayerEntity player) {
        if (world instanceof ServerWorld sw) {
            if (sw.getBlockEntity(pos) instanceof FramedBlockEntity be) {
                for (int i = 0; i < FramedBlockEntity.MAX_CAMO_PARTS; i++) {
                    if (be.hasCamoPart(i)) {
                        ItemStack drop = FramedCamoLogic.camoRefundStack(be, i);
                        if (!drop.isEmpty()) Block.dropStack(sw, pos, drop);
                    }
                }
            }
        }
        return super.onBreak(world, pos, state, player);
    }

    private static boolean shouldMerge(BlockState existing, ItemPlacementContext ctx) {
        if (existing.get(TYPE) == VerticalSlabType.DOUBLE) return false;

        Direction facing = existing.get(FACING);
        Direction side = ctx.getSide();

        if (side == facing.getOpposite()) return true;

        if (side == Direction.UP || side == Direction.DOWN) {
            BlockPos pos = ctx.getBlockPos();
            Vec3d hit = ctx.getHitPos();
            double lx = hit.x - pos.getX();
            double lz = hit.z - pos.getZ();

            return switch (facing) {
                case NORTH -> lz >= 0.5; // empty is SOUTH
                case SOUTH -> lz < 0.5;  // empty is NORTH
                case WEST  -> lx >= 0.5; // empty is EAST
                case EAST  -> lx < 0.5;  // empty is WEST
                default    -> false;
            };
        }

        return false;
    }

    @Override
    public VoxelShape getCollisionShape(BlockState state, BlockView world, BlockPos pos, ShapeContext context) {
        return getOutlineShape(state, world, pos, context);
    }

    @Override
    public VoxelShape getRaycastShape(BlockState state, BlockView world, BlockPos pos) {
        // Raycast has a slightly different signature than outline/collision
        if (state.get(TYPE) == VerticalSlabType.DOUBLE) return VoxelShapes.fullCube();

        return switch (state.get(FACING)) {
            case NORTH -> NORTH_SHAPE;
            case SOUTH -> SOUTH_SHAPE;
            case EAST  -> EAST_SHAPE;
            case WEST  -> WEST_SHAPE;
            default    -> VoxelShapes.fullCube();
        };
    }

    // Optional: helps players not suffocate / camera inside behavior
    @Override
    public VoxelShape getCameraCollisionShape(BlockState state, BlockView world, BlockPos pos, ShapeContext context) {
        return getOutlineShape(state, world, pos, context);
    }
}