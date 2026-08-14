package com.spider.framedfabric.compat.voxy;

import com.spider.framedfabric.camo.FramedCamoAccess;
import com.spider.framedfabric.registry.ModBlocks;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.block.BlockState;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.minecraft.world.chunk.Chunk;
import net.minecraft.world.chunk.ChunkSection;
import net.minecraft.world.chunk.WorldChunk;

import java.lang.reflect.Method;
import java.util.Map;

public final class VoxyCompat {
    private static final boolean VOXY_LOADED = FabricLoader.getInstance().isModLoaded("voxy");
    private static final ThreadLocal<WorldChunk> CURRENT_INGEST_CHUNK = new ThreadLocal<>();

    private static volatile Method tryAutoIngestChunk;

    private VoxyCompat() {}

    public static void beginChunkIngest(WorldChunk chunk) {
        if (VOXY_LOADED) {
            CURRENT_INGEST_CHUNK.set(chunk);
        }
    }

    public static void endChunkIngest() {
        if (VOXY_LOADED) {
            CURRENT_INGEST_CHUNK.remove();
        }
    }

    public static ChunkSection rewriteSectionForVoxy(ChunkSection section) {
        if (!VOXY_LOADED || section == null) {
            return section;
        }

        WorldChunk chunk = CURRENT_INGEST_CHUNK.get();
        if (chunk == null) {
            return section;
        }

        int sectionIndex = findSectionIndex(chunk, section);
        if (sectionIndex < 0) {
            return section;
        }

        int sectionY = chunk.getBottomSectionCoord() + sectionIndex;
        ChunkSection copy = null;

        for (Map.Entry<BlockPos, BlockEntity> entry : chunk.getBlockEntities().entrySet()) {
            BlockPos pos = entry.getKey();
            if ((pos.getY() >> 4) != sectionY) {
                continue;
            }

            if (!(entry.getValue() instanceof FramedCamoAccess camo)) {
                continue;
            }

            BlockState replacement = firstVoxyCamoState(camo);
            if (replacement == null) {
                continue;
            }

            if (copy == null) {
                copy = section.copy();
            }

            copy.setBlockState(pos.getX() & 15, pos.getY() & 15, pos.getZ() & 15, replacement, false);
        }

        return copy == null ? section : copy;
    }

    public static void refreshChunk(World world, BlockPos pos) {
        if (!VOXY_LOADED || world == null || pos == null || !world.isClient()) {
            return;
        }

        try {
            Chunk chunk = world.getChunk(pos);
            if (chunk instanceof WorldChunk worldChunk) {
                getTryAutoIngestChunk().invoke(null, worldChunk);
            }
        } catch (ReflectiveOperationException | LinkageError ignored) {
            // Voxy is optional and may change internals between releases.
        }
    }

    private static int findSectionIndex(WorldChunk chunk, ChunkSection section) {
        ChunkSection[] sections = chunk.getSectionArray();
        for (int i = 0; i < sections.length; i++) {
            if (sections[i] == section) {
                return i;
            }
        }
        return -1;
    }

    private static BlockState firstVoxyCamoState(FramedCamoAccess camo) {
        for (int i = 0; i < FramedCamoAccess.MAX_CAMO_PARTS; i++) {
            if (!camo.hasCamoPart(i)) {
                continue;
            }

            BlockState state = camo.getCamoPart(i);
            if (state == null || state.isAir() || ModBlocks.FRAMED_ALL.contains(state.getBlock())) {
                continue;
            }

            return state;
        }

        return null;
    }

    private static Method getTryAutoIngestChunk() throws ReflectiveOperationException {
        Method method = tryAutoIngestChunk;
        if (method == null) {
            method = Class.forName("me.cortex.voxy.common.world.service.VoxelIngestService")
                    .getMethod("tryAutoIngestChunk", WorldChunk.class);
            tryAutoIngestChunk = method;
        }
        return method;
    }
}
