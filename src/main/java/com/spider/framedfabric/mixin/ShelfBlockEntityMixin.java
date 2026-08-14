package com.spider.framedfabric.mixin;

import com.spider.framedfabric.camo.FramedCamoAccess;
import com.spider.framedfabric.compat.voxy.VoxyCompat;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.block.entity.ShelfBlockEntity;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.storage.ReadView;
import net.minecraft.storage.WriteView;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import static com.spider.framedfabric.blockentity.FramedProperties.HAS_CAMO;

@Mixin(ShelfBlockEntity.class)
public abstract class ShelfBlockEntityMixin implements FramedCamoAccess {
    @Unique private static final String KEY_PART_HAS_PREFIX = "camo_part_has_";
    @Unique private static final String KEY_PART_CAMO_PREFIX = "camo_part_";
    @Unique private static final String KEY_PART_ROT_PREFIX = "camo_rot_";

    @Unique private final boolean[] framedfabric$hasPart = new boolean[MAX_CAMO_PARTS];
    @Unique private final BlockState[] framedfabric$partCamo = new BlockState[MAX_CAMO_PARTS];
    @Unique private final int[] framedfabric$partRot = new int[MAX_CAMO_PARTS];

    @Unique
    private void framedfabric$ensureInitialized() {
        for (int i = 0; i < MAX_CAMO_PARTS; i++) {
            if (framedfabric$partCamo[i] == null) {
                framedfabric$partCamo[i] = Blocks.OAK_PLANKS.getDefaultState();
                framedfabric$partRot[i] = 1;
            }
        }
    }

    @Override
    public boolean hasAnyCamo() {
        framedfabric$ensureInitialized();
        for (boolean part : framedfabric$hasPart) {
            if (part) return true;
        }
        return false;
    }

    @Override
    public boolean hasCamoPart(int index) {
        framedfabric$ensureInitialized();
        return index >= 0 && index < MAX_CAMO_PARTS && framedfabric$hasPart[index];
    }

    @Override
    public BlockState getCamoPart(int index) {
        framedfabric$ensureInitialized();
        if (index < 0 || index >= MAX_CAMO_PARTS) return Blocks.OAK_PLANKS.getDefaultState();
        return framedfabric$partCamo[index];
    }

    @Override
    public int getCamoRotPart(int index) {
        framedfabric$ensureInitialized();
        if (index < 0 || index >= MAX_CAMO_PARTS) return 1;
        return framedfabric$partRot[index];
    }

    @Override
    public void setCamoPart(int index, BlockState camo) {
        framedfabric$ensureInitialized();
        if (index < 0 || index >= MAX_CAMO_PARTS || camo == null) return;

        framedfabric$partCamo[index] = camo;
        framedfabric$hasPart[index] = true;

        framedfabric$syncHasCamoProp();
        framedfabric$syncAndRerender();
    }

    @Override
    public void clearCamoPart(int index) {
        framedfabric$ensureInitialized();
        if (index < 0 || index >= MAX_CAMO_PARTS) return;

        framedfabric$hasPart[index] = false;
        framedfabric$partCamo[index] = Blocks.OAK_PLANKS.getDefaultState();
        framedfabric$partRot[index] = 1;

        framedfabric$syncHasCamoProp();
        framedfabric$syncAndRerender();
    }

    @Override
    public void setCamoRotPart(int index, int rot) {
        framedfabric$ensureInitialized();
        if (index < 0 || index >= MAX_CAMO_PARTS) return;

        int value = framedfabric$clampRot(rot);
        if (framedfabric$partRot[index] == value) return;

        framedfabric$partRot[index] = value;
        framedfabric$syncAndRerender();
    }

    @Override
    public void cycleCamoRotPart(int index) {
        setCamoRotPart(index, getCamoRotPart(index) >= 6 ? 1 : getCamoRotPart(index) + 1);
    }

    @Inject(method = "readData", at = @At("TAIL"))
    private void framedfabric$readData(ReadView view, CallbackInfo ci) {
        framedfabric$ensureInitialized();
        if (!framedfabric$self().getCachedState().contains(HAS_CAMO)) return;

        boolean[] oldHas = null;
        BlockState[] oldCamo = null;
        int[] oldRot = null;

        World world = framedfabric$self().getWorld();
        boolean isClient = world != null && world.isClient();
        if (isClient) {
            oldHas = framedfabric$hasPart.clone();
            oldCamo = framedfabric$partCamo.clone();
            oldRot = framedfabric$partRot.clone();
        }

        for (int i = 0; i < MAX_CAMO_PARTS; i++) {
            framedfabric$hasPart[i] = view.getBoolean(KEY_PART_HAS_PREFIX + i, false);
            framedfabric$partCamo[i] = view.read(KEY_PART_CAMO_PREFIX + i, BlockState.CODEC)
                    .orElse(Blocks.OAK_PLANKS.getDefaultState());
            framedfabric$partRot[i] = framedfabric$clampRot(view.getInt(KEY_PART_ROT_PREFIX + i, 1));
        }

        if (world instanceof ServerWorld) {
            framedfabric$syncHasCamoProp();
        }

        if (isClient && framedfabric$hasCamoChanged(oldHas, oldCamo, oldRot)) {
            BlockState state = framedfabric$self().getCachedState();
            world.updateListeners(framedfabric$self().getPos(), state, state, 3);
            VoxyCompat.refreshChunk(world, framedfabric$self().getPos());
        }
    }

    @Inject(method = "writeData", at = @At("TAIL"))
    private void framedfabric$writeData(WriteView view, CallbackInfo ci) {
        framedfabric$ensureInitialized();
        if (!framedfabric$self().getCachedState().contains(HAS_CAMO)) return;

        for (int i = 0; i < MAX_CAMO_PARTS; i++) {
            view.putBoolean(KEY_PART_HAS_PREFIX + i, framedfabric$hasPart[i]);
            view.put(KEY_PART_CAMO_PREFIX + i, BlockState.CODEC, framedfabric$partCamo[i]);
            view.putInt(KEY_PART_ROT_PREFIX + i, framedfabric$partRot[i]);
        }
    }

    @Inject(method = "toInitialChunkDataNbt", at = @At("RETURN"))
    private void framedfabric$toInitialChunkDataNbt(
            net.minecraft.registry.RegistryWrapper.WrapperLookup registries,
            CallbackInfoReturnable<NbtCompound> cir
    ) {
        framedfabric$ensureInitialized();
        if (!framedfabric$self().getCachedState().contains(HAS_CAMO)) return;

        NbtCompound nbt = cir.getReturnValue();
        for (int i = 0; i < MAX_CAMO_PARTS; i++) {
            nbt.putBoolean(KEY_PART_HAS_PREFIX + i, framedfabric$hasPart[i]);
            nbt.put(KEY_PART_CAMO_PREFIX + i, BlockState.CODEC, framedfabric$partCamo[i]);
            nbt.putInt(KEY_PART_ROT_PREFIX + i, framedfabric$partRot[i]);
        }
    }

    @Unique
    private boolean framedfabric$hasCamoChanged(boolean[] oldHas, BlockState[] oldCamo, int[] oldRot) {
        if (oldHas == null || oldCamo == null || oldRot == null) return false;

        for (int i = 0; i < MAX_CAMO_PARTS; i++) {
            if (oldHas[i] != framedfabric$hasPart[i]) return true;
            if (oldRot[i] != framedfabric$partRot[i]) return true;
            if (oldCamo[i] != framedfabric$partCamo[i]) return true;
        }

        return false;
    }

    @Unique
    private void framedfabric$syncHasCamoProp() {
        BlockEntity self = framedfabric$self();
        if (!(self.getWorld() instanceof ServerWorld serverWorld)) return;

        BlockState state = self.getCachedState();
        if (!state.contains(HAS_CAMO)) return;

        boolean value = hasAnyCamo();
        if (state.get(HAS_CAMO) != value) {
            serverWorld.setBlockState(self.getPos(), state.with(HAS_CAMO, value), 3);
        }
    }

    @Unique
    private void framedfabric$syncAndRerender() {
        BlockEntity self = framedfabric$self();
        self.markDirty();

        if (self.getWorld() instanceof ServerWorld serverWorld) {
            BlockPos pos = self.getPos();
            serverWorld.getChunkManager().markForUpdate(pos);
            BlockState state = self.getCachedState();
            serverWorld.updateListeners(pos, state, state, 3);
        }
    }

    @Unique
    private int framedfabric$clampRot(int rot) {
        if (rot < 1) return 1;
        return Math.min(rot, 6);
    }

    @Unique
    private BlockEntity framedfabric$self() {
        return (BlockEntity) (Object) this;
    }
}
