package com.spider.framedfabric.block;

import com.spider.framedfabric.block.custom.FramedVerticalSlabBlock;
import com.spider.framedfabric.blockentity.FramedBlockEntity;
import com.spider.framedfabric.camo.FramedCamoLogic;
import com.spider.framedfabric.registry.FramedTags;
import com.spider.framedfabric.registry.ModItems;
import net.minecraft.block.BlockState;
import net.minecraft.block.DoorBlock;
import net.minecraft.block.SlabBlock;
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

        // Slabs: SINGLE slab uses exactly one part
        if (state.getBlock() instanceof net.minecraft.block.SlabBlock) {
            SlabType type = state.get(net.minecraft.block.SlabBlock.TYPE);

            return switch (type) {
                case BOTTOM -> 0; // always Part0
                case TOP    -> 1; // always Part1
                case DOUBLE -> (localY >= 0.5) ? 1 : 0; // split only for double
            };
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
            @Nullable FramedBlockEntity be
    ) {
        if (be == null) return ActionResult.PASS;

        int part = pickPartIndex(state, hit, pos);

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

    // NOTE: your handleUseOpenable/handleUseVanillaInteractive also need to be part-aware.
    // The smallest change: compute "part" and use be.hasCamoPart(part) + FramedCamoLogic(...part...)
    // Everything else stays identical.

    public static ActionResult handleUseOpenable(
            BlockState state,
            World world,
            BlockPos pos,
            PlayerEntity player,
            BlockHitResult hit,
            @Nullable FramedBlockEntity be
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
            @Nullable FramedBlockEntity be
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
}
