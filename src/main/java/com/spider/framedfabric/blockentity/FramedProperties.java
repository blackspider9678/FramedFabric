// com/spider/framedfabric/block/FramedProperties.java
package com.spider.framedfabric.blockentity;

import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.block.state.properties.IntegerProperty;

public final class FramedProperties {
    private FramedProperties() {}

    public static final IntegerProperty ROT = IntegerProperty.create("rot", 1, 6);

    // ✅ one shared instance used by all blocks + BE
    public static final BooleanProperty HAS_CAMO = BooleanProperty.create("has_camo");
}
