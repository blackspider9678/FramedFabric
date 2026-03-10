package com.spider.framedfabric.block.custom;

import com.mojang.serialization.MapCodec;
import com.spider.framedfabric.block.enums.BarShape;
import com.spider.framedfabric.blockentity.AbstractFramedEntityBlock;
import com.spider.framedfabric.blockentity.FramedBlockEntity;
import com.spider.framedfabric.blockentity.FramedUseHandler;
import net.minecraft.block.*;
import net.minecraft.block.enums.BlockHalf;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemPlacementContext;
import net.minecraft.item.ItemStack;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.state.StateManager;
import net.minecraft.state.property.EnumProperty;
import net.minecraft.state.property.Properties;
import net.minecraft.state.property.Property;
import net.minecraft.util.ActionResult;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.shape.VoxelShape;
import net.minecraft.util.shape.VoxelShapes;
import net.minecraft.world.BlockView;
import net.minecraft.world.World;
import net.minecraft.world.WorldAccess;
import org.jetbrains.annotations.Nullable;

import static com.spider.framedfabric.blockentity.FramedProperties.HAS_CAMO;

public class FramedBarBlock extends AbstractFramedEntityBlock {
    public static final MapCodec<FramedBarBlock> CODEC = createCodec(FramedBarBlock::new);

    // mapping-proof: avoid DirectionProperty
    public static final Property<Direction> FACING = Properties.HORIZONTAL_FACING;

    public static final EnumProperty<BlockHalf> HALF = Properties.BLOCK_HALF;
    public static final EnumProperty<BarShape> SHAPE = EnumProperty.of("shape", BarShape.class);

    public FramedBarBlock(Settings settings) {
        super(settings);
        setDefaultState(getStateManager().getDefaultState()
                .with(ROT, 1)
                .with(HAS_CAMO, false)
                .with(FACING, Direction.NORTH)
                .with(HALF, BlockHalf.BOTTOM)
                .with(SHAPE, BarShape.SINGLE)
        );
    }

    @Override
    protected MapCodec<? extends AbstractFramedEntityBlock> getCodec() {
        return CODEC;
    }

    @Override
    protected void appendProperties(StateManager.Builder<Block, BlockState> builder) {
        super.appendProperties(builder);
        builder.add(HAS_CAMO, FACING, HALF, SHAPE);
    }

    // Placement: slab-like top/bottom + facing
    @Override
    public @Nullable BlockState getPlacementState(ItemPlacementContext ctx) {
        Direction facing = ctx.getHorizontalPlayerFacing().getOpposite();

        double localY = ctx.getHitPos().y - ctx.getBlockPos().getY();
        BlockHalf half = (localY > 0.5) ? BlockHalf.TOP : BlockHalf.BOTTOM;

        BlockState placed = getDefaultState()
                .with(FACING, facing)
                .with(HALF, half);

        return withComputedShape(placed, ctx.getWorld(), ctx.getBlockPos());
    }

    // ✅ Stable hook: called when a neighbor changes
    @Override
    protected void neighborUpdate(BlockState state, World world, BlockPos pos, Block sourceBlock, @Nullable net.minecraft.world.block.WireOrientation wireOrientation, boolean notify) {
        // If your mappings don't have WireOrientation, your IDE will redline this.
        // If that happens, see the alternate neighborUpdate signature just below.
        if (world.isClient()) return;

        BlockState updated = withComputedShape(state, (WorldAccess) world, pos);
        if (updated != state) {
            world.setBlockState(pos, updated, Block.NOTIFY_ALL);
        }
    }

    /*
    // ✅ Alternate neighborUpdate signature (comment out the one above and use this if needed)
    @Override
    protected void neighborUpdate(BlockState state, World world, BlockPos pos, Block sourceBlock, BlockPos sourcePos, boolean notify) {
        if (world.isClient()) return;

        BlockState updated = withComputedShape(state, (WorldAccess) world, pos);
        if (updated != state) {
            world.setBlockState(pos, updated, Block.NOTIFY_ALL);
        }
    }
    */

    @Override
    public void onPlaced(World world, BlockPos pos, BlockState state, @Nullable LivingEntity placer, net.minecraft.item.ItemStack itemStack) {
        super.onPlaced(world, pos, state, placer, itemStack);
        if (!world.isClient()) {
            BlockState updated = withComputedShape(state, (WorldAccess) world, pos);
            if (updated != state) world.setBlockState(pos, updated, Block.NOTIFY_ALL);

            // nudge neighbors so they re-evaluate
            Direction facing = updated.get(FACING);
            world.updateNeighbors(pos.offset(facing.rotateYCounterclockwise()), this);
            world.updateNeighbors(pos.offset(facing.rotateYClockwise()), this);
        }
    }

    // Your mappings: super.onStateReplaced has only (state, ServerWorld, pos, moved)
    @Override
    public BlockState onBreak(World world, BlockPos pos, BlockState state, PlayerEntity player) {
        // let your BE drop camo parts like other framed blocks
        BlockState out = super.onBreak(world, pos, state, player);

        if (!world.isClient()) {
            Direction facing = state.get(FACING);
            world.updateNeighbors(pos.offset(facing.rotateYCounterclockwise()), this);
            world.updateNeighbors(pos.offset(facing.rotateYClockwise()), this);
        }
        return out;
    }

    private BlockState withComputedShape(BlockState state, WorldAccess world, BlockPos pos) {
        Direction facing = state.get(FACING);
        BlockHalf half = state.get(HALF);

        Direction leftDir = facing.rotateYCounterclockwise();
        Direction rightDir = facing.rotateYClockwise();

        boolean left = connectsTo(world, pos.offset(leftDir), facing, half);
        boolean right = connectsTo(world, pos.offset(rightDir), facing, half);

        BarShape shape =
                (left && right) ? BarShape.MIDDLE :
                        left ? BarShape.LEFT :
                                right ? BarShape.RIGHT :
                                        BarShape.SINGLE;

        return state.with(SHAPE, shape);
    }

    private boolean connectsTo(WorldAccess world, BlockPos npos, Direction facing, BlockHalf half) {
        BlockState n = world.getBlockState(npos);
        if (!n.isOf(this)) return false;
        return n.get(FACING) == facing && n.get(HALF) == half;
    }

    // No collisions
    @Override
    protected VoxelShape getCollisionShape(BlockState state, BlockView world, BlockPos pos, ShapeContext context) {
        return VoxelShapes.empty();
    }

    @Override
    protected VoxelShape getCullingShape(BlockState state) {
        return VoxelShapes.empty();
    }

    @Override
    protected VoxelShape getOutlineShape(BlockState state, BlockView world, BlockPos pos, ShapeContext context) {
        boolean top = state.get(HALF) == BlockHalf.TOP;
        Direction f = state.get(FACING);

        return switch (f) {
            case NORTH -> top ? Block.createCuboidShape(0, 15, 15, 16, 16, 16) : Block.createCuboidShape(0, 0, 15, 16, 1, 16);
            case SOUTH -> top ? Block.createCuboidShape(0, 15, 0,  16, 16, 1)  : Block.createCuboidShape(0, 0, 0,  16, 1, 1);
            case EAST  -> top ? Block.createCuboidShape(0, 15, 0,  1, 16, 16)  : Block.createCuboidShape(0, 0, 0,  1, 1, 16);
            case WEST  -> top ? Block.createCuboidShape(15,15, 0,  16,16, 16)  : Block.createCuboidShape(15,0, 0,  16,1, 16);
            default -> VoxelShapes.empty();
        };
    }

    @Override
    protected ActionResult onUse(BlockState state, World world, BlockPos pos, PlayerEntity player, BlockHitResult hit) {
        FramedBlockEntity be = (world.getBlockEntity(pos) instanceof FramedBlockEntity fbe) ? fbe : null;
        return FramedUseHandler.handleUse(state, world, pos, player, hit, be, 0);
    }
}