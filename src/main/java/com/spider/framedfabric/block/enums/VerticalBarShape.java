package com.spider.framedfabric.block.enums;

import net.minecraft.util.StringIdentifiable;

public enum VerticalBarShape implements StringIdentifiable {
    SINGLE("single"),
    TOP("top"),
    BOTTOM("bottom"),
    MIDDLE("middle");

    private final String id;

    VerticalBarShape(String id) {
        this.id = id;
    }

    @Override
    public String asString() {
        return id;
    }
}