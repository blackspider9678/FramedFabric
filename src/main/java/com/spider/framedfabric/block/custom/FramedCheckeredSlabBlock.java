package com.spider.framedfabric.block.custom;

import com.mojang.serialization.MapCodec;
import com.spider.framedfabric.blockentity.AbstractFramedEntityBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockBehaviour.Properties;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.level.block.SlabBlock;
import net.minecraft.world.level.block.SimpleWaterloggedBlock;
import net.minecraft.world.level.block.state.properties.SlabType;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.level.material.Fluids;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;

import static com.spider.framedfabric.blockentity.FramedProperties.HAS_CAMO;

public class FramedCheckeredSlabBlock extends AbstractFramedEntityBlock implements SimpleWaterloggedBlock {

    public static final MapCodec<FramedCheckeredSlabBlock> CODEC = simpleCodec(FramedCheckeredSlabBlock::new);

    public static final EnumProperty<SlabType> TYPE = SlabBlock.TYPE;
    public static final BooleanProperty WATERLOGGED = BlockStateProperties.WATERLOGGED;

    private static final VoxelShape SHAPE_BOTTOM = Block.box(0, 0, 0, 16, 8, 16);
    private static final VoxelShape SHAPE_TOP    = Block.box(0, 8, 0, 16, 16, 16);

    public FramedCheckeredSlabBlock(Properties settings) {
        super(settings);
        this.registerDefaultState(
                this.getStateDefinition().any()
                        .setValue(ROT, 1)
                        .setValue(HAS_CAMO, false)
                        .setValue(WATERLOGGED, false)
                        .setValue(TYPE, SlabType.BOTTOM)
        );
    }

    @Override
    protected MapCodec<? extends AbstractFramedEntityBlock> codec() {
        return CODEC;
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        super.createBlockStateDefinition(builder);
        builder.add(HAS_CAMO, WATERLOGGED, TYPE);
    }

    @Override
    public BlockState getStateForPlacement(BlockPlaceContext ctx) {
        BlockPos pos = ctx.getClickedPos();
        Level world = ctx.getLevel();
        BlockState existing = world.getBlockState(pos);

        // Merge into DOUBLE if placing onto existing checkered slab
        if (existing.is(this)) {
            return existing.setValue(TYPE, SlabType.DOUBLE).setValue(WATERLOGGED, false);
        }

        FluidState fluid = world.getFluidState(pos);
        boolean water = fluid.getType() == Fluids.WATER;

        Direction side = ctx.getClickedFace();
        double ly = ctx.getClickLocation().y - pos.getY(); // 0..1

        SlabType type;
        if (side == Direction.DOWN) {
            type = SlabType.TOP;
        } else if (side == Direction.UP) {
            type = SlabType.BOTTOM;
        } else {
            // side placement: use hit Y
            type = (ly > 0.5) ? SlabType.TOP : SlabType.BOTTOM;
        }

        return this.defaultBlockState()
                .setValue(WATERLOGGED, water)
                .setValue(TYPE, type);
    }

    @Override
    protected boolean canBeReplaced(BlockState state, BlockPlaceContext ctx) {
        if (state.getValue(TYPE) == SlabType.DOUBLE) return false;
        ItemStack stack = ctx.getItemInHand();
        return stack.is(this.asItem());
    }

    // Shapes (optional but nice)
    @Override
    public VoxelShape getShape(BlockState state, BlockGetter world, BlockPos pos, CollisionContext context) {
        return switch (state.getValue(TYPE)) {
            case DOUBLE -> Shapes.block();
            case TOP    -> SHAPE_TOP;
            default     -> SHAPE_BOTTOM;
        };
    }

    @Override
    public VoxelShape getCollisionShape(BlockState state, BlockGetter world, BlockPos pos, CollisionContext context) {
        return getShape(state, world, pos, context);
    }

    @Override
    public VoxelShape getInteractionShape(BlockState state, BlockGetter world, BlockPos pos) {
        return switch (state.getValue(TYPE)) {
            case DOUBLE -> Shapes.block();
            case TOP    -> SHAPE_TOP;
            default     -> SHAPE_BOTTOM;
        };
    }

    @Override
    public FluidState getFluidState(BlockState state) {
        return state.getValue(WATERLOGGED) ? Fluids.WATER.getSource(false) : super.getFluidState(state);
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level world, BlockPos pos, Player player, BlockHitResult hit) {
        var be = (world.getBlockEntity(pos) instanceof com.spider.framedfabric.blockentity.FramedBlockEntity fbe) ? fbe : null;
        return com.spider.framedfabric.blockentity.FramedUseHandler.handleUse(state, world, pos, player, hit, be);
    }
}