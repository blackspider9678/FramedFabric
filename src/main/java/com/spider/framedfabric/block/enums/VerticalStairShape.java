package com.spider.framedfabric.block.enums;

import net.minecraft.util.StringIdentifiable;

public enum VerticalStairShape implements StringIdentifiable {
    STRAIGHT("straight"),
    INNER_LEFT("inner_left"),
    INNER_RIGHT("inner_right"),
    OUTER_LEFT("outer_left"),
    OUTER_RIGHT("outer_right");

    private final String id;
    VerticalStairShape(String id) { this.id = id; }
    @Override public String asString() { return id; }
}
