package com.spider.framedfabric.client.model;

import com.spider.framedfabric.block.FramedSlopeBlock;
import java.util.List;
import java.util.function.Predicate;
import net.fabricmc.fabric.api.client.renderer.v1.mesh.MutableQuadView;
import net.fabricmc.fabric.api.client.renderer.v1.mesh.QuadEmitter;
import net.fabricmc.fabric.api.client.renderer.v1.model.FabricBlockStateModel;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.block.BlockAndTintGetter;
import net.minecraft.client.renderer.block.dispatch.BlockStateModel;
import net.minecraft.client.renderer.block.dispatch.BlockStateModelPart;
import net.minecraft.client.resources.model.sprite.Material;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.Half;
import org.jspecify.annotations.Nullable;

public final class FramedSlopeStateModel implements BlockStateModel, FabricBlockStateModel {
    private static Material.Baked baseMaterial() {
        return Minecraft.getInstance()
                .getModelManager()
                .getBlockStateModelSet()
                .getParticleMaterial(Blocks.STONE.defaultBlockState());
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
        if (!(state.getBlock() instanceof FramedSlopeBlock)) {
            return;
        }

        Material.Baked material = baseMaterial();
        Direction facing = state.getValue(FramedSlopeBlock.FACING);
        Half half = state.getValue(FramedSlopeBlock.HALF);
        int rotSteps = rotStepsFromNorthTo(facing);

        if (half == Half.BOTTOM) {
            emitBottomCanonicalNorth(emitter, material, rotSteps);
        } else {
            emitTopCanonicalNorth(emitter, material, rotSteps);
        }
    }

    @Override
    public @Nullable Object createGeometryKey(BlockAndTintGetter view, BlockPos pos, BlockState state, RandomSource random) {
        if (!(state.getBlock() instanceof FramedSlopeBlock)) {
            return null;
        }

        return new GeometryKey(
                state.getValue(FramedSlopeBlock.FACING),
                state.getValue(FramedSlopeBlock.HALF)
        );
    }

    @Override
    public void collectParts(RandomSource random, List<BlockStateModelPart> parts) {
    }

    @Override
    public Material.Baked particleMaterial() {
        return baseMaterial();
    }

    @Override
    public Material.Baked particleMaterial(BlockAndTintGetter view, BlockPos pos, BlockState state) {
        return baseMaterial();
    }

    @Override
    public int materialFlags() {
        return 0;
    }

    private record GeometryKey(Direction facing, Half half) {}

    private static int rotStepsFromNorthTo(Direction facing) {
        return switch (facing) {
            case NORTH -> 0;
            case EAST -> 1;
            case SOUTH -> 2;
            case WEST -> 3;
            default -> 0;
        };
    }

    private static void emitBottomCanonicalNorth(QuadEmitter emitter, Material.Baked material, int rotSteps) {
        emitAxisSquare(emitter, material, Direction.DOWN, rotSteps);
        emitAxisSquare(emitter, material, Direction.NORTH, rotSteps);

        emitTri(emitter, material, rotateDir(Direction.EAST, rotSteps), rotSteps,
                new float[] {1, 0, 1},
                new float[] {1, 0, 0},
                new float[] {1, 1, 0});

        emitTri(emitter, material, rotateDir(Direction.WEST, rotSteps), rotSteps,
                new float[] {0, 0, 0},
                new float[] {0, 0, 1},
                new float[] {0, 1, 0});

        emitFreeQuad(emitter, material, rotateDir(Direction.UP, rotSteps), rotSteps,
                new float[] {0, 0, 1},
                new float[] {1, 0, 1},
                new float[] {1, 1, 0},
                new float[] {0, 1, 0});
    }

    private static void emitTopCanonicalNorth(QuadEmitter emitter, Material.Baked material, int rotSteps) {
        emitAxisSquare(emitter, material, Direction.UP, rotSteps);
        emitAxisSquare(emitter, material, Direction.NORTH, rotSteps);

        emitTri(emitter, material, rotateDir(Direction.EAST, rotSteps), rotSteps,
                new float[] {1, 1, 1},
                new float[] {1, 1, 0},
                new float[] {1, 0, 1});

        emitTri(emitter, material, rotateDir(Direction.WEST, rotSteps), rotSteps,
                new float[] {0, 1, 0},
                new float[] {0, 1, 1},
                new float[] {0, 0, 1});

        emitFreeQuad(emitter, material, rotateDir(Direction.DOWN, rotSteps), rotSteps,
                new float[] {0, 1, 1},
                new float[] {1, 1, 1},
                new float[] {1, 0, 0},
                new float[] {0, 0, 0});
    }

    private static void emitAxisSquare(QuadEmitter emitter, Material.Baked material, Direction canonicalFace, int rotSteps) {
        Direction face = rotateDir(canonicalFace, rotSteps);

        float depth = switch (face) {
            case DOWN, NORTH, WEST -> 0F;
            case UP, SOUTH, EAST -> 1F;
        };

        emitter.square(face, 0F, 0F, 1F, 1F, depth);
        emitter.color(-1, -1, -1, -1);
        emitter.nominalFace(face);
        emitter.uvUnitSquare();
        emitter.materialBake(material, MutableQuadView.BAKE_LOCK_UV);
        emitter.cullFace(null);
        emitter.emit();
    }

    private static void emitTri(QuadEmitter emitter, Material.Baked material, Direction nominalFace, int rotSteps,
                                float[] a, float[] b, float[] c) {
        float[] aa = rotY(a, rotSteps);
        float[] bb = rotY(b, rotSteps);
        float[] cc = rotY(c, rotSteps);

        if (dot(triNormal(aa, bb, cc), faceVec(nominalFace)) < 0F) {
            float[] tmp = bb;
            bb = cc;
            cc = tmp;
        }

        emitter.pos(0, aa[0], aa[1], aa[2]);
        emitter.pos(1, bb[0], bb[1], bb[2]);
        emitter.pos(2, cc[0], cc[1], cc[2]);
        emitter.pos(3, cc[0], cc[1], cc[2]);

        emitter.uv(0, 0F, 1F);
        emitter.uv(1, 1F, 1F);
        emitter.uv(2, 1F, 0F);
        emitter.uv(3, 1F, 0F);

        emitter.nominalFace(nominalFace);
        emitter.color(-1, -1, -1, -1);
        emitter.materialBake(material, MutableQuadView.BAKE_LOCK_UV);
        emitter.cullFace(null);
        emitter.emit();
    }

    private static void emitFreeQuad(QuadEmitter emitter, Material.Baked material, Direction nominalFace, int rotSteps,
                                     float[] p0, float[] p1, float[] p2, float[] p3) {
        float[] aa = rotY(p0, rotSteps);
        float[] bb = rotY(p1, rotSteps);
        float[] cc = rotY(p2, rotSteps);
        float[] dd = rotY(p3, rotSteps);

        if (dot(triNormal(aa, bb, cc), faceVec(nominalFace)) < 0F) {
            float[] tmp = bb;
            bb = dd;
            dd = tmp;
        }

        emitter.pos(0, aa[0], aa[1], aa[2]);
        emitter.pos(1, bb[0], bb[1], bb[2]);
        emitter.pos(2, cc[0], cc[1], cc[2]);
        emitter.pos(3, dd[0], dd[1], dd[2]);

        emitter.uv(0, 0F, 1F);
        emitter.uv(1, 1F, 1F);
        emitter.uv(2, 1F, 0F);
        emitter.uv(3, 0F, 0F);

        emitter.nominalFace(nominalFace);
        emitter.color(-1, -1, -1, -1);
        emitter.materialBake(material, MutableQuadView.BAKE_LOCK_UV);
        emitter.cullFace(null);
        emitter.emit();
    }

    private static float[] faceVec(Direction direction) {
        return switch (direction) {
            case DOWN -> new float[] {0, -1, 0};
            case UP -> new float[] {0, 1, 0};
            case NORTH -> new float[] {0, 0, -1};
            case SOUTH -> new float[] {0, 0, 1};
            case WEST -> new float[] {-1, 0, 0};
            case EAST -> new float[] {1, 0, 0};
        };
    }

    private static float dot(float[] a, float[] b) {
        return a[0] * b[0] + a[1] * b[1] + a[2] * b[2];
    }

    private static float[] triNormal(float[] a, float[] b, float[] c) {
        float abx = b[0] - a[0];
        float aby = b[1] - a[1];
        float abz = b[2] - a[2];
        float acx = c[0] - a[0];
        float acy = c[1] - a[1];
        float acz = c[2] - a[2];

        return new float[] {
                aby * acz - abz * acy,
                abz * acx - abx * acz,
                abx * acy - aby * acx
        };
    }

    private static float[] rotY(float[] point, int stepsClockwise) {
        float x = point[0] - 0.5F;
        float y = point[1];
        float z = point[2] - 0.5F;

        int steps = stepsClockwise & 3;
        for (int i = 0; i < steps; i++) {
            float nextX = -z;
            float nextZ = x;
            x = nextX;
            z = nextZ;
        }

        return new float[] {x + 0.5F, y, z + 0.5F};
    }

    private static Direction rotateDir(Direction direction, int stepsClockwise) {
        if (direction.getAxis().isVertical()) {
            return direction;
        }

        Direction out = direction;
        int steps = stepsClockwise & 3;
        for (int i = 0; i < steps; i++) {
            out = out.getClockWise();
        }
        return out;
    }
}
