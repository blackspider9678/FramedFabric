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
import net.minecraft.block.ButtonBlock;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.state.StateManager;
import net.minecraft.util.ActionResult;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import org.jetbrains.annotations.Nullable;

import static com.spider.framedfabric.blockentity.FramedProperties.HAS_CAMO;
import static com.spider.framedfabric.blockentity.FramedProperties.ROT;

public class FramedButtonBlock extends ButtonBlock implements BlockEntityProvider {

    public static final MapCodec<FramedButtonBlock> CODEC = createCodec(FramedButtonBlock::new);

    public FramedButtonBlock(Settings settings) {
        super(BlockSetType.OAK, 30, settings);

        this.setDefaultState(this.getStateManager().getDefaultState()
                .with(POWERED, false)
                .with(HAS_CAMO, false)
                .with(ROT, 1)
        );

    }

    @Override
    @SuppressWarnings({"unchecked", "rawtypes"})
    public MapCodec<ButtonBlock> getCodec() {
        return (MapCodec) CODEC;
    }

    @Override
    protected void appendProperties(StateManager.Builder<Block, BlockState> builder) {
        super.appendProperties(builder);
        builder.add(HAS_CAMO, ROT);
    }

    @Override
    public @Nullable BlockEntity createBlockEntity(BlockPos pos, BlockState state) {
        return new FramedBlockEntity(ModBlockEntities.FRAMED, pos, state);
    }

    // NOTE: ButtonBlock's onUse has NO Hand parameter in your version.
    @Override
    protected ActionResult onUse(BlockState state, World world, BlockPos pos,
                                 PlayerEntity player, BlockHitResult hit) {

        FramedBlockEntity be = (world.getBlockEntity(pos) instanceof FramedBlockEntity fbe) ? fbe : null;

        // Run framed logic first; if it doesn't consume, let vanilla button press happen.
        ActionResult r = FramedUseHandler.handleUseVanillaInteractive(state, world, pos, player, hit, be);
        return (r == ActionResult.PASS) ? super.onUse(state, world, pos, player, hit) : r;
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
