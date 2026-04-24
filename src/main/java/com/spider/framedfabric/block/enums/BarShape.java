package com.spider.framedfabric.block.enums;

import net.minecraft.util.StringRepresentable;

public enum BarShape implements StringRepresentable {
    SINGLE("single"),
    LEFT("left"),
    RIGHT("right"),
    MIDDLE("middle");

    private final String id;
    BarShape(String id) { this.id = id; }

    @Override
    public String getSerializedName() { return id; }
}