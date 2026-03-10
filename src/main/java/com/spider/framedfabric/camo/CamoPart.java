package com.spider.framedfabric.camo;

public enum CamoPart {
    PART_ONE(0),
    PART_TWO(1),
    PART_THREE(2);

    private final int index;
    CamoPart(int index) { this.index = index; }
    public int index() { return index; }
}
