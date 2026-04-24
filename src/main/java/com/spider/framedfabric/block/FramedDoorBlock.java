package com.spider.framedfabric.block;

import com.mojang.serialization.MapCodec;
import com.spider.framedfabric.blockentity.FramedBlockEntity;
import com.spider.framedfabric.blockentity.FramedUseHandler;
import com.spider.framedfabric.camo.FramedCamoLogic;
import com.spider.framedfabric.registry.ModBlockEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.DoorBlock;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockSetType;
import net.minecraft.world.phys.BlockHitResult;
import org.jetbrains.annotations.Nullable;

import static com.spider.framedfabric.blockentity.FramedProperties.HAS_CAMO;

public class FramedDoorBlock extends DoorBlock implements EntityBlock {

    public static final MapCodec<FramedDoorBlock> CODEC = simpleCodec(FramedDoorBlock::new);

    public FramedDoorBlock(Properties settings) {
        super(BlockSetType.OAK, settings);
        this.registerDefaultState(this.getStateDefinition().any().setValue(HAS_CAMO, false));
    }

    @Override
    @SuppressWarnings({"unchecked", "rawtypes"})
    public MapCodec<DoorBlock> codec() {
        return (MapCodec) CODEC;
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        super.createBlockStateDefinition(builder);
        builder.add(HAS_CAMO);
    }

    /**
     * BlockEntity exists on BOTH halves now.
     */
    @Override
    public @Nullable BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new FramedBlockEntity(ModBlockEntities.FRAMED, pos, state);
    }

    @Override
    public InteractionResult useWithoutItem(BlockState state, Level world, BlockPos pos, Player player, BlockHitResult hit) {
        FramedBlockEntity be = (world.getBlockEntity(pos) instanceof FramedBlockEntity fbe) ? fbe : null;
        if (be == null) {
            return super.useWithoutItem(state, world, pos, player, hit);
        }

        // Let your shared framed/openable logic try first.
        // - Wrench rotates camo (if present) and blocks door open
        // - Hammer refunds/clears camo (if present) and blocks door open
        // - Applying camo when empty blocks door open
        // - Otherwise PASS so door can open/close normally
        InteractionResult framed = FramedUseHandler.handleUseOpenable(state, world, pos, player, hit, be);
        if (framed != InteractionResult.PASS) return framed;

        return super.useWithoutItem(state, world, pos, player, hit);
    }

    @Override
    public BlockState playerWillDestroy(Level world, BlockPos pos, BlockState state, Player player) {
        // With BE on both halves:
        // - refund camo for THIS half only
        // - vanilla will break the other half too -> it will refund its own camo if it has one
        if (world instanceof ServerLevel sw) {
            if (sw.getBlockEntity(pos) instanceof FramedBlockEntity be && be.hasCamo()) {
                var camoDrop = FramedCamoLogic.camoRefundStack(be);
                if (!camoDrop.isEmpty()) Block.popResource(sw, pos, camoDrop);
            }
        }
        return super.playerWillDestroy(world, pos, state, player);
    }
}
