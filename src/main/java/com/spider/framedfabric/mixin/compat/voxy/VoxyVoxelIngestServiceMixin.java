package com.spider.framedfabric.mixin.compat.voxy;

import com.spider.framedfabric.compat.voxy.VoxyCompat;
import net.minecraft.world.chunk.ChunkSection;
import net.minecraft.world.chunk.WorldChunk;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Coerce;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyArg;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(targets = "me.cortex.voxy.common.world.service.VoxelIngestService", remap = false)
public abstract class VoxyVoxelIngestServiceMixin {
    @Inject(method = "enqueueIngest", at = @At("HEAD"), remap = false)
    private void framedfabric$beginChunkIngest(@Coerce Object engine, WorldChunk chunk, CallbackInfoReturnable<Boolean> cir) {
        VoxyCompat.beginChunkIngest(chunk);
    }

    @Inject(method = "enqueueIngest", at = @At("RETURN"), remap = false)
    private void framedfabric$endChunkIngest(@Coerce Object engine, WorldChunk chunk, CallbackInfoReturnable<Boolean> cir) {
        VoxyCompat.endChunkIngest();
    }

    @ModifyArg(
            method = "enqueueIngest",
            at = @At(
                    value = "INVOKE",
                    target = "Lme/cortex/voxy/common/world/service/VoxelIngestService$IngestSection;<init>(IIILme/cortex/voxy/common/world/WorldEngine;Lnet/minecraft/class_2826;Lnet/minecraft/class_2804;Lnet/minecraft/class_2804;)V",
                    remap = false
            ),
            index = 4,
            remap = false
    )
    private ChunkSection framedfabric$rewriteSectionForVoxy(ChunkSection section) {
        return VoxyCompat.rewriteSectionForVoxy(section);
    }
}
