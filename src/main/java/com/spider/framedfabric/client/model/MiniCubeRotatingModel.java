package com.spider.framedfabric.client.model;

import com.spider.framedfabric.block.FramedMiniCubeBlock;
import java.util.List;
import java.util.function.Predicate;
import net.fabricmc.fabric.api.client.renderer.v1.mesh.MutableQuadView;
import net.fabricmc.fabric.api.client.renderer.v1.mesh.QuadEmitter;
import net.fabricmc.fabric.api.client.renderer.v1.mesh.QuadTransform;
import net.fabricmc.fabric.api.client.renderer.v1.model.FabricBlockStateModel;
import net.minecraft.client.renderer.block.BlockAndTintGetter;
import net.minecraft.client.renderer.block.dispatch.BlockStateModel;
import net.minecraft.client.renderer.block.dispatch.BlockStateModelPart;
import net.minecraft.client.resources.model.sprite.Material;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.AttachFace;
import org.jspecify.annotations.Nullable;

public final class MiniCubeRotatingModel implements BlockStateModel, FabricBlockStateModel {
    private final BlockStateModel parent;

    public MiniCubeRotatingModel(BlockStateModel parent) {
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
        if (!(state.getBlock() instanceof FramedMiniCubeBlock)) {
            ((FabricBlockStateModel) parent).emitQuads(emitter, view, pos, state, random, cullTest);
            return;
        }

        AttachFace mount = state.getValue(FramedMiniCubeBlock.FACE);
        if (mount == AttachFace.WALL) {
            ((FabricBlockStateModel) parent).emitQuads(emitter, view, pos, state, random, cullTest);
            return;
        }

        int rot16 = state.getValue(FramedMiniCubeBlock.ROTATION_16) & 15;
        if (rot16 == 0) {
            ((FabricBlockStateModel) parent).emitQuads(emitter, view, pos, state, random, cullTest);
            return;
        }

        float angle = (float) (rot16 * (Math.PI / 8.0));
        float sin = (float) Math.sin(angle);
        float cos = (float) Math.cos(angle);

        QuadTransform transform = quad -> {
            rotateQuadYAboutCenter(quad, sin, cos);
            quad.cullFace(null);

            Direction guess = guessFaceFromGeometry(quad);
            if (guess != null) {
                quad.nominalFace(guess);
            }

            return true;
        };

        emitter.pushTransform(transform);
        ((FabricBlockStateModel) parent).emitQuads(emitter, view, pos, state, random, cullTest);
        emitter.popTransform();
    }

    @Override
    public @Nullable Object createGeometryKey(BlockAndTintGetter view, BlockPos pos, BlockState state, RandomSource random) {
        Object parentKey = ((FabricBlockStateModel) parent).createGeometryKey(view, pos, state, random);
        if (parentKey == null || !(state.getBlock() instanceof FramedMiniCubeBlock)) {
            return parentKey;
        }

        AttachFace mount = state.getValue(FramedMiniCubeBlock.FACE);
        if (mount == AttachFace.WALL) {
            return parentKey;
        }

        int rot16 = state.getValue(FramedMiniCubeBlock.ROTATION_16) & 15;
        if (rot16 == 0) {
            return parentKey;
        }

        return new GeometryKey(parentKey, mount, rot16);
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
    public Material.Baked particleMaterial(BlockAndTintGetter view, BlockPos pos, BlockState state) {
        return ((FabricBlockStateModel) parent).particleMaterial(view, pos, state);
    }

    @Override
    public int materialFlags() {
        return parent.materialFlags();
    }

    private record GeometryKey(Object parentKey, AttachFace face, int rotation16) {}

    private static void rotateQuadYAboutCenter(MutableQuadView quad, float sin, float cos) {
        float centerX = 0.5F;
        float centerZ = 0.5F;

        for (int i = 0; i < 4; i++) {
            float x = quad.x(i) - centerX;
            float z = quad.z(i) - centerZ;

            float rotatedX = x * cos - z * sin;
            float rotatedZ = x * sin + z * cos;

            quad.pos(i, rotatedX + centerX, quad.y(i), rotatedZ + centerZ);
        }
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
}
