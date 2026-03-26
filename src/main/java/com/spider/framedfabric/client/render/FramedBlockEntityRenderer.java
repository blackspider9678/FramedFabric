package com.spider.framedfabric.client.render;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.QuadInstance;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.spider.framedfabric.FramedFabric;
import com.spider.framedfabric.block.FramedMiniCubeBlock;
import com.spider.framedfabric.block.custom.FramedCheckeredBlock;
import com.spider.framedfabric.block.custom.FramedCheckeredSlabBlock;
import com.spider.framedfabric.block.custom.FramedCheckeredVerticalSlabBlock;
import com.spider.framedfabric.block.custom.FramedVerticalSlabBlock;
import com.spider.framedfabric.blockentity.FramedBlockEntity;
import com.spider.framedfabric.blockentity.FramedProperties;
import com.spider.framedfabric.registry.ModBlocks;
import net.minecraft.client.Minecraft;
import net.minecraft.client.color.block.BlockTintSource;
import net.minecraft.client.model.geom.builders.UVPair;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.block.BlockAndTintGetter;
import net.minecraft.client.renderer.block.BlockModelLighter;
import net.minecraft.client.renderer.block.BlockModelRenderState;
import net.minecraft.client.renderer.block.MovingBlockRenderState;
import net.minecraft.client.renderer.block.dispatch.BlockStateModel;
import net.minecraft.client.renderer.block.dispatch.BlockStateModelPart;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.blockentity.state.BlockEntityRenderState;
import net.minecraft.client.renderer.chunk.ChunkSectionLayer;
import net.minecraft.client.renderer.feature.ModelFeatureRenderer;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.client.renderer.texture.TextureAtlas;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.renderer.Sheets;
import net.minecraft.client.resources.model.geometry.BakedQuad;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Holder;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.SlabBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.AttachFace;
import net.minecraft.world.level.block.state.properties.SlabType;
import net.minecraft.world.level.ColorResolver;
import net.minecraft.world.level.lighting.LevelLightEngine;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;
import org.joml.Vector3f;
import org.joml.Vector3fc;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

public final class FramedBlockEntityRenderer implements BlockEntityRenderer<FramedBlockEntity, FramedBERenderState> {
    private static final int TINT_BASE = 32;
    private static final float GEOMETRY_EPSILON = 1.0E-4F;
    private static final float CAMO_OUTSET_SCALE = 1.0F;
    private static final Set<String> DEBUG_RENDER_KEYS = ConcurrentHashMap.newKeySet();

    public FramedBlockEntityRenderer(BlockEntityRendererProvider.Context ctx) {}

    @Override
    public FramedBERenderState createRenderState() {
        return new FramedBERenderState();
    }

    @Override
    public void extractRenderState(
            FramedBlockEntity be,
            FramedBERenderState rs,
            float tickProgress,
            Vec3 cameraPos,
            ModelFeatureRenderer.CrumblingOverlay crumblingOverlay
    ) {
        BlockEntityRenderState.extractBase(be, rs, crumblingOverlay);

        BlockState self = be.getBlockState();
        rs.isFlowerPot = self.getBlock() == ModBlocks.FRAMED_FLOWER_POT;
        rs.plant = null;
        rs.baseView = null;
        rs.baseParts = null;
        rs.camoView = null;
        rs.camoParts = null;
        rs.camoHasTranslucency = false;
        rs.camoTints = BlockModelRenderState.EMPTY_TINTS;

        if (be.getLevel() instanceof ClientLevel level) {
            if (!be.hasAnyCamo() && needsDynamicBaseRender(self)) {
                BaseRenderData base = buildBaseRenderData(be, self, level);
                if (base != null) {
                    rs.baseView = base.view();
                    rs.baseParts = base.parts();
                }
            }

            CamoRenderData camo = buildCamoRenderData(be, self, level);
            if (camo != null) {
                rs.camoView = camo.view();
                rs.camoParts = camo.parts();
                rs.camoHasTranslucency = camo.hasTranslucency();
                rs.camoTints = camo.tints();
            }

            if (rs.isFlowerPot) {
                ItemStack plantStack = be.getPotPlantStack();
                BlockState plantState = plantStack.getItem() instanceof BlockItem blockItem
                        ? blockItem.getBlock().defaultBlockState()
                        : Blocks.AIR.defaultBlockState();

                if (!plantState.isAir()) {
                    BlockPos pos = be.getBlockPos();
                    Holder<Biome> biome = level.getBiome(pos);
                    rs.plant = createMovingBlock(pos, plantState, biome, level);
                }
            }
        }
    }

    @Override
    public void submit(FramedBERenderState rs, PoseStack matrices, SubmitNodeCollector queue, CameraRenderState cameraState) {
        if (rs.baseView != null && rs.baseParts != null && !rs.baseParts.isEmpty()) {
            matrices.pushPose();
            Vec3 offset = rs.baseView.blockState.getOffset(rs.baseView.blockPos);
            matrices.translate(offset.x, offset.y, offset.z);

            submitCamoLayer(matrices, queue, rs.baseView, BlockModelRenderState.EMPTY_TINTS, rs.baseParts, ChunkSectionLayer.SOLID);
            submitCamoLayer(matrices, queue, rs.baseView, BlockModelRenderState.EMPTY_TINTS, rs.baseParts, ChunkSectionLayer.CUTOUT);
            submitCamoLayer(matrices, queue, rs.baseView, BlockModelRenderState.EMPTY_TINTS, rs.baseParts, ChunkSectionLayer.TRANSLUCENT);

            matrices.popPose();
        }

        if (rs.camoView != null && rs.camoParts != null && !rs.camoParts.isEmpty()) {
            matrices.pushPose();
            matrices.translate(0.5F, 0.5F, 0.5F);
            matrices.scale(CAMO_OUTSET_SCALE, CAMO_OUTSET_SCALE, CAMO_OUTSET_SCALE);
            matrices.translate(-0.5F, -0.5F, -0.5F);
            Vec3 offset = rs.camoView.blockState.getOffset(rs.camoView.blockPos);
            matrices.translate(offset.x, offset.y, offset.z);

            submitCamoLayer(matrices, queue, rs.camoView, rs.camoTints, rs.camoParts, ChunkSectionLayer.SOLID);
            submitCamoLayer(matrices, queue, rs.camoView, rs.camoTints, rs.camoParts, ChunkSectionLayer.CUTOUT);
            submitCamoLayer(matrices, queue, rs.camoView, rs.camoTints, rs.camoParts, ChunkSectionLayer.TRANSLUCENT);

            matrices.popPose();
        }

        if (rs.isFlowerPot && rs.plant != null) {
            matrices.pushPose();
            matrices.translate(0.5, 0.25, 0.5);
            matrices.scale(0.5f, 0.5f, 0.5f);
            matrices.translate(-0.5, 0.0, -0.5);
            queue.submitMovingBlock(matrices, rs.plant);
            matrices.popPose();
        }
    }

    private static @Nullable BaseRenderData buildBaseRenderData(FramedBlockEntity be, BlockState self, ClientLevel level) {
        Minecraft client = Minecraft.getInstance();
        if (client == null) {
            return null;
        }

        BlockPos pos = be.getBlockPos();
        BlockState sourceState = sourceStateForCamo(self);
        BlockStateModel sourceModel = client.getModelManager().getBlockStateModelSet().get(sourceState);
        List<BlockStateModelPart> sourceParts = new ArrayList<>();
        sourceModel.collectParts(RandomSource.create(pos.asLong()), sourceParts);
        if (sourceParts.isEmpty()) {
            return null;
        }

        PartRender[] noParts = new PartRender[FramedBlockEntity.MAX_CAMO_PARTS];
        List<BlockStateModelPart> wrappedParts = new ArrayList<>(sourceParts.size());
        MovingBlockRenderState view = createMovingBlock(pos, sourceState, level.getBiome(pos), level);
        Map<Integer, Integer> tintValues = new HashMap<>();

        for (BlockStateModelPart sourcePart : sourceParts) {
            wrappedParts.add(wrapPart(sourcePart, self, noParts, view, pos, tintValues, client));
        }

        return new BaseRenderData(view, List.copyOf(wrappedParts));
    }

    private static @Nullable CamoRenderData buildCamoRenderData(FramedBlockEntity be, BlockState self, ClientLevel level) {
        if (!be.hasAnyCamo()) {
            return null;
        }

        Minecraft client = Minecraft.getInstance();
        if (client == null) {
            return null;
        }

        BlockPos pos = be.getBlockPos();
        BlockState sourceState = sourceStateForCamo(self);
        BlockStateModel sourceModel = client.getModelManager().getBlockStateModelSet().get(sourceState);
        List<BlockStateModelPart> sourceParts = new ArrayList<>();
        sourceModel.collectParts(RandomSource.create(pos.asLong()), sourceParts);

        if (sourceParts.isEmpty()) {
            return null;
        }

        PartRender[] partRenders = buildPartRenders(be, pos, client);
        boolean anyCamo = false;
        for (PartRender partRender : partRenders) {
            if (partRender != null) {
                anyCamo = true;
                break;
            }
        }

        if (!anyCamo) {
            return null;
        }

        BlockAndTintGetter tintView = level;
        Map<Integer, Integer> tintValues = new HashMap<>();
        List<BlockStateModelPart> wrappedParts = new ArrayList<>(sourceParts.size());

        for (BlockStateModelPart sourcePart : sourceParts) {
            wrappedParts.add(wrapPart(sourcePart, self, partRenders, tintView, pos, tintValues, client));
        }

        MovingBlockRenderState lightingView = createMovingBlock(pos, sourceState, level.getBiome(pos), level);
        return new CamoRenderData(lightingView, List.copyOf(wrappedParts), hasAnyTranslucency(partRenders), buildTintArray(tintValues));
    }

    private static PartRender[] buildPartRenders(FramedBlockEntity be, BlockPos pos, Minecraft client) {
        PartRender[] partRenders = new PartRender[FramedBlockEntity.MAX_CAMO_PARTS];

        for (int i = 0; i < FramedBlockEntity.MAX_CAMO_PARTS; i++) {
            if (!be.hasCamoPart(i)) {
                continue;
            }

            BlockState camo = be.getCamoPart(i);
            if (camo == null || camo.isAir()) {
                continue;
            }

            BlockStateModel camoModel = client.getModelManager().getBlockStateModelSet().get(camo);
            Map<Direction, FaceInfo> faceInfo = pickFaceInfo(camoModel, RandomSource.create(pos.asLong() ^ i));
            TextureAtlasSprite fallbackSprite = camoModel.particleMaterial().sprite();
            partRenders[i] = new PartRender(be.getCamoRotPart(i), camo, faceInfo, fallbackSprite);
            debugPartRender(pos, i, camo, faceInfo, fallbackSprite);
        }

        return partRenders;
    }

    private static BlockStateModelPart wrapPart(
            BlockStateModelPart sourcePart,
            BlockState state,
            PartRender[] partRenders,
            BlockAndTintGetter tintView,
            BlockPos pos,
            Map<Integer, Integer> tintValues,
            Minecraft client
    ) {
        EnumMap<Direction, List<BakedQuad>> remapped = new EnumMap<>(Direction.class);
        boolean changed = false;

        for (Direction direction : Direction.values()) {
            List<BakedQuad> sourceQuads = sourcePart.getQuads(direction);
            RemappedQuads remappedQuads = remapQuads(sourceQuads, state, partRenders, tintView, pos, tintValues, client);
            changed |= remappedQuads.changed();
            remapped.put(direction, remappedQuads.quads());
        }

        RemappedQuads unculledQuads = remapQuads(sourcePart.getQuads(null), state, partRenders, tintView, pos, tintValues, client);
        changed |= unculledQuads.changed();

        if (!changed) {
            return sourcePart;
        }

        return new StaticPart(
                remapped,
                unculledQuads.quads(),
                sourcePart.useAmbientOcclusion(),
                sourcePart.particleMaterial(),
                sourcePart.materialFlags()
        );
    }

    private static RemappedQuads remapQuads(
            List<BakedQuad> sourceQuads,
            BlockState state,
            PartRender[] partRenders,
            BlockAndTintGetter tintView,
            BlockPos pos,
            Map<Integer, Integer> tintValues,
            Minecraft client
    ) {
        if (sourceQuads.isEmpty()) {
            return RemappedQuads.EMPTY;
        }

        List<BakedQuad> out = new ArrayList<>(sourceQuads.size());
        boolean changed = false;
        for (BakedQuad quad : sourceQuads) {
            BakedQuad mapped = remapQuad(quad, state, partRenders, tintView, pos, tintValues, client);
            changed |= mapped != quad;
            out.add(mapped);
        }

        return new RemappedQuads(List.copyOf(out), changed);
    }

    private static BakedQuad remapQuad(
            BakedQuad quad,
            BlockState state,
            PartRender[] partRenders,
            BlockAndTintGetter tintView,
            BlockPos pos,
            Map<Integer, Integer> tintValues,
            Minecraft client
    ) {
        Vector3f[] originalPositions = copyPositions(quad);
        Vector3f[] positions = copyPositions(quad);
        boolean geometryChanged = applyMiniCubeRotation(state, positions);

        Direction outFace = guessFaceFromGeometry(positions);
        if (outFace == null) {
            outFace = quad.direction();
        }

        int partIndex = pickPartIndex(state, positions);
        PartRender part = (partIndex >= 0 && partIndex < partRenders.length) ? partRenders[partIndex] : null;

        if (part == null) {
            if (!geometryChanged) {
                return quad;
            }

            long[] packedUvs = remapUvs(positions, outFace, quad.materialInfo().sprite(), 0, state.getBlock() instanceof FramedMiniCubeBlock);

            return new BakedQuad(
                    positions[0],
                    positions[1],
                    positions[2],
                    positions[3],
                    packedUvs[0],
                    packedUvs[1],
                    packedUvs[2],
                    packedUvs[3],
                    outFace,
                    quad.materialInfo()
            );
        }

        FaceMap faceMap = mapFace(part.rot(), outFace);
        FaceInfo faceInfo = part.faceInfo().get(faceMap.srcFace());
        TextureAtlasSprite targetSprite = faceInfo != null ? faceInfo.sprite() : part.fallbackSprite();
        int tintLayer = faceInfo != null ? faceInfo.tintIndex() : -1;
        Vector3fc[] uvPositions = positions;
        if (state.getBlock() instanceof FramedMiniCubeBlock && geometryChanged && (outFace == Direction.UP || outFace == Direction.DOWN)) {
            uvPositions = originalPositions;
        }
        long[] packedUvs = remapUvs(uvPositions, outFace, targetSprite, faceMap.uvTurnsCW(), state.getBlock() instanceof FramedMiniCubeBlock);
        debugQuadMapping(pos, partIndex, part.camoState(), faceMap.srcFace(), outFace, targetSprite, tintLayer, packedUvs);

        int packedTint = -1;
        if (tintLayer >= 0) {
            packedTint = TINT_BASE + (partIndex * 8) + tintLayer;
            tintValues.computeIfAbsent(packedTint, key -> resolveTint(client, part.camoState(), tintLayer, tintView, pos));
        }

        BakedQuad.MaterialInfo materialInfo = quad.materialInfo();
        ChunkSectionLayer layer = ChunkSectionLayer.byTransparency(targetSprite.transparency());
        BakedQuad.MaterialInfo remappedInfo = new BakedQuad.MaterialInfo(
                targetSprite,
                layer,
                itemRenderTypeFor(targetSprite, layer),
                packedTint,
                faceInfo != null ? faceInfo.shade() : true,
                faceInfo != null ? faceInfo.lightEmission() : materialInfo.lightEmission()
        );

        return new BakedQuad(
                positions[0],
                positions[1],
                positions[2],
                positions[3],
                packedUvs[0],
                packedUvs[1],
                packedUvs[2],
                packedUvs[3],
                outFace,
                remappedInfo
        );
    }

    private static int resolveTint(
            Minecraft client,
            BlockState camo,
            int tintLayer,
            BlockAndTintGetter tintView,
            BlockPos pos
    ) {
        BlockTintSource source = client.getBlockColors().getTintSource(camo, tintLayer);
        if (source == null) {
            return -1;
        }

        return source.colorInWorld(camo, tintView, pos);
    }

    private static int[] buildTintArray(Map<Integer, Integer> tintValues) {
        if (tintValues.isEmpty()) {
            return BlockModelRenderState.EMPTY_TINTS;
        }

        int maxTint = -1;
        for (int tintIndex : tintValues.keySet()) {
            if (tintIndex > maxTint) {
                maxTint = tintIndex;
            }
        }

        int[] values = new int[maxTint + 1];
        Arrays.fill(values, -1);
        tintValues.forEach((tintIndex, tintValue) -> values[tintIndex] = tintValue);
        return values;
    }

    private static Map<Direction, FaceInfo> pickFaceInfo(BlockStateModel model, RandomSource random) {
        EnumMap<Direction, FaceInfo> out = new EnumMap<>(Direction.class);
        List<BlockStateModelPart> parts = new ArrayList<>();
        model.collectParts(random, parts);

        for (BlockStateModelPart part : parts) {
            for (Direction direction : Direction.values()) {
                List<BakedQuad> quads = part.getQuads(direction);
                if (!quads.isEmpty() && !out.containsKey(direction)) {
                    BakedQuad quad = quads.get(0);
                    out.put(direction, new FaceInfo(
                            quad.materialInfo().sprite(),
                            quad.materialInfo().tintIndex(),
                            quad.materialInfo().shade(),
                            quad.materialInfo().lightEmission()
                    ));
                }
            }

            if (out.size() == Direction.values().length) {
                break;
            }
        }

        return out;
    }

    private static long[] remapUvs(Vector3fc[] positions, Direction face, TextureAtlasSprite targetSprite, int uvTurnsCW, boolean stretchFullSprite) {
        float[] u = new float[4];
        float[] v = new float[4];

        for (int i = 0; i < 4; i++) {
            float[] locked = geometryLockedUv(positions[i], face);
            float[] rotated = rotateUv(locked[0], locked[1], uvTurnsCW);
            u[i] = rotated[0];
            v[i] = rotated[1];
        }

        if (stretchFullSprite) {
            stretchUvsToFullSprite(u, v);
        }

        long[] packed = new long[4];
        for (int i = 0; i < 4; i++) {
            packed[i] = UVPair.pack(targetSprite.getU(clamp01(u[i])), targetSprite.getV(clamp01(v[i])));
        }

        return packed;
    }

    private static float[] geometryLockedUv(Vector3fc position, Direction face) {
        float u = switch (face) {
            case DOWN, UP, SOUTH -> position.x();
            case NORTH -> 1.0F - position.x();
            case WEST -> position.z();
            case EAST -> 1.0F - position.z();
        };
        float v = switch (face) {
            case UP -> position.z();
            case DOWN -> 1.0F - position.z();
            case NORTH, SOUTH, WEST, EAST -> 1.0F - position.y();
        };
        return new float[]{u, v};
    }

    private static void stretchUvsToFullSprite(float[] u, float[] v) {
        float uMin = Math.min(Math.min(u[0], u[1]), Math.min(u[2], u[3]));
        float uMax = Math.max(Math.max(u[0], u[1]), Math.max(u[2], u[3]));
        float vMin = Math.min(Math.min(v[0], v[1]), Math.min(v[2], v[3]));
        float vMax = Math.max(Math.max(v[0], v[1]), Math.max(v[2], v[3]));

        float du = uMax - uMin;
        float dv = vMax - vMin;
        if (du <= 0.0F || dv <= 0.0F) {
            return;
        }

        for (int i = 0; i < 4; i++) {
            u[i] = (u[i] - uMin) / du;
            v[i] = (v[i] - vMin) / dv;
        }
    }

    private static float[] rotateUv(float u, float v, int turnsCW) {
        return switch (turnsCW & 3) {
            case 1 -> new float[]{v, 1.0F - u};
            case 2 -> new float[]{1.0F - u, 1.0F - v};
            case 3 -> new float[]{1.0F - v, u};
            default -> new float[]{u, v};
        };
    }

    private static float clamp01(float value) {
        if (value <= 0.0F) {
            return 0.0F;
        }
        if (value >= 1.0F) {
            return 1.0F;
        }
        return value;
    }

    private static Vector3f[] copyPositions(BakedQuad quad) {
        return new Vector3f[]{
                new Vector3f(quad.position0()),
                new Vector3f(quad.position1()),
                new Vector3f(quad.position2()),
                new Vector3f(quad.position3())
        };
    }

    private static boolean applyMiniCubeRotation(BlockState state, Vector3f[] positions) {
        if (!(state.getBlock() instanceof FramedMiniCubeBlock)) {
            return false;
        }

        if (state.getValue(FramedMiniCubeBlock.FACE) == AttachFace.WALL) {
            return false;
        }

        int rot16 = state.getValue(FramedMiniCubeBlock.ROTATION_16) & 15;
        if (rot16 == 0) {
            return false;
        }

        float angle = (float) (rot16 * (Math.PI / 8.0));
        float sin = (float) Math.sin(angle);
        float cos = (float) Math.cos(angle);

        for (Vector3f position : positions) {
            float x = position.x() - 0.5F;
            float z = position.z() - 0.5F;
            float nextX = x * cos - z * sin;
            float nextZ = x * sin + z * cos;
            position.set(nextX + 0.5F, position.y(), nextZ + 0.5F);
        }

        return true;
    }

    private static @Nullable Direction guessFaceFromGeometry(Vector3fc[] positions) {
        Vector3fc a = positions[0];
        Vector3fc b = positions[1];
        Vector3fc c = positions[2];

        float ax = b.x() - a.x();
        float ay = b.y() - a.y();
        float az = b.z() - a.z();
        float bx = c.x() - a.x();
        float by = c.y() - a.y();
        float bz = c.z() - a.z();

        float nx = ay * bz - az * by;
        float ny = az * bx - ax * bz;
        float nz = ax * by - ay * bx;

        float absX = Math.abs(nx);
        float absY = Math.abs(ny);
        float absZ = Math.abs(nz);
        if (absX < 1.0E-6F && absY < 1.0E-6F && absZ < 1.0E-6F) {
            return null;
        }

        if (absY >= absX && absY >= absZ) {
            return ny > 0.0F ? Direction.UP : Direction.DOWN;
        }

        if (absX >= absZ) {
            return nx > 0.0F ? Direction.EAST : Direction.WEST;
        }

        return nz > 0.0F ? Direction.SOUTH : Direction.NORTH;
    }

    private static int pickPartIndex(BlockState state, Vector3fc[] positions) {
        if (state.getBlock() instanceof FramedCheckeredSlabBlock) {
            return quadPartForCheckeredSlab(state, positions);
        }

        if (state.getBlock() instanceof SlabBlock) {
            return quadPartForSlab(state, positions);
        }

        if (state.getBlock() instanceof FramedCheckeredVerticalSlabBlock) {
            return quadPartForCheckeredVerticalSlab(state, positions);
        }

        if (state.getBlock() instanceof FramedVerticalSlabBlock) {
            return quadPartForVerticalSlab(state, positions);
        }

        if (state.getBlock() instanceof FramedCheckeredBlock) {
            return quadPartForCheckered(positions);
        }

        if (state.getBlock() instanceof net.minecraft.world.level.block.DoorBlock) {
            return centerY(positions) >= 0.5F ? 1 : 0;
        }

        return 0;
    }

    private static int quadPartForSlab(BlockState state, Vector3fc[] positions) {
        SlabType type = state.getValue(SlabBlock.TYPE);
        if (type == SlabType.BOTTOM) {
            return 0;
        }
        if (type == SlabType.TOP) {
            return 1;
        }

        float minY = minY(positions);
        float maxY = maxY(positions);
        if (maxY <= 0.5F + GEOMETRY_EPSILON) {
            return 0;
        }
        if (minY >= 0.5F - GEOMETRY_EPSILON) {
            return 1;
        }
        return centerY(positions) >= 0.5F ? 1 : 0;
    }

    private static int quadPartForVerticalSlab(BlockState state, Vector3fc[] positions) {
        if (state.getValue(FramedVerticalSlabBlock.TYPE) != com.spider.framedfabric.block.enums.VerticalSlabType.DOUBLE) {
            return 0;
        }

        Direction facing = state.getValue(FramedVerticalSlabBlock.FACING);
        if (facing == Direction.NORTH || facing == Direction.SOUTH) {
            float minZ = minZ(positions);
            float maxZ = maxZ(positions);
            if (maxZ <= 0.5F + GEOMETRY_EPSILON) {
                return 0;
            }
            if (minZ >= 0.5F - GEOMETRY_EPSILON) {
                return 1;
            }
            return centerZ(positions) >= 0.5F ? 1 : 0;
        }

        float minX = minX(positions);
        float maxX = maxX(positions);
        if (maxX <= 0.5F + GEOMETRY_EPSILON) {
            return 0;
        }
        if (minX >= 0.5F - GEOMETRY_EPSILON) {
            return 1;
        }
        return centerX(positions) >= 0.5F ? 1 : 0;
    }

    private static int quadPartForCheckered(Vector3fc[] positions) {
        int xi = centerX(positions) >= 0.5F ? 1 : 0;
        int yi = centerY(positions) >= 0.5F ? 1 : 0;
        int zi = centerZ(positions) >= 0.5F ? 1 : 0;
        return (xi ^ yi ^ zi) & 1;
    }

    private static int quadPartForCheckeredSlab(BlockState state, Vector3fc[] positions) {
        SlabType type = state.getValue(SlabBlock.TYPE);
        int parity = ((centerX(positions) >= 0.5F) ? 1 : 0) ^ ((centerZ(positions) >= 0.5F) ? 1 : 0);

        int base;
        if (type == SlabType.DOUBLE) {
            float minY = minY(positions);
            float maxY = maxY(positions);
            if (maxY <= 0.5F + GEOMETRY_EPSILON) {
                base = 0;
            } else if (minY >= 0.5F - GEOMETRY_EPSILON) {
                base = 2;
            } else {
                base = centerY(positions) >= 0.5F ? 2 : 0;
            }
        } else if (type == SlabType.TOP) {
            base = 2;
        } else {
            base = 0;
        }

        return base + parity;
    }

    private static int quadPartForCheckeredVerticalSlab(BlockState state, Vector3fc[] positions) {
        var type = state.getValue(FramedCheckeredVerticalSlabBlock.TYPE);
        Direction facing = state.getValue(FramedCheckeredVerticalSlabBlock.FACING);

        int half = 0;
        if (type == com.spider.framedfabric.block.enums.VerticalSlabType.DOUBLE) {
            if (facing == Direction.NORTH || facing == Direction.SOUTH) {
                float minZ = minZ(positions);
                float maxZ = maxZ(positions);
                if (maxZ <= 0.5F + GEOMETRY_EPSILON) {
                    half = 0;
                } else if (minZ >= 0.5F - GEOMETRY_EPSILON) {
                    half = 1;
                } else {
                    half = centerZ(positions) >= 0.5F ? 1 : 0;
                }
            } else {
                float minX = minX(positions);
                float maxX = maxX(positions);
                if (maxX <= 0.5F + GEOMETRY_EPSILON) {
                    half = 0;
                } else if (minX >= 0.5F - GEOMETRY_EPSILON) {
                    half = 1;
                } else {
                    half = centerX(positions) >= 0.5F ? 1 : 0;
                }
            }
        }

        int a = (facing == Direction.NORTH || facing == Direction.SOUTH)
                ? (centerX(positions) >= 0.5F ? 1 : 0)
                : (centerZ(positions) >= 0.5F ? 1 : 0);
        int b = centerY(positions) >= 0.5F ? 1 : 0;
        return (half * 2) + ((a ^ b) & 1);
    }

    private static float minX(Vector3fc[] positions) {
        return Math.min(Math.min(positions[0].x(), positions[1].x()), Math.min(positions[2].x(), positions[3].x()));
    }

    private static float maxX(Vector3fc[] positions) {
        return Math.max(Math.max(positions[0].x(), positions[1].x()), Math.max(positions[2].x(), positions[3].x()));
    }

    private static float minY(Vector3fc[] positions) {
        return Math.min(Math.min(positions[0].y(), positions[1].y()), Math.min(positions[2].y(), positions[3].y()));
    }

    private static float maxY(Vector3fc[] positions) {
        return Math.max(Math.max(positions[0].y(), positions[1].y()), Math.max(positions[2].y(), positions[3].y()));
    }

    private static float minZ(Vector3fc[] positions) {
        return Math.min(Math.min(positions[0].z(), positions[1].z()), Math.min(positions[2].z(), positions[3].z()));
    }

    private static float maxZ(Vector3fc[] positions) {
        return Math.max(Math.max(positions[0].z(), positions[1].z()), Math.max(positions[2].z(), positions[3].z()));
    }

    private static float centerX(Vector3fc[] positions) {
        return (positions[0].x() + positions[1].x() + positions[2].x() + positions[3].x()) * 0.25F;
    }

    private static float centerY(Vector3fc[] positions) {
        return (positions[0].y() + positions[1].y() + positions[2].y() + positions[3].y()) * 0.25F;
    }

    private static float centerZ(Vector3fc[] positions) {
        return (positions[0].z() + positions[1].z() + positions[2].z() + positions[3].z()) * 0.25F;
    }

    private static MovingBlockRenderState createMovingBlock(BlockPos pos, BlockState blockState, Holder<Biome> biome, ClientLevel level) {
        MovingBlockRenderState movingBlock = new MovingBlockRenderState();
        movingBlock.randomSeedPos = pos;
        movingBlock.blockPos = pos;
        movingBlock.blockState = blockState;
        movingBlock.biome = biome;
        movingBlock.cardinalLighting = level.cardinalLighting();
        movingBlock.lightEngine = level.getLightEngine();
        return movingBlock;
    }

    private static BlockState sourceStateForCamo(BlockState state) {
        return FramedProperties.withCamo(state, false);
    }

    private static boolean needsDynamicBaseRender(BlockState state) {
        return state.getBlock() instanceof FramedMiniCubeBlock
                && state.getValue(FramedMiniCubeBlock.FACE) != AttachFace.WALL;
    }

    private static void submitCamoLayer(
            PoseStack matrices,
            SubmitNodeCollector queue,
            MovingBlockRenderState camoView,
            int[] tintLayers,
            List<BlockStateModelPart> parts,
            ChunkSectionLayer layer
    ) {
        if (!hasQuadsForLayer(parts, layer)) {
            return;
        }

        queue.submitCustomGeometry(matrices, renderTypeFor(layer), (pose, buffer) -> renderLayerQuads(pose, buffer, camoView, tintLayers, parts, layer));
    }

    private static boolean hasQuadsForLayer(List<BlockStateModelPart> parts, ChunkSectionLayer layer) {
        for (BlockStateModelPart part : parts) {
            for (Direction direction : Direction.values()) {
                for (BakedQuad quad : part.getQuads(direction)) {
                    if (quad.materialInfo().layer() == layer) {
                        return true;
                    }
                }
            }

            for (BakedQuad quad : part.getQuads(null)) {
                if (quad.materialInfo().layer() == layer) {
                    return true;
                }
            }
        }

        return false;
    }

    private static void renderLayerQuads(
            PoseStack.Pose pose,
            VertexConsumer buffer,
            MovingBlockRenderState camoView,
            int[] tintLayers,
            List<BlockStateModelPart> parts,
            ChunkSectionLayer layer
    ) {
        Minecraft client = Minecraft.getInstance();
        ClientLevel level = client != null ? client.level : null;
        if (level == null) {
            return;
        }

        BlockAndTintGetter blockView = new LevelBlockRenderView(level, camoView.blockPos, camoView.blockState);
        boolean ambientOcclusion = camoView.blockState.getLightEmission() == 0;
        BlockModelLighter lighter = new BlockModelLighter();
        QuadInstance instance = new QuadInstance();
        instance.setOverlayCoords(OverlayTexture.NO_OVERLAY);

        for (BlockStateModelPart part : parts) {
            for (Direction direction : Direction.values()) {
                for (BakedQuad quad : part.getQuads(direction)) {
                    if (quad.materialInfo().layer() == layer) {
                        putQuad(pose, quad, instance, tintLayers, buffer, lighter, blockView, camoView, ambientOcclusion);
                    }
                }
            }

            for (BakedQuad quad : part.getQuads(null)) {
                if (quad.materialInfo().layer() == layer) {
                    putQuad(pose, quad, instance, tintLayers, buffer, lighter, blockView, camoView, ambientOcclusion);
                }
            }
        }
    }

    private static void putQuad(
            PoseStack.Pose pose,
            BakedQuad quad,
            QuadInstance instance,
            int[] tintLayers,
            VertexConsumer buffer,
            BlockModelLighter lighter,
            BlockAndTintGetter blockView,
            MovingBlockRenderState camoView,
            boolean useAmbientOcclusion
    ) {
        if (useAmbientOcclusion) {
            lighter.prepareQuadAmbientOcclusion(blockView, camoView.blockState, camoView.blockPos, quad, instance);
        } else {
            lighter.prepareQuadFlat(blockView, camoView.blockState, camoView.blockPos, BlockModelLighter.CHECK_LIGHT, quad, instance);
        }

        instance.setOverlayCoords(OverlayTexture.NO_OVERLAY);
        int tintIndex = quad.materialInfo().tintIndex();
        if (tintIndex >= 0 && tintIndex < tintLayers.length) {
            int tintColor = tintLayers[tintIndex];
            if (tintColor != -1) {
                instance.multiplyColor(tintColor);
            }
        }

        buffer.putBakedQuad(pose, quad, instance);
    }

    private static RenderType renderTypeFor(ChunkSectionLayer layer) {
        return switch (layer) {
            case SOLID -> RenderTypes.solidMovingBlock();
            case CUTOUT -> RenderTypes.cutoutMovingBlock();
            case TRANSLUCENT -> RenderTypes.translucentMovingBlock();
        };
    }

    private static RenderType itemRenderTypeFor(TextureAtlasSprite sprite, ChunkSectionLayer layer) {
        boolean translucent = layer == ChunkSectionLayer.TRANSLUCENT;
        if (TextureAtlas.LOCATION_BLOCKS.equals(sprite.atlasLocation())) {
            return translucent ? Sheets.translucentBlockItemSheet() : Sheets.cutoutBlockItemSheet();
        }

        return translucent ? Sheets.translucentItemSheet() : Sheets.cutoutItemSheet();
    }

    private static boolean hasAnyTranslucency(PartRender[] partRenders) {
        for (PartRender partRender : partRenders) {
            if (partRender == null) {
                continue;
            }

            if (partRender.fallbackSprite().transparency().hasTranslucent()) {
                return true;
            }

            for (FaceInfo faceInfo : partRender.faceInfo().values()) {
                if (faceInfo.sprite().transparency().hasTranslucent()) {
                    return true;
                }
            }
        }

        return false;
    }

    private static void debugPartRender(BlockPos pos, int partIndex, BlockState camo, Map<Direction, FaceInfo> faceInfo, TextureAtlasSprite fallbackSprite) {
        String key = "part:" + pos + ":" + partIndex + ":" + camo;
        if (!DEBUG_RENDER_KEYS.add(key)) {
            return;
        }

        StringBuilder builder = new StringBuilder();
        builder.append("[FramedFabric] Camo part at ").append(pos)
                .append(" part ").append(partIndex)
                .append(" state=").append(camo)
                .append(" fallback=").append(fallbackSprite.contents().name());

        for (Direction direction : Direction.values()) {
            FaceInfo info = faceInfo.get(direction);
            if (info != null) {
                builder.append(" ").append(direction.getName())
                        .append("=")
                        .append(info.sprite().contents().name())
                        .append("#")
                        .append(info.tintIndex());
            }
        }

        FramedFabric.LOGGER.info(builder.toString());
    }

    private static void debugQuadMapping(
            BlockPos pos,
            int partIndex,
            BlockState camo,
            Direction srcFace,
            Direction outFace,
            TextureAtlasSprite sprite,
            int tintLayer,
            long[] packedUvs
    ) {
        String key = "quad:" + pos + ":" + partIndex + ":" + camo + ":" + srcFace + ":" + outFace;
        if (!DEBUG_RENDER_KEYS.add(key)) {
            return;
        }

        FramedFabric.LOGGER.info(
                "[FramedFabric] Camo quad at {} part {} state={} srcFace={} outFace={} sprite={} tint={} uv0=({}, {}) uv2=({}, {})",
                pos,
                partIndex,
                camo,
                srcFace,
                outFace,
                sprite.contents().name(),
                tintLayer,
                UVPair.unpackU(packedUvs[0]),
                UVPair.unpackV(packedUvs[0]),
                UVPair.unpackU(packedUvs[2]),
                UVPair.unpackV(packedUvs[2])
        );
    }

    private record CamoRenderData(MovingBlockRenderState view, List<BlockStateModelPart> parts, boolean hasTranslucency, int[] tints) {}

    private record BaseRenderData(MovingBlockRenderState view, List<BlockStateModelPart> parts) {}

    private record PartRender(int rot, BlockState camoState, Map<Direction, FaceInfo> faceInfo, TextureAtlasSprite fallbackSprite) {}

    private record FaceMap(Direction srcFace, int uvTurnsCW) {}

    private record FaceInfo(TextureAtlasSprite sprite, int tintIndex, boolean shade, int lightEmission) {}

    private record RemappedQuads(List<BakedQuad> quads, boolean changed) {
        private static final RemappedQuads EMPTY = new RemappedQuads(List.of(), false);
    }

    private record StaticPart(
            Map<Direction, List<BakedQuad>> quadsByDirection,
            List<BakedQuad> unculledQuads,
            boolean useAmbientOcclusion,
            net.minecraft.client.resources.model.sprite.Material.Baked particleMaterial,
            int materialFlags
    ) implements BlockStateModelPart {
        @Override
        public List<BakedQuad> getQuads(Direction direction) {
            if (direction == null) {
                return unculledQuads;
            }

            return quadsByDirection.getOrDefault(direction, List.of());
        }
    }

    private record LevelBlockRenderView(ClientLevel level, BlockPos blockPos, BlockState blockState) implements BlockAndTintGetter {
        @Override
        public net.minecraft.world.level.CardinalLighting cardinalLighting() {
            return level.cardinalLighting();
        }

        @Override
        public int getBlockTint(BlockPos pos, ColorResolver colorResolver) {
            return level.getBlockTint(pos, colorResolver);
        }

        @Override
        public @Nullable BlockEntity getBlockEntity(BlockPos pos) {
            return level.getBlockEntity(pos);
        }

        @Override
        public BlockState getBlockState(BlockPos pos) {
            return pos.equals(blockPos) ? blockState : level.getBlockState(pos);
        }

        @Override
        public FluidState getFluidState(BlockPos pos) {
            return pos.equals(blockPos) ? blockState.getFluidState() : level.getFluidState(pos);
        }

        @Override
        public int getHeight() {
            return level.getHeight();
        }

        @Override
        public int getMinY() {
            return level.getMinY();
        }

        @Override
        public LevelLightEngine getLightEngine() {
            return level.getLightEngine();
        }
    }

    private static FaceMap mapFace(int rot, Direction out) {
        return switch (rot) {
            case 2 -> switch (out) {
                case UP -> new FaceMap(Direction.DOWN, 0);
                case DOWN -> new FaceMap(Direction.UP, 0);
                case NORTH -> new FaceMap(Direction.SOUTH, 2);
                case SOUTH -> new FaceMap(Direction.NORTH, 2);
                case EAST -> new FaceMap(Direction.EAST, 2);
                case WEST -> new FaceMap(Direction.WEST, 2);
            };
            case 3 -> switch (out) {
                case UP -> new FaceMap(Direction.NORTH, 2);
                case DOWN -> new FaceMap(Direction.SOUTH, 0);
                case NORTH -> new FaceMap(Direction.DOWN, 0);
                case SOUTH -> new FaceMap(Direction.UP, 2);
                case EAST -> new FaceMap(Direction.EAST, 3);
                case WEST -> new FaceMap(Direction.WEST, 1);
            };
            case 4 -> switch (out) {
                case UP -> new FaceMap(Direction.SOUTH, 0);
                case DOWN -> new FaceMap(Direction.NORTH, 2);
                case NORTH -> new FaceMap(Direction.UP, 0);
                case SOUTH -> new FaceMap(Direction.DOWN, 2);
                case EAST -> new FaceMap(Direction.EAST, 1);
                case WEST -> new FaceMap(Direction.WEST, 3);
            };
            case 5 -> switch (out) {
                case UP -> new FaceMap(Direction.EAST, 3);
                case DOWN -> new FaceMap(Direction.WEST, 3);
                case NORTH -> new FaceMap(Direction.NORTH, 1);
                case SOUTH -> new FaceMap(Direction.SOUTH, 3);
                case EAST -> new FaceMap(Direction.DOWN, 3);
                case WEST -> new FaceMap(Direction.UP, 3);
            };
            case 6 -> switch (out) {
                case UP -> new FaceMap(Direction.WEST, 1);
                case DOWN -> new FaceMap(Direction.EAST, 1);
                case NORTH -> new FaceMap(Direction.NORTH, 3);
                case SOUTH -> new FaceMap(Direction.SOUTH, 1);
                case EAST -> new FaceMap(Direction.UP, 1);
                case WEST -> new FaceMap(Direction.DOWN, 1);
            };
            default -> new FaceMap(out, 0);
        };
    }
}
