package com.spider.framedfabric.block.enums;

import net.minecraft.util.StringRepresentable;

public enum VerticalStairShape implements StringRepresentable {
    STRAIGHT("straight"),
    INNER_LEFT("inner_left"),
    INNER_RIGHT("inner_right"),
    OUTER_LEFT("outer_left"),
    OUTER_RIGHT("outer_right");

    private final String id;
    VerticalStairShape(String id) { this.id = id; }
    @Override public String getSerializedName() { return id; }
}
