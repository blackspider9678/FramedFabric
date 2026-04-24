package com.spider.framedfabric.block.custom;

import com.mojang.serialization.MapCodec;
import com.spider.framedfabric.blockentity.FramedBlockEntity;
import com.spider.framedfabric.blockentity.FramedUseHandler;
import com.spider.framedfabric.block.enums.VerticalSlabType;
import com.spider.framedfabric.camo.FramedCamoLogic;
import com.spider.framedfabric.registry.ModBlockEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.ScheduledTickAccess;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.SimpleWaterloggedBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.level.block.state.properties.Property;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.level.material.Fluids;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jetbrains.annotations.Nullable;

import static com.spider.framedfabric.blockentity.FramedProperties.ROT;

public class FramedVerticalSlabBlock extends Block implements EntityBlock, SimpleWaterloggedBlock {
    public static final MapCodec<FramedVerticalSlabBlock> CODEC = simpleCodec(FramedVerticalSlabBlock::new);

    public static final BooleanProperty HAS_CAMO = BooleanProperty.create("has_camo");

    public static final EnumProperty<VerticalSlabType> TYPE =
            EnumProperty.create("type", VerticalSlabType.class);

    // 1.21.10: no DirectionProperty; use EnumProperty<Direction>
    public static final EnumProperty<Direction> FACING = BlockStateProperties.HORIZONTAL_FACING;

    public static final BooleanProperty WATERLOGGED = BlockStateProperties.WATERLOGGED;

    // Base SINGLE occupies NORTH half (z 0..8)
    private static final VoxelShape NORTH_SHAPE = Block.box(0, 0, 0, 16, 16, 8);
    private static final VoxelShape SOUTH_SHAPE = Block.box(0, 0, 8, 16, 16, 16);
    private static final VoxelShape WEST_SHAPE  = Block.box(0, 0, 0, 8, 16, 16);
    private static final VoxelShape EAST_SHAPE  = Block.box(8, 0, 0, 16, 16, 16);

    public FramedVerticalSlabBlock(Properties settings) {
        super(settings);
        this.registerDefaultState(this.getStateDefinition().any()
                .setValue(HAS_CAMO, false)
                .setValue(TYPE, VerticalSlabType.SINGLE)
                .setValue(FACING, Direction.NORTH)
                .setValue(WATERLOGGED, false)
                .setValue(ROT, 1)
        );
    }

    @Override
    @SuppressWarnings({"unchecked", "rawtypes"})
    public MapCodec<? extends Block> codec() {
        return (MapCodec) CODEC;
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(new Property[]{HAS_CAMO, TYPE, FACING, WATERLOGGED, ROT});
    }

    @Override
    public @Nullable BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new FramedBlockEntity(ModBlockEntities.FRAMED, pos, state);
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level world, BlockPos pos, net.minecraft.world.entity.player.Player player, BlockHitResult hit) {
        FramedBlockEntity be = (world.getBlockEntity(pos) instanceof FramedBlockEntity fbe) ? fbe : null;
        return FramedUseHandler.handleUse(state, world, pos, player, hit, be);
    }

    // -------------------------
    // Placement / merge
    // -------------------------

    @Override
    public @Nullable BlockState getStateForPlacement(BlockPlaceContext ctx) {
        BlockPos pos = ctx.getClickedPos();
        Level world = ctx.getLevel();
        BlockState existing = world.getBlockState(pos);

        // Merge ONLY when the click indicates merging.
        if (existing.is(this) && shouldMerge(existing, ctx)) {
            BlockState placed = existing
                    .setValue(TYPE, VerticalSlabType.DOUBLE)
                    .setValue(WATERLOGGED, false);

            // Preserve HAS_CAMO + ROT (same as you already do)
            boolean has = existing.hasProperty(HAS_CAMO) && existing.getValue(HAS_CAMO);
            int rot = existing.hasProperty(ROT) ? existing.getValue(ROT) : 1;

            if (world.getBlockEntity(pos) instanceof FramedBlockEntity fbe) {
                has = fbe.hasAnyCamo();
            }

            placed = placed.setValue(HAS_CAMO, has).setValue(ROT, rot);

            // Keep original facing after merging
            if (existing.hasProperty(FACING)) {
                placed = placed.setValue(FACING, existing.getValue(FACING));
            }

            return placed;
        }

        // Normal placement (new block space)
        FluidState fluidState = world.getFluidState(pos);
        boolean water = fluidState.getType() == Fluids.WATER;

        Direction facing = pickFacing(ctx);

        return this.defaultBlockState()
                .setValue(TYPE, VerticalSlabType.SINGLE)
                .setValue(FACING, facing)
                .setValue(WATERLOGGED, water);
    }

    // Looser replace rules are fine because we already merge in getPlacementState when existing.isOf(this).
    // Keeping it simple avoids fighting 1.21's changed signatures.
    @Override
    protected boolean canBeReplaced(BlockState state, BlockPlaceContext ctx) {
        if (state.getValue(TYPE) == VerticalSlabType.DOUBLE) return false;
        if (!ctx.getItemInHand().is(this.asItem())) return false;

        // Only allow replacing/merging when the click indicates a merge.
        return shouldMerge(state, ctx);
    }

    private static Direction pickFacing(BlockPlaceContext ctx) {
        Direction side = ctx.getClickedFace();

        // clicked a side -> occupy the half that TOUCHES the clicked block
        // (so we don't leave a gap)
        if (side.getAxis().isHorizontal()) return side.getOpposite();

        // clicked top/bottom -> choose based on hit position (dominant axis)
        Vec3 hit = ctx.getClickLocation();
        BlockPos pos = ctx.getClickedPos();

        double lx = hit.x - pos.getX(); // 0..1
        double lz = hit.z - pos.getZ(); // 0..1

        double dx = lx - 0.5;
        double dz = lz - 0.5;

        if (Math.abs(dx) >= Math.abs(dz)) {
            return dx >= 0 ? Direction.EAST : Direction.WEST;
        } else {
            return dz >= 0 ? Direction.SOUTH : Direction.NORTH;
        }
    }

    // -------------------------
    // Shapes
    // -------------------------

    @Override
    protected VoxelShape getShape(BlockState state, BlockGetter world, BlockPos pos, CollisionContext context) {
        if (state.getValue(TYPE) == VerticalSlabType.DOUBLE) return Shapes.block();

        return switch (state.getValue(FACING)) {
            case NORTH -> NORTH_SHAPE;
            case SOUTH -> SOUTH_SHAPE;
            case EAST  -> EAST_SHAPE;
            case WEST  -> WEST_SHAPE;
            default    -> Shapes.block();
        };
    }

    // -------------------------
    // Waterlogging (match SlabBlock signature)
    // -------------------------

    @Override
    protected FluidState getFluidState(BlockState state) {
        return state.getValue(WATERLOGGED) ? Fluids.WATER.getSource(false) : super.getFluidState(state);
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
        if (state.getValue(WATERLOGGED)) {
            tickView.scheduleTick(pos, Fluids.WATER, Fluids.WATER.getTickDelay(world));
        }
        return super.updateShape(state, world, tickView, pos, direction, neighborPos, neighborState, random);
    }

    // -------------------------
    // Drops
    // -------------------------

    @Override
    public BlockState playerWillDestroy(Level world, BlockPos pos, BlockState state, Player player) {
        if (world instanceof ServerLevel sw) {
            if (sw.getBlockEntity(pos) instanceof FramedBlockEntity be) {
                for (int i = 0; i < FramedBlockEntity.MAX_CAMO_PARTS; i++) {
                    if (be.hasCamoPart(i)) {
                        ItemStack drop = FramedCamoLogic.camoRefundStack(be, i);
                        if (!drop.isEmpty()) Block.popResource(sw, pos, drop);
                    }
                }
            }
        }
        return super.playerWillDestroy(world, pos, state, player);
    }

    private static boolean shouldMerge(BlockState existing, BlockPlaceContext ctx) {
        if (!existing.is(ctx.getLevel().getBlockState(ctx.getClickedPos()).getBlock())) return false; // defensive
        if (existing.getValue(TYPE) == VerticalSlabType.DOUBLE) return false;

        Direction facing = existing.getValue(FACING);
        Direction side = ctx.getClickedFace();

        // If you clicked the slab's "inside face" (the face towards the empty half), merge.
        if (side == facing.getOpposite()) return true;

        // If you clicked top/bottom, merge only if you're aiming at the EMPTY half.
        if (side == Direction.UP || side == Direction.DOWN) {
            BlockPos pos = ctx.getClickedPos();
            Vec3 hit = ctx.getClickLocation();
            double lx = hit.x - pos.getX(); // 0..1
            double lz = hit.z - pos.getZ(); // 0..1

            return switch (facing) {
                case NORTH -> lz >= 0.5; // empty is SOUTH half
                case SOUTH -> lz < 0.5;  // empty is NORTH half
                case WEST  -> lx >= 0.5; // empty is EAST half
                case EAST  -> lx < 0.5;  // empty is WEST half
                default    -> false;
            };
        }

        return false;
    }
}
