package com.spider.framedfabric.block;

import com.mojang.serialization.MapCodec;
import com.spider.framedfabric.blockentity.FramedUseHandler;
import com.spider.framedfabric.camo.FramedCamoAccess;
import com.spider.framedfabric.camo.FramedCamoLogic;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.ShelfBlock;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.block.entity.ShelfBlockEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.state.StateManager;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Hand;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import org.jetbrains.annotations.Nullable;

import static com.spider.framedfabric.blockentity.FramedProperties.HAS_CAMO;

public class FramedShelfBlock extends ShelfBlock {
    public static final MapCodec<FramedShelfBlock> CODEC = createCodec(FramedShelfBlock::new);

    public FramedShelfBlock(Settings settings) {
        super(settings);
        this.setDefaultState(this.getStateManager().getDefaultState().with(HAS_CAMO, false));
    }

    @Override
    @SuppressWarnings({"unchecked", "rawtypes"})
    public MapCodec<ShelfBlock> getCodec() {
        return (MapCodec) CODEC;
    }

    @Override
    protected void appendProperties(StateManager.Builder<Block, BlockState> builder) {
        super.appendProperties(builder);
        builder.add(HAS_CAMO);
    }

    @Override
    public @Nullable BlockEntity createBlockEntity(BlockPos pos, BlockState state) {
        return new ShelfBlockEntity(pos, state);
    }

    @Override
    protected ActionResult onUseWithItem(
            ItemStack stack,
            BlockState state,
            World world,
            BlockPos pos,
            PlayerEntity player,
            Hand hand,
            BlockHitResult hit
    ) {
        ActionResult framed = FramedUseHandler.handleUseBeforeVanilla(state, world, pos, player, hit, camoAccess(world, pos));
        if (framed != ActionResult.PASS) return framed;
        return super.onUseWithItem(stack, state, world, pos, player, hand, hit);
    }

    @Override
    public BlockState onBreak(World world, BlockPos pos, BlockState state, PlayerEntity player) {
        if (world instanceof ServerWorld serverWorld && serverWorld.getBlockEntity(pos) instanceof FramedCamoAccess camo) {
            for (int i = 0; i < FramedCamoAccess.MAX_CAMO_PARTS; i++) {
                if (!camo.hasCamoPart(i)) continue;

                ItemStack camoDrop = FramedCamoLogic.camoRefundStack(camo, i);
                if (!camoDrop.isEmpty()) {
                    Block.dropStack(serverWorld, pos, camoDrop);
                }
            }
        }

        return super.onBreak(world, pos, state, player);
    }

    private static @Nullable FramedCamoAccess camoAccess(World world, BlockPos pos) {
        return world.getBlockEntity(pos) instanceof FramedCamoAccess camo ? camo : null;
    }
}
