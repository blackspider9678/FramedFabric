package com.spider.framedfabric.block;

import com.mojang.serialization.MapCodec;
import com.spider.framedfabric.blockentity.FramedBlockEntity;
import com.spider.framedfabric.camo.FramedCamoLogic;
import com.spider.framedfabric.registry.ModItems;
import net.minecraft.block.Block;
import net.minecraft.block.BlockEntityProvider;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.block.StairsBlock;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.state.StateManager;
import net.minecraft.state.property.BooleanProperty;
import net.minecraft.state.property.IntProperty;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Hand;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import org.jetbrains.annotations.Nullable;

public class FramedStairsBlock extends StairsBlock implements BlockEntityProvider {
    // IMPORTANT: baseState must NOT be null
    public static final MapCodec<FramedStairsBlock> CODEC =
            createCodec(s -> new FramedStairsBlock(Blocks.OAK_PLANKS.getDefaultState(), s));

    public static final BooleanProperty HAS_CAMO = BooleanProperty.of("has_camo");
    public static final IntProperty ROT = IntProperty.of("rot", 1, 6);

    public FramedStairsBlock(BlockState baseState, Settings settings) {
        super(baseState, settings);

        // StairsBlock already has FACING + HALF + SHAPE + WATERLOGGED
        this.setDefaultState(this.getStateManager().getDefaultState()
                .with(HAS_CAMO, false)
                .with(ROT, 1)
        );
    }

    // Same codec-cast trick as slab (keeps 1.21.11 happy)
    @Override
    @SuppressWarnings({"unchecked", "rawtypes"})
    public MapCodec<StairsBlock> getCodec() {
        return (MapCodec) CODEC;
    }

    @Override
    protected void appendProperties(StateManager.Builder<Block, BlockState> builder) {
        super.appendProperties(builder);
        builder.add(HAS_CAMO, ROT);
    }

    @Override
    public @Nullable BlockEntity createBlockEntity(BlockPos pos, BlockState state) {
        return new FramedBlockEntity(pos, state);
    }

    @Override
    protected ActionResult onUse(BlockState state, World world, BlockPos pos, PlayerEntity player, BlockHitResult hit) {
        // Wrench cycles ROT
        if (player.getStackInHand(Hand.MAIN_HAND).isOf(ModItems.WRENCH)
                || player.getStackInHand(Hand.OFF_HAND).isOf(ModItems.WRENCH)) {

            if (!world.isClient()) {
                int cur = state.get(ROT);
                int next = (cur >= 6) ? 1 : (cur + 1);
                world.setBlockState(pos, state.with(ROT, next), 3);
            }
            return ActionResult.SUCCESS;
        }

        // Camo / hammer logic
        if (!(world.getBlockEntity(pos) instanceof FramedBlockEntity be)) return ActionResult.PASS;

        ActionResult r = FramedCamoLogic.onUse(world, player, Hand.MAIN_HAND, be, true);
        if (r == ActionResult.PASS) {
            r = FramedCamoLogic.onUse(world, player, Hand.OFF_HAND, be, true);
        }
        return r;
    }

    @Override
    public BlockState onBreak(World world, BlockPos pos, BlockState state, PlayerEntity player) {
        // Drop stored camo item on break (extra)
        if (world instanceof ServerWorld sw) {
            if (sw.getBlockEntity(pos) instanceof FramedBlockEntity be && be.hasCamo()) {
                ItemStack camoDrop = FramedCamoLogic.camoRefundStack(be);
                if (!camoDrop.isEmpty()) {
                    Block.dropStack(sw, pos, camoDrop);
                }
            }
        }
        return super.onBreak(world, pos, state, player);
    }
}
