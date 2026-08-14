package com.spider.framedfabric.blockentity;

import com.spider.framedfabric.camo.FramedCamoAccess;
import com.spider.framedfabric.compat.voxy.VoxyCompat;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.block.entity.BlockEntityType;
import net.minecraft.item.ItemStack;
import net.minecraft.network.listener.ClientPlayPacketListener;
import net.minecraft.network.packet.Packet;
import net.minecraft.network.packet.s2c.play.BlockEntityUpdateS2CPacket;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.storage.ReadView;
import net.minecraft.storage.WriteView;
import net.minecraft.util.math.BlockPos;
import org.jetbrains.annotations.Nullable;

import static com.spider.framedfabric.blockentity.FramedProperties.HAS_CAMO;

public final class FramedBlockEntity extends BlockEntity implements FramedCamoAccess {

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
            partCamo[i] = Blocks.OAK_PLANKS.getDefaultState();
            partRot[i] = 1; // 1..6
        }
    }

    // ------------------------------------------------------------
    // New generic API
    // ------------------------------------------------------------

    @Override
    public boolean hasAnyCamo() {
        for (boolean b : hasPart) if (b) return true;
        return false;
    }

    @Override
    public boolean hasCamoPart(int index) {
        if (index < 0 || index >= MAX_CAMO_PARTS) return false;
        return hasPart[index];
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
        if (index < 0 || index >= MAX_CAMO_PARTS) return;
        if (camo == null) return;

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

        int r = rot;
        if (r < 1) r = 1;
        if (r > 6) r = 6;
        if (partRot[index] == r) return;

        partRot[index] = r;
        syncAndRerender();
    }

    @Override
    public void cycleCamoRotPart(int index) {
        setCamoRotPart(index, (getCamoRotPart(index) >= 6) ? 1 : (getCamoRotPart(index) + 1));
    }

    private void syncHasCamoProp() {
        if (!(world instanceof ServerWorld sw)) return;

        BlockState s = getCachedState();
        if (!s.contains(HAS_CAMO)) return;

        boolean value = hasAnyCamo();
        if (s.get(HAS_CAMO) == value) return;

        sw.setBlockState(pos, s.with(HAS_CAMO, value), 3);
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
    protected void readData(ReadView view) {
        super.readData(view);

        final boolean isClient = (world != null && world.isClient());

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
        boolean hasPlant = view.getBoolean(KEY_POT_HAS_PLANT, false);
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

            boolean has = view.getBoolean(hasK, false);
            BlockState camo = view.read(camK, BlockState.CODEC).orElse(partCamo[i]);
            int rot = view.getInt(rotK, partRot[i]);

            if (rot < 1) rot = 1;
            if (rot > 6) rot = 6;

            hasPart[i] = has;
            partCamo[i] = camo;
            partRot[i] = rot;

            if (has) anyNew = true;
        }

        // ---- If no new keys were present, migrate legacy ----
        if (!anyNew) {
            boolean legacyHas = view.getBoolean(KEY_HAS_CAMO, false);
            BlockState legacyCamo = view.read(KEY_CAMO, BlockState.CODEC).orElse(partCamo[0]);

            int legacyRot = view.getInt(KEY_CAMO_ROT, partRot[0]);
            if (legacyRot < 1) legacyRot = 1;
            if (legacyRot > 6) legacyRot = 6;

            hasPart[0] = legacyHas;
            partCamo[0] = legacyCamo;
            partRot[0] = legacyRot;

            boolean doorHasTop = view.getBoolean(KEY_DOOR_HAS_TOP_CAMO, false);
            BlockState doorTop = view.read(KEY_DOOR_TOP_CAMO, BlockState.CODEC).orElse(partCamo[1]);
            int doorRot = view.getInt(KEY_DOOR_TOP_ROT, partRot[1]);
            if (doorRot < 1) doorRot = 1;
            if (doorRot > 6) doorRot = 6;

            hasPart[1] = doorHasTop;
            partCamo[1] = doorTop;
            partRot[1] = doorRot;

            hasPart[2] = false;
            partCamo[2] = Blocks.OAK_PLANKS.getDefaultState();
            partRot[2] = 1;
        }

        // server: keep HAS_CAMO in sync
        if (world instanceof ServerWorld) {
            syncHasCamoProp();
        }

        // ✅ client: rerender if ANY part OR plant changed
        if (isClient) {
            boolean changed = false;

            // plant change check
            if (!ItemStack.areItemsAndComponentsEqual(oldPlant, potPlant)) {
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
                BlockState s = getCachedState();
                world.updateListeners(pos, s, s, 3);
                VoxyCompat.refreshChunk(world, pos);
            }
        }
    }


    @Override
    protected void writeData(WriteView view) {
        boolean hasPlant = !potPlant.isEmpty();
        view.putBoolean(KEY_POT_HAS_PLANT, hasPlant);

        if (hasPlant) {
            ItemStack one = potPlant;
            if (one.getCount() != 1) one = one.copyWithCount(1);
            view.put(KEY_POT_PLANT, ItemStack.CODEC, one);
        } else {
            // ✅ IMPORTANT: clear stale data so it doesn't try to encode "air x0"
            view.remove(KEY_POT_PLANT);
        }

        for (int i = 0; i < MAX_CAMO_PARTS; i++) {
            view.putBoolean(KEY_PART_HAS_PREFIX + i, hasPart[i]);
            view.put(KEY_PART_CAMO_PREFIX + i, BlockState.CODEC, partCamo[i]);
            view.putInt(KEY_PART_ROT_PREFIX + i, partRot[i]);
        }

        super.writeData(view);
    }

    private void syncAndRerender() {
        markDirty();

        if (world instanceof ServerWorld sw) {
            sw.getChunkManager().markForUpdate(pos); // BE update packet
            BlockState s = getCachedState();
            sw.updateListeners(pos, s, s, 3);        // rerender
        }
    }

    @Override
    public @Nullable Packet<ClientPlayPacketListener> toUpdatePacket() {
        return BlockEntityUpdateS2CPacket.create(this);
    }

    @Override
    public net.minecraft.nbt.NbtCompound toInitialChunkDataNbt(net.minecraft.registry.RegistryWrapper.WrapperLookup registries) {
        return createNbt(registries);
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
