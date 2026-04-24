package com.spider.framedfabric.registry;

import com.spider.framedfabric.FramedFabric;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.tags.TagKey;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;

public final class FramedTags {
    private FramedTags() {}

    // Keep the TagKey for datapacks / future use, but DON'T rely on it at runtime.
    public static final TagKey<Block> FRAMED_BLOCKS =
            TagKey.create(Registries.BLOCK, Identifier.fromNamespaceAndPath(FramedFabric.MOD_ID, "framed"));

    /** Reliable: checks against the blocks you registered (no tag system needed). */
    public static boolean isFramedStack(ItemStack stack) {
        if (stack == null || stack.isEmpty()) return false;
        if (!(stack.getItem() instanceof BlockItem bi)) return false;
        return ModBlocks.FRAMED_ALL.contains(bi.getBlock());
    }

    public static boolean isHoldingFramedBlock(Player player, InteractionHand hand) {
        return isFramedStack(player.getItemInHand(hand));
    }
}
