package com.spider.framedfabric.block.enums;

import net.minecraft.util.StringIdentifiable;

public enum BarShape implements StringIdentifiable {
    SINGLE("single"),
    LEFT("left"),
    RIGHT("right"),
    MIDDLE("middle");

    private final String id;
    BarShape(String id) { this.id = id; }

    @Override
    public String asString() { return id; }
}