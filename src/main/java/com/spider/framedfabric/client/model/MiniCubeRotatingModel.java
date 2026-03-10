package com.spider.framedfabric.client.model;

import com.spider.framedfabric.block.FramedMiniCubeBlock;
import net.fabricmc.fabric.api.renderer.v1.mesh.MutableQuadView;
import net.fabricmc.fabric.api.renderer.v1.mesh.QuadEmitter;
import net.fabricmc.fabric.api.renderer.v1.mesh.QuadTransform;
import net.fabricmc.fabric.api.renderer.v1.model.FabricBlockStateModel;
import net.minecraft.block.BlockState;
import net.minecraft.block.enums.BlockFace;
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

public final class MiniCubeRotatingModel implements BlockStateModel, FabricBlockStateModel {

    private final BlockStateModel parent;

    public MiniCubeRotatingModel(BlockStateModel parent) {
        this.parent = parent;
    }

    @Override
    public void emitQuads(
            QuadEmitter emitter,
            BlockRenderView view,
            BlockPos pos,
            BlockState state,
            Random random,
            Predicate<@Nullable Direction> cullTest
    ) {
        // Only our mini cube
        if (!(state.getBlock() instanceof FramedMiniCubeBlock)) {
            parent.emitQuads(emitter, view, pos, state, random, cullTest);
            return;
        }

        // If you ALSO want wall-mounted to rotate in 16 steps, delete this WALL early-return.
        BlockFace mount = state.get(FramedMiniCubeBlock.FACE);
        if (mount == BlockFace.WALL) {
            parent.emitQuads(emitter, view, pos, state, random, cullTest);
            return;
        }

        int rot16 = state.get(FramedMiniCubeBlock.ROTATION_16) & 15; // 0..15
        if (rot16 == 0) {
            parent.emitQuads(emitter, view, pos, state, random, cullTest);
            return;
        }

        final float ang = (float) (rot16 * (Math.PI / 8.0)); // 22.5° per step
        final float sin = (float) Math.sin(ang);
        final float cos = (float) Math.cos(ang);

        QuadTransform t = quad -> {
            rotateQuadYAboutCenter(quad, sin, cos);

            // IMPORTANT: non-90° rotation breaks vanilla directional cull.
            quad.cullFace(null);

            // Optional: update nominal face (helps lighting / AO a bit in some pipelines)
            Direction guess = guessFaceFromGeometry(quad);
            if (guess != null) {
                quad.nominalFace(guess);
                // quad.lightFace(...) is NOT a setter in 1.21.11 -> don't call it
            }

            return true;
        };

        emitter.pushTransform(t);
        parent.emitQuads(emitter, view, pos, state, random, cullTest);
        emitter.popTransform();
    }

    private static void rotateQuadYAboutCenter(MutableQuadView q, float sin, float cos) {
        final float cx = 0.5f;
        final float cz = 0.5f;

        for (int i = 0; i < 4; i++) {
            float x = q.x(i) - cx;
            float z = q.z(i) - cz;

            float rx = x * cos - z * sin;
            float rz = x * sin + z * cos;

            q.pos(i, rx + cx, q.y(i), rz + cz);
        }
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
}
