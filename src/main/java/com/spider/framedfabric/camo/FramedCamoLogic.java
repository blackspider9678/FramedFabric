package com.spider.framedfabric.camo;

import com.spider.framedfabric.blockentity.FramedBlockEntity;
import com.spider.framedfabric.registry.FramedTags;
import com.spider.framedfabric.registry.ModItems;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;

public final class FramedCamoLogic {
    private FramedCamoLogic() {}

    public static InteractionResult onUse(Level world, Player player, InteractionHand hand, FramedBlockEntity be, int partIndex, boolean blockAllWhenLocked) {
        ItemStack held = player.getItemInHand(hand);

        if (FramedTags.isFramedStack(held)) return InteractionResult.PASS;

        // LOCKED (for THIS PART): only hammer works
        if (be.hasCamoPart(partIndex)) {
            if (held.is(ModItems.HAMMER)) {
                if (!world.isClientSide()) {
                    ItemStack refund = camoRefundStack(be, partIndex);
                    giveOrDrop(player, refund);
                    be.clearCamoPart(partIndex);
                }
                return InteractionResult.SUCCESS;
            }
            return blockAllWhenLocked ? InteractionResult.FAIL : InteractionResult.PASS;
        }

        // EMPTY: accept BlockItem to apply camo
        if (held.getItem() instanceof BlockItem bi) {
            if (!world.isClientSide()) {
                BlockState camo = bi.getBlock().defaultBlockState();
                be.setCamoPart(partIndex, camo);
                if (!player.isCreative()) held.shrink(1);
            }
            return InteractionResult.SUCCESS;
        }

        return InteractionResult.PASS;
    }

    public static ItemStack camoRefundStack(FramedBlockEntity be, int partIndex) {
        return new ItemStack(be.getCamoPart(partIndex).getBlock().asItem());
    }

    // legacy helper (keeps older callers compiling if any remain)
    public static ItemStack camoRefundStack(FramedBlockEntity be) {
        return camoRefundStack(be, 0);
    }

    private static void giveOrDrop(Player player, ItemStack stack) {
        if (stack.isEmpty()) return;
        if (!player.getInventory().add(stack)) {
            player.drop(stack, false);
        }
    }
}
