package com.spider.framedfabric.block;

import com.mojang.serialization.MapCodec;
import com.spider.framedfabric.blockentity.FramedBlockEntity;
import com.spider.framedfabric.blockentity.FramedUseHandler;
import com.spider.framedfabric.camo.FramedCamoLogic;
import com.spider.framedfabric.registry.ModBlockEntities;
import net.minecraft.block.Block;
import net.minecraft.block.BlockEntityProvider;
import net.minecraft.block.BlockSetType;
import net.minecraft.block.BlockState;
import net.minecraft.block.DoorBlock;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.state.StateManager;
import net.minecraft.util.ActionResult;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import org.jetbrains.annotations.Nullable;

import static com.spider.framedfabric.blockentity.FramedProperties.HAS_CAMO;

public class FramedDoorBlock extends DoorBlock implements BlockEntityProvider {

    public static final MapCodec<FramedDoorBlock> CODEC = createCodec(FramedDoorBlock::new);

    public FramedDoorBlock(Settings settings) {
        super(BlockSetType.OAK, settings);
        this.setDefaultState(this.getStateManager().getDefaultState().with(HAS_CAMO, false));
    }

    @Override
    @SuppressWarnings({"unchecked", "rawtypes"})
    public MapCodec<DoorBlock> getCodec() {
        return (MapCodec) CODEC;
    }

    @Override
    protected void appendProperties(StateManager.Builder<Block, BlockState> builder) {
        super.appendProperties(builder);
        builder.add(HAS_CAMO);
    }

    /**
     * BlockEntity exists on BOTH halves now.
     */
    @Override
    public @Nullable BlockEntity createBlockEntity(BlockPos pos, BlockState state) {
        return new FramedBlockEntity(ModBlockEntities.FRAMED, pos, state);
    }

    @Override
    public ActionResult onUse(BlockState state, World world, BlockPos pos, PlayerEntity player, BlockHitResult hit) {
        FramedBlockEntity be = (world.getBlockEntity(pos) instanceof FramedBlockEntity fbe) ? fbe : null;
        if (be == null) {
            return super.onUse(state, world, pos, player, hit);
        }

        // Let your shared framed/openable logic try first.
        // - Wrench rotates camo (if present) and blocks door open
        // - Hammer refunds/clears camo (if present) and blocks door open
        // - Applying camo when empty blocks door open
        // - Otherwise PASS so door can open/close normally
        ActionResult framed = FramedUseHandler.handleUseOpenable(state, world, pos, player, hit, be);
        if (framed != ActionResult.PASS) return framed;

        return super.onUse(state, world, pos, player, hit);
    }

    @Override
    public BlockState onBreak(World world, BlockPos pos, BlockState state, PlayerEntity player) {
        // With BE on both halves:
        // - refund camo for THIS half only
        // - vanilla will break the other half too -> it will refund its own camo if it has one
        if (world instanceof ServerWorld sw) {
            if (sw.getBlockEntity(pos) instanceof FramedBlockEntity be && be.hasCamo()) {
                var camoDrop = FramedCamoLogic.camoRefundStack(be);
                if (!camoDrop.isEmpty()) Block.dropStack(sw, pos, camoDrop);
            }
        }
        return super.onBreak(world, pos, state, player);
    }
}
