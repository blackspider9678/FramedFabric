package com.spider.framedfabric.block;

import com.mojang.serialization.MapCodec;
import com.spider.framedfabric.blockentity.FramedBlockEntity;
import com.spider.framedfabric.blockentity.FramedUseHandler;
import com.spider.framedfabric.camo.FramedCamoLogic;
import com.spider.framedfabric.registry.ModBlockEntities;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.state.BlockBehaviour.Properties;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.SlabBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.item.ItemStack;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.Nullable;

import static com.spider.framedfabric.blockentity.FramedProperties.ROT;

public class FramedSlabBlock extends SlabBlock implements EntityBlock {
    public static final MapCodec<FramedSlabBlock> CODEC = simpleCodec(FramedSlabBlock::new);

    public static final BooleanProperty HAS_CAMO = BooleanProperty.create("has_camo");

    public FramedSlabBlock(Properties settings) {
        super(settings);

        this.registerDefaultState(this.getStateDefinition().any()
                .setValue(HAS_CAMO, false)
                .setValue(ROT, 1)
        );
    }

    @Override
    @SuppressWarnings({"unchecked", "rawtypes"})
    public MapCodec<SlabBlock> codec() {
        return (MapCodec) CODEC;
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        super.createBlockStateDefinition(builder);
        builder.add(HAS_CAMO, ROT, com.spider.framedfabric.blockentity.FramedProperties.CAMO_LIGHT);
    }

    @Override
    public @Nullable BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new FramedBlockEntity(ModBlockEntities.FRAMED, pos, state);
    }

    @Override
    protected net.minecraft.world.level.block.RenderShape getRenderShape(BlockState state) {
        return com.spider.framedfabric.blockentity.FramedProperties.hasCamo(state)
                ? net.minecraft.world.level.block.RenderShape.INVISIBLE
                : net.minecraft.world.level.block.RenderShape.MODEL;
    }

    /**
     * IMPORTANT: when placing a second slab onto an existing slab, vanilla will return a new state
     * (usually TYPE=DOUBLE). Without this override, HAS_CAMO/ROT can reset to defaults.
     *
     * Option C behavior: preserve existing camo, do NOT auto-fill the new half.
     */
    @Override
    public @Nullable BlockState getStateForPlacement(BlockPlaceContext ctx) {
        BlockState placed = super.getStateForPlacement(ctx);
        if (placed == null) return null;

        Level world = ctx.getLevel();
        BlockPos pos = ctx.getClickedPos();
        BlockState existing = world.getBlockState(pos);

        // If we are placing onto an existing slab of the same block (merge case),
        // carry forward HAS_CAMO and ROT so visuals don't "blink" or reset.
        if (existing.is(this)) {
            boolean has = existing.hasProperty(HAS_CAMO) && existing.getValue(HAS_CAMO);
            int rot = existing.hasProperty(ROT) ? existing.getValue(ROT) : 1;
            int camoLight = com.spider.framedfabric.blockentity.FramedProperties.lightLevel(existing);

            // Prefer BE truth if present (handles multi-part camo properly)
            if (world.getBlockEntity(pos) instanceof FramedBlockEntity fbe) {
                has = fbe.hasAnyCamo();
            }

            placed = placed.setValue(HAS_CAMO, has).setValue(ROT, rot);
            placed = com.spider.framedfabric.blockentity.FramedProperties.withCamoLight(placed, camoLight);
        }

        return placed;
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level world, BlockPos pos, Player player, BlockHitResult hit) {
        FramedBlockEntity be = (world.getBlockEntity(pos) instanceof FramedBlockEntity fbe) ? fbe : null;
        return FramedUseHandler.handleUse(state, world, pos, player, hit, be);
    }

    @Override
    protected InteractionResult useItemOn(
            ItemStack itemStack,
            BlockState state,
            Level world,
            BlockPos pos,
            Player player,
            InteractionHand hand,
            BlockHitResult hit
    ) {
        FramedBlockEntity be = (world.getBlockEntity(pos) instanceof FramedBlockEntity fbe) ? fbe : null;
        return FramedUseHandler.handleUseItemOn(state, world, pos, player, hand, hit, be);
    }

    @Override
    public BlockState playerWillDestroy(Level world, BlockPos pos, BlockState state, Player player) {
        if (world instanceof ServerLevel sw) {
            if (sw.getBlockEntity(pos) instanceof FramedBlockEntity be) {
                for (int i = 0; i < FramedBlockEntity.MAX_CAMO_PARTS; i++) {
                    if (be.hasCamoPart(i)) {
                        ItemStack drop = FramedCamoLogic.camoRefundStack(be, i);
                        if (!drop.isEmpty()) Block.popResource(sw, pos, drop);
                    }
                }
            }
        }
        return super.playerWillDestroy(world, pos, state, player);
    }
}
