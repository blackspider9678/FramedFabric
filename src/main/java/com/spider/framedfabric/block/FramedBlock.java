package com.spider.framedfabric.block;

import com.mojang.serialization.MapCodec;
import com.spider.framedfabric.registry.ModItems;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.state.StateManager;
import net.minecraft.state.property.BooleanProperty;
import net.minecraft.state.property.IntProperty;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Hand;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.minecraft.entity.player.PlayerEntity;

public class FramedBlock extends AbstractFramedEntityBlock {
    public static final MapCodec<FramedBlock> CODEC = createCodec(FramedBlock::new);
    public static final BooleanProperty HAS_CAMO = BooleanProperty.of("has_camo");

    public FramedBlock(Settings settings) {
        super(settings);
        this.setDefaultState(
                this.getStateManager().getDefaultState()
                        .with(AbstractFramedEntityBlock.ROT, 1)
                        .with(HAS_CAMO, false)
        );
    }

    @Override
    protected MapCodec<? extends AbstractFramedEntityBlock> getCodec() {
        return CODEC;
    }

    @Override
    protected void appendProperties(StateManager.Builder<Block, BlockState> builder) {
        super.appendProperties(builder);      // adds ROT
        builder.add(HAS_CAMO);                // adds has_camo
    }


    @Override
    protected ActionResult onUse(BlockState state, World world, BlockPos pos, PlayerEntity player, BlockHitResult hit) {
        // Wrench cycles ROT
        if (player.getStackInHand(Hand.MAIN_HAND).isOf(ModItems.WRENCH)
                || player.getStackInHand(Hand.OFF_HAND).isOf(ModItems.WRENCH)) {

            if (!world.isClient()) {
                int cur = state.get(AbstractFramedEntityBlock.ROT);
                int next = (cur >= 6) ? 1 : (cur + 1);
                world.setBlockState(pos, state.with(AbstractFramedEntityBlock.ROT, next), 3);
            }
            return ActionResult.SUCCESS;
        }

        // Otherwise run the normal framed interaction (hammer/camo)
        return super.onUse(state, world, pos, player, hit);
    }
}
