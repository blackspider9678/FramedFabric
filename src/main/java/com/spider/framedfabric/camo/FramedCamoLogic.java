package com.spider.framedfabric.camo;

import com.spider.framedfabric.blockentity.FramedBlockEntity;
import com.spider.framedfabric.registry.FramedTags;
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

    public static ActionResult onUse(World world, PlayerEntity player, Hand hand, FramedBlockEntity be, int partIndex, boolean blockAllWhenLocked) {
        ItemStack held = player.getStackInHand(hand);

        if (FramedTags.isFramedStack(held)) return ActionResult.PASS;

        // LOCKED (for THIS PART): only hammer works
        if (be.hasCamoPart(partIndex)) {
            if (held.isOf(ModItems.HAMMER)) {
                if (!world.isClient()) {
                    ItemStack refund = camoRefundStack(be, partIndex);
                    giveOrDrop(player, refund);
                    be.clearCamoPart(partIndex);
                }
                return ActionResult.SUCCESS;
            }
            return blockAllWhenLocked ? ActionResult.FAIL : ActionResult.PASS;
        }

        // EMPTY: accept BlockItem to apply camo
        if (held.getItem() instanceof BlockItem bi) {
            if (!world.isClient()) {
                BlockState camo = bi.getBlock().getDefaultState();
                be.setCamoPart(partIndex, camo);
                if (!player.isCreative()) held.decrement(1);
            }
            return ActionResult.SUCCESS;
        }

        return ActionResult.PASS;
    }

    public static ItemStack camoRefundStack(FramedBlockEntity be, int partIndex) {
        return new ItemStack(be.getCamoPart(partIndex).getBlock().asItem());
    }

    // legacy helper (keeps older callers compiling if any remain)
    public static ItemStack camoRefundStack(FramedBlockEntity be) {
        return camoRefundStack(be, 0);
    }

    private static void giveOrDrop(PlayerEntity player, ItemStack stack) {
        if (stack.isEmpty()) return;
        if (!player.getInventory().insertStack(stack)) {
            player.dropItem(stack, false);
        }
    }
}
