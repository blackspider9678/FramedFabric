package com.spider.framedfabric.blockentity;

import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.item.ItemStack;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.core.BlockPos;
import org.jetbrains.annotations.Nullable;

public final class FramedBlockEntity extends BlockEntity {

    // ---- legacy keys (migrate) ----
    private static final String KEY_HAS_CAMO = "has_camo";
    private static final String KEY_CAMO = "camo";
    private static final String KEY_CAMO_ROT = "camo_rot";

    private static final String KEY_DOOR_HAS_TOP_CAMO = "door_has_top_camo";
    private static final String KEY_DOOR_TOP_CAMO     = "door_top_camo";
    private static final String KEY_DOOR_TOP_ROT      = "door_top_rot";

    // ---- new keys (parts) ----
    public static final int MAX_CAMO_PARTS = 4;

    private static final String KEY_PART_HAS_PREFIX = "camo_part_has_"; // + index
    private static final String KEY_PART_CAMO_PREFIX = "camo_part_";     // + index
    private static final String KEY_PART_ROT_PREFIX = "camo_rot_";       // + index

    private final boolean[] hasPart = new boolean[MAX_CAMO_PARTS];
    private final BlockState[] partCamo = new BlockState[MAX_CAMO_PARTS];
    private final int[] partRot = new int[MAX_CAMO_PARTS];

    // ---- framed flower pot ----
    private static final String KEY_POT_HAS_PLANT = "pot_has_plant";
    private static final String KEY_POT_PLANT = "pot_plant";
    private ItemStack potPlant = ItemStack.EMPTY;

    public FramedBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state) {
        super(type, pos, state);

        for (int i = 0; i < MAX_CAMO_PARTS; i++) {
            hasPart[i] = false;
            partCamo[i] = Blocks.OAK_PLANKS.defaultBlockState();
            partRot[i] = 1; // 1..6
        }
    }

    // ------------------------------------------------------------
    // New generic API
    // ------------------------------------------------------------

    public boolean hasAnyCamo() {
        for (boolean b : hasPart) if (b) return true;
        return false;
    }

    public boolean hasCamoPart(int index) {
        if (index < 0 || index >= MAX_CAMO_PARTS) return false;
        return hasPart[index];
    }

    public BlockState getCamoPart(int index) {
        if (index < 0 || index >= MAX_CAMO_PARTS) return Blocks.OAK_PLANKS.defaultBlockState();
        return partCamo[index];
    }

    public int getCamoRotPart(int index) {
        if (index < 0 || index >= MAX_CAMO_PARTS) return 1;
        return partRot[index];
    }

    public void setCamoPart(int index, BlockState camo) {
        if (index < 0 || index >= MAX_CAMO_PARTS) return;
        if (camo == null) return;

        partCamo[index] = camo;
        hasPart[index] = true;

        syncFramedStateProps();
        syncAndRerender();
    }

    public void clearCamoPart(int index) {
        if (index < 0 || index >= MAX_CAMO_PARTS) return;

        hasPart[index] = false;
        partCamo[index] = Blocks.OAK_PLANKS.defaultBlockState();
        partRot[index] = 1;

        syncFramedStateProps();
        syncAndRerender();
    }

    public void setCamoRotPart(int index, int rot) {
        if (index < 0 || index >= MAX_CAMO_PARTS) return;

        int r = rot;
        if (r < 1) r = 1;
        if (r > 6) r = 6;
        if (partRot[index] == r) return;

        partRot[index] = r;
        syncAndRerender();
    }

    public void cycleCamoRotPart(int index) {
        setCamoRotPart(index, (getCamoRotPart(index) >= 6) ? 1 : (getCamoRotPart(index) + 1));
    }

    private void syncFramedStateProps() {
        if (!(level instanceof ServerLevel sw)) return;

        BlockState s = getBlockState();
        BlockState updated = FramedProperties.withCamo(s, hasAnyCamo());
        updated = FramedProperties.withCamoLight(updated, getMaxCamoLight());
        if (updated == s) return;

        sw.setBlock(worldPosition, updated, 3);
    }

    private int getMaxCamoLight() {
        int light = 0;
        for (int i = 0; i < MAX_CAMO_PARTS; i++) {
            if (!hasPart[i]) continue;
            light = Math.max(light, partCamo[i].getLightEmission());
            if (light >= 15) return 15;
        }
        return light;
    }

    // ------------------------------------------------------------
    // Legacy API (kept so existing code keeps compiling)
    // Part0 is the “default” camo for simple blocks.
    // ------------------------------------------------------------

    public boolean hasCamo() { return hasCamoPart(0); }
    public BlockState getCamo() { return getCamoPart(0); }

    public int getCamoRot() { return getCamoRotPart(0); }
    public void setCamoRot(int rot) { setCamoRotPart(0, rot); }
    public void cycleCamoRot() { cycleCamoRotPart(0); }

    public void setCamo(BlockState camo) { setCamoPart(0, camo); }
    public void clearCamo() { clearCamoPart(0); }

    // ------------------------------------------------------------
    // Networking + NBT
    // ------------------------------------------------------------

    @Override
    protected void loadAdditional(ValueInput view) {
        super.loadAdditional(view);

        final boolean isClient = (level != null && level.isClientSide());

        // snapshot for client rerender decision (ALL parts + plant)
        boolean[] oldHas = null;
        BlockState[] oldCamo = null;
        int[] oldRot = null;
        ItemStack oldPlant = ItemStack.EMPTY;

        if (isClient) {
            oldHas = hasPart.clone();
            oldCamo = partCamo.clone();
            oldRot = partRot.clone();
            oldPlant = potPlant.copy();
        }

        // ---- pot plant ----
        boolean hasPlant = view.getBooleanOr(KEY_POT_HAS_PLANT, false);
        if (hasPlant) {
            potPlant = view.read(KEY_POT_PLANT, ItemStack.CODEC).orElse(ItemStack.EMPTY);
            if (!potPlant.isEmpty() && potPlant.getCount() != 1) {
                potPlant = potPlant.copyWithCount(1);
            }
        } else {
            potPlant = ItemStack.EMPTY;
        }

        // ---- Prefer new keys if present ----
        boolean anyNew = false;
        for (int i = 0; i < MAX_CAMO_PARTS; i++) {
            String hasK = KEY_PART_HAS_PREFIX + i;
            String camK = KEY_PART_CAMO_PREFIX + i;
            String rotK = KEY_PART_ROT_PREFIX + i;

            boolean has = view.getBooleanOr(hasK, false);
            BlockState camo = view.read(camK, BlockState.CODEC).orElse(partCamo[i]);
            int rot = view.getIntOr(rotK, partRot[i]);

            if (rot < 1) rot = 1;
            if (rot > 6) rot = 6;

            hasPart[i] = has;
            partCamo[i] = camo;
            partRot[i] = rot;

            if (has) anyNew = true;
        }

        // ---- If no new keys were present, migrate legacy ----
        if (!anyNew) {
            boolean legacyHas = view.getBooleanOr(KEY_HAS_CAMO, false);
            BlockState legacyCamo = view.read(KEY_CAMO, BlockState.CODEC).orElse(partCamo[0]);

            int legacyRot = view.getIntOr(KEY_CAMO_ROT, partRot[0]);
            if (legacyRot < 1) legacyRot = 1;
            if (legacyRot > 6) legacyRot = 6;

            hasPart[0] = legacyHas;
            partCamo[0] = legacyCamo;
            partRot[0] = legacyRot;

            boolean doorHasTop = view.getBooleanOr(KEY_DOOR_HAS_TOP_CAMO, false);
            BlockState doorTop = view.read(KEY_DOOR_TOP_CAMO, BlockState.CODEC).orElse(partCamo[1]);
            int doorRot = view.getIntOr(KEY_DOOR_TOP_ROT, partRot[1]);
            if (doorRot < 1) doorRot = 1;
            if (doorRot > 6) doorRot = 6;

            hasPart[1] = doorHasTop;
            partCamo[1] = doorTop;
            partRot[1] = doorRot;

            hasPart[2] = false;
            partCamo[2] = Blocks.OAK_PLANKS.defaultBlockState();
            partRot[2] = 1;
        }

        // server: keep HAS_CAMO in sync
        if (level instanceof ServerLevel) {
            syncFramedStateProps();
        }

        // ✅ client: rerender if ANY part OR plant changed
        if (isClient) {
            boolean changed = false;

            // plant change check
            if (!ItemStack.isSameItemSameComponents(oldPlant, potPlant)) {
                changed = true;
            }

            // camo change check
            if (!changed) {
                for (int i = 0; i < MAX_CAMO_PARTS; i++) {
                    if (oldHas[i] != hasPart[i]) { changed = true; break; }
                    if (oldRot[i] != partRot[i]) { changed = true; break; }
                    if (oldCamo[i] != partCamo[i]) { changed = true; break; }
                }
            }

            if (changed) {
                BlockState s = getBlockState();
                level.sendBlockUpdated(worldPosition, s, s, 3);
            }
        }
    }


    @Override
    protected void saveAdditional(ValueOutput view) {
        boolean hasPlant = !potPlant.isEmpty();
        view.putBoolean(KEY_POT_HAS_PLANT, hasPlant);

        if (hasPlant) {
            ItemStack one = potPlant;
            if (one.getCount() != 1) one = one.copyWithCount(1);
            view.store(KEY_POT_PLANT, ItemStack.CODEC, one);
        } else {
            // ✅ IMPORTANT: clear stale data so it doesn't try to encode "air x0"
            view.discard(KEY_POT_PLANT);
        }

        for (int i = 0; i < MAX_CAMO_PARTS; i++) {
            view.putBoolean(KEY_PART_HAS_PREFIX + i, hasPart[i]);
            view.store(KEY_PART_CAMO_PREFIX + i, BlockState.CODEC, partCamo[i]);
            view.putInt(KEY_PART_ROT_PREFIX + i, partRot[i]);
        }

        super.saveAdditional(view);
    }

    private void syncAndRerender() {
        setChanged();

        if (level instanceof ServerLevel sw) {
            sw.getChunkSource().blockChanged(worldPosition); // BE update packet
            BlockState s = getBlockState();
            sw.sendBlockUpdated(worldPosition, s, s, 3);        // rerender
        }
    }

    @Override
    public @Nullable Packet<ClientGamePacketListener> getUpdatePacket() {
        return ClientboundBlockEntityDataPacket.create(this);
    }

    @Override
    public net.minecraft.nbt.CompoundTag getUpdateTag(net.minecraft.core.HolderLookup.Provider registries) {
        return saveWithoutMetadata(registries);
    }

    // ---- framed flower pot ----
    public boolean hasPotPlant() {
        return !potPlant.isEmpty();
    }

    public ItemStack getPotPlantStack() {
        return potPlant;
    }

    public void setPotPlantStack(ItemStack stack) {
        if (stack == null || stack.isEmpty()) {
            potPlant = ItemStack.EMPTY;
        } else {
            potPlant = stack.copyWithCount(1);
        }
        syncAndRerender();
    }

}
