package com.spider.framedfabric.block;

import com.spider.framedfabric.blockentity.FramedBlockEntity;
import com.spider.framedfabric.camo.FramedCamoLogic;
import com.spider.framedfabric.registry.ModBlockEntities;
import com.spider.framedfabric.registry.ModBlocks;
import com.spider.framedfabric.registry.ModItems;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.BlockWithEntity;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.state.StateManager;
import net.minecraft.state.property.IntProperty;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Hand;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import org.jetbrains.annotations.Nullable;

import static com.spider.framedfabric.registry.FramedTags.isHoldingFramedBlock;

public abstract class AbstractFramedEntityBlock extends BlockWithEntity {

    protected AbstractFramedEntityBlock(Settings settings) {
        super(settings);
    }

    // in AbstractFramedEntityBlock
    public static final IntProperty ROT = FramedProperties.ROT;

    @Override
    protected void appendProperties(StateManager.Builder<Block, BlockState> builder) {
        builder.add(ROT);
    }


    @Override
    public @Nullable BlockEntity createBlockEntity(BlockPos pos, BlockState state) {
        return new FramedBlockEntity(ModBlockEntities.FRAMED, pos, state);
    }


    protected BlockPos getCamoOwnerPos(BlockState state, World world, BlockPos pos) {
        return pos;
    }

    @Override
    protected ActionResult onUse(BlockState state, World world, BlockPos pos, PlayerEntity player, BlockHitResult hit) {
        FramedBlockEntity be = (world.getBlockEntity(pos) instanceof FramedBlockEntity fbe) ? fbe : null;
        return FramedUseHandler.handleUse(state, world, pos, player, hit, be);
    }

    @Override
    public BlockState onBreak(World world, BlockPos pos, BlockState state, PlayerEntity player) {
        if (world instanceof ServerWorld sw) {
            if (sw.getBlockEntity(pos) instanceof FramedBlockEntity be) {
                for (int i = 0; i < FramedBlockEntity.MAX_CAMO_PARTS; i++) {
                    if (be.hasCamoPart(i)) {
                        ItemStack camoDrop = FramedCamoLogic.camoRefundStack(be, i);
                        if (!camoDrop.isEmpty()) Block.dropStack(sw, pos, camoDrop);
                    }
                }
            }
        }

        return super.onBreak(world, pos, state, player);
    }
}
