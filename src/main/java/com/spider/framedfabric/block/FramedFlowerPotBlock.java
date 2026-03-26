package com.spider.framedfabric.block;

import com.mojang.serialization.MapCodec;
import com.spider.framedfabric.blockentity.AbstractFramedEntityBlock;
import com.spider.framedfabric.blockentity.FramedBlockEntity;
import com.spider.framedfabric.blockentity.FramedUseHandler;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SimpleWaterloggedBlock;
import net.minecraft.world.level.block.state.BlockBehaviour.Properties;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.level.material.Fluids;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.item.ItemStack;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.InteractionHand;
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

public class FramedFlowerPotBlock extends AbstractFramedEntityBlock implements SimpleWaterloggedBlock {
    public static final MapCodec<FramedFlowerPotBlock> CODEC = simpleCodec(FramedFlowerPotBlock::new);

    public static final BooleanProperty WATERLOGGED = BlockStateProperties.WATERLOGGED;

    // roughly vanilla-ish footprint
    private static final VoxelShape OUTLINE = Block.box(5, 0, 5, 11, 6, 11);

    public FramedFlowerPotBlock(Properties settings) {
        super(settings.noOcclusion());
        this.registerDefaultState(
                this.getStateDefinition().any()
                        .setValue(ROT, 1)
                        .setValue(HAS_CAMO, false)
                        .setValue(WATERLOGGED, false)
        );
    }

    @Override
    protected MapCodec<? extends AbstractFramedEntityBlock> codec() {
        return CODEC;
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        super.createBlockStateDefinition(builder);
        builder.add(HAS_CAMO, WATERLOGGED);
    }

    @Override
    public VoxelShape getShape(BlockState state, BlockGetter world, BlockPos pos, CollisionContext context) {
        return OUTLINE;
    }

    @Override
    public VoxelShape getCollisionShape(BlockState state, BlockGetter world, BlockPos pos, CollisionContext context) {
        return OUTLINE;
    }

    @Override
    public FluidState getFluidState(BlockState state) {
        return state.getValue(WATERLOGGED) ? Fluids.WATER.getSource(false) : super.getFluidState(state);
    }

    @Override
    public BlockState getStateForPlacement(BlockPlaceContext ctx) {
        FluidState fs = ctx.getLevel().getFluidState(ctx.getClickedPos());
        return this.defaultBlockState()
                .setValue(WATERLOGGED, fs.getType() == Fluids.WATER);
    }

    @Override
    protected void neighborChanged(BlockState state, Level world, BlockPos pos,
                                   Block sourceBlock, @Nullable net.minecraft.world.level.redstone.Orientation wireOrientation,
                                   boolean notify) {
        if (state.getValue(WATERLOGGED)) {
            world.scheduleTick(pos, Fluids.WATER, Fluids.WATER.getTickDelay(world));
        }
        super.neighborChanged(state, world, pos, sourceBlock, wireOrientation, notify);
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level world, BlockPos pos, Player player, BlockHitResult hit) {
        FramedBlockEntity be = (world.getBlockEntity(pos) instanceof FramedBlockEntity fbe) ? fbe : null;
        if (be == null) return InteractionResult.PASS;

        ItemStack main = player.getItemInHand(InteractionHand.MAIN_HAND);
        ItemStack off  = player.getItemInHand(InteractionHand.OFF_HAND);

        // 0) Tools should always do camo behavior
        boolean hasWrench = main.is(com.spider.framedfabric.registry.ModItems.WRENCH) || off.is(com.spider.framedfabric.registry.ModItems.WRENCH);
        boolean hasHammer = main.is(com.spider.framedfabric.registry.ModItems.HAMMER) || off.is(com.spider.framedfabric.registry.ModItems.HAMMER);

        if (hasWrench || hasHammer) {
            return FramedUseHandler.handleUse(state, world, pos, player, hit, be);
        }

        // 1) Empty hand: remove plant if present
        if (main.isEmpty()) {
            if (!be.hasPotPlant()) return InteractionResult.PASS;
            if (!world.isClientSide()) {
                ItemStack out = be.getPotPlantStack();
                be.setPotPlantStack(ItemStack.EMPTY);
                if (!player.getInventory().add(out)) player.drop(out, false);
            }
            return InteractionResult.SUCCESS;
        }

        // 2) If holding a framed block, allow placement (don’t eat interaction)
        if (com.spider.framedfabric.registry.FramedTags.isFramedStack(main)
                || com.spider.framedfabric.registry.FramedTags.isFramedStack(off)) {
            return InteractionResult.PASS;
        }

        // 3) Try plant insert first (only if pot empty)
        if (!be.hasPotPlant() && main.getItem() instanceof BlockItem bi) {
            BlockState plantState = bi.getBlock().defaultBlockState();
            if (isValidPottedPlant(plantState)) {
                if (!world.isClientSide()) {
                    be.setPotPlantStack(main);
                    if (!player.isCreative()) main.shrink(1);
                }
                return InteractionResult.SUCCESS;
            }
        }

        // 4) Otherwise treat right-click as camo apply/removal (outer shell, part 0)
        InteractionResult r = com.spider.framedfabric.camo.FramedCamoLogic.onUse(world, player, InteractionHand.MAIN_HAND, be, 0, true);
        if (r == InteractionResult.PASS) r = com.spider.framedfabric.camo.FramedCamoLogic.onUse(world, player, InteractionHand.OFF_HAND, be, 0, true);
        return r;
    }

    @Override
    protected InteractionResult useItemOn(
            ItemStack itemStack,
            BlockState state,
            Level world,
            BlockPos pos,
            Player player,
            InteractionHand hand,
            BlockHitResult hit
    ) {
        FramedBlockEntity be = (world.getBlockEntity(pos) instanceof FramedBlockEntity fbe) ? fbe : null;
        if (be == null) {
            return itemStack.isEmpty() ? InteractionResult.TRY_WITH_EMPTY_HAND : InteractionResult.PASS;
        }

        if (itemStack.is(com.spider.framedfabric.registry.ModItems.WRENCH)
                || itemStack.is(com.spider.framedfabric.registry.ModItems.HAMMER)) {
            return FramedUseHandler.handleUseItemOn(state, world, pos, player, hand, hit, be);
        }

        if (com.spider.framedfabric.registry.FramedTags.isFramedStack(itemStack)) {
            return InteractionResult.PASS;
        }

        if (itemStack.isEmpty()) {
            return InteractionResult.TRY_WITH_EMPTY_HAND;
        }

        if (!be.hasPotPlant() && itemStack.getItem() instanceof BlockItem blockItem) {
            BlockState plantState = blockItem.getBlock().defaultBlockState();
            if (isValidPottedPlant(plantState)) {
                if (!world.isClientSide()) {
                    be.setPotPlantStack(itemStack);
                    if (!player.isCreative()) itemStack.shrink(1);
                }
                return InteractionResult.SUCCESS;
            }
        }

        InteractionResult result = com.spider.framedfabric.camo.FramedCamoLogic.onUse(world, player, hand, be, 0, true);
        return (result == InteractionResult.PASS) ? InteractionResult.PASS : result;
    }

    // Modded-friendly: accept most PlantBlock-based things + common tags
    private static boolean isValidPottedPlant(BlockState s) {
        Block b = s.getBlock();

        // Most modded plants extend PlantBlock
        if (b instanceof net.minecraft.world.level.block.VegetationBlock) return true;

        // Also accept common vanilla/mod tags if used
        if (s.is(net.minecraft.tags.BlockTags.FLOWERS)) return true;
        if (s.is(net.minecraft.tags.BlockTags.SAPLINGS)) return true;
        if (s.is(net.minecraft.tags.BlockTags.CROPS)) return true;

        return false;
    }

    // keeps it “modded-friendly” without hardcoding block classes/tags
    private static boolean isValidPottedPlant(Level world, BlockPos pos, BlockState state) {
        // reject blocks with collision
        if (!state.getCollisionShape(world, pos).isEmpty()) return false;

        // reject “full” stuff
        if (state.canOcclude()) return false;

        // enforce size bounds so you don’t pot a whole machine block
        var shape = state.getShape(world, pos);
        if (shape == Shapes.block() || shape.isEmpty()) return false;

        var bb = shape.bounds(); // 0..1
        // fit inside pot opening-ish
        if (bb.maxX > 0.875 || bb.minX < 0.125) return false;
        if (bb.maxZ > 0.875 || bb.minZ < 0.125) return false;
        if (bb.maxY > 1.0) return false;

        return true;
    }

    @Override
    public BlockState playerWillDestroy(Level world, BlockPos pos, BlockState state, Player player) {
        if (world instanceof ServerLevel sw) {
            if (sw.getBlockEntity(pos) instanceof FramedBlockEntity be) {
                // drop plant
                if (be.hasPotPlant()) {
                    Block.popResource(sw, pos, be.getPotPlantStack());
                    be.setPotPlantStack(ItemStack.EMPTY);
                }
            }
        }
        return super.playerWillDestroy(world, pos, state, player);
    }
}
