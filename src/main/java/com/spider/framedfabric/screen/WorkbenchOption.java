package com.spider.framedfabric.screen;

import net.minecraft.world.item.ItemStack;

import java.util.function.Predicate;
import java.util.function.Supplier;

public record WorkbenchOption(
        String id,
        Predicate<ItemStack> inputMatcher,
        Supplier<ItemStack> resultSupplier,
        int inputCount
) {
    public boolean matches(ItemStack stack) {
        return !stack.isEmpty() && inputMatcher.test(stack);
    }

    public ItemStack createResult() {
        return resultSupplier.get().copy();
    }
}