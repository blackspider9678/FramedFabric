package com.spider.framedfabric.camo;

import com.spider.framedfabric.blockentity.FramedBlockEntity;
import com.spider.framedfabric.registry.ModItems;
import net.minecraft.block.BlockState;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.BlockItem;
import net.minecraft.item.ItemStack;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Hand;
import net.minecraft.world.World;

public final class FramedCamoLogic {
    private FramedCamoLogic() {}

    /**
     * Behavior:
     * - If camo NOT set: right click with BlockItem consumes 1 and locks
     * - If camo IS set: only HAMMER can unlock + refund; otherwise no interaction allowed
     */
    public static ActionResult onUse(World world, PlayerEntity player, Hand hand, FramedBlockEntity be, boolean blockAllWhenLocked) {
        ItemStack held = player.getStackInHand(hand);

        // LOCKED: only hammer works
        if (be.hasCamo()) {
            if (held.isOf(ModItems.HAMMER)) {
                if (!world.isClient()) {
                    ItemStack refund = camoRefundStack(be);
                    giveOrDrop(player, refund);
                    be.clearCamo();
                }
                return ActionResult.SUCCESS;
            }
            return blockAllWhenLocked ? ActionResult.FAIL : ActionResult.PASS;
        }

        // EMPTY: accept BlockItem to apply camo
        if (held.getItem() instanceof BlockItem bi) {
            if (!world.isClient()) {
                BlockState camo = bi.getBlock().getDefaultState();
                be.setCamo(camo);
                if (!player.isCreative()) held.decrement(1);
            }
            return ActionResult.SUCCESS;
        }

        return ActionResult.PASS;
    }

    public static ItemStack camoRefundStack(FramedBlockEntity be) {
        // If the camo block has no item, this will be AIR; caller should handle.
        return new ItemStack(be.getCamo().getBlock().asItem());
    }

    private static void giveOrDrop(PlayerEntity player, ItemStack stack) {
        if (stack.isEmpty()) return;
        if (!player.getInventory().insertStack(stack)) {
            player.dropItem(stack, false);
        }
    }
}
