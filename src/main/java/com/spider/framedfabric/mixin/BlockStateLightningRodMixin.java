package com.spider.framedfabric.mixin;

import net.minecraft.block.AbstractBlock;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.registry.tag.BlockTags;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(AbstractBlock.AbstractBlockState.class)
public abstract class BlockStateLightningRodMixin {

    @Inject(
            method = "isOf(Lnet/minecraft/registry/entry/RegistryEntry;)Z",
            at = @At("HEAD"),
            cancellable = true
    )
    private void framedfabric$lightningRodIsOfTag(RegistryEntry<Block> entry, CallbackInfoReturnable<Boolean> cir) {
        // Only intercept checks that are explicitly asking: "is this a lightning rod?"
        if (entry.value() == Blocks.LIGHTNING_ROD) {
            BlockState self = (BlockState) (Object) this;

            // If our block is in the lightning rods tag, pretend it is a vanilla lightning rod
            if (self.isIn(BlockTags.LIGHTNING_RODS)) {
                cir.setReturnValue(true);
            }
        }
    }
}