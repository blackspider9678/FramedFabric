package com.spider.framedfabric.camo;

import net.minecraft.block.BlockState;

public interface FramedCamoAccess {
    int MAX_CAMO_PARTS = 4;

    boolean hasAnyCamo();

    boolean hasCamoPart(int index);

    BlockState getCamoPart(int index);

    int getCamoRotPart(int index);

    void setCamoPart(int index, BlockState camo);

    void clearCamoPart(int index);

    void setCamoRotPart(int index, int rot);

    void cycleCamoRotPart(int index);
}
