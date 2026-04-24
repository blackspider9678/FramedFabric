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
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.TrapDoorBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockSetType;
import net.minecraft.world.phys.BlockHitResult;
import org.jetbrains.annotations.Nullable;

import static com.spider.framedfabric.blockentity.FramedProperties.HAS_CAMO;
import static com.spider.framedfabric.blockentity.FramedProperties.ROT;

public class FramedTrapdoorBlock extends TrapDoorBlock implements EntityBlock {
    public static final MapCodec<FramedTrapdoorBlock> CODEC = simpleCodec(FramedTrapdoorBlock::new);

    public FramedTrapdoorBlock(Properties settings) {
        super(BlockSetType.OAK, settings); // change BlockSetType if desired
        this.registerDefaultState(this.getStateDefinition().any()
                .setValue(HAS_CAMO, false)
                .setValue(ROT, 1)
        );
    }

    @Override
    @SuppressWarnings({"unchecked", "rawtypes"})
    public MapCodec<TrapDoorBlock> codec() {
        return (MapCodec) CODEC;
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        super.createBlockStateDefinition(builder);
        builder.add(HAS_CAMO, ROT);
    }

    @Override
    public @Nullable BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new FramedBlockEntity(ModBlockEntities.FRAMED, pos, state);
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level world, BlockPos pos, Player player, BlockHitResult hit) {
        FramedBlockEntity be = (world.getBlockEntity(pos) instanceof FramedBlockEntity fbe) ? fbe : null;

        // Run your “openable” framed handler first (wrench/hammer/apply camo),
        // and only fall back to vanilla trapdoor toggle if it returns PASS.
        InteractionResult r = FramedUseHandler.handleUseOpenable(state, world, pos, player, hit, be);
        if (r != InteractionResult.PASS) return r;

        return super.useWithoutItem(state, world, pos, player, hit);
    }

    @Override
    public BlockState playerWillDestroy(Level world, BlockPos pos, BlockState state, Player player) {
        if (world instanceof ServerLevel sw) {
            if (sw.getBlockEntity(pos) instanceof FramedBlockEntity be && be.hasCamo()) {
                ItemStack camoDrop = FramedCamoLogic.camoRefundStack(be);
                if (!camoDrop.isEmpty()) Block.popResource(sw, pos, camoDrop);
            }
        }
        return super.playerWillDestroy(world, pos, state, player);
    }
}
