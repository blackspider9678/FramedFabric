package com.spider.framedfabric.blockentity;

import com.spider.framedfabric.block.custom.FramedCheckeredBlock;
import com.spider.framedfabric.block.custom.FramedCheckeredSlabBlock;
import com.spider.framedfabric.block.custom.FramedVerticalSlabBlock;
import com.spider.framedfabric.camo.FramedCamoLogic;
import com.spider.framedfabric.registry.FramedTags;
import com.spider.framedfabric.registry.ModItems;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.SlabType;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.Nullable;

public final class FramedUseHandler {
    private FramedUseHandler() {}

    private static InteractionResult fallbackForUnhandledItem(ItemStack held, boolean allowEmptyHandFallback) {
        if (allowEmptyHandFallback && held.isEmpty()) {
            return InteractionResult.TRY_WITH_EMPTY_HAND;
        }

        return InteractionResult.PASS;
    }

    private static int pickPartIndex(BlockState state, BlockHitResult hit, BlockPos pos) {
        double localY = hit.getLocation().y - pos.getY(); // 0..1

        // Checkered slab: bottom uses parts 0/1, top uses parts 2/3 (double has both)
        if (state.getBlock() instanceof FramedCheckeredSlabBlock) {
            SlabType type = state.getValue(net.minecraft.world.level.block.SlabBlock.TYPE);

            double localX = hit.getLocation().x - pos.getX(); // 0..1
            double localZ = hit.getLocation().z - pos.getZ(); // 0..1
            // use localY from top of method (don't redeclare)

            int parity = ((localX >= 0.5) ? 1 : 0) ^ ((localZ >= 0.5) ? 1 : 0); // 0 or 1

            int base;
            if (type == SlabType.DOUBLE) {
                base = (localY >= 0.5) ? 2 : 0; // top half => parts 2/3
            } else if (type == SlabType.TOP) {
                base = 2;
            } else {
                base = 0; // bottom
            }

            return base + parity; // 0..3
        }

        // Slabs: SINGLE slab uses exactly one part
        if (state.getBlock() instanceof net.minecraft.world.level.block.SlabBlock) {
            SlabType type = state.getValue(net.minecraft.world.level.block.SlabBlock.TYPE);

            return switch (type) {
                case BOTTOM -> 0; // always Part0
                case TOP    -> 1; // always Part1
                case DOUBLE -> (localY >= 0.5) ? 1 : 0; // split only for double
            };
        }

        // Checkered vertical slab:
        // SINGLE -> parts 0/1 (2x2 checker on vertical face: X/Y or Z/Y)
        // DOUBLE -> halves get 0/1 and 2/3
        if (state.getBlock() instanceof com.spider.framedfabric.block.custom.FramedCheckeredVerticalSlabBlock) {
            var type = state.getValue(com.spider.framedfabric.block.custom.FramedCheckeredVerticalSlabBlock.TYPE);
            Direction facing = state.getValue(com.spider.framedfabric.block.custom.FramedCheckeredVerticalSlabBlock.FACING);

            double lx = hit.getLocation().x - pos.getX(); // 0..1
            double ly = hit.getLocation().y - pos.getY(); // 0..1
            double lz = hit.getLocation().z - pos.getZ(); // 0..1

            int half = 0;
            if (type == com.spider.framedfabric.block.enums.VerticalSlabType.DOUBLE) {
                // Split axis depends on facing (same as your vertical slab)
                if (facing == Direction.NORTH || facing == Direction.SOUTH) {
                    half = (lz >= 0.5) ? 1 : 0;  // north/south halves
                } else {
                    half = (lx >= 0.5) ? 1 : 0;  // west/east halves
                }
            }

            // checker is on the vertical face:
            // N/S -> X/Y checker, E/W -> Z/Y checker
            int a = (facing == Direction.NORTH || facing == Direction.SOUTH) ? ((lx >= 0.5) ? 1 : 0) : ((lz >= 0.5) ? 1 : 0);
            int b = (ly >= 0.5) ? 1 : 0;
            int parity = (a ^ b) & 1; // 0/1

            return half * 2 + parity; // 0..3
        }

        // Vertical slabs: SINGLE uses part0. DOUBLE splits into part0/part1 depending on axis.
        if (state.getBlock() instanceof FramedVerticalSlabBlock) {
            var type = state.getValue(FramedVerticalSlabBlock.TYPE);
            if (type == com.spider.framedfabric.block.enums.VerticalSlabType.SINGLE) return 0;

            Direction facing = state.getValue(FramedVerticalSlabBlock.FACING);

            double localX = hit.getLocation().x - pos.getX(); // 0..1
            double localZ = hit.getLocation().z - pos.getZ(); // 0..1

            // If slab axis is Z (facing north/south), split north/south using Z
            if (facing == Direction.NORTH || facing == Direction.SOUTH) {
                return (localZ >= 0.5) ? 1 : 0;
            }

            // Else axis is X (facing east/west), split west/east using X
            return (localX >= 0.5) ? 1 : 0;
        }

        // Checkered: 2-part camo based on 2x2 pattern on X/Z (NOT 3D parity)
        if (state.getBlock() instanceof FramedCheckeredBlock) {
            double lx = hit.getLocation().x - pos.getX();
            double ly = hit.getLocation().y - pos.getY();
            double lz = hit.getLocation().z - pos.getZ();

            int xi = (lx >= 0.5) ? 1 : 0;
            int yi = (ly >= 0.5) ? 1 : 0;
            int zi = (lz >= 0.5) ? 1 : 0;

            return (xi ^ yi ^ zi) & 1;
        }


        // Doors: you can keep this (bottom=0, top=1)
        if (state.getBlock() instanceof net.minecraft.world.level.block.DoorBlock) {
            return (localY >= 0.5) ? 1 : 0;
        }

        return 0;
    }

    public static InteractionResult handleUse(
            BlockState state,
            Level world,
            BlockPos pos,
            Player player,
            BlockHitResult hit,
            @Nullable FramedBlockEntity be
    ) {
        return handleUse(state, world, pos, player, hit, be, -1);
    }

    // NOTE: your handleUseOpenable/handleUseVanillaInteractive also need to be part-aware.
    // The smallest change: compute "part" and use be.hasCamoPart(part) + FramedCamoLogic(...part...)
    // Everything else stays identical.

    public static InteractionResult handleUseOpenable(
            BlockState state,
            Level world,
            BlockPos pos,
            Player player,
            BlockHitResult hit,
            @Nullable FramedBlockEntity be
    ) {
        if (be == null) return InteractionResult.PASS;

        int part = pickPartIndex(state, hit, pos);

        ItemStack main = player.getItemInHand(InteractionHand.MAIN_HAND);
        ItemStack off  = player.getItemInHand(InteractionHand.OFF_HAND);

        if (FramedTags.isFramedStack(main)) return InteractionResult.PASS;

        boolean hasCamo = be.hasCamoPart(part);

        boolean hasWrench = main.is(ModItems.WRENCH) || off.is(ModItems.WRENCH);
        boolean hasHammer = main.is(ModItems.HAMMER) || off.is(ModItems.HAMMER);

        if (hasWrench) {
            if (!hasCamo) return InteractionResult.PASS;
            if (!world.isClientSide()) be.cycleCamoRotPart(part);
            return InteractionResult.SUCCESS;
        }

        if (hasHammer) {
            if (!hasCamo) return InteractionResult.PASS;
            InteractionResult r = FramedCamoLogic.onUse(world, player, main.is(ModItems.HAMMER) ? InteractionHand.MAIN_HAND : InteractionHand.OFF_HAND, be, part, false);
            return (r == InteractionResult.PASS) ? InteractionResult.PASS : r;
        }

        if (!hasCamo) {
            boolean mainCanApply = (main.getItem() instanceof BlockItem) && !FramedTags.isFramedStack(main);
            boolean offCanApply  = (off.getItem()  instanceof BlockItem) && !FramedTags.isFramedStack(off);

            if (!mainCanApply && !offCanApply) return InteractionResult.PASS;

            InteractionResult r = InteractionResult.PASS;
            if (mainCanApply) r = FramedCamoLogic.onUse(world, player, InteractionHand.MAIN_HAND, be, part, false);
            if (r == InteractionResult.PASS && offCanApply) r = FramedCamoLogic.onUse(world, player, InteractionHand.OFF_HAND, be, part, false);
            return (r == InteractionResult.PASS) ? InteractionResult.PASS : r;
        }

        return InteractionResult.PASS;
    }

    public static InteractionResult handleUseVanillaInteractive(
            BlockState state,
            Level world,
            BlockPos pos,
            Player player,
            BlockHitResult hit,
            @Nullable FramedBlockEntity be
    ) {
        if (be == null) return InteractionResult.PASS;

        int part = pickPartIndex(state, hit, pos);

        ItemStack main = player.getItemInHand(InteractionHand.MAIN_HAND);
        ItemStack off  = player.getItemInHand(InteractionHand.OFF_HAND);

        if (FramedTags.isFramedStack(main) || FramedTags.isFramedStack(off)) return InteractionResult.PASS;

        boolean hasCamo = be.hasCamoPart(part);
        boolean hasWrench = main.is(ModItems.WRENCH) || off.is(ModItems.WRENCH);
        boolean hasHammer = main.is(ModItems.HAMMER) || off.is(ModItems.HAMMER);

        if (hasWrench) {
            if (!hasCamo) return InteractionResult.PASS;
            if (!world.isClientSide()) be.cycleCamoRotPart(part);
            return InteractionResult.SUCCESS;
        }

        if (hasHammer) {
            if (!hasCamo) return InteractionResult.PASS;
            InteractionHand hammerHand = main.is(ModItems.HAMMER) ? InteractionHand.MAIN_HAND : InteractionHand.OFF_HAND;
            return FramedCamoLogic.onUse(world, player, hammerHand, be, part, false);
        }

        if (!hasCamo) {
            boolean mainCanApply = (main.getItem() instanceof BlockItem) && !FramedTags.isFramedStack(main);
            boolean offCanApply  = (off.getItem()  instanceof BlockItem) && !FramedTags.isFramedStack(off);

            if (!mainCanApply && !offCanApply) return InteractionResult.PASS;

            InteractionResult r = InteractionResult.PASS;
            if (mainCanApply) r = FramedCamoLogic.onUse(world, player, InteractionHand.MAIN_HAND, be, part, false);
            if (r == InteractionResult.PASS && offCanApply) r = FramedCamoLogic.onUse(world, player, InteractionHand.OFF_HAND, be, part, false);
            return r;
        }

        return InteractionResult.PASS;
    }

    public static InteractionResult handleUseItemOn(
            BlockState state,
            Level world,
            BlockPos pos,
            Player player,
            InteractionHand hand,
            BlockHitResult hit,
            @Nullable FramedBlockEntity be
    ) {
        if (be == null) {
            return fallbackForUnhandledItem(player.getItemInHand(hand), true);
        }

        ItemStack held = player.getItemInHand(hand);
        int part = pickPartIndex(state, hit, pos);

        if (held.is(ModItems.WRENCH)) {
            if (!world.isClientSide()) {
                be.cycleCamoRotPart(part);
            }
            return InteractionResult.SUCCESS;
        }

        if (FramedTags.isFramedStack(held)) {
            return InteractionResult.PASS;
        }

        InteractionResult result = FramedCamoLogic.onUse(world, player, hand, be, part, true);
        if (result != InteractionResult.PASS) {
            return result;
        }

        return fallbackForUnhandledItem(held, true);
    }

    public static InteractionResult handleUseItemOnOpenable(
            BlockState state,
            Level world,
            BlockPos pos,
            Player player,
            InteractionHand hand,
            BlockHitResult hit,
            @Nullable FramedBlockEntity be
    ) {
        ItemStack held = player.getItemInHand(hand);
        if (be == null) {
            return fallbackForUnhandledItem(held, true);
        }

        int part = pickPartIndex(state, hit, pos);

        if (FramedTags.isFramedStack(held)) {
            return InteractionResult.PASS;
        }

        boolean hasCamo = be.hasCamoPart(part);

        if (held.is(ModItems.WRENCH)) {
            if (!hasCamo) {
                return fallbackForUnhandledItem(held, true);
            }

            if (!world.isClientSide()) {
                be.cycleCamoRotPart(part);
            }
            return InteractionResult.SUCCESS;
        }

        if (held.is(ModItems.HAMMER)) {
            if (!hasCamo) {
                return fallbackForUnhandledItem(held, true);
            }

            InteractionResult result = FramedCamoLogic.onUse(world, player, hand, be, part, false);
            return (result == InteractionResult.PASS) ? fallbackForUnhandledItem(held, true) : result;
        }

        if (!hasCamo && held.getItem() instanceof BlockItem) {
            InteractionResult result = FramedCamoLogic.onUse(world, player, hand, be, part, false);
            return (result == InteractionResult.PASS) ? fallbackForUnhandledItem(held, true) : result;
        }

        return fallbackForUnhandledItem(held, true);
    }

    public static InteractionResult handleUseItemOnVanillaInteractive(
            BlockState state,
            Level world,
            BlockPos pos,
            Player player,
            InteractionHand hand,
            BlockHitResult hit,
            @Nullable FramedBlockEntity be
    ) {
        ItemStack held = player.getItemInHand(hand);
        if (be == null) {
            return fallbackForUnhandledItem(held, true);
        }

        int part = pickPartIndex(state, hit, pos);

        if (FramedTags.isFramedStack(held)) {
            return InteractionResult.PASS;
        }

        boolean hasCamo = be.hasCamoPart(part);

        if (held.is(ModItems.WRENCH)) {
            if (!hasCamo) {
                return fallbackForUnhandledItem(held, true);
            }

            if (!world.isClientSide()) {
                be.cycleCamoRotPart(part);
            }
            return InteractionResult.SUCCESS;
        }

        if (held.is(ModItems.HAMMER)) {
            if (!hasCamo) {
                return fallbackForUnhandledItem(held, true);
            }

            InteractionResult result = FramedCamoLogic.onUse(world, player, hand, be, part, false);
            return (result == InteractionResult.PASS) ? fallbackForUnhandledItem(held, true) : result;
        }

        if (!hasCamo && held.getItem() instanceof BlockItem) {
            InteractionResult result = FramedCamoLogic.onUse(world, player, hand, be, part, false);
            return (result == InteractionResult.PASS) ? fallbackForUnhandledItem(held, true) : result;
        }

        return fallbackForUnhandledItem(held, true);
    }

    public static InteractionResult handleUse(
            BlockState state,
            Level world,
            BlockPos pos,
            Player player,
            BlockHitResult hit,
            @Nullable FramedBlockEntity be,
            int forcedPart // <--- NEW
    ) {
        if (be == null) return InteractionResult.PASS;

        int part = (forcedPart >= 0) ? forcedPart : pickPartIndex(state, hit, pos);

        // 1) Wrench -> rotate CAMO only (for this part)
        if (player.getItemInHand(InteractionHand.MAIN_HAND).is(ModItems.WRENCH)
                || player.getItemInHand(InteractionHand.OFF_HAND).is(ModItems.WRENCH)) {

            if (!world.isClientSide()) {
                be.cycleCamoRotPart(part);
            }
            return InteractionResult.SUCCESS;
        }

        // 2) Holding a framed block? allow placement
        if (FramedTags.isFramedStack(player.getItemInHand(InteractionHand.MAIN_HAND))
                || FramedTags.isFramedStack(player.getItemInHand(InteractionHand.OFF_HAND))) {
            return InteractionResult.PASS;
        }

        // 3) Camo/Hammer logic (try main then off)
        InteractionResult r = FramedCamoLogic.onUse(world, player, InteractionHand.MAIN_HAND, be, part, true);
        if (r == InteractionResult.PASS) {
            r = FramedCamoLogic.onUse(world, player, InteractionHand.OFF_HAND, be, part, true);
        }
        return r;
    }
}
