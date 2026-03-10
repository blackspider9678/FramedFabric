package com.spider.framedfabric.block;

import com.mojang.serialization.MapCodec;
import com.spider.framedfabric.blockentity.AbstractFramedEntityBlock;
import com.spider.framedfabric.blockentity.FramedBlockEntity;
import com.spider.framedfabric.blockentity.FramedUseHandler;
import net.minecraft.block.*;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.fluid.FluidState;
import net.minecraft.fluid.Fluids;
import net.minecraft.item.BlockItem;
import net.minecraft.item.ItemPlacementContext;
import net.minecraft.item.ItemStack;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.state.StateManager;
import net.minecraft.state.property.BooleanProperty;
import net.minecraft.state.property.Properties;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Hand;
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

public class FramedFlowerPotBlock extends AbstractFramedEntityBlock implements Waterloggable {
    public static final MapCodec<FramedFlowerPotBlock> CODEC = createCodec(FramedFlowerPotBlock::new);

    public static final BooleanProperty WATERLOGGED = Properties.WATERLOGGED;

    // roughly vanilla-ish footprint
    private static final VoxelShape OUTLINE = Block.createCuboidShape(5, 0, 5, 11, 6, 11);

    public FramedFlowerPotBlock(Settings settings) {
        super(settings.nonOpaque());
        this.setDefaultState(
                this.getStateManager().getDefaultState()
                        .with(ROT, 1)
                        .with(HAS_CAMO, false)
                        .with(WATERLOGGED, false)
        );
    }

    @Override
    protected MapCodec<? extends AbstractFramedEntityBlock> getCodec() {
        return CODEC;
    }

    @Override
    protected void appendProperties(StateManager.Builder<Block, BlockState> builder) {
        super.appendProperties(builder);
        builder.add(HAS_CAMO, WATERLOGGED);
    }

    @Override
    public VoxelShape getOutlineShape(BlockState state, BlockView world, BlockPos pos, ShapeContext context) {
        return OUTLINE;
    }

    @Override
    public VoxelShape getCollisionShape(BlockState state, BlockView world, BlockPos pos, ShapeContext context) {
        return OUTLINE;
    }

    @Override
    public FluidState getFluidState(BlockState state) {
        return state.get(WATERLOGGED) ? Fluids.WATER.getStill(false) : super.getFluidState(state);
    }

    @Override
    public BlockState getPlacementState(ItemPlacementContext ctx) {
        FluidState fs = ctx.getWorld().getFluidState(ctx.getBlockPos());
        return this.getDefaultState()
                .with(WATERLOGGED, fs.getFluid() == Fluids.WATER);
    }

    @Override
    protected void neighborUpdate(BlockState state, World world, BlockPos pos,
                                  Block sourceBlock, @Nullable net.minecraft.world.block.WireOrientation wireOrientation,
                                  boolean notify) {
        if (state.get(WATERLOGGED)) {
            world.scheduleFluidTick(pos, Fluids.WATER, Fluids.WATER.getTickRate(world));
        }
        super.neighborUpdate(state, world, pos, sourceBlock, wireOrientation, notify);
    }

    @Override
    protected ActionResult onUse(BlockState state, World world, BlockPos pos, PlayerEntity player, BlockHitResult hit) {
        FramedBlockEntity be = (world.getBlockEntity(pos) instanceof FramedBlockEntity fbe) ? fbe : null;
        if (be == null) return ActionResult.PASS;

        ItemStack main = player.getStackInHand(Hand.MAIN_HAND);
        ItemStack off  = player.getStackInHand(Hand.OFF_HAND);

        // 0) Tools should always do camo behavior
        boolean hasWrench = main.isOf(com.spider.framedfabric.registry.ModItems.WRENCH) || off.isOf(com.spider.framedfabric.registry.ModItems.WRENCH);
        boolean hasHammer = main.isOf(com.spider.framedfabric.registry.ModItems.HAMMER) || off.isOf(com.spider.framedfabric.registry.ModItems.HAMMER);

        if (hasWrench || hasHammer) {
            return FramedUseHandler.handleUse(state, world, pos, player, hit, be);
        }

        // 1) Empty hand: remove plant if present
        if (main.isEmpty()) {
            if (!be.hasPotPlant()) return ActionResult.PASS;
            if (!world.isClient()) {
                ItemStack out = be.getPotPlantStack();
                be.setPotPlantStack(ItemStack.EMPTY);
                if (!player.getInventory().insertStack(out)) player.dropItem(out, false);
            }
            return ActionResult.SUCCESS;
        }

        // 2) If holding a framed block, allow placement (don’t eat interaction)
        if (com.spider.framedfabric.registry.FramedTags.isFramedStack(main)
                || com.spider.framedfabric.registry.FramedTags.isFramedStack(off)) {
            return ActionResult.PASS;
        }

        // 3) Try plant insert first (only if pot empty)
        if (!be.hasPotPlant() && main.getItem() instanceof BlockItem bi) {
            BlockState plantState = bi.getBlock().getDefaultState();
            if (isValidPottedPlant(plantState)) {
                if (!world.isClient()) {
                    be.setPotPlantStack(main);
                    if (!player.isCreative()) main.decrement(1);
                }
                return ActionResult.SUCCESS;
            }
        }

        // 4) Otherwise treat right-click as camo apply/removal (outer shell, part 0)
        ActionResult r = com.spider.framedfabric.camo.FramedCamoLogic.onUse(world, player, Hand.MAIN_HAND, be, 0, true);
        if (r == ActionResult.PASS) r = com.spider.framedfabric.camo.FramedCamoLogic.onUse(world, player, Hand.OFF_HAND, be, 0, true);
        return r;
    }

    // Modded-friendly: accept most PlantBlock-based things + common tags
    private static boolean isValidPottedPlant(BlockState s) {
        Block b = s.getBlock();

        // Most modded plants extend PlantBlock
        if (b instanceof net.minecraft.block.PlantBlock) return true;

        // Also accept common vanilla/mod tags if used
        if (s.isIn(net.minecraft.registry.tag.BlockTags.FLOWERS)) return true;
        if (s.isIn(net.minecraft.registry.tag.BlockTags.SAPLINGS)) return true;
        if (s.isIn(net.minecraft.registry.tag.BlockTags.CROPS)) return true;

        return false;
    }

    // keeps it “modded-friendly” without hardcoding block classes/tags
    private static boolean isValidPottedPlant(World world, BlockPos pos, BlockState state) {
        // reject blocks with collision
        if (!state.getCollisionShape(world, pos).isEmpty()) return false;

        // reject “full” stuff
        if (state.isOpaque()) return false;

        // enforce size bounds so you don’t pot a whole machine block
        var shape = state.getOutlineShape(world, pos);
        if (shape == VoxelShapes.fullCube() || shape.isEmpty()) return false;

        var bb = shape.getBoundingBox(); // 0..1
        // fit inside pot opening-ish
        if (bb.maxX > 0.875 || bb.minX < 0.125) return false;
        if (bb.maxZ > 0.875 || bb.minZ < 0.125) return false;
        if (bb.maxY > 1.0) return false;

        return true;
    }

    @Override
    public BlockState onBreak(World world, BlockPos pos, BlockState state, PlayerEntity player) {
        if (world instanceof ServerWorld sw) {
            if (sw.getBlockEntity(pos) instanceof FramedBlockEntity be) {
                // drop plant
                if (be.hasPotPlant()) {
                    Block.dropStack(sw, pos, be.getPotPlantStack());
                    be.setPotPlantStack(ItemStack.EMPTY);
                }
            }
        }
        return super.onBreak(world, pos, state, player);
    }
}
