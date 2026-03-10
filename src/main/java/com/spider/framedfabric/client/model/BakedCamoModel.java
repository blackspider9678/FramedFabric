package com.spider.framedfabric.client.model;

import com.spider.framedfabric.block.FramedMiniCubeBlock;
import com.spider.framedfabric.block.custom.FramedCheckeredBlock;
import com.spider.framedfabric.block.custom.FramedVerticalSlabBlock;
import com.spider.framedfabric.blockentity.FramedBlockEntity;
import net.fabricmc.fabric.api.renderer.v1.mesh.MutableQuadView;
import net.fabricmc.fabric.api.renderer.v1.mesh.QuadEmitter;
import net.fabricmc.fabric.api.renderer.v1.mesh.QuadTransform;
import net.fabricmc.fabric.api.renderer.v1.model.FabricBlockStateModel;
import net.minecraft.block.BlockState;
import net.minecraft.block.SlabBlock;
import net.minecraft.block.enums.SlabType;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.render.model.BlockModelPart;
import net.minecraft.client.render.model.BlockStateModel;
import net.minecraft.client.texture.Sprite;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.random.Random;
import net.minecraft.world.BlockRenderView;
import org.jetbrains.annotations.Nullable;

import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.function.Predicate;

public final class BakedCamoModel implements BlockStateModel, FabricBlockStateModel {
    private final BlockStateModel parent;
    public static final int TINT_BASE = 32; // anything >= 16 is fine; 32 is nice and safe

    public BakedCamoModel(BlockStateModel parent) {
        this.parent = parent;
    }

    private static final net.minecraft.util.Identifier POT_DIRT_TEX =
            net.minecraft.util.Identifier.of("minecraft", "block/dirt");

    @Override
    public void emitQuads(
            QuadEmitter emitter,
            BlockRenderView view,
            BlockPos pos,
            BlockState state,
            Random random,
            Predicate<@Nullable Direction> cullTest
    ) {
        var be = view.getBlockEntity(pos);
        if (!(be instanceof FramedBlockEntity fbe) || !fbe.hasAnyCamo()) {
            parent.emitQuads(emitter, view, pos, state, random, cullTest);
            return;
        }

        // Precompute camo models/faceInfo per part we might use (0/1 for slabs, else 0)
        final boolean isCheckeredSlab = (state.getBlock() instanceof com.spider.framedfabric.block.custom.FramedCheckeredSlabBlock);
        final boolean isSlab = (state.getBlock() instanceof net.minecraft.block.SlabBlock);
        final boolean isCheckeredVSlab = (state.getBlock() instanceof com.spider.framedfabric.block.custom.FramedCheckeredVerticalSlabBlock);
        final boolean isVSlab = (state.getBlock() instanceof FramedVerticalSlabBlock);
        final boolean isCheckered = (state.getBlock() instanceof FramedCheckeredBlock);

        PartRender[] parts = new PartRender[FramedBlockEntity.MAX_CAMO_PARTS];
        for (int i = 0; i < FramedBlockEntity.MAX_CAMO_PARTS; i++) {
            if (!fbe.hasCamoPart(i)) continue;

            BlockState camo = fbe.getCamoPart(i);
            if (camo == null || camo.isAir()) continue;

            BlockStateModel camoModel = MinecraftClient.getInstance()
                    .getBlockRenderManager()
                    .getModel(camo);

            Map<Direction, FaceInfo> faceInfo = pickFaceInfo(camoModel, random);
            Sprite fallbackSprite = camoModel.particleSprite(view, pos, camo);

            parts[i] = new PartRender(camo, fbe.getCamoRotPart(i), faceInfo, fallbackSprite);
        }

        // If somehow all are null, fallback
        boolean any = false;
        for (PartRender pr : parts) if (pr != null) { any = true; break; }
        if (!any) {
            parent.emitQuads(emitter, view, pos, state, random, cullTest);
            return;
        }

        QuadTransform swap = quad -> {
            // ✅ don't camo-swap the flower pot dirt layer
            if (quad.tintIndex() == 7) {
                return true;
            }

            int partIndex = 0;

            Direction outFace = guessFaceFromGeometry(quad);
            if (outFace == null) outFace = quad.nominalFace();
            if (outFace == null) outFace = quad.lightFace();
            if (outFace == null) outFace = quad.cullFace();
            if (outFace == null) return true;

            if (isSlab) {
                partIndex = quadPartForSlab(state, quad, outFace);
            } else if (isVSlab) {
                partIndex = quadPartForVerticalSlab(state, quad);
            } else if (isCheckered) {
                partIndex = quadPartForCheckered(quad); // <--- NEW
            } else if (isCheckeredSlab) {
            partIndex = quadPartForCheckeredSlab(state, quad);
            } else if (isCheckeredVSlab) {
                partIndex = quadPartForCheckeredVerticalSlab(state, quad);
            }


            PartRender pr = parts[partIndex];

            // If this part has no camo, make sure it NEVER participates in tinting.
            // (Prevents part0 camo tint bleeding onto part1 geometry.)
            if (pr == null) {
                if (quad.tintIndex() != 7) { // keep flower pot rule safe
                    quad.tintIndex(-1);
                }
                return true;
            }

            FaceMap map = mapFace(pr.rot, outFace);
            Direction srcFace = map.srcFace();
            int turnsCW = map.uvTurnsCW();

            FaceInfo info = pr.faceInfo.get(srcFace);
            Sprite sprite = (info != null) ? info.sprite() : pr.fallbackSprite;
            int tint = (info != null) ? info.tintIndex() : -1;

            quad.nominalFace(outFace);

            boolean isMini = state.getBlock() instanceof FramedMiniCubeBlock;

            int bakeFlags = (isMini ? 0 : MutableQuadView.BAKE_LOCK_UV) | bakeRotFlag(turnsCW);
            quad.spriteBake(sprite, bakeFlags);

            if (isMini) {
                // Force the camo sprite to use the full texture on this tiny face
                stretchUvToFullSprite(quad, sprite);
            }

            SlabType slabType = isSlab ? state.get(SlabBlock.TYPE) : null;

            // tint from camo model (0 for grass/leaves overlay, -1 for untinted)
            int tintLayer = (info != null) ? info.tintIndex() : -1;

            if (isSlab && slabType == SlabType.DOUBLE) {
                // existing behavior (keep)
                if (tintLayer >= 0) {
                    quad.tintIndex(TINT_BASE + tintLayer + (partIndex * 8)); // bottom 32..39, top 40..47
                } else {
                    quad.tintIndex(-1);
                }
            } else if (isVSlab && state.get(FramedVerticalSlabBlock.TYPE) == com.spider.framedfabric.block.enums.VerticalSlabType.DOUBLE) {
                // NEW: vertical double slab must also pack part into tint index
                if (tintLayer >= 0) {
                    quad.tintIndex(TINT_BASE + tintLayer + (partIndex * 8)); // part0 32..39, part1 40..47
                } else {
                    quad.tintIndex(-1);
                }
            } else if (isCheckered) {
                if (tintLayer >= 0) {
                    quad.tintIndex(TINT_BASE + tintLayer + (partIndex * 8));
                } else {
                    quad.tintIndex(-1);
                }
            } else if (isCheckeredSlab) {
                if (tintLayer >= 0) quad.tintIndex(TINT_BASE + tintLayer + (partIndex * 8));
                else quad.tintIndex(-1);
            } else if (isCheckeredVSlab) {
                if (tintLayer >= 0) quad.tintIndex(TINT_BASE + tintLayer + (partIndex * 8));
                else quad.tintIndex(-1);
            }
            else {
                // ✅ Single-part blocks + single slabs + any non-packed case:
                // carry the camo's tint index directly (0..), or -1 if untinted.
                quad.tintIndex(tintLayer);
            }


            return true;
        };

        emitter.pushTransform(swap);
        parent.emitQuads(emitter, view, pos, state, random, cullTest);
        emitter.popTransform();
    }

    private record PartRender(
            BlockState camo,
            int rot,
            Map<Direction, FaceInfo> faceInfo,
            Sprite fallbackSprite
    ) {}


    private static int bakeRotFlag(int turnsCW) {
        return switch (turnsCW & 3) {
            case 1 -> MutableQuadView.BAKE_ROTATE_90;
            case 2 -> MutableQuadView.BAKE_ROTATE_180;
            case 3 -> MutableQuadView.BAKE_ROTATE_270;
            default -> MutableQuadView.BAKE_ROTATE_NONE;
        };
    }

    // ----------------------------
    // Face map + UV rotate tables
    // ----------------------------

    private record FaceMap(Direction srcFace, int uvTurnsCW) {}

    /**
     * ROT modes (your wrench):
     * 1 = normal
     * 2 = flip upside down (X 180)          (UP<->DOWN, NORTH<->SOUTH)
     * 3 = NORTH becomes UP (X +90)
     * 4 = SOUTH becomes UP (X -90)
     * 5 = EAST becomes UP  (Z +90)
     * 6 = WEST becomes UP  (Z -90)
     *
     * uvTurnsCW:
     * 0=0°, 1=90° cw, 2=180°, 3=270° cw
     */
    private static FaceMap mapFace(int rot, Direction out) {
        return switch (rot) {
            case 2 -> switch (out) {
                case UP    -> new FaceMap(Direction.DOWN, 0);
                case DOWN  -> new FaceMap(Direction.UP, 0);
                case NORTH -> new FaceMap(Direction.SOUTH, 2);
                case SOUTH -> new FaceMap(Direction.NORTH, 2);
                case EAST  -> new FaceMap(Direction.EAST, 2);
                case WEST  -> new FaceMap(Direction.WEST, 2);
            };

            case 3 -> switch (out) {
                case UP    -> new FaceMap(Direction.NORTH, 2);
                case DOWN  -> new FaceMap(Direction.SOUTH, 0);
                case NORTH -> new FaceMap(Direction.DOWN, 0);
                case SOUTH -> new FaceMap(Direction.UP, 2);
                case EAST  -> new FaceMap(Direction.EAST, 3);
                case WEST  -> new FaceMap(Direction.WEST, 1);
            };

            case 4 -> switch (out) {
                case UP    -> new FaceMap(Direction.SOUTH, 0);
                case DOWN  -> new FaceMap(Direction.NORTH, 2);
                case NORTH -> new FaceMap(Direction.UP, 0);
                case SOUTH -> new FaceMap(Direction.DOWN, 2);
                case EAST  -> new FaceMap(Direction.EAST, 1);
                case WEST  -> new FaceMap(Direction.WEST, 3);
            };

            case 5 -> switch (out) {
                case UP    -> new FaceMap(Direction.EAST, 3); // was 1
                case DOWN  -> new FaceMap(Direction.WEST, 3); // was 1
                case NORTH -> new FaceMap(Direction.NORTH, 1);
                case SOUTH -> new FaceMap(Direction.SOUTH, 3);
                case EAST  -> new FaceMap(Direction.DOWN, 3);
                case WEST  -> new FaceMap(Direction.UP, 3);
            };

            case 6 -> switch (out) {
                case UP    -> new FaceMap(Direction.WEST, 1); // was 3
                case DOWN  -> new FaceMap(Direction.EAST, 1); // was 3
                case NORTH -> new FaceMap(Direction.NORTH, 3);
                case SOUTH -> new FaceMap(Direction.SOUTH, 1);
                case EAST  -> new FaceMap(Direction.UP, 1);
                case WEST  -> new FaceMap(Direction.DOWN, 1);
            };

            default -> new FaceMap(out, 0); // rot 1 or anything else
        };
    }

    /**
     * Rotate sprite UVs on the quad around its own UV bounds.
     * spriteIndex is usually 0.
     */
    private static void rotateSpriteUV(MutableQuadView quad, int turnsCW) {
        turnsCW &= 3;
        if (turnsCW == 0) return;

        // QuadView getters (MutableQuadView extends QuadView)
        float u0 = quad.u(0), v0 = quad.v(0);
        float u1 = quad.u(1), v1 = quad.v(1);
        float u2 = quad.u(2), v2 = quad.v(2);
        float u3 = quad.u(3), v3 = quad.v(3);

        float uMin = Math.min(Math.min(u0, u1), Math.min(u2, u3));
        float uMax = Math.max(Math.max(u0, u1), Math.max(u2, u3));
        float vMin = Math.min(Math.min(v0, v1), Math.min(v2, v3));
        float vMax = Math.max(Math.max(v0, v1), Math.max(v2, v3));

        float du = uMax - uMin;
        float dv = vMax - vMin;
        if (du == 0 || dv == 0) return;

        for (int i = 0; i < 4; i++) {
            float u = quad.u(i);
            float v = quad.v(i);

            float nu = (u - uMin) / du; // 0..1
            float nv = (v - vMin) / dv; // 0..1

            float ru, rv;
            switch (turnsCW) {
                case 1 -> { // 90 cw
                    ru = nv;
                    rv = 1f - nu;
                }
                case 2 -> { // 180
                    ru = 1f - nu;
                    rv = 1f - nv;
                }
                case 3 -> { // 270 cw
                    ru = 1f - nv;
                    rv = nu;
                }
                default -> {
                    ru = nu;
                    rv = nv;
                }
            }

            quad.uv(i, uMin + ru * du, vMin + rv * dv);
        }
    }

    // ----------------------------
    // Sprite/tint extraction
    // ----------------------------

    private record FaceInfo(Sprite sprite, int tintIndex) {}

    private static Map<Direction, FaceInfo> pickFaceInfo(BlockStateModel model, Random random) {
        EnumMap<Direction, FaceInfo> out = new EnumMap<>(Direction.class);

        java.util.ArrayList<BlockModelPart> parts = new java.util.ArrayList<>();
        model.addParts(random, parts);

        for (BlockModelPart part : parts) {
            for (Direction d : Direction.values()) {
                var qs = part.getQuads(d);
                if (!qs.isEmpty() && !out.containsKey(d)) {
                    var q = qs.getFirst();
                    out.put(d, new FaceInfo(q.sprite(), q.tintIndex()));
                }
            }
            if (out.size() == 6) break;
        }
        return out;
    }

    private static @Nullable Direction guessFaceFromGeometry(MutableQuadView q) {
        float x0 = q.x(0), y0 = q.y(0), z0 = q.z(0);
        float x1 = q.x(1), y1 = q.y(1), z1 = q.z(1);
        float x2 = q.x(2), y2 = q.y(2), z2 = q.z(2);

        float ax = x1 - x0, ay = y1 - y0, az = z1 - z0;
        float bx = x2 - x0, by = y2 - y0, bz = z2 - z0;

        float nx = ay * bz - az * by;
        float ny = az * bx - ax * bz;
        float nz = ax * by - ay * bx;

        float absX = Math.abs(nx), absY = Math.abs(ny), absZ = Math.abs(nz);
        if (absX < 1e-6f && absY < 1e-6f && absZ < 1e-6f) return null;

        if (absY >= absX && absY >= absZ) return ny > 0 ? Direction.UP : Direction.DOWN;
        if (absX >= absZ)                return nx > 0 ? Direction.EAST : Direction.WEST;
        return nz > 0 ? Direction.SOUTH : Direction.NORTH;
    }

    @Override
    public Sprite particleSprite(BlockRenderView view, BlockPos pos, BlockState state) {
        return parent.particleSprite(view, pos, state);
    }

    @Override
    public void addParts(Random random, List<BlockModelPart> parts) {
        parent.addParts(random, parts);
    }

    @Override
    public Sprite particleSprite() {
        return parent.particleSprite();
    }

    private static int quadPartForSlab(BlockState state, MutableQuadView quad, Direction outFace) {
        SlabType type = state.get(SlabBlock.TYPE);

        // SINGLE slabs: everything is one part
        if (type == SlabType.BOTTOM) return 0;
        if (type == SlabType.TOP)    return 1;

        // DOUBLE slabs: decide per-quad using minY/maxY so y=0.5 seam works correctly
        float minY = Math.min(Math.min(quad.y(0), quad.y(1)), Math.min(quad.y(2), quad.y(3)));
        float maxY = Math.max(Math.max(quad.y(0), quad.y(1)), Math.max(quad.y(2), quad.y(3)));

        // bottom element quads are in [0..0.5], top element quads are in [0.5..1]
        // seam faces sit exactly at 0.5, so we nudge based on which face it is.
        final float EPS = 1e-4f;

        if (maxY <= 0.5f + EPS) {
            // this includes the bottom element UP face at y=0.5
            return 0;
        }
        if (minY >= 0.5f - EPS) {
            // this includes the top element DOWN face at y=0.5
            return 1;
        }

        // fallback (should be rare)
        float y = (quad.y(0) + quad.y(1) + quad.y(2) + quad.y(3)) * 0.25f;
        return (y >= 0.5f) ? 1 : 0;
    }

    private static int quadPartForVerticalSlab(BlockState state, MutableQuadView quad) {
        var type = state.get(FramedVerticalSlabBlock.TYPE);

        // SINGLE: one part
        if (type == com.spider.framedfabric.block.enums.VerticalSlabType.SINGLE) return 0;

        Direction facing = state.get(FramedVerticalSlabBlock.FACING);

        float minX = Math.min(Math.min(quad.x(0), quad.x(1)), Math.min(quad.x(2), quad.x(3)));
        float maxX = Math.max(Math.max(quad.x(0), quad.x(1)), Math.max(quad.x(2), quad.x(3)));
        float minZ = Math.min(Math.min(quad.z(0), quad.z(1)), Math.min(quad.z(2), quad.z(3)));
        float maxZ = Math.max(Math.max(quad.z(0), quad.z(1)), Math.max(quad.z(2), quad.z(3)));

        final float EPS = 1e-4f;

        // Split axis depends on facing
        if (facing == Direction.NORTH || facing == Direction.SOUTH) {
            // Z split (north/south) at 0.5
            if (maxZ <= 0.5f + EPS) return 0;
            if (minZ >= 0.5f - EPS) return 1;
            float z = (quad.z(0) + quad.z(1) + quad.z(2) + quad.z(3)) * 0.25f;
            return (z >= 0.5f) ? 1 : 0;
        } else {
            // X split (west/east) at 0.5
            if (maxX <= 0.5f + EPS) return 0;
            if (minX >= 0.5f - EPS) return 1;
            float x = (quad.x(0) + quad.x(1) + quad.x(2) + quad.x(3)) * 0.25f;
            return (x >= 0.5f) ? 1 : 0;
        }
    }
    private static void stretchUvToFullSprite(MutableQuadView quad, Sprite sprite) {
        float u0 = quad.u(0), v0 = quad.v(0);
        float u1 = quad.u(1), v1 = quad.v(1);
        float u2 = quad.u(2), v2 = quad.v(2);
        float u3 = quad.u(3), v3 = quad.v(3);

        float uMin = Math.min(Math.min(u0, u1), Math.min(u2, u3));
        float uMax = Math.max(Math.max(u0, u1), Math.max(u2, u3));
        float vMin = Math.min(Math.min(v0, v1), Math.min(v2, v3));
        float vMax = Math.max(Math.max(v0, v1), Math.max(v2, v3));

        float du = uMax - uMin;
        float dv = vMax - vMin;
        if (du == 0 || dv == 0) return;

        float sMinU = sprite.getMinU(), sMaxU = sprite.getMaxU();
        float sMinV = sprite.getMinV(), sMaxV = sprite.getMaxV();
        float sDu = sMaxU - sMinU;
        float sDv = sMaxV - sMinV;

        for (int i = 0; i < 4; i++) {
            float nu = (quad.u(i) - uMin) / du; // 0..1 across current quad UV bounds
            float nv = (quad.v(i) - vMin) / dv;
            quad.uv(i, sMinU + nu * sDu, sMinV + nv * sDv);
        }
    }

    private static int quadPartForCheckered(MutableQuadView quad) {
        float cx = (quad.x(0) + quad.x(1) + quad.x(2) + quad.x(3)) * 0.25f;
        float cy = (quad.y(0) + quad.y(1) + quad.y(2) + quad.y(3)) * 0.25f;
        float cz = (quad.z(0) + quad.z(1) + quad.z(2) + quad.z(3)) * 0.25f;

        int xi = (cx >= 0.5f) ? 1 : 0;
        int yi = (cy >= 0.5f) ? 1 : 0;
        int zi = (cz >= 0.5f) ? 1 : 0;

        return (xi ^ yi ^ zi) & 1; // ✅ true 3D checker
    }

    private static int quadPartForCheckeredSlab(BlockState state, MutableQuadView quad) {
        SlabType type = state.get(SlabBlock.TYPE);

        float cx = (quad.x(0) + quad.x(1) + quad.x(2) + quad.x(3)) * 0.25f;
        float cy = (quad.y(0) + quad.y(1) + quad.y(2) + quad.y(3)) * 0.25f;
        float cz = (quad.z(0) + quad.z(1) + quad.z(2) + quad.z(3)) * 0.25f;

        int parity = ((cx >= 0.5f) ? 1 : 0) ^ ((cz >= 0.5f) ? 1 : 0);

        int base;
        if (type == SlabType.DOUBLE) {
            // Use minY/maxY to be seam-safe
            float minY = Math.min(Math.min(quad.y(0), quad.y(1)), Math.min(quad.y(2), quad.y(3)));
            float maxY = Math.max(Math.max(quad.y(0), quad.y(1)), Math.max(quad.y(2), quad.y(3)));
            final float EPS = 1e-4f;

            if (maxY <= 0.5f + EPS) base = 0;
            else if (minY >= 0.5f - EPS) base = 2;
            else base = (cy >= 0.5f) ? 2 : 0;
        } else if (type == SlabType.TOP) {
            base = 2;
        } else {
            base = 0;
        }

        return base + parity; // 0..3
    }

    private static int quadPartForCheckeredVerticalSlab(BlockState state, MutableQuadView quad) {
        var type = state.get(com.spider.framedfabric.block.custom.FramedCheckeredVerticalSlabBlock.TYPE);
        Direction facing = state.get(com.spider.framedfabric.block.custom.FramedCheckeredVerticalSlabBlock.FACING);

        float cx = (quad.x(0) + quad.x(1) + quad.x(2) + quad.x(3)) * 0.25f;
        float cy = (quad.y(0) + quad.y(1) + quad.y(2) + quad.y(3)) * 0.25f;
        float cz = (quad.z(0) + quad.z(1) + quad.z(2) + quad.z(3)) * 0.25f;

        final float EPS = 1e-4f;

        int half = 0;
        if (type == com.spider.framedfabric.block.enums.VerticalSlabType.DOUBLE) {
            if (facing == Direction.NORTH || facing == Direction.SOUTH) {
                float minZ = Math.min(Math.min(quad.z(0), quad.z(1)), Math.min(quad.z(2), quad.z(3)));
                float maxZ = Math.max(Math.max(quad.z(0), quad.z(1)), Math.max(quad.z(2), quad.z(3)));
                if (maxZ <= 0.5f + EPS) half = 0;
                else if (minZ >= 0.5f - EPS) half = 1;
                else half = (cz >= 0.5f) ? 1 : 0;
            } else {
                float minX = Math.min(Math.min(quad.x(0), quad.x(1)), Math.min(quad.x(2), quad.x(3)));
                float maxX = Math.max(Math.max(quad.x(0), quad.x(1)), Math.max(quad.x(2), quad.x(3)));
                if (maxX <= 0.5f + EPS) half = 0;
                else if (minX >= 0.5f - EPS) half = 1;
                else half = (cx >= 0.5f) ? 1 : 0;
            }
        }

        // N/S -> checker on X/Y, E/W -> checker on Z/Y
        int a = (facing == Direction.NORTH || facing == Direction.SOUTH) ? ((cx >= 0.5f) ? 1 : 0) : ((cz >= 0.5f) ? 1 : 0);
        int b = (cy >= 0.5f) ? 1 : 0;

        int parity = (a ^ b) & 1; // 0/1
        return half * 2 + parity; // 0..3
    }

}
