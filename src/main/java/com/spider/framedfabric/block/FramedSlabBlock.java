package com.spider.framedfabric.block;

import com.mojang.serialization.MapCodec;
import com.spider.framedfabric.blockentity.FramedBlockEntity;
import com.spider.framedfabric.blockentity.FramedUseHandler;
import com.spider.framedfabric.camo.FramedCamoLogic;
import com.spider.framedfabric.registry.ModBlockEntities;
import net.minecraft.block.Block;
import net.minecraft.block.BlockEntityProvider;
import net.minecraft.block.BlockState;
import net.minecraft.block.SlabBlock;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemPlacementContext;
import net.minecraft.item.ItemStack;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.state.StateManager;
import net.minecraft.state.property.BooleanProperty;
import net.minecraft.util.ActionResult;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import org.jetbrains.annotations.Nullable;

import static com.spider.framedfabric.blockentity.FramedProperties.ROT;

public class FramedSlabBlock extends SlabBlock implements BlockEntityProvider {
    public static final MapCodec<FramedSlabBlock> CODEC = createCodec(FramedSlabBlock::new);

    public static final BooleanProperty HAS_CAMO = BooleanProperty.of("has_camo");

    public FramedSlabBlock(Settings settings) {
        super(settings);

        this.setDefaultState(this.getStateManager().getDefaultState()
                .with(HAS_CAMO, false)
                .with(ROT, 1)
        );
    }

    @Override
    @SuppressWarnings({"unchecked", "rawtypes"})
    public MapCodec<SlabBlock> getCodec() {
        return (MapCodec) CODEC;
    }

    @Override
    protected void appendProperties(StateManager.Builder<Block, BlockState> builder) {
        super.appendProperties(builder);
        builder.add(HAS_CAMO, ROT);
    }

    @Override
    public @Nullable BlockEntity createBlockEntity(BlockPos pos, BlockState state) {
        return new FramedBlockEntity(ModBlockEntities.FRAMED, pos, state);
    }

    /**
     * IMPORTANT: when placing a second slab onto an existing slab, vanilla will return a new state
     * (usually TYPE=DOUBLE). Without this override, HAS_CAMO/ROT can reset to defaults.
     *
     * Option C behavior: preserve existing camo, do NOT auto-fill the new half.
     */
    @Override
    public @Nullable BlockState getPlacementState(ItemPlacementContext ctx) {
        BlockState placed = super.getPlacementState(ctx);
        if (placed == null) return null;

        World world = ctx.getWorld();
        BlockPos pos = ctx.getBlockPos();
        BlockState existing = world.getBlockState(pos);

        // If we are placing onto an existing slab of the same block (merge case),
        // carry forward HAS_CAMO and ROT so visuals don't "blink" or reset.
        if (existing.isOf(this)) {
            boolean has = existing.contains(HAS_CAMO) && existing.get(HAS_CAMO);
            int rot = existing.contains(ROT) ? existing.get(ROT) : 1;

            // Prefer BE truth if present (handles multi-part camo properly)
            if (world.getBlockEntity(pos) instanceof FramedBlockEntity fbe) {
                has = fbe.hasAnyCamo();
            }

            placed = placed.with(HAS_CAMO, has).with(ROT, rot);
        }

        return placed;
    }

    @Override
    protected ActionResult onUse(BlockState state, World world, BlockPos pos, PlayerEntity player, BlockHitResult hit) {
        FramedBlockEntity be = (world.getBlockEntity(pos) instanceof FramedBlockEntity fbe) ? fbe : null;
        return FramedUseHandler.handleUse(state, world, pos, player, hit, be);
    }

    @Override
    public BlockState onBreak(World world, BlockPos pos, BlockState state, PlayerEntity player) {
        if (world instanceof ServerWorld sw) {
            if (sw.getBlockEntity(pos) instanceof FramedBlockEntity be) {
                for (int i = 0; i < FramedBlockEntity.MAX_CAMO_PARTS; i++) {
                    if (be.hasCamoPart(i)) {
                        ItemStack drop = FramedCamoLogic.camoRefundStack(be, i);
                        if (!drop.isEmpty()) Block.dropStack(sw, pos, drop);
                    }
                }
            }
        }
        return super.onBreak(world, pos, state, player);
    }
}
