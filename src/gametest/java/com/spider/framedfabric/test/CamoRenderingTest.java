package com.spider.framedfabric.test;

import com.spider.framedfabric.FramedFabric;
import com.spider.framedfabric.blockentity.FramedBlockEntity;
import com.spider.framedfabric.blockentity.FramedUseHandler;
import com.spider.framedfabric.registry.ModBlockEntities;
import com.spider.framedfabric.registry.ModBlocks;
import java.util.List;
import java.util.HashSet;
import java.util.Set;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.gametest.v1.FabricClientGameTest;
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;
import net.fabricmc.fabric.api.client.model.loading.v1.ModelLoadingPlugin;
import net.fabricmc.fabric.api.client.renderer.v1.Renderer;
import net.fabricmc.fabric.api.client.renderer.v1.mesh.MutableMesh;
import net.fabricmc.fabric.api.client.renderer.v1.mesh.QuadEmitter;
import net.fabricmc.fabric.api.client.renderer.v1.model.FabricBlockStateModel;
import net.fabricmc.fabric.api.client.renderer.v1.model.ModelHelper;
import net.fabricmc.fabric.api.client.renderer.v1.sprite.FabricTextureAtlas;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.block.BlockAndTintGetter;
import net.minecraft.client.renderer.block.dispatch.BlockStateModel;
import net.minecraft.client.renderer.block.dispatch.BlockStateModelPart;
import net.minecraft.client.resources.model.sprite.Material;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.CardinalLighting;
import net.minecraft.world.level.ColorResolver;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.DoubleBlockHalf;
import net.minecraft.world.level.block.DoorBlock;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.level.lighting.LevelLightEngine;
import net.minecraft.world.level.material.FluidState;
import java.util.function.Predicate;

/** Runs against real baked models and the active Fabric renderer, outside the shipped mod. */
public final class CamoRenderingTest implements ClientModInitializer, FabricClientGameTest {
    private static final BlockPos POS = new BlockPos(0, 10, 0);
    private static final List<Block> MATERIALS = List.of(
            Blocks.STONE, Blocks.OAK_LOG, Blocks.GRASS_BLOCK, Blocks.GLASS,
            Blocks.OAK_LEAVES, Blocks.MAGMA_BLOCK);

    @Override
    public void onInitializeClient() {
        // Simulate a Fabric-only source model: collectParts is intentionally empty and
        // the particle texture differs from the textures emitted for its faces.
        ModelLoadingPlugin.register(context -> context.modifyBlockModelAfterBake().register((model, bake) ->
                bake.state().is(Blocks.OAK_LOG) ? new EmitOnlyModel(model) : model));
    }

    @Override
    public void runTest(ClientGameTestContext context) {
        context.runOnClient(client -> {
            checkLogTextures(client);
            int checked = 0;
            for (Block block : ModBlocks.FRAMED_ALL) {
                for (BlockState state : block.getStateDefinition().getPossibleStates()) {
                    FramedBlockEntity be = new FramedBlockEntity(ModBlockEntities.FRAMED, POS, state);
                    SampleView view = new SampleView(state, be);
                    BlockStateModel model = client.getModelManager().getBlockStateModelSet().get(state);
                    MutableMesh bare = emit(model, view, state);
                    int count = countQuads(bare);
                    if (state == block.defaultBlockState()) {
                        check(count > 0, "Invisible default model: " + state);
                    }
                    // Some unplaceable multipart states (e.g. a wall with no post or sides)
                    // intentionally have no geometry. Camo must preserve even those states.
                    for (Block material : MATERIALS) {
                        Set<Identifier> expectedSprites = sourceSprites(client, material.defaultBlockState(), view);
                        for (int part = 0; part < FramedBlockEntity.MAX_CAMO_PARTS; part++) {
                            be.setCamoPart(part, material.defaultBlockState());
                        }
                        for (int rotation = 1; rotation <= 6; rotation++) {
                            for (int part = 0; part < FramedBlockEntity.MAX_CAMO_PARTS; part++) {
                                be.setCamoRotPart(part, rotation);
                            }
                            MutableMesh camo = emit(model, view, state);
                            check(countQuads(camo) == count, "Camo lost geometry: " + state + " / " + material);
                            validateQuads(client, camo, model, state, view, expectedSprites);
                            checked++;
                        }
                    }
                }
                FramedFabric.LOGGER.info("[CamoRenderingTest] Checked {}", BuiltInRegistries.BLOCK.getKey(block));
            }
            FramedFabric.LOGGER.info("[CamoRenderingTest] Passed {} state/material/rotation combinations", checked);
        });
        // Also exercise server-to-client block entity synchronization and actual chunk rendering.
        try (var world = context.worldBuilder().create()) {
            world.getServer().runCommand("gamemode spectator @p");
            world.getServer().runCommand("tp @p 7 12 15 180 20");
            world.getServer().runOnServer(server -> {
                var level = server.overworld();
                var player = server.getPlayerList().getPlayers().getFirst();
                for (DoubleBlockHalf half : DoubleBlockHalf.values()) {
                    for (double hitY : new double[] {0.25, 0.75}) {
                        BlockState door = ModBlocks.FRAMED_DOOR.defaultBlockState().setValue(DoorBlock.HALF, half);
                        var doorEntity = new FramedBlockEntity(ModBlockEntities.FRAMED, POS, door);
                        player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(Blocks.STONE));
                        var hit = new BlockHitResult(new Vec3(0.5, POS.getY() + hitY, 0), Direction.NORTH, POS, false);
                        FramedUseHandler.handleUseOpenable(door, level, POS, player, hit, doorEntity);
                        check(doorEntity.hasCamoPart(0) && !doorEntity.hasCamoPart(1),
                                "Door click stored camo in a part its model does not render: " + half + " / " + hitY);
                    }
                }
                int index = 0;
                for (Block block : ModBlocks.FRAMED_ALL) {
                    BlockPos pos = new BlockPos((index % 8) * 2, 10, (index / 8) * 2);
                    level.setBlock(pos.below(), Blocks.STONE.defaultBlockState(), 3);
                    level.setBlock(pos, block.defaultBlockState(), 3);
                    if (level.getBlockEntity(pos) instanceof FramedBlockEntity be) {
                        for (int part = 0; part < FramedBlockEntity.MAX_CAMO_PARTS; part++) {
                            be.setCamoPart(part, MATERIALS.get(index % MATERIALS.size()).defaultBlockState());
                        }
                    }
                    index++;
                }
            });
            world.getConnection().waitForClientboundPackets();
            world.getConnection().waitForChunksRender();
            context.waitTicks(20);
            context.runOnClient(client -> {
                check(client.level.getBlockEntity(POS) instanceof FramedBlockEntity be && be.hasCamo(),
                        "Server camo did not reach the client");
            });
            context.takeScreenshot("camo-gallery");
        }
    }

    private static void checkLogTextures(Minecraft client) {
        BlockState state = ModBlocks.FRAMED_BLOCK.defaultBlockState();
        FramedBlockEntity be = new FramedBlockEntity(ModBlockEntities.FRAMED, POS, state);
        be.setCamo(Blocks.OAK_LOG.defaultBlockState());
        MutableMesh camo = emit(client.getModelManager().getBlockStateModelSet().get(state), new SampleView(state, be), state);
        camo.forEach(quad -> {
            var atlas = client.getAtlasManager().getAtlasOrThrow(quad.atlas().getId());
            var sprite = ((FabricTextureAtlas) atlas).spriteFinder().find(quad);
            String expected = quad.lightFace().getAxis().isVertical() ? "block/oak_log_top" : "block/oak_log";
            check(sprite.contents().name().equals(Identifier.withDefaultNamespace(expected)),
                    "Fabric-only camo used particle texture on " + quad.lightFace() + ": " + sprite.contents().name());
        });
    }

    private static MutableMesh emit(BlockStateModel model, BlockAndTintGetter view, BlockState state) {
        MutableMesh mesh = Renderer.get().mutableMesh();
        ((FabricBlockStateModel) model).emitQuads(mesh.emitter(), view, POS, state, RandomSource.create(0), face -> false);
        return mesh;
    }

    private static int countQuads(MutableMesh mesh) {
        int[] count = {0};
        mesh.forEach(quad -> count[0]++);
        return count[0];
    }

    private static Set<Identifier> sourceSprites(Minecraft client, BlockState state, SampleView view) {
        var modelSet = client.getModelManager().getBlockStateModelSet();
        Set<Identifier> sprites = new HashSet<>();
        emit(modelSet.get(state), view, state).forEach(quad -> {
            var atlas = client.getAtlasManager().getAtlasOrThrow(quad.atlas().getId());
            sprites.add(((FabricTextureAtlas) atlas).spriteFinder().find(quad).contents().name());
        });
        sprites.add(modelSet.getParticleMaterial(state).sprite().contents().name());
        return sprites;
    }

    private static void validateQuads(Minecraft client, MutableMesh mesh, BlockStateModel model, BlockState state,
                                      SampleView view, Set<Identifier> expectedSprites) {
        int flags = ((FabricBlockStateModel) model).materialFlags(view, POS, state, RandomSource.create(0));
        mesh.forEach(quad -> {
            var atlas = client.getAtlasManager().getAtlasOrThrow(quad.atlas().getId());
            var sprite = ((FabricTextureAtlas) atlas).spriteFinder().find(quad);
            check(sprite != atlas.missingSprite(), "Missing camo sprite: " + state);
            if (quad.tintIndex() != 7) {
                check(expectedSprites.contains(sprite.contents().name()),
                        "Camo sampled a neighboring texture: " + state + " / " + sprite.contents().name());
            }
            check((ModelHelper.computeMaterialFlags(quad) & ~flags) == 0, "Incorrect render flags: " + state);
            for (int vertex = 0; vertex < 4; vertex++) {
                check(Float.isFinite(quad.x(vertex)) && Float.isFinite(quad.y(vertex)) && Float.isFinite(quad.z(vertex)),
                        "Invalid camo geometry: " + state);
                check(Float.isFinite(quad.u(vertex)) && Float.isFinite(quad.v(vertex)), "Invalid camo UVs: " + state);
                float epsilon = 1.0E-6F;
                check(quad.u(vertex) >= sprite.getU0() - epsilon && quad.u(vertex) <= sprite.getU1() + epsilon
                                && quad.v(vertex) >= sprite.getV0() - epsilon && quad.v(vertex) <= sprite.getV1() + epsilon,
                        "Camo UVs escaped their sprite: " + state);
            }
        });
    }

    private static void check(boolean condition, String message) {
        if (!condition) throw new AssertionError(message);
    }

    private record SampleView(BlockState state, FramedBlockEntity be) implements BlockAndTintGetter {
        @Override public BlockEntity getBlockEntity(BlockPos pos) { return pos.equals(POS) ? be : null; }
        @Override public BlockState getBlockState(BlockPos pos) { return pos.equals(POS) ? state : Blocks.AIR.defaultBlockState(); }
        @Override public FluidState getFluidState(BlockPos pos) { return getBlockState(pos).getFluidState(); }
        @Override public CardinalLighting cardinalLighting() { return EMPTY.cardinalLighting(); }
        @Override public int getBlockTint(BlockPos pos, ColorResolver resolver) { return 0x80C040; }
        @Override public LevelLightEngine getLightEngine() { return EMPTY.getLightEngine(); }
        @Override public int getHeight() { return 384; }
        @Override public int getMinY() { return -64; }
    }

    private record EmitOnlyModel(BlockStateModel parent) implements BlockStateModel, FabricBlockStateModel {
        @Override public void collectParts(RandomSource random, List<BlockStateModelPart> parts) { }
        @Override public Material.Baked particleMaterial() {
            return Minecraft.getInstance().getModelManager().getBlockStateModelSet().getParticleMaterial(Blocks.DIRT.defaultBlockState());
        }
        @Override public int materialFlags() { return parent.materialFlags(); }
        @Override public void emitQuads(QuadEmitter emitter, BlockAndTintGetter view, BlockPos pos, BlockState state,
                                        RandomSource random, Predicate<Direction> cullTest) {
            ((FabricBlockStateModel) parent).emitQuads(emitter, view, pos, state, random, cullTest);
        }
    }
}
