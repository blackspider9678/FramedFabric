package com.spider.framedfabric.item;

import com.spider.framedfabric.blockentity.AbstractFramedEntityBlock;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Item.Properties;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.level.Level;

public class FramedWrenchItem extends Item {
    public FramedWrenchItem(Properties settings) {
        super(settings);
    }

    @Override
    public InteractionResult useOn(UseOnContext ctx) {
        Level world = ctx.getLevel();
        var pos = ctx.getClickedPos();
        var state = world.getBlockState(pos);

        if (!(state.getBlock() instanceof AbstractFramedEntityBlock)) {
            return InteractionResult.PASS;
        }

        if (!state.hasProperty(AbstractFramedEntityBlock.ROT)) {
            return InteractionResult.PASS;
        }

        if (!world.isClientSide()) {
            int rot = state.getValue(AbstractFramedEntityBlock.ROT);
            int next = (rot % 6) + 1;
            world.setBlock(pos, state.setValue(AbstractFramedEntityBlock.ROT, next), 3);
        }

        return InteractionResult.SUCCESS;
    }
}
