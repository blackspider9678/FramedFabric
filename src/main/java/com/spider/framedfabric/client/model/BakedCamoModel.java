package com.spider.framedfabric.client.model;

import com.spider.framedfabric.block.FramedMiniCubeBlock;
import com.spider.framedfabric.block.custom.FramedCheckeredBlock;
import com.spider.framedfabric.block.custom.FramedCheckeredSlabBlock;
import com.spider.framedfabric.block.custom.FramedCheckeredVerticalSlabBlock;
import com.spider.framedfabric.block.custom.FramedVerticalSlabBlock;
import com.spider.framedfabric.block.enums.VerticalSlabType;
import com.spider.framedfabric.blockentity.FramedBlockEntity;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.function.Predicate;
import net.fabricmc.fabric.api.client.renderer.v1.mesh.MutableQuadView;
import net.fabricmc.fabric.api.client.renderer.v1.mesh.QuadEmitter;
import net.fabricmc.fabric.api.client.renderer.v1.mesh.QuadTransform;
import net.fabricmc.fabric.api.client.renderer.v1.model.FabricBlockStateModel;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.block.BlockAndTintGetter;
import net.minecraft.client.renderer.block.BlockStateModelSet;
import net.minecraft.client.renderer.block.dispatch.BlockStateModel;
import net.minecraft.client.renderer.block.dispatch.BlockStateModelPart;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.resources.model.geometry.BakedQuad;
import net.minecraft.client.resources.model.sprite.Material;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.block.SlabBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.SlabType;
import org.jspecify.annotations.Nullable;

public final class BakedCamoModel implements BlockStateModel, FabricBlockStateModel {
    public static final int TINT_BASE = 32;

    private final BlockStateModel parent;

    public BakedCamoModel(BlockStateModel parent) {
        this.parent = parent;
    }

    @Override
    public void emitQuads(
            QuadEmitter emitter,
            BlockAndTintGetter view,
            BlockPos pos,
            BlockState state,
            RandomSource random,
            Predicate<@Nullable Direction> cullTest
    ) {
        var be = view.getBlockEntity(pos);
        if (!(be instanceof FramedBlockEntity framed) || !framed.hasAnyCamo()) {
            ((FabricBlockStateModel) parent).emitQuads(emitter, view, pos, state, random, cullTest);
            return;
        }

        boolean isCheckeredSlab = state.getBlock() instanceof FramedCheckeredSlabBlock;
        boolean isSlab = state.getBlock() instanceof SlabBlock;
        boolean isCheckeredVerticalSlab = state.getBlock() instanceof FramedCheckeredVerticalSlabBlock;
        boolean isVerticalSlab = state.getBlock() instanceof FramedVerticalSlabBlock;
        boolean isCheckered = state.getBlock() instanceof FramedCheckeredBlock;

        PartRender[] parts = new PartRender[FramedBlockEntity.MAX_CAMO_PARTS];
        BlockStateModelSet modelSet = Minecraft.getInstance().getModelManager().getBlockStateModelSet();

        for (int i = 0; i < FramedBlockEntity.MAX_CAMO_PARTS; i++) {
            if (!framed.hasCamoPart(i)) {
                continue;
            }

            BlockState camo = framed.getCamoPart(i);
            if (camo == null || camo.isAir()) {
                continue;
            }

            BlockStateModel camoModel = modelSet.get(camo);
            Material.Baked fallbackMaterial = modelSet.getParticleMaterial(camo);

            parts[i] = new PartRender(
                    framed.getCamoRotPart(i),
                    pickFaceInfo(camoModel),
                    fallbackMaterial
            );
        }

        boolean anyPart = false;
        for (PartRender part : parts) {
            if (part != null) {
                anyPart = true;
                break;
            }
        }

        if (!anyPart) {
            ((FabricBlockStateModel) parent).emitQuads(emitter, view, pos, state, random, cullTest);
            return;
        }

        QuadTransform transform = quad -> {
            if (quad.tintIndex() == 7) {
                return true;
            }

            int partIndex = 0;

            Direction outFace = guessFaceFromGeometry(quad);
            if (outFace == null) {
                outFace = quad.nominalFace();
            }
            if (outFace == null) {
                outFace = quad.lightFace();
            }
            if (outFace == null) {
                outFace = quad.cullFace();
            }
            if (outFace == null) {
                return true;
            }

            if (isSlab) {
                partIndex = quadPartForSlab(state, quad);
            } else if (isVerticalSlab) {
                partIndex = quadPartForVerticalSlab(state, quad);
            } else if (isCheckered) {
                partIndex = quadPartForCheckered(quad);
            } else if (isCheckeredSlab) {
                partIndex = quadPartForCheckeredSlab(state, quad);
            } else if (isCheckeredVerticalSlab) {
                partIndex = quadPartForCheckeredVerticalSlab(state, quad);
            }

            if (partIndex < 0 || partIndex >= parts.length) {
                return true;
            }

            PartRender part = parts[partIndex];
            if (part == null) {
                quad.tintIndex(-1);
                return true;
            }

            FaceMap faceMap = mapFace(part.rot(), outFace);
            FaceInfo faceInfo = part.faceInfo().get(faceMap.srcFace());
            Material.Baked material = (faceInfo != null) ? faceInfo.material() : part.fallbackMaterial();
            int tintLayer = (faceInfo != null) ? faceInfo.tintIndex() : -1;

            quad.nominalFace(outFace);

            boolean isMiniCube = state.getBlock() instanceof FramedMiniCubeBlock;
            int bakeFlags = (isMiniCube ? 0 : MutableQuadView.BAKE_LOCK_UV) | bakeRotFlag(faceMap.uvTurnsCW());

            quad.materialBake(material, bakeFlags);

            if (isMiniCube) {
                stretchUvToFullSprite(quad, material.sprite());
            }

            SlabType slabType = isSlab ? state.getValue(SlabBlock.TYPE) : null;

            if (isSlab && slabType == SlabType.DOUBLE) {
                quad.tintIndex((tintLayer >= 0) ? TINT_BASE + tintLayer + (partIndex * 8) : -1);
            } else if (isVerticalSlab && state.getValue(FramedVerticalSlabBlock.TYPE) == VerticalSlabType.DOUBLE) {
                quad.tintIndex((tintLayer >= 0) ? TINT_BASE + tintLayer + (partIndex * 8) : -1);
            } else if (isCheckered || isCheckeredSlab || isCheckeredVerticalSlab) {
                quad.tintIndex((tintLayer >= 0) ? TINT_BASE + tintLayer + (partIndex * 8) : -1);
            } else {
                quad.tintIndex(tintLayer);
            }

            return true;
        };

        emitter.pushTransform(transform);
        ((FabricBlockStateModel) parent).emitQuads(emitter, view, pos, state, random, cullTest);
        emitter.popTransform();
    }

    @Override
    public @Nullable Object createGeometryKey(BlockAndTintGetter view, BlockPos pos, BlockState state, RandomSource random) {
        var be = view.getBlockEntity(pos);
        if (be instanceof FramedBlockEntity framed && framed.hasAnyCamo()) {
            return null;
        }

        return ((FabricBlockStateModel) parent).createGeometryKey(view, pos, state, random);
    }

    @Override
    public Material.Baked particleMaterial(BlockAndTintGetter view, BlockPos pos, BlockState state) {
        var be = view.getBlockEntity(pos);
        if (be instanceof FramedBlockEntity framed) {
            BlockStateModelSet modelSet = Minecraft.getInstance().getModelManager().getBlockStateModelSet();

            for (int i = 0; i < FramedBlockEntity.MAX_CAMO_PARTS; i++) {
                if (framed.hasCamoPart(i)) {
                    BlockState camo = framed.getCamoPart(i);
                    if (camo != null && !camo.isAir()) {
                        return modelSet.getParticleMaterial(camo);
                    }
                }
            }
        }

        return ((FabricBlockStateModel) parent).particleMaterial(view, pos, state);
    }

    @Override
    public void collectParts(RandomSource random, List<BlockStateModelPart> parts) {
        parent.collectParts(random, parts);
    }

    @Override
    public Material.Baked particleMaterial() {
        return parent.particleMaterial();
    }

    @Override
    public int materialFlags() {
        return parent.materialFlags() | BakedQuad.FLAG_TRANSLUCENT | BakedQuad.FLAG_ANIMATED;
    }

    @Override
    public int materialFlags(BlockAndTintGetter view, BlockPos pos, BlockState state, RandomSource random) {
        return materialFlags();
    }

    private record PartRender(
            int rot,
            Map<Direction, FaceInfo> faceInfo,
            Material.Baked fallbackMaterial
    ) {}

    private record FaceMap(Direction srcFace, int uvTurnsCW) {}

    private record FaceInfo(Material.Baked material, int tintIndex) {}

    private static int bakeRotFlag(int turnsCW) {
        return switch (turnsCW & 3) {
            case 1 -> MutableQuadView.BAKE_ROTATE_90;
            case 2 -> MutableQuadView.BAKE_ROTATE_180;
            case 3 -> MutableQuadView.BAKE_ROTATE_270;
            default -> MutableQuadView.BAKE_ROTATE_NONE;
        };
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

    private static Map<Direction, FaceInfo> pickFaceInfo(BlockStateModel model) {
        EnumMap<Direction, FaceInfo> out = new EnumMap<>(Direction.class);
        List<BlockStateModelPart> parts = new ArrayList<>();

        model.collectParts(RandomSource.create(0L), parts);

        for (BlockStateModelPart part : parts) {
            for (Direction direction : Direction.values()) {
                List<BakedQuad> quads = part.getQuads(direction);
                if (!quads.isEmpty() && !out.containsKey(direction)) {
                    BakedQuad quad = quads.getFirst();
                    Material.Baked material = new Material.Baked(quad.materialInfo().sprite(), false);
                    out.put(direction, new FaceInfo(material, quad.materialInfo().tintIndex()));
                }
            }

            if (out.size() == Direction.values().length) {
                break;
            }
        }

        return out;
    }

    private static @Nullable Direction guessFaceFromGeometry(MutableQuadView quad) {
        float x0 = quad.x(0);
        float y0 = quad.y(0);
        float z0 = quad.z(0);
        float x1 = quad.x(1);
        float y1 = quad.y(1);
        float z1 = quad.z(1);
        float x2 = quad.x(2);
        float y2 = quad.y(2);
        float z2 = quad.z(2);

        float ax = x1 - x0;
        float ay = y1 - y0;
        float az = z1 - z0;
        float bx = x2 - x0;
        float by = y2 - y0;
        float bz = z2 - z0;

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
            return ny > 0 ? Direction.UP : Direction.DOWN;
        }
        if (absX >= absZ) {
            return nx > 0 ? Direction.EAST : Direction.WEST;
        }
        return nz > 0 ? Direction.SOUTH : Direction.NORTH;
    }

    private static int quadPartForSlab(BlockState state, MutableQuadView quad) {
        SlabType type = state.getValue(SlabBlock.TYPE);
        if (type == SlabType.BOTTOM) {
            return 0;
        }
        if (type == SlabType.TOP) {
            return 1;
        }

        float minY = Math.min(Math.min(quad.y(0), quad.y(1)), Math.min(quad.y(2), quad.y(3)));
        float maxY = Math.max(Math.max(quad.y(0), quad.y(1)), Math.max(quad.y(2), quad.y(3)));
        float epsilon = 1.0E-4F;

        if (maxY <= 0.5F + epsilon) {
            return 0;
        }
        if (minY >= 0.5F - epsilon) {
            return 1;
        }

        float y = (quad.y(0) + quad.y(1) + quad.y(2) + quad.y(3)) * 0.25F;
        return (y >= 0.5F) ? 1 : 0;
    }

    private static int quadPartForVerticalSlab(BlockState state, MutableQuadView quad) {
        VerticalSlabType type = state.getValue(FramedVerticalSlabBlock.TYPE);
        if (type == VerticalSlabType.SINGLE) {
            return 0;
        }

        Direction facing = state.getValue(FramedVerticalSlabBlock.FACING);
        float minX = Math.min(Math.min(quad.x(0), quad.x(1)), Math.min(quad.x(2), quad.x(3)));
        float maxX = Math.max(Math.max(quad.x(0), quad.x(1)), Math.max(quad.x(2), quad.x(3)));
        float minZ = Math.min(Math.min(quad.z(0), quad.z(1)), Math.min(quad.z(2), quad.z(3)));
        float maxZ = Math.max(Math.max(quad.z(0), quad.z(1)), Math.max(quad.z(2), quad.z(3)));
        float epsilon = 1.0E-4F;

        if (facing == Direction.NORTH || facing == Direction.SOUTH) {
            if (maxZ <= 0.5F + epsilon) {
                return 0;
            }
            if (minZ >= 0.5F - epsilon) {
                return 1;
            }

            float z = (quad.z(0) + quad.z(1) + quad.z(2) + quad.z(3)) * 0.25F;
            return (z >= 0.5F) ? 1 : 0;
        }

        if (maxX <= 0.5F + epsilon) {
            return 0;
        }
        if (minX >= 0.5F - epsilon) {
            return 1;
        }

        float x = (quad.x(0) + quad.x(1) + quad.x(2) + quad.x(3)) * 0.25F;
        return (x >= 0.5F) ? 1 : 0;
    }

    private static void stretchUvToFullSprite(MutableQuadView quad, TextureAtlasSprite sprite) {
        float u0 = quad.u(0);
        float v0 = quad.v(0);
        float u1 = quad.u(1);
        float v1 = quad.v(1);
        float u2 = quad.u(2);
        float v2 = quad.v(2);
        float u3 = quad.u(3);
        float v3 = quad.v(3);

        float uMin = Math.min(Math.min(u0, u1), Math.min(u2, u3));
        float uMax = Math.max(Math.max(u0, u1), Math.max(u2, u3));
        float vMin = Math.min(Math.min(v0, v1), Math.min(v2, v3));
        float vMax = Math.max(Math.max(v0, v1), Math.max(v2, v3));

        float du = uMax - uMin;
        float dv = vMax - vMin;
        if (du == 0 || dv == 0) {
            return;
        }

        float spriteU0 = sprite.getU0();
        float spriteU1 = sprite.getU1();
        float spriteV0 = sprite.getV0();
        float spriteV1 = sprite.getV1();
        float spriteDU = spriteU1 - spriteU0;
        float spriteDV = spriteV1 - spriteV0;

        for (int i = 0; i < 4; i++) {
            float normalizedU = (quad.u(i) - uMin) / du;
            float normalizedV = (quad.v(i) - vMin) / dv;
            quad.uv(i, spriteU0 + normalizedU * spriteDU, spriteV0 + normalizedV * spriteDV);
        }
    }

    private static int quadPartForCheckered(MutableQuadView quad) {
        float centerX = (quad.x(0) + quad.x(1) + quad.x(2) + quad.x(3)) * 0.25F;
        float centerY = (quad.y(0) + quad.y(1) + quad.y(2) + quad.y(3)) * 0.25F;
        float centerZ = (quad.z(0) + quad.z(1) + quad.z(2) + quad.z(3)) * 0.25F;

        int x = (centerX >= 0.5F) ? 1 : 0;
        int y = (centerY >= 0.5F) ? 1 : 0;
        int z = (centerZ >= 0.5F) ? 1 : 0;
        return (x ^ y ^ z) & 1;
    }

    private static int quadPartForCheckeredSlab(BlockState state, MutableQuadView quad) {
        SlabType type = state.getValue(FramedCheckeredSlabBlock.TYPE);
        float centerX = (quad.x(0) + quad.x(1) + quad.x(2) + quad.x(3)) * 0.25F;
        float centerY = (quad.y(0) + quad.y(1) + quad.y(2) + quad.y(3)) * 0.25F;
        float centerZ = (quad.z(0) + quad.z(1) + quad.z(2) + quad.z(3)) * 0.25F;
        int parity = ((centerX >= 0.5F) ? 1 : 0) ^ ((centerZ >= 0.5F) ? 1 : 0);

        int base;
        if (type == SlabType.DOUBLE) {
            float minY = Math.min(Math.min(quad.y(0), quad.y(1)), Math.min(quad.y(2), quad.y(3)));
            float maxY = Math.max(Math.max(quad.y(0), quad.y(1)), Math.max(quad.y(2), quad.y(3)));
            float epsilon = 1.0E-4F;

            if (maxY <= 0.5F + epsilon) {
                base = 0;
            } else if (minY >= 0.5F - epsilon) {
                base = 2;
            } else {
                base = (centerY >= 0.5F) ? 2 : 0;
            }
        } else if (type == SlabType.TOP) {
            base = 2;
        } else {
            base = 0;
        }

        return base + parity;
    }

    private static int quadPartForCheckeredVerticalSlab(BlockState state, MutableQuadView quad) {
        VerticalSlabType type = state.getValue(FramedCheckeredVerticalSlabBlock.TYPE);
        Direction facing = state.getValue(FramedCheckeredVerticalSlabBlock.FACING);
        float centerX = (quad.x(0) + quad.x(1) + quad.x(2) + quad.x(3)) * 0.25F;
        float centerY = (quad.y(0) + quad.y(1) + quad.y(2) + quad.y(3)) * 0.25F;
        float centerZ = (quad.z(0) + quad.z(1) + quad.z(2) + quad.z(3)) * 0.25F;
        float epsilon = 1.0E-4F;

        int half = 0;
        if (type == VerticalSlabType.DOUBLE) {
            if (facing == Direction.NORTH || facing == Direction.SOUTH) {
                float minZ = Math.min(Math.min(quad.z(0), quad.z(1)), Math.min(quad.z(2), quad.z(3)));
                float maxZ = Math.max(Math.max(quad.z(0), quad.z(1)), Math.max(quad.z(2), quad.z(3)));
                if (maxZ <= 0.5F + epsilon) {
                    half = 0;
                } else if (minZ >= 0.5F - epsilon) {
                    half = 1;
                } else {
                    half = (centerZ >= 0.5F) ? 1 : 0;
                }
            } else {
                float minX = Math.min(Math.min(quad.x(0), quad.x(1)), Math.min(quad.x(2), quad.x(3)));
                float maxX = Math.max(Math.max(quad.x(0), quad.x(1)), Math.max(quad.x(2), quad.x(3)));
                if (maxX <= 0.5F + epsilon) {
                    half = 0;
                } else if (minX >= 0.5F - epsilon) {
                    half = 1;
                } else {
                    half = (centerX >= 0.5F) ? 1 : 0;
                }
            }
        }

        int a = (facing == Direction.NORTH || facing == Direction.SOUTH)
                ? ((centerX >= 0.5F) ? 1 : 0)
                : ((centerZ >= 0.5F) ? 1 : 0);
        int b = (centerY >= 0.5F) ? 1 : 0;
        int parity = (a ^ b) & 1;
        return half * 2 + parity;
    }
}
