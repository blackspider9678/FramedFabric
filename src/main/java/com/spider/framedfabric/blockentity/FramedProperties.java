// com/spider/framedfabric/block/FramedProperties.java
package com.spider.framedfabric.blockentity;

import net.minecraft.state.property.BooleanProperty;
import net.minecraft.state.property.IntProperty;

public final class FramedProperties {
    private FramedProperties() {}

    public static final IntProperty ROT = IntProperty.of("rot", 1, 6);

    // ✅ one shared instance used by all blocks + BE
    public static final BooleanProperty HAS_CAMO = BooleanProperty.of("has_camo");
}
