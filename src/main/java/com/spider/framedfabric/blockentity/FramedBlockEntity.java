package com.spider.framedfabric.blockentity;

import com.spider.framedfabric.registry.ModBlockEntities;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.network.listener.ClientPlayPacketListener;
import net.minecraft.network.packet.Packet;
import net.minecraft.network.packet.s2c.play.BlockEntityUpdateS2CPacket;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.state.StateManager;
import net.minecraft.state.property.BooleanProperty;
import net.minecraft.storage.ReadView;
import net.minecraft.storage.WriteView;
import net.minecraft.util.math.BlockPos;
import org.jetbrains.annotations.Nullable;

import java.util.Optional;

public final class FramedBlockEntity extends BlockEntity {

    private static final String KEY_HAS_CAMO = "has_camo";
    private static final String KEY_CAMO = "camo";

    private boolean hasCamo = false;
    private BlockState camo = Blocks.OAK_PLANKS.getDefaultState();

    // Render attachment payload
    public record FramedRenderData(boolean hasCamo, BlockState camo) {}

    public FramedBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.FRAMED, pos, state);
    }

    public boolean hasCamo() { return hasCamo; }
    public BlockState getCamo() { return camo; }

    public void setCamo(BlockState camo) {
        if (camo == null) return;
        this.camo = camo;
        this.hasCamo = true;

        if (world != null && !world.isClient()) {
            setHasCamoProperty(true);
        }

        syncAndRerender();
    }

    public void clearCamo() {
        this.hasCamo = false;
        this.camo = Blocks.OAK_PLANKS.getDefaultState();

        if (world != null && !world.isClient()) {
            setHasCamoProperty(false);
        }

        syncAndRerender();
    }

    private void setHasCamoProperty(boolean value) {
        if (world == null) return;

        BlockState bs = world.getBlockState(pos);
        Optional<BooleanProperty> propOpt = findBooleanProperty(bs.getBlock().getStateManager(), KEY_HAS_CAMO);
        if (propOpt.isEmpty()) return;

        BooleanProperty prop = propOpt.get();
        if (!bs.contains(prop)) return;

        BlockState next = bs.with(prop, value);
        if (next != bs) {
            world.setBlockState(pos, next, 3);
        }
    }

    private static Optional<BooleanProperty> findBooleanProperty(StateManager<?, ?> sm, String name) {
        var p = sm.getProperty(name);
        if (p instanceof BooleanProperty bp) return Optional.of(bp);
        return Optional.empty();
    }

    private void syncAndRerender() {
        markDirty();

        if (world instanceof ServerWorld sw) {
            sw.getChunkManager().markForUpdate(pos);
        }

        if (world != null) {
            BlockState s = getCachedState();
            world.updateListeners(pos, s, s, 3);
        }
    }

    @Override
    protected void readData(ReadView view) {
        super.readData(view);
        this.hasCamo = view.getBoolean(KEY_HAS_CAMO, false);
        this.camo = view.read(KEY_CAMO, BlockState.CODEC).orElse(this.camo);
    }

    @Override
    protected void writeData(WriteView view) {
        view.putBoolean(KEY_HAS_CAMO, this.hasCamo);
        view.put(KEY_CAMO, BlockState.CODEC, this.camo);
        super.writeData(view);
    }

    @Override
    public @Nullable Packet<ClientPlayPacketListener> toUpdatePacket() {
        return BlockEntityUpdateS2CPacket.create(this);
    }

    @Override
    public net.minecraft.nbt.NbtCompound toInitialChunkDataNbt(net.minecraft.registry.RegistryWrapper.WrapperLookup registries) {
        return createNbt(registries);
    }
}
