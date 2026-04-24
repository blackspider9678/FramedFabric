package com.spider.framedfabric.block.enums;

import net.minecraft.util.StringRepresentable;

public enum VerticalBarShape implements StringRepresentable {
    SINGLE("single"),
    TOP("top"),
    BOTTOM("bottom"),
    MIDDLE("middle");

    private final String id;

    VerticalBarShape(String id) {
        this.id = id;
    }

    @Override
    public String getSerializedName() {
        return id;
    }
}