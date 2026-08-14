package com.spider.framedfabric.blockentity;

import com.spider.framedfabric.block.custom.FramedCheckeredBlock;
import com.spider.framedfabric.block.custom.FramedCheckeredSlabBlock;
import com.spider.framedfabric.block.custom.FramedVerticalSlabBlock;
import com.spider.framedfabric.camo.FramedCamoAccess;
import com.spider.framedfabric.camo.FramedCamoLogic;
import com.spider.framedfabric.registry.FramedTags;
import com.spider.framedfabric.registry.ModItems;
import net.minecraft.block.BlockState;
import net.minecraft.block.enums.SlabType;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.BlockItem;
import net.minecraft.item.ItemStack;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Hand;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.world.World;
import org.jetbrains.annotations.Nullable;

public final class FramedUseHandler {
    private FramedUseHandler() {}

    private static int pickPartIndex(BlockState state, BlockHitResult hit, BlockPos pos) {
        double localY = hit.getPos().y - pos.getY(); // 0..1

        // Checkered slab: bottom uses parts 0/1, top uses parts 2/3 (double has both)
        if (state.getBlock() instanceof FramedCheckeredSlabBlock) {
            SlabType type = state.get(net.minecraft.block.SlabBlock.TYPE);

            double localX = hit.getPos().x - pos.getX(); // 0..1
            double localZ = hit.getPos().z - pos.getZ(); // 0..1
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
        if (state.getBlock() instanceof net.minecraft.block.SlabBlock) {
            SlabType type = state.get(net.minecraft.block.SlabBlock.TYPE);

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
            var type = state.get(com.spider.framedfabric.block.custom.FramedCheckeredVerticalSlabBlock.TYPE);
            Direction facing = state.get(com.spider.framedfabric.block.custom.FramedCheckeredVerticalSlabBlock.FACING);

            double lx = hit.getPos().x - pos.getX(); // 0..1
            double ly = hit.getPos().y - pos.getY(); // 0..1
            double lz = hit.getPos().z - pos.getZ(); // 0..1

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
            var type = state.get(FramedVerticalSlabBlock.TYPE);
            if (type == com.spider.framedfabric.block.enums.VerticalSlabType.SINGLE) return 0;

            Direction facing = state.get(FramedVerticalSlabBlock.FACING);

            double localX = hit.getPos().x - pos.getX(); // 0..1
            double localZ = hit.getPos().z - pos.getZ(); // 0..1

            // If slab axis is Z (facing north/south), split north/south using Z
            if (facing == Direction.NORTH || facing == Direction.SOUTH) {
                return (localZ >= 0.5) ? 1 : 0;
            }

            // Else axis is X (facing east/west), split west/east using X
            return (localX >= 0.5) ? 1 : 0;
        }

        // Checkered: 2-part camo based on 2x2 pattern on X/Z (NOT 3D parity)
        if (state.getBlock() instanceof FramedCheckeredBlock) {
            double lx = hit.getPos().x - pos.getX();
            double ly = hit.getPos().y - pos.getY();
            double lz = hit.getPos().z - pos.getZ();

            int xi = (lx >= 0.5) ? 1 : 0;
            int yi = (ly >= 0.5) ? 1 : 0;
            int zi = (lz >= 0.5) ? 1 : 0;

            return (xi ^ yi ^ zi) & 1;
        }


        // Doors: you can keep this (bottom=0, top=1)
        if (state.getBlock() instanceof net.minecraft.block.DoorBlock) {
            return (localY >= 0.5) ? 1 : 0;
        }

        return 0;
    }

    public static ActionResult handleUse(
            BlockState state,
            World world,
            BlockPos pos,
            PlayerEntity player,
            BlockHitResult hit,
            @Nullable FramedCamoAccess be
    ) {
        return handleUse(state, world, pos, player, hit, be, -1);
    }

    // NOTE: your handleUseOpenable/handleUseVanillaInteractive also need to be part-aware.
    // The smallest change: compute "part" and use be.hasCamoPart(part) + FramedCamoLogic(...part...)
    // Everything else stays identical.

    public static ActionResult handleUseOpenable(
            BlockState state,
            World world,
            BlockPos pos,
            PlayerEntity player,
            BlockHitResult hit,
            @Nullable FramedCamoAccess be
    ) {
        if (be == null) return ActionResult.PASS;

        int part = pickPartIndex(state, hit, pos);

        ItemStack main = player.getStackInHand(Hand.MAIN_HAND);
        ItemStack off  = player.getStackInHand(Hand.OFF_HAND);

        if (FramedTags.isFramedStack(main)) return ActionResult.PASS;

        boolean hasCamo = be.hasCamoPart(part);

        boolean hasWrench = main.isOf(ModItems.WRENCH) || off.isOf(ModItems.WRENCH);
        boolean hasHammer = main.isOf(ModItems.HAMMER) || off.isOf(ModItems.HAMMER);

        if (hasWrench) {
            if (!hasCamo) return ActionResult.PASS;
            if (!world.isClient()) be.cycleCamoRotPart(part);
            return ActionResult.SUCCESS;
        }

        if (hasHammer) {
            if (!hasCamo) return ActionResult.PASS;
            ActionResult r = FramedCamoLogic.onUse(world, player, main.isOf(ModItems.HAMMER) ? Hand.MAIN_HAND : Hand.OFF_HAND, be, part, false);
            return (r == ActionResult.PASS) ? ActionResult.PASS : r;
        }

        if (!hasCamo) {
            boolean mainCanApply = (main.getItem() instanceof BlockItem) && !FramedTags.isFramedStack(main);
            boolean offCanApply  = (off.getItem()  instanceof BlockItem) && !FramedTags.isFramedStack(off);

            if (!mainCanApply && !offCanApply) return ActionResult.PASS;

            ActionResult r = ActionResult.PASS;
            if (mainCanApply) r = FramedCamoLogic.onUse(world, player, Hand.MAIN_HAND, be, part, false);
            if (r == ActionResult.PASS && offCanApply) r = FramedCamoLogic.onUse(world, player, Hand.OFF_HAND, be, part, false);
            return (r == ActionResult.PASS) ? ActionResult.PASS : r;
        }

        return ActionResult.PASS;
    }

    public static ActionResult handleUseVanillaInteractive(
            BlockState state,
            World world,
            BlockPos pos,
            PlayerEntity player,
            BlockHitResult hit,
            @Nullable FramedCamoAccess be
    ) {
        if (be == null) return ActionResult.PASS;

        int part = pickPartIndex(state, hit, pos);

        ItemStack main = player.getStackInHand(Hand.MAIN_HAND);
        ItemStack off  = player.getStackInHand(Hand.OFF_HAND);

        if (FramedTags.isFramedStack(main) || FramedTags.isFramedStack(off)) return ActionResult.PASS;

        boolean hasCamo = be.hasCamoPart(part);
        boolean hasWrench = main.isOf(ModItems.WRENCH) || off.isOf(ModItems.WRENCH);
        boolean hasHammer = main.isOf(ModItems.HAMMER) || off.isOf(ModItems.HAMMER);

        if (hasWrench) {
            if (!hasCamo) return ActionResult.PASS;
            if (!world.isClient()) be.cycleCamoRotPart(part);
            return ActionResult.SUCCESS;
        }

        if (hasHammer) {
            if (!hasCamo) return ActionResult.PASS;
            Hand hammerHand = main.isOf(ModItems.HAMMER) ? Hand.MAIN_HAND : Hand.OFF_HAND;
            return FramedCamoLogic.onUse(world, player, hammerHand, be, part, false);
        }

        if (!hasCamo) {
            boolean mainCanApply = (main.getItem() instanceof BlockItem) && !FramedTags.isFramedStack(main);
            boolean offCanApply  = (off.getItem()  instanceof BlockItem) && !FramedTags.isFramedStack(off);

            if (!mainCanApply && !offCanApply) return ActionResult.PASS;

            ActionResult r = ActionResult.PASS;
            if (mainCanApply) r = FramedCamoLogic.onUse(world, player, Hand.MAIN_HAND, be, part, false);
            if (r == ActionResult.PASS && offCanApply) r = FramedCamoLogic.onUse(world, player, Hand.OFF_HAND, be, part, false);
            return r;
        }

        return ActionResult.PASS;
    }

    public static ActionResult handleUse(
            BlockState state,
            World world,
            BlockPos pos,
            PlayerEntity player,
            BlockHitResult hit,
            @Nullable FramedCamoAccess be,
            int forcedPart // <--- NEW
    ) {
        if (be == null) return ActionResult.PASS;

        int part = (forcedPart >= 0) ? forcedPart : pickPartIndex(state, hit, pos);

        // 1) Wrench -> rotate CAMO only (for this part)
        if (player.getStackInHand(Hand.MAIN_HAND).isOf(ModItems.WRENCH)
                || player.getStackInHand(Hand.OFF_HAND).isOf(ModItems.WRENCH)) {

            if (!world.isClient()) {
                be.cycleCamoRotPart(part);
            }
            return ActionResult.SUCCESS;
        }

        // 2) Holding a framed block? allow placement
        if (FramedTags.isFramedStack(player.getStackInHand(Hand.MAIN_HAND))
                || FramedTags.isFramedStack(player.getStackInHand(Hand.OFF_HAND))) {
            return ActionResult.PASS;
        }

        // 3) Camo/Hammer logic (try main then off)
        ActionResult r = FramedCamoLogic.onUse(world, player, Hand.MAIN_HAND, be, part, true);
        if (r == ActionResult.PASS) {
            r = FramedCamoLogic.onUse(world, player, Hand.OFF_HAND, be, part, true);
        }
        return r;
    }

    public static ActionResult handleUseBeforeVanilla(
            BlockState state,
            World world,
            BlockPos pos,
            PlayerEntity player,
            BlockHitResult hit,
            @Nullable FramedCamoAccess be
    ) {
        if (be == null) return ActionResult.PASS;

        ItemStack main = player.getStackInHand(Hand.MAIN_HAND);
        ItemStack off = player.getStackInHand(Hand.OFF_HAND);

        if (FramedTags.isFramedStack(main) || FramedTags.isFramedStack(off)) {
            return ActionResult.PASS;
        }

        int part = pickPartIndex(state, hit, pos);
        boolean hasCamo = be.hasCamoPart(part);
        boolean hasWrench = main.isOf(ModItems.WRENCH) || off.isOf(ModItems.WRENCH);
        boolean hasHammer = main.isOf(ModItems.HAMMER) || off.isOf(ModItems.HAMMER);

        if (hasWrench) {
            if (hasCamo && !world.isClient()) {
                be.cycleCamoRotPart(part);
            }
            return ActionResult.SUCCESS;
        }

        if (hasHammer) {
            if (!hasCamo) {
                return ActionResult.SUCCESS;
            }

            Hand hammerHand = main.isOf(ModItems.HAMMER) ? Hand.MAIN_HAND : Hand.OFF_HAND;
            ActionResult result = FramedCamoLogic.onUse(world, player, hammerHand, be, part, false);
            return result == ActionResult.PASS ? ActionResult.SUCCESS : result;
        }

        if (!hasCamo) {
            boolean mainCanApply = main.getItem() instanceof BlockItem;
            boolean offCanApply = off.getItem() instanceof BlockItem;

            ActionResult result = ActionResult.PASS;
            if (mainCanApply) {
                result = FramedCamoLogic.onUse(world, player, Hand.MAIN_HAND, be, part, false);
            }
            if (result == ActionResult.PASS && offCanApply) {
                result = FramedCamoLogic.onUse(world, player, Hand.OFF_HAND, be, part, false);
            }

            if (result != ActionResult.PASS) {
                return result;
            }
        }

        return ActionResult.PASS;
    }
}
