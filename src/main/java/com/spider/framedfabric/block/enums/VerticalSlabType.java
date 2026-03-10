package com.spider.framedfabric.block.enums;

import net.minecraft.util.StringIdentifiable;

public enum VerticalSlabType implements StringIdentifiable {
    SINGLE("single"),
    DOUBLE("double");

    private final String name;
    VerticalSlabType(String name) { this.name = name; }
    @Override public String asString() { return name; }
}
