package com.spider.framedfabric.block.enums;

import net.minecraft.util.StringRepresentable;

public enum VerticalSlabType implements StringRepresentable {
    SINGLE("single"),
    DOUBLE("double");

    private final String name;
    VerticalSlabType(String name) { this.name = name; }
    @Override public String getSerializedName() { return name; }
}
