package com.spider.framedfabric.blockentity;

import com.spider.framedfabric.camo.FramedCamoAccess;
import com.spider.framedfabric.compat.voxy.VoxyCompat;
import com.spider.framedfabric.registry.ModBlockEntities;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.block.entity.SignBlockEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.storage.ReadView;
import net.minecraft.storage.WriteView;
import net.minecraft.util.math.BlockPos;

import static com.spider.framedfabric.blockentity.FramedProperties.HAS_CAMO;

public class FramedSignBlockEntity extends SignBlockEntity implements FramedCamoAccess {
    private static final String KEY_PART_HAS_PREFIX = "camo_part_has_";
    private static final String KEY_PART_CAMO_PREFIX = "camo_part_";
    private static final String KEY_PART_ROT_PREFIX = "camo_rot_";

    private final boolean[] hasPart = new boolean[MAX_CAMO_PARTS];
    private final BlockState[] partCamo = new BlockState[MAX_CAMO_PARTS];
    private final int[] partRot = new int[MAX_CAMO_PARTS];

    public FramedSignBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.FRAMED_SIGN, pos, state);

        for (int i = 0; i < MAX_CAMO_PARTS; i++) {
            hasPart[i] = false;
            partCamo[i] = Blocks.OAK_PLANKS.getDefaultState();
            partRot[i] = 1;
        }
    }

    @Override
    public boolean hasAnyCamo() {
        for (boolean part : hasPart) {
            if (part) return true;
        }
        return false;
    }

    @Override
    public boolean hasCamoPart(int index) {
        return index >= 0 && index < MAX_CAMO_PARTS && hasPart[index];
    }

    @Override
    public BlockState getCamoPart(int index) {
        if (index < 0 || index >= MAX_CAMO_PARTS) return Blocks.OAK_PLANKS.getDefaultState();
        return partCamo[index];
    }

    @Override
    public int getCamoRotPart(int index) {
        if (index < 0 || index >= MAX_CAMO_PARTS) return 1;
        return partRot[index];
    }

    @Override
    public void setCamoPart(int index, BlockState camo) {
        if (index < 0 || index >= MAX_CAMO_PARTS || camo == null) return;

        partCamo[index] = camo;
        hasPart[index] = true;

        syncHasCamoProp();
        syncAndRerender();
    }

    @Override
    public void clearCamoPart(int index) {
        if (index < 0 || index >= MAX_CAMO_PARTS) return;

        hasPart[index] = false;
        partCamo[index] = Blocks.OAK_PLANKS.getDefaultState();
        partRot[index] = 1;

        syncHasCamoProp();
        syncAndRerender();
    }

    @Override
    public void setCamoRotPart(int index, int rot) {
        if (index < 0 || index >= MAX_CAMO_PARTS) return;

        int value = clampRot(rot);
        if (partRot[index] == value) return;

        partRot[index] = value;
        syncAndRerender();
    }

    @Override
    public void cycleCamoRotPart(int index) {
        setCamoRotPart(index, getCamoRotPart(index) >= 6 ? 1 : getCamoRotPart(index) + 1);
    }

    @Override
    protected void readData(ReadView view) {
        super.readData(view);

        boolean[] oldHas = null;
        BlockState[] oldCamo = null;
        int[] oldRot = null;

        boolean isClient = world != null && world.isClient();
        if (isClient) {
            oldHas = hasPart.clone();
            oldCamo = partCamo.clone();
            oldRot = partRot.clone();
        }

        for (int i = 0; i < MAX_CAMO_PARTS; i++) {
            hasPart[i] = view.getBoolean(KEY_PART_HAS_PREFIX + i, false);
            partCamo[i] = view.read(KEY_PART_CAMO_PREFIX + i, BlockState.CODEC)
                    .orElse(Blocks.OAK_PLANKS.getDefaultState());
            partRot[i] = clampRot(view.getInt(KEY_PART_ROT_PREFIX + i, 1));
        }

        if (world instanceof ServerWorld) {
            syncHasCamoProp();
        }

        if (isClient && hasCamoChanged(oldHas, oldCamo, oldRot)) {
            BlockState state = getCachedState();
            world.updateListeners(pos, state, state, 3);
            VoxyCompat.refreshChunk(world, pos);
        }
    }

    @Override
    protected void writeData(WriteView view) {
        super.writeData(view);

        for (int i = 0; i < MAX_CAMO_PARTS; i++) {
            view.putBoolean(KEY_PART_HAS_PREFIX + i, hasPart[i]);
            view.put(KEY_PART_CAMO_PREFIX + i, BlockState.CODEC, partCamo[i]);
            view.putInt(KEY_PART_ROT_PREFIX + i, partRot[i]);
        }
    }

    private boolean hasCamoChanged(boolean[] oldHas, BlockState[] oldCamo, int[] oldRot) {
        if (oldHas == null || oldCamo == null || oldRot == null) return false;

        for (int i = 0; i < MAX_CAMO_PARTS; i++) {
            if (oldHas[i] != hasPart[i]) return true;
            if (oldRot[i] != partRot[i]) return true;
            if (oldCamo[i] != partCamo[i]) return true;
        }

        return false;
    }

    private void syncHasCamoProp() {
        if (!(world instanceof ServerWorld serverWorld)) return;

        BlockState state = getCachedState();
        if (!state.contains(HAS_CAMO)) return;

        boolean value = hasAnyCamo();
        if (state.get(HAS_CAMO) != value) {
            serverWorld.setBlockState(pos, state.with(HAS_CAMO, value), 3);
        }
    }

    private void syncAndRerender() {
        markDirty();

        if (world instanceof ServerWorld serverWorld) {
            serverWorld.getChunkManager().markForUpdate(pos);
            BlockState state = getCachedState();
            serverWorld.updateListeners(pos, state, state, 3);
        }
    }

    private static int clampRot(int rot) {
        if (rot < 1) return 1;
        return Math.min(rot, 6);
    }
}
