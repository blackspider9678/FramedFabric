package com.spider.framedfabric.block.custom;

import com.mojang.serialization.MapCodec;
import com.spider.framedfabric.blockentity.AbstractFramedEntityBlock;
import com.spider.framedfabric.blockentity.FramedBlockEntity;
import com.spider.framedfabric.blockentity.FramedUseHandler;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.ShapeContext;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemPlacementContext;
import net.minecraft.item.ItemStack;
import net.minecraft.loot.context.LootWorldContext;
import net.minecraft.state.StateManager;
import net.minecraft.state.property.EnumProperty;
import net.minecraft.state.property.IntProperty;
import net.minecraft.state.property.Properties;
import net.minecraft.util.ActionResult;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.shape.VoxelShape;
import net.minecraft.world.BlockView;
import net.minecraft.world.World;

import java.util.List;

import static com.spider.framedfabric.blockentity.FramedProperties.HAS_CAMO;

public class FramedThinPlateBlock extends AbstractFramedEntityBlock {

    public static final MapCodec<FramedThinPlateBlock> CODEC = createCodec(FramedThinPlateBlock::new);

    public static final int MAX_LAYERS = 16;
    public static final EnumProperty<Direction> FACING = Properties.FACING;
    public static final IntProperty LAYERS = IntProperty.of("layers", 1, MAX_LAYERS);

    private static final VoxelShape[] DOWN_SHAPES =
            Block.createShapeArray(MAX_LAYERS + 1, layer -> Block.createCuboidShape(0, 0, 0, 16, layer, 16));
    private static final VoxelShape[] UP_SHAPES =
            Block.createShapeArray(MAX_LAYERS + 1, layer -> Block.createCuboidShape(0, 16 - layer, 0, 16, 16, 16));
    private static final VoxelShape[] NORTH_SHAPES =
            Block.createShapeArray(MAX_LAYERS + 1, layer -> Block.createCuboidShape(0, 0, 0, 16, 16, layer));
    private static final VoxelShape[] SOUTH_SHAPES =
            Block.createShapeArray(MAX_LAYERS + 1, layer -> Block.createCuboidShape(0, 0, 16 - layer, 16, 16, 16));
    private static final VoxelShape[] WEST_SHAPES =
            Block.createShapeArray(MAX_LAYERS + 1, layer -> Block.createCuboidShape(0, 0, 0, layer, 16, 16));
    private static final VoxelShape[] EAST_SHAPES =
            Block.createShapeArray(MAX_LAYERS + 1, layer -> Block.createCuboidShape(16 - layer, 0, 0, 16, 16, 16));

    public FramedThinPlateBlock(Settings settings) {
        super(settings);
        this.setDefaultState(
                this.getStateManager().getDefaultState()
                        .with(ROT, 1)
                        .with(HAS_CAMO, false)
                        .with(FACING, Direction.DOWN)
                        .with(LAYERS, 1)
        );
    }

    @Override
    protected void appendProperties(StateManager.Builder<Block, BlockState> builder) {
        super.appendProperties(builder);
        builder.add(HAS_CAMO, FACING, LAYERS);
    }

    @Override
    protected MapCodec<? extends AbstractFramedEntityBlock> getCodec() {
        return CODEC;
    }

    @Override
    public BlockState getPlacementState(ItemPlacementContext ctx) {
        BlockState existing = ctx.getWorld().getBlockState(ctx.getBlockPos());
        if (existing.isOf(this)) {
            return existing.with(LAYERS, Math.min(MAX_LAYERS, existing.get(LAYERS) + 1));
        }

        BlockState state = super.getPlacementState(ctx);
        if (state == null) state = this.getDefaultState();

        return state.with(FACING, ctx.getSide().getOpposite());
    }

    @Override
    protected VoxelShape getOutlineShape(BlockState state, BlockView world, BlockPos pos, ShapeContext context) {
        return shapeFor(state, state.get(LAYERS));
    }

    @Override
    protected VoxelShape getCollisionShape(BlockState state, BlockView world, BlockPos pos, ShapeContext context) {
        return shapeFor(state, Math.max(0, state.get(LAYERS) - 1));
    }

    @Override
    protected VoxelShape getSidesShape(BlockState state, BlockView world, BlockPos pos) {
        return shapeFor(state, state.get(LAYERS));
    }

    @Override
    protected VoxelShape getCameraCollisionShape(BlockState state, BlockView world, BlockPos pos, ShapeContext context) {
        return shapeFor(state, state.get(LAYERS));
    }

    @Override
    protected VoxelShape getRaycastShape(BlockState state, BlockView world, BlockPos pos) {
        return shapeFor(state, state.get(LAYERS));
    }

    @Override
    protected boolean hasSidedTransparency(BlockState state) {
        return true;
    }

    @Override
    protected float getAmbientOcclusionLightLevel(BlockState state, BlockView world, BlockPos pos) {
        return state.get(LAYERS) == MAX_LAYERS ? 0.2f : 1.0f;
    }

    @Override
    protected boolean canReplace(BlockState state, ItemPlacementContext context) {
        int layers = state.get(LAYERS);
        if (context.getStack().isOf(this.asItem()) && layers < MAX_LAYERS) {
            return context.canReplaceExisting() ? context.getSide() == state.get(FACING).getOpposite() : true;
        }

        return !state.get(HAS_CAMO) && layers == 1;
    }

    @Override
    protected List<ItemStack> getDroppedStacks(BlockState state, LootWorldContext.Builder builder) {
        return List.of(new ItemStack(this, state.get(LAYERS)));
    }

    @Override
    protected ActionResult onUse(BlockState state, World world, BlockPos pos, PlayerEntity player, BlockHitResult hit) {
        FramedBlockEntity be = (world.getBlockEntity(pos) instanceof FramedBlockEntity fbe) ? fbe : null;
        return FramedUseHandler.handleUse(state, world, pos, player, hit, be);
    }

    private static VoxelShape shapeFor(BlockState state, int layers) {
        return switch (state.get(FACING)) {
            case DOWN -> DOWN_SHAPES[layers];
            case UP -> UP_SHAPES[layers];
            case NORTH -> NORTH_SHAPES[layers];
            case SOUTH -> SOUTH_SHAPES[layers];
            case WEST -> WEST_SHAPES[layers];
            case EAST -> EAST_SHAPES[layers];
        };
    }
}
