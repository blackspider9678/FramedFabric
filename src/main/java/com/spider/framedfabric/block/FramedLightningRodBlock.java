package com.spider.framedfabric.block;

import com.mojang.serialization.MapCodec;
import com.spider.framedfabric.blockentity.FramedBlockEntity;
import com.spider.framedfabric.blockentity.FramedUseHandler;
import com.spider.framedfabric.camo.FramedCamoLogic;
import com.spider.framedfabric.registry.ModBlockEntities;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.LightningRodBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.state.BlockBehaviour.Properties;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.core.BlockPos;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.Nullable;

import static com.spider.framedfabric.blockentity.FramedProperties.HAS_CAMO;
import static com.spider.framedfabric.blockentity.FramedProperties.ROT;

public class FramedLightningRodBlock extends LightningRodBlock implements EntityBlock {
    public static final MapCodec<FramedLightningRodBlock> CODEC = simpleCodec(FramedLightningRodBlock::new);

    public FramedLightningRodBlock(Properties settings) {
        super(settings);

        // LightningRodBlock already sets defaults for FACING/WATERLOGGED/POWERED.
        // We add our extra props here.
        this.registerDefaultState(this.getStateDefinition().any()
                .setValue(HAS_CAMO, false)
                .setValue(ROT, 1)
        );
    }

    @Override
    public void onLightningStrike(BlockState state, Level world, BlockPos pos) {
        // TEMP DEBUG
        System.out.println("[FramedFabric] FramedLightningRodBlock#setPowered at " + pos);

        super.onLightningStrike(state, world, pos);
    }

    @Override
    @SuppressWarnings({"unchecked", "rawtypes"})
    public MapCodec<LightningRodBlock> codec() {
        return (MapCodec) CODEC;
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        super.createBlockStateDefinition(builder); // adds FACING, POWERED, WATERLOGGED
        builder.add(HAS_CAMO, ROT);
    }

    @Override
    public @Nullable BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new FramedBlockEntity(ModBlockEntities.FRAMED, pos, state);
    }

    @Override
    protected net.minecraft.world.level.block.RenderShape getRenderShape(BlockState state) {
        return com.spider.framedfabric.blockentity.FramedProperties.hasCamo(state)
                ? net.minecraft.world.level.block.RenderShape.INVISIBLE
                : net.minecraft.world.level.block.RenderShape.MODEL;
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level world, BlockPos pos, Player player, BlockHitResult hit) {
        FramedBlockEntity be = (world.getBlockEntity(pos) instanceof FramedBlockEntity fbe) ? fbe : null;
        return FramedUseHandler.handleUse(state, world, pos, player, hit, be);
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
        return FramedUseHandler.handleUseItemOn(state, world, pos, player, hand, hit, be);
    }

    @Override
    public BlockState playerWillDestroy(Level world, BlockPos pos, BlockState state, Player player) {
        if (world instanceof ServerLevel sw) {
            if (sw.getBlockEntity(pos) instanceof FramedBlockEntity be) {
                for (int i = 0; i < FramedBlockEntity.MAX_CAMO_PARTS; i++) {
                    if (be.hasCamoPart(i)) {
                        ItemStack camoDrop = FramedCamoLogic.camoRefundStack(be, i);
                        if (!camoDrop.isEmpty()) Block.popResource(sw, pos, camoDrop);
                    }
                }
            }
        }
        return super.playerWillDestroy(world, pos, state, player);
    }

    @Override
    protected VoxelShape getCollisionShape(BlockState state, BlockGetter world, BlockPos pos, CollisionContext context) {
        // Use the same shape as outline so you can stand/bump into it
        return getShape(state, world, pos, context);
    }

    @Override
    protected VoxelShape getInteractionShape(BlockState state, BlockGetter world, BlockPos pos) {
        // Makes hit detection match the rod too (optional but feels right)
        return getShape(state, world, pos, CollisionContext.empty());
    }
}
