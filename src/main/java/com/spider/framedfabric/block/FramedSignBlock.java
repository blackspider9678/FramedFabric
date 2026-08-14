package com.spider.framedfabric.block;

import com.mojang.serialization.MapCodec;
import com.spider.framedfabric.blockentity.FramedSignBlockEntity;
import com.spider.framedfabric.blockentity.FramedUseHandler;
import com.spider.framedfabric.camo.FramedCamoAccess;
import com.spider.framedfabric.camo.FramedCamoLogic;
import com.spider.framedfabric.registry.ModBlockEntities;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.SignBlock;
import net.minecraft.block.WoodType;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.block.entity.BlockEntityTicker;
import net.minecraft.block.entity.BlockEntityType;
import net.minecraft.block.entity.SignBlockEntity;
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

public class FramedSignBlock extends SignBlock {
    public static final MapCodec<FramedSignBlock> CODEC = createCodec(settings -> new FramedSignBlock(WoodType.OAK, settings));

    public FramedSignBlock(WoodType woodType, Settings settings) {
        super(woodType, settings);
        this.setDefaultState(this.getStateManager().getDefaultState().with(HAS_CAMO, false));
    }

    @Override
    @SuppressWarnings({"unchecked", "rawtypes"})
    public MapCodec<SignBlock> getCodec() {
        return (MapCodec) CODEC;
    }

    @Override
    protected void appendProperties(StateManager.Builder<Block, BlockState> builder) {
        super.appendProperties(builder);
        builder.add(HAS_CAMO);
    }

    @Override
    public @Nullable BlockEntity createBlockEntity(BlockPos pos, BlockState state) {
        return new FramedSignBlockEntity(pos, state);
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
    protected ActionResult onUse(BlockState state, World world, BlockPos pos, PlayerEntity player, BlockHitResult hit) {
        ActionResult framed = FramedUseHandler.handleUseBeforeVanilla(state, world, pos, player, hit, camoAccess(world, pos));
        if (framed != ActionResult.PASS) return framed;
        return super.onUse(state, world, pos, player, hit);
    }

    @Override
    public BlockState onBreak(World world, BlockPos pos, BlockState state, PlayerEntity player) {
        refundCamo(world, pos);
        return super.onBreak(world, pos, state, player);
    }

    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(World world, BlockState state, BlockEntityType<T> type) {
        return validateTicker(type, ModBlockEntities.FRAMED_SIGN, SignBlockEntity::tick);
    }

    private static @Nullable FramedCamoAccess camoAccess(World world, BlockPos pos) {
        return world.getBlockEntity(pos) instanceof FramedCamoAccess camo ? camo : null;
    }

    protected static void refundCamo(World world, BlockPos pos) {
        if (!(world instanceof ServerWorld serverWorld)) return;
        if (!(serverWorld.getBlockEntity(pos) instanceof FramedCamoAccess camo)) return;

        for (int i = 0; i < FramedCamoAccess.MAX_CAMO_PARTS; i++) {
            if (!camo.hasCamoPart(i)) continue;

            ItemStack camoDrop = FramedCamoLogic.camoRefundStack(camo, i);
            if (!camoDrop.isEmpty()) {
                Block.dropStack(serverWorld, pos, camoDrop);
            }
        }
    }
}
