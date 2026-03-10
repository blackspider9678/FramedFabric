package com.spider.framedfabric.client.model;

import com.spider.framedfabric.block.FramedSlopeBlock;
import net.fabricmc.fabric.api.renderer.v1.mesh.MutableQuadView;
import net.fabricmc.fabric.api.renderer.v1.mesh.QuadEmitter;
import net.fabricmc.fabric.api.renderer.v1.model.FabricBlockStateModel;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.block.enums.BlockHalf;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.render.model.BlockModelPart;
import net.minecraft.client.render.model.BlockStateModel;
import net.minecraft.client.texture.Sprite;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.random.Random;
import net.minecraft.world.BlockRenderView;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.function.Predicate;

public final class FramedSlopeStateModel implements BlockStateModel, FabricBlockStateModel {

    // Stable sprite source (no atlas API guessing)
    private static Sprite baseSprite() {
        return MinecraftClient.getInstance()
                .getBlockRenderManager()
                .getModels()
                .getModel(Blocks.STONE.getDefaultState())
                .particleSprite();
    }

    @Override
    public void emitQuads(
            QuadEmitter e,
            BlockRenderView view,
            BlockPos pos,
            BlockState state,
            Random random,
            Predicate<@Nullable Direction> cullTest
    ) {
        if (!(state.getBlock() instanceof FramedSlopeBlock)) return;

        Sprite sprite = baseSprite();

        Direction facing = state.get(FramedSlopeBlock.FACING);
        BlockHalf half = state.get(FramedSlopeBlock.HALF);

        // ✅ CANONICAL MODEL = NORTH
        // so rotate NORTH->target facing
        int rotSteps = rotStepsFromNorthTo(facing);

        if (half == BlockHalf.BOTTOM) {
            emitBottomCanonicalNorth(e, sprite, rotSteps);
        } else {
            emitTopCanonicalNorth(e, sprite, rotSteps);
        }
    }

    private static int rotStepsFromNorthTo(Direction facing) {
        // rotateYClockwise: NORTH->EAST->SOUTH->WEST
        return switch (facing) {
            case NORTH -> 0;
            case EAST  -> 1;
            case SOUTH -> 2;
            case WEST  -> 3;
            default -> 0;
        };
    }

    // =========================================================
    // Canonical geometry (FACING = NORTH)
    //
    // Define “high end” and the vertical back wall to match the
    // one case you confirmed is correct in-game.
    //
    // So for BOTTOM half:
    // - back wall is at NORTH
    // - slope rises toward NORTH
    // =========================================================

    private static void emitBottomCanonicalNorth(QuadEmitter e, Sprite sprite, int rotSteps) {
        // bottom face
        emitAxisSquare(e, sprite, Direction.DOWN, rotSteps);

        // back wall at the HIGH end (canonical NORTH)
        emitAxisSquare(e, sprite, Direction.NORTH, rotSteps);

        // side caps (same geometry you had working in NORTH case)
        emitTri(e, sprite, rotateDir(Direction.EAST, rotSteps), rotSteps,
                new float[]{1, 0, 1},
                new float[]{1, 0, 0},
                new float[]{1, 1, 0});

        emitTri(e, sprite, rotateDir(Direction.WEST, rotSteps), rotSteps,
                new float[]{0, 0, 0},
                new float[]{0, 0, 1},
                new float[]{0, 1, 0});

        // slope face (diagonal) rising toward NORTH
        emitFreeQuad(e, sprite, rotateDir(Direction.UP, rotSteps), rotSteps,
                new float[]{0, 0, 1},
                new float[]{1, 0, 1},
                new float[]{1, 1, 0},
                new float[]{0, 1, 0});
    }

    private static void emitTopCanonicalNorth(QuadEmitter e, Sprite sprite, int rotSteps) {
        // top face
        emitAxisSquare(e, sprite, Direction.UP, rotSteps);

        // back wall at the HIGH end (canonical NORTH)
        emitAxisSquare(e, sprite, Direction.NORTH, rotSteps);

        // side caps
        emitTri(e, sprite, rotateDir(Direction.EAST, rotSteps), rotSteps,
                new float[]{1, 1, 1},
                new float[]{1, 1, 0},
                new float[]{1, 0, 1});

        emitTri(e, sprite, rotateDir(Direction.WEST, rotSteps), rotSteps,
                new float[]{0, 1, 0},
                new float[]{0, 1, 1},
                new float[]{0, 0, 1});

        // underside slope (diagonal) descending away from NORTH
        emitFreeQuad(e, sprite, rotateDir(Direction.DOWN, rotSteps), rotSteps,
                new float[]{0, 1, 1},
                new float[]{1, 1, 1},
                new float[]{1, 0, 0},
                new float[]{0, 0, 0});
    }

    // -------------------------
    // Axis-aligned face
    // -------------------------
    private static void emitAxisSquare(QuadEmitter e, Sprite sprite, Direction canonicalFace, int rotSteps) {
        Direction face = rotateDir(canonicalFace, rotSteps);

        float depth = switch (face) {
            case DOWN, NORTH, WEST -> 0f;
            case UP, SOUTH, EAST -> 1f;
        };

        e.square(face, 0f, 0f, 1f, 1f, depth);
        e.color(-1, -1, -1, -1);
        e.nominalFace(face);
        e.uvUnitSquare();
        e.spriteBake(sprite, MutableQuadView.BAKE_LOCK_UV);
        e.cullFace(null);
        e.emit();
    }

    // -------------------------
    // Triangle as degenerate quad
    // -------------------------
    private static void emitTri(QuadEmitter e, Sprite sprite, Direction nominalFace, int rotSteps,
                                float[] a, float[] b, float[] c) {
        float[] A = rotY(a, rotSteps);
        float[] B = rotY(b, rotSteps);
        float[] C = rotY(c, rotSteps);

        if (dot(triNormal(A, B, C), faceVec(nominalFace)) < 0f) {
            float[] tmp = B; B = C; C = tmp;
        }

        e.pos(0, A[0], A[1], A[2]);
        e.pos(1, B[0], B[1], B[2]);
        e.pos(2, C[0], C[1], C[2]);
        e.pos(3, C[0], C[1], C[2]); // degenerate

        e.uv(0, 0f, 1f);
        e.uv(1, 1f, 1f);
        e.uv(2, 1f, 0f);
        e.uv(3, 1f, 0f);

        e.nominalFace(nominalFace);
        e.color(-1, -1, -1, -1);
        e.spriteBake(sprite, MutableQuadView.BAKE_LOCK_UV);
        e.cullFace(null);
        e.emit();
    }

    // -------------------------
    // Free quad
    // -------------------------
    private static void emitFreeQuad(QuadEmitter e, Sprite sprite, Direction nominalFace, int rotSteps,
                                     float[] p0, float[] p1, float[] p2, float[] p3) {
        float[] A = rotY(p0, rotSteps);
        float[] B = rotY(p1, rotSteps);
        float[] C = rotY(p2, rotSteps);
        float[] D = rotY(p3, rotSteps);

        if (dot(triNormal(A, B, C), faceVec(nominalFace)) < 0f) {
            float[] tmp = B; B = D; D = tmp;
        }

        e.pos(0, A[0], A[1], A[2]);
        e.pos(1, B[0], B[1], B[2]);
        e.pos(2, C[0], C[1], C[2]);
        e.pos(3, D[0], D[1], D[2]);

        e.uv(0, 0f, 1f);
        e.uv(1, 1f, 1f);
        e.uv(2, 1f, 0f);
        e.uv(3, 0f, 0f);

        e.nominalFace(nominalFace);
        e.color(-1, -1, -1, -1);
        e.spriteBake(sprite, MutableQuadView.BAKE_LOCK_UV);
        e.cullFace(null);
        e.emit();
    }

    // -------------------------
    // Math
    // -------------------------
    private static float[] faceVec(Direction d) {
        return switch (d) {
            case DOWN  -> new float[]{ 0, -1,  0};
            case UP    -> new float[]{ 0,  1,  0};
            case NORTH -> new float[]{ 0,  0, -1};
            case SOUTH -> new float[]{ 0,  0,  1};
            case WEST  -> new float[]{-1,  0,  0};
            case EAST  -> new float[]{ 1,  0,  0};
        };
    }

    private static float dot(float[] a, float[] b) {
        return a[0]*b[0] + a[1]*b[1] + a[2]*b[2];
    }

    private static float[] triNormal(float[] a, float[] b, float[] c) {
        float abx = b[0]-a[0], aby = b[1]-a[1], abz = b[2]-a[2];
        float acx = c[0]-a[0], acy = c[1]-a[1], acz = c[2]-a[2];
        return new float[]{
                aby*acz - abz*acy,
                abz*acx - abx*acz,
                abx*acy - aby*acx
        };
    }

    private static float[] rotY(float[] p, int stepsCW) {
        float x = p[0], y = p[1], z = p[2];
        x -= 0.5f; z -= 0.5f;
        int s = stepsCW & 3;
        for (int i = 0; i < s; i++) {
            float nx = -z;
            float nz = x;
            x = nx;
            z = nz;
        }
        x += 0.5f; z += 0.5f;
        return new float[]{x, y, z};
    }

    private static Direction rotateDir(Direction d, int stepsCW) {
        if (d.getAxis().isVertical()) return d;
        Direction out = d;
        int s = stepsCW & 3;
        for (int i = 0; i < s; i++) out = out.rotateYClockwise();
        return out;
    }

    @Override
    public Sprite particleSprite(BlockRenderView view, BlockPos pos, BlockState state) {
        return baseSprite();
    }

    @Override
    public Sprite particleSprite() {
        return baseSprite();
    }

    @Override
    public void addParts(Random random, List<BlockModelPart> parts) {
        // no-op
    }
}
