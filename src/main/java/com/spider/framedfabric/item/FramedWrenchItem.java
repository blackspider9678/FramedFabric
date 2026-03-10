package com.spider.framedfabric.item;

import com.spider.framedfabric.block.AbstractFramedEntityBlock;
import net.minecraft.item.Item;
import net.minecraft.item.ItemUsageContext;
import net.minecraft.util.ActionResult;
import net.minecraft.world.World;

public class FramedWrenchItem extends Item {
    public FramedWrenchItem(Settings settings) {
        super(settings);
    }

    @Override
    public ActionResult useOnBlock(ItemUsageContext ctx) {
        World world = ctx.getWorld();
        var pos = ctx.getBlockPos();
        var state = world.getBlockState(pos);

        if (!(state.getBlock() instanceof AbstractFramedEntityBlock)) {
            return ActionResult.PASS;
        }

        if (!state.contains(AbstractFramedEntityBlock.ROT)) {
            return ActionResult.PASS;
        }

        if (!world.isClient()) {
            int rot = state.get(AbstractFramedEntityBlock.ROT);
            int next = (rot % 6) + 1;
            world.setBlockState(pos, state.with(AbstractFramedEntityBlock.ROT, next), 3);
        }

        return ActionResult.SUCCESS;
    }
}
