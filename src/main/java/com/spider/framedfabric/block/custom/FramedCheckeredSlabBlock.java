package com.spider.framedfabric.block.custom;

import com.mojang.serialization.MapCodec;
import com.spider.framedfabric.blockentity.AbstractFramedEntityBlock;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.ShapeContext;
import net.minecraft.block.SlabBlock;
import net.minecraft.block.Waterloggable;
import net.minecraft.block.enums.SlabType;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.fluid.FluidState;
import net.minecraft.fluid.Fluids;
import net.minecraft.item.ItemPlacementContext;
import net.minecraft.item.ItemStack;
import net.minecraft.state.StateManager;
import net.minecraft.state.property.BooleanProperty;
import net.minecraft.state.property.EnumProperty;
import net.minecraft.state.property.Properties;
import net.minecraft.util.ActionResult;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.shape.VoxelShape;
import net.minecraft.util.shape.VoxelShapes;
import net.minecraft.world.BlockView;
import net.minecraft.world.World;

import static com.spider.framedfabric.blockentity.FramedProperties.HAS_CAMO;

public class FramedCheckeredSlabBlock extends AbstractFramedEntityBlock implements Waterloggable {

    public static final MapCodec<FramedCheckeredSlabBlock> CODEC = createCodec(FramedCheckeredSlabBlock::new);

    public static final EnumProperty<SlabType> TYPE = SlabBlock.TYPE;
    public static final BooleanProperty WATERLOGGED = Properties.WATERLOGGED;

    private static final VoxelShape SHAPE_BOTTOM = Block.createCuboidShape(0, 0, 0, 16, 8, 16);
    private static final VoxelShape SHAPE_TOP    = Block.createCuboidShape(0, 8, 0, 16, 16, 16);

    public FramedCheckeredSlabBlock(Settings settings) {
        super(settings);
        this.setDefaultState(
                this.getStateManager().getDefaultState()
                        .with(ROT, 1)
                        .with(HAS_CAMO, false)
                        .with(WATERLOGGED, false)
                        .with(TYPE, SlabType.BOTTOM)
        );
    }

    @Override
    protected MapCodec<? extends AbstractFramedEntityBlock> getCodec() {
        return CODEC;
    }

    @Override
    protected void appendProperties(StateManager.Builder<Block, BlockState> builder) {
        super.appendProperties(builder);
        builder.add(HAS_CAMO, WATERLOGGED, TYPE);
    }

    @Override
    public BlockState getPlacementState(ItemPlacementContext ctx) {
        BlockPos pos = ctx.getBlockPos();
        World world = ctx.getWorld();
        BlockState existing = world.getBlockState(pos);

        // Merge into DOUBLE if placing onto existing checkered slab
        if (existing.isOf(this)) {
            return existing.with(TYPE, SlabType.DOUBLE).with(WATERLOGGED, false);
        }

        FluidState fluid = world.getFluidState(pos);
        boolean water = fluid.getFluid() == Fluids.WATER;

        Direction side = ctx.getSide();
        double ly = ctx.getHitPos().y - pos.getY(); // 0..1

        SlabType type;
        if (side == Direction.DOWN) {
            type = SlabType.TOP;
        } else if (side == Direction.UP) {
            type = SlabType.BOTTOM;
        } else {
            // side placement: use hit Y
            type = (ly > 0.5) ? SlabType.TOP : SlabType.BOTTOM;
        }

        return this.getDefaultState()
                .with(WATERLOGGED, water)
                .with(TYPE, type);
    }

    @Override
    protected boolean canReplace(BlockState state, ItemPlacementContext ctx) {
        if (state.get(TYPE) == SlabType.DOUBLE) return false;
        ItemStack stack = ctx.getStack();
        return stack.isOf(this.asItem());
    }

    // Shapes (optional but nice)
    @Override
    public VoxelShape getOutlineShape(BlockState state, BlockView world, BlockPos pos, ShapeContext context) {
        return switch (state.get(TYPE)) {
            case DOUBLE -> VoxelShapes.fullCube();
            case TOP    -> SHAPE_TOP;
            default     -> SHAPE_BOTTOM;
        };
    }

    @Override
    public VoxelShape getCollisionShape(BlockState state, BlockView world, BlockPos pos, ShapeContext context) {
        return getOutlineShape(state, world, pos, context);
    }

    @Override
    public VoxelShape getRaycastShape(BlockState state, BlockView world, BlockPos pos) {
        return switch (state.get(TYPE)) {
            case DOUBLE -> VoxelShapes.fullCube();
            case TOP    -> SHAPE_TOP;
            default     -> SHAPE_BOTTOM;
        };
    }

    @Override
    public FluidState getFluidState(BlockState state) {
        return state.get(WATERLOGGED) ? Fluids.WATER.getStill(false) : super.getFluidState(state);
    }

    @Override
    protected ActionResult onUse(BlockState state, World world, BlockPos pos, PlayerEntity player, BlockHitResult hit) {
        var be = (world.getBlockEntity(pos) instanceof com.spider.framedfabric.blockentity.FramedBlockEntity fbe) ? fbe : null;
        return com.spider.framedfabric.blockentity.FramedUseHandler.handleUse(state, world, pos, player, hit, be);
    }
}