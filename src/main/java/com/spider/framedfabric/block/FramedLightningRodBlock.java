package com.spider.framedfabric.block;

import com.mojang.serialization.MapCodec;
import com.spider.framedfabric.blockentity.FramedBlockEntity;
import com.spider.framedfabric.blockentity.FramedUseHandler;
import com.spider.framedfabric.camo.FramedCamoLogic;
import com.spider.framedfabric.registry.ModBlockEntities;
import net.minecraft.block.*;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.state.StateManager;
import net.minecraft.util.ActionResult;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.shape.VoxelShape;
import net.minecraft.world.BlockView;
import net.minecraft.world.World;
import org.jetbrains.annotations.Nullable;

import static com.spider.framedfabric.blockentity.FramedProperties.HAS_CAMO;
import static com.spider.framedfabric.blockentity.FramedProperties.ROT;

public class FramedLightningRodBlock extends LightningRodBlock implements BlockEntityProvider {
    public static final MapCodec<FramedLightningRodBlock> CODEC = createCodec(FramedLightningRodBlock::new);

    public FramedLightningRodBlock(Settings settings) {
        super(settings);

        // LightningRodBlock already sets defaults for FACING/WATERLOGGED/POWERED.
        // We add our extra props here.
        this.setDefaultState(this.getStateManager().getDefaultState()
                .with(HAS_CAMO, false)
                .with(ROT, 1)
        );
    }

    @Override
    public void setPowered(BlockState state, World world, BlockPos pos) {
        // TEMP DEBUG
        System.out.println("[FramedFabric] FramedLightningRodBlock#setPowered at " + pos);

        super.setPowered(state, world, pos);
    }

    @Override
    @SuppressWarnings({"unchecked", "rawtypes"})
    public MapCodec<LightningRodBlock> getCodec() {
        return (MapCodec) CODEC;
    }

    @Override
    protected void appendProperties(StateManager.Builder<Block, BlockState> builder) {
        super.appendProperties(builder); // adds FACING, POWERED, WATERLOGGED
        builder.add(HAS_CAMO, ROT);
    }

    @Override
    public @Nullable BlockEntity createBlockEntity(BlockPos pos, BlockState state) {
        return new FramedBlockEntity(ModBlockEntities.FRAMED, pos, state);
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

    @Override
    protected VoxelShape getCollisionShape(BlockState state, BlockView world, BlockPos pos, ShapeContext context) {
        // Use the same shape as outline so you can stand/bump into it
        return getOutlineShape(state, world, pos, context);
    }

    @Override
    protected VoxelShape getRaycastShape(BlockState state, BlockView world, BlockPos pos) {
        // Makes hit detection match the rod too (optional but feels right)
        return getOutlineShape(state, world, pos, ShapeContext.absent());
    }
}