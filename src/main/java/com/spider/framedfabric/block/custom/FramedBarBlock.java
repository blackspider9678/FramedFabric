package com.spider.framedfabric.block.custom;

import com.mojang.serialization.MapCodec;
import com.spider.framedfabric.block.enums.StripShape;
import net.minecraft.block.*;
import net.minecraft.block.enums.BlockHalf;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemPlacementContext;
import net.minecraft.state.StateManager;
import net.minecraft.state.property.EnumProperty;
import net.minecraft.state.property.Properties;
import net.minecraft.state.property.DirectionProperty;
import net.minecraft.state.property.Property;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.shape.VoxelShape;
import net.minecraft.util.shape.VoxelShapes;
import net.minecraft.world.BlockView;
import net.minecraft.world.World;
import net.minecraft.world.WorldAccess;
import org.jetbrains.annotations.Nullable;

public class YourCustomStripBlock extends Block {
    public static final MapCodec<YourCustomStripBlock> CODEC = createCodec(YourCustomStripBlock::new);

    public static final DirectionProperty FACING = Properties.HORIZONTAL_FACING;
    public static final EnumProperty<BlockHalf> HALF = Properties.BLOCK_HALF;
    public static final EnumProperty<StripShape> SHAPE = EnumProperty.of("shape", StripShape.class);

    // A thin “selection” outline (still no collision). Adjust thickness if you want.
    // This matches a strip that sits along the “front” edge of the block.
    private static final VoxelShape OUTLINE_NORTH_BOTTOM = Block.createCuboidShape(0, 0, 15, 16, 1, 16);
    private static final VoxelShape OUTLINE_SOUTH_BOTTOM = Block.createCuboidShape(0, 0, 0,  16, 1, 1);
    private static final VoxelShape OUTLINE_EAST_BOTTOM  = Block.createCuboidShape(0, 0, 0,  1, 1, 16);
    private static final VoxelShape OUTLINE_WEST_BOTTOM  = Block.createCuboidShape(15,0, 0,  16,1, 16);

    private static final VoxelShape OUTLINE_NORTH_TOP = Block.createCuboidShape(0, 15, 15, 16, 16, 16);
    private static final VoxelShape OUTLINE_SOUTH_TOP = Block.createCuboidShape(0, 15, 0,  16, 16, 1);
    private static final VoxelShape OUTLINE_EAST_TOP  = Block.createCuboidShape(0, 15, 0,  1, 16, 16);
    private static final VoxelShape OUTLINE_WEST_TOP  = Block.createCuboidShape(15,15, 0,  16,16, 16);

    public YourCustomStripBlock(Settings settings) {
        super(settings);
        this.setDefaultState(this.stateManager.getDefaultState()
                .with(FACING, Direction.NORTH)
                .with(HALF, BlockHalf.BOTTOM)
                .with(SHAPE, StripShape.SINGLE));
    }

    @Override
    protected MapCodec<? extends Block> getCodec() {
        return CODEC;
    }

    @Override
    protected void appendProperties(StateManager.Builder<Block, BlockState> builder) {
        builder.add(FACING, HALF, SHAPE);
    }

    @Override
    public @Nullable BlockState getPlacementState(ItemPlacementContext ctx) {
        Direction facing = ctx.getHorizontalPlayerFacing().getOpposite();

        // Slab-like: top if clicked upper half of block space
        boolean top = ctx.getHitPos().y - ctx.getBlockPos().getY() > 0.5;
        BlockHalf half = top ? BlockHalf.TOP : BlockHalf.BOTTOM;

        BlockState placed = this.getDefaultState()
                .with(FACING, facing)
                .with(HALF, half);

        // Compute initial shape based on neighbors
        return withComputedShape(placed, ctx.getWorld(), ctx.getBlockPos());
    }

    @Override
    protected BlockState getStateForNeighborUpdate(
            BlockState state,
            Direction direction,
            BlockState neighborState,
            WorldAccess world,
            BlockPos pos,
            BlockPos neighborPos
    ) {
        Direction facing = state.get(FACING);
        Direction left = facing.rotateYCounterclockwise();
        Direction right = facing.rotateYClockwise();

        // Only care when left/right neighbor changes; keeps updates cheap
        if (direction == left || direction == right) {
            return withComputedShape(state, world, pos);
        }
        return state;
    }

    @Override
    public void onPlaced(World world, BlockPos pos, BlockState state, @Nullable PlayerEntity placer, net.minecraft.item.ItemStack itemStack) {
        super.onPlaced(world, pos, state, placer, itemStack);
        updateSelfAndNeighbors(world, pos, state);
    }

    @Override
    public void onStateReplaced(BlockState state, World world, BlockPos pos, BlockState newState, boolean moved) {
        super.onStateReplaced(state, world, pos, newState, moved);

        // If removed or changed to a different block, update old neighbors
        if (!state.isOf(newState.getBlock())) {
            Direction facing = state.get(FACING);
            world.updateNeighbor(pos.offset(facing.rotateYCounterclockwise()), this, pos);
            world.updateNeighbor(pos.offset(facing.rotateYClockwise()), this, pos);
        }
    }

    private void updateSelfAndNeighbors(World world, BlockPos pos, BlockState state) {
        // Update this block
        BlockState updated = withComputedShape(state, world, pos);
        if (updated != state) world.setBlockState(pos, updated, Block.NOTIFY_ALL);

        // Update neighbors that might depend on us
        Direction facing = updated.get(FACING);
        world.updateNeighbor(pos.offset(facing.rotateYCounterclockwise()), this, pos);
        world.updateNeighbor(pos.offset(facing.rotateYClockwise()), this, pos);
    }

    private BlockState withComputedShape(BlockState state, WorldAccess world, BlockPos pos) {
        Direction facing = state.get(FACING);
        BlockHalf half = state.get(HALF);

        Direction leftDir = facing.rotateYCounterclockwise();
        Direction rightDir = facing.rotateYClockwise();

        boolean left = connectsTo(world, pos.offset(leftDir), facing, half);
        boolean right = connectsTo(world, pos.offset(rightDir), facing, half);

        StripShape shape =
                left && right ? StripShape.MIDDLE :
                        left ? StripShape.LEFT :
                                right ? StripShape.RIGHT :
                                        StripShape.SINGLE;

        return state.with(SHAPE, shape);
    }

    private boolean connectsTo(WorldAccess world, BlockPos neighborPos, Direction requiredFacing, BlockHalf requiredHalf) {
        BlockState n = world.getBlockState(neighborPos);
        if (!n.isOf(this)) return false;
        return n.get(FACING) == requiredFacing && n.get(HALF) == requiredHalf;
    }

    // NO COLLISION AT ALL
    @Override
    protected VoxelShape getCollisionShape(BlockState state, BlockView world, BlockPos pos, ShapeContext context) {
        return VoxelShapes.empty();
    }

    // Optional: also stop being treated as full cube for culling/occlusion
    @Override
    protected VoxelShape getCullingShape(BlockState state, BlockView world, BlockPos pos) {
        return VoxelShapes.empty();
    }

    // Still selectable: outline depends on facing + half
    @Override
    protected VoxelShape getOutlineShape(BlockState state, BlockView world, BlockPos pos, ShapeContext context) {
        Direction f = state.get(FACING);
        boolean top = state.get(HALF) == BlockHalf.TOP;

        return switch (f) {
            case NORTH -> top ? OUTLINE_NORTH_TOP : OUTLINE_NORTH_BOTTOM;
            case SOUTH -> top ? OUTLINE_SOUTH_TOP : OUTLINE_SOUTH_BOTTOM;
            case EAST  -> top ? OUTLINE_EAST_TOP  : OUTLINE_EAST_BOTTOM;
            case WEST  -> top ? OUTLINE_WEST_TOP  : OUTLINE_WEST_BOTTOM;
            default -> VoxelShapes.empty();
        };
    }
}