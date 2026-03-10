package com.spider.framedfabric.block;

import com.mojang.serialization.MapCodec;
import com.spider.framedfabric.blockentity.FramedBlockEntity;
import com.spider.framedfabric.blockentity.FramedUseHandler;
import com.spider.framedfabric.camo.FramedCamoLogic;
import com.spider.framedfabric.registry.ModBlockEntities;
import net.minecraft.block.Block;
import net.minecraft.block.BlockEntityProvider;
import net.minecraft.block.BlockState;
import net.minecraft.block.FenceGateBlock;
import net.minecraft.block.WoodType;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.state.StateManager;
import net.minecraft.state.property.BooleanProperty;
import net.minecraft.util.ActionResult;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import org.jetbrains.annotations.Nullable;

import static com.spider.framedfabric.blockentity.FramedProperties.ROT;

public class FramedFenceGateBlock extends FenceGateBlock implements BlockEntityProvider {
    public static final MapCodec<FramedFenceGateBlock> CODEC = createCodec(FramedFenceGateBlock::new);

    public static final BooleanProperty HAS_CAMO = BooleanProperty.of("has_camo");

    public FramedFenceGateBlock(Settings settings) {
        super(WoodType.OAK, settings);
        this.setDefaultState(this.getStateManager().getDefaultState()
                .with(HAS_CAMO, false)
                .with(ROT, 1)
        );
    }

    @Override
    protected void appendProperties(StateManager.Builder<Block, BlockState> builder) {
        super.appendProperties(builder);
        builder.add(HAS_CAMO, ROT);
    }

    @Override
    @SuppressWarnings({"unchecked", "rawtypes"})
    public MapCodec<FenceGateBlock> getCodec() {
        return (MapCodec) CODEC;
    }

    @Override
    public @Nullable BlockEntity createBlockEntity(BlockPos pos, BlockState state) {
        return new FramedBlockEntity(ModBlockEntities.FRAMED, pos, state);
    }

    @Override
    protected ActionResult onUse(BlockState state, World world, BlockPos pos, PlayerEntity player, BlockHitResult hit) {
        FramedBlockEntity be = (world.getBlockEntity(pos) instanceof FramedBlockEntity fbe) ? fbe : null;
        if (be == null) return super.onUse(state, world, pos, player, hit);

        // Handle framed tools/camo first. If it actually did something, consume the click.
        ActionResult framed = FramedUseHandler.handleUseOpenable(state, world, pos, player, hit, be);
        if (framed != ActionResult.PASS) return framed;

        // Otherwise: do vanilla gate open/close
        return super.onUse(state, world, pos, player, hit);
    }

    @Override
    public BlockState onBreak(World world, BlockPos pos, BlockState state, PlayerEntity player) {
        if (world instanceof ServerWorld sw) {
            if (sw.getBlockEntity(pos) instanceof FramedBlockEntity be && be.hasCamo()) {
                ItemStack camoDrop = FramedCamoLogic.camoRefundStack(be);
                if (!camoDrop.isEmpty()) Block.dropStack(sw, pos, camoDrop);
            }
        }
        return super.onBreak(world, pos, state, player);
    }
}
