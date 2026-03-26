// com/spider/framedfabric/block/FramedProperties.java
package com.spider.framedfabric.blockentity;

import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import net.minecraft.world.level.block.state.properties.Property;
import org.jetbrains.annotations.Nullable;

public final class FramedProperties {
    private FramedProperties() {}

    public static final IntegerProperty ROT = IntegerProperty.create("rot", 1, 6);

    // ✅ one shared instance used by all blocks + BE
    public static final BooleanProperty HAS_CAMO = BooleanProperty.create("has_camo");

    public static boolean hasCamo(BlockState state) {
        BooleanProperty property = camoProperty(state);
        return property != null && state.getValue(property);
    }

    public static BlockState withCamo(BlockState state, boolean value) {
        BooleanProperty property = camoProperty(state);
        if (property == null || state.getValue(property) == value) {
            return state;
        }

        return state.setValue(property, value);
    }

    public static @Nullable BooleanProperty camoProperty(BlockState state) {
        for (Property<?> property : state.getProperties()) {
            if (property instanceof BooleanProperty booleanProperty && "has_camo".equals(booleanProperty.getName())) {
                return booleanProperty;
            }
        }

        return null;
    }
}
