package com.spider.framedfabric.registry;

import com.spider.framedfabric.FramedFabric;
import net.minecraft.block.Block;
import net.minecraft.item.BlockItem;
import net.minecraft.item.ItemStack;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.registry.tag.TagKey;
import net.minecraft.util.Hand;
import net.minecraft.util.Identifier;
import net.minecraft.entity.player.PlayerEntity;

public final class FramedTags {
    private FramedTags() {}

    // Keep the TagKey for datapacks / future use, but DON'T rely on it at runtime.
    public static final TagKey<Block> FRAMED_BLOCKS =
            TagKey.of(RegistryKeys.BLOCK, Identifier.of(FramedFabric.MOD_ID, "framed"));

    /** Reliable: checks against the blocks you registered (no tag system needed). */
    public static boolean isFramedStack(ItemStack stack) {
        if (stack == null || stack.isEmpty()) return false;
        if (!(stack.getItem() instanceof BlockItem bi)) return false;
        return ModBlocks.FRAMED_ALL.contains(bi.getBlock());
    }

    public static boolean isHoldingFramedBlock(PlayerEntity player, Hand hand) {
        return isFramedStack(player.getStackInHand(hand));
    }
}
