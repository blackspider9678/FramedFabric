package com.spider.framedfabric.block.custom;

import com.mojang.serialization.MapCodec;
import com.spider.framedfabric.block.enums.BarShape;
import com.spider.framedfabric.blockentity.AbstractFramedEntityBlock;
import com.spider.framedfabric.blockentity.FramedBlockEntity;
import com.spider.framedfabric.blockentity.FramedUseHandler;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockBehaviour.Properties;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.Half;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.item.ItemStack;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.Property;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import org.jetbrains.annotations.Nullable;

import static com.spider.framedfabric.blockentity.FramedProperties.HAS_CAMO;

public class FramedBarBlock extends AbstractFramedEntityBlock {
    public static final MapCodec<FramedBarBlock> CODEC = simpleCodec(FramedBarBlock::new);

    // mapping-proof: avoid DirectionProperty
    public static final Property<Direction> FACING = BlockStateProperties.HORIZONTAL_FACING;

    public static final EnumProperty<Half> HALF = BlockStateProperties.HALF;
    public static final EnumProperty<BarShape> SHAPE = EnumProperty.create("shape", BarShape.class);

    public FramedBarBlock(Properties settings) {
        super(settings);
        registerDefaultState(getStateDefinition().any()
                .setValue(ROT, 1)
                .setValue(HAS_CAMO, false)
                .setValue(FACING, Direction.NORTH)
                .setValue(HALF, Half.BOTTOM)
                .setValue(SHAPE, BarShape.SINGLE)
        );
    }

    @Override
    protected MapCodec<? extends AbstractFramedEntityBlock> codec() {
        return CODEC;
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        super.createBlockStateDefinition(builder);
        builder.add(HAS_CAMO, FACING, HALF, SHAPE);
    }

    // Placement: slab-like top/bottom + facing
    @Override
    public @Nullable BlockState getStateForPlacement(BlockPlaceContext ctx) {
        Direction facing = ctx.getHorizontalDirection().getOpposite();

        double localY = ctx.getClickLocation().y - ctx.getClickedPos().getY();
        Half half = (localY > 0.5) ? Half.TOP : Half.BOTTOM;

        BlockState placed = defaultBlockState()
                .setValue(FACING, facing)
                .setValue(HALF, half);

        return withComputedShape(placed, ctx.getLevel(), ctx.getClickedPos());
    }

    // ✅ Stable hook: called when a neighbor changes
    @Override
    protected void neighborChanged(BlockState state, Level world, BlockPos pos, Block sourceBlock, @Nullable net.minecraft.world.level.redstone.Orientation wireOrientation, boolean notify) {
        // If your mappings don't have WireOrientation, your IDE will redline this.
        // If that happens, see the alternate neighborUpdate signature just below.
        if (world.isClientSide()) return;

        BlockState updated = withComputedShape(state, (LevelAccessor) world, pos);
        if (updated != state) {
            world.setBlock(pos, updated, Block.UPDATE_ALL);
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
    public void setPlacedBy(Level world, BlockPos pos, BlockState state, @Nullable LivingEntity placer, net.minecraft.world.item.ItemStack itemStack) {
        super.setPlacedBy(world, pos, state, placer, itemStack);
        if (!world.isClientSide()) {
            BlockState updated = withComputedShape(state, (LevelAccessor) world, pos);
            if (updated != state) world.setBlock(pos, updated, Block.UPDATE_ALL);

            // nudge neighbors so they re-evaluate
            Direction facing = updated.getValue(FACING);
            world.updateNeighborsAt(pos.relative(facing.getCounterClockWise()), this);
            world.updateNeighborsAt(pos.relative(facing.getClockWise()), this);
        }
    }

    // Your mappings: super.onStateReplaced has only (state, ServerWorld, pos, moved)
    @Override
    public BlockState playerWillDestroy(Level world, BlockPos pos, BlockState state, Player player) {
        // let your BE drop camo parts like other framed blocks
        BlockState out = super.playerWillDestroy(world, pos, state, player);

        if (!world.isClientSide()) {
            Direction facing = state.getValue(FACING);
            world.updateNeighborsAt(pos.relative(facing.getCounterClockWise()), this);
            world.updateNeighborsAt(pos.relative(facing.getClockWise()), this);
        }
        return out;
    }

    private BlockState withComputedShape(BlockState state, LevelAccessor world, BlockPos pos) {
        Direction facing = state.getValue(FACING);
        Half half = state.getValue(HALF);

        Direction leftDir = facing.getCounterClockWise();
        Direction rightDir = facing.getClockWise();

        boolean left = connectsTo(world, pos.relative(leftDir), facing, half);
        boolean right = connectsTo(world, pos.relative(rightDir), facing, half);

        BarShape shape =
                (left && right) ? BarShape.MIDDLE :
                        left ? BarShape.LEFT :
                                right ? BarShape.RIGHT :
                                        BarShape.SINGLE;

        return state.setValue(SHAPE, shape);
    }

    private boolean connectsTo(LevelAccessor world, BlockPos npos, Direction facing, Half half) {
        BlockState n = world.getBlockState(npos);
        if (!n.is(this)) return false;
        return n.getValue(FACING) == facing && n.getValue(HALF) == half;
    }

    // No collisions
    @Override
    protected VoxelShape getCollisionShape(BlockState state, BlockGetter world, BlockPos pos, CollisionContext context) {
        return Shapes.empty();
    }

    @Override
    protected VoxelShape getOcclusionShape(BlockState state) {
        return Shapes.empty();
    }

    @Override
    protected VoxelShape getShape(BlockState state, BlockGetter world, BlockPos pos, CollisionContext context) {
        boolean top = state.getValue(HALF) == Half.TOP;
        Direction f = state.getValue(FACING);

        return switch (f) {
            case NORTH -> top ? Block.box(0, 15, 15, 16, 16, 16) : Block.box(0, 0, 15, 16, 1, 16);
            case SOUTH -> top ? Block.box(0, 15, 0,  16, 16, 1)  : Block.box(0, 0, 0,  16, 1, 1);
            case EAST  -> top ? Block.box(0, 15, 0,  1, 16, 16)  : Block.box(0, 0, 0,  1, 1, 16);
            case WEST  -> top ? Block.box(15,15, 0,  16,16, 16)  : Block.box(15,0, 0,  16,1, 16);
            default -> Shapes.empty();
        };
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level world, BlockPos pos, Player player, BlockHitResult hit) {
        FramedBlockEntity be = (world.getBlockEntity(pos) instanceof FramedBlockEntity fbe) ? fbe : null;
        return FramedUseHandler.handleUse(state, world, pos, player, hit, be, 0);
    }
}