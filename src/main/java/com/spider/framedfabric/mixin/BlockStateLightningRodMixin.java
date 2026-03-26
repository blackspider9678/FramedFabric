package com.spider.framedfabric.mixin;

import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.core.Holder;
import net.minecraft.tags.BlockTags;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(BlockBehaviour.BlockStateBase.class)
public abstract class BlockStateLightningRodMixin {

    @Inject(
            method = "is(Lnet/minecraft/core/Holder;)Z",
            at = @At("HEAD"),
            cancellable = true
    )
    private void framedfabric$lightningRodIsOfTag(Holder<Block> entry, CallbackInfoReturnable<Boolean> cir) {
        // Only intercept checks that are explicitly asking: "is this a lightning rod?"
        if (entry.value() == Blocks.LIGHTNING_ROD) {
            BlockState self = (BlockState) (Object) this;

            // If our block is in the lightning rods tag, pretend it is a vanilla lightning rod
            if (self.is(BlockTags.LIGHTNING_RODS)) {
                cir.setReturnValue(true);
            }
        }
    }
}