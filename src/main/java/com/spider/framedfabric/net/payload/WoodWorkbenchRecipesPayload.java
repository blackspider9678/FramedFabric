package com.spider.framedfabric.net.payload;

import com.spider.framedfabric.FramedFabric;
import net.minecraft.item.ItemStack;
import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.codec.PacketCodecs;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.util.Identifier;

import java.util.List;

public record WoodWorkbenchRecipesPayload(int syncId, List<ItemStack> recipes) implements CustomPayload {
    public static final Id<WoodWorkbenchRecipesPayload> ID =
            new Id<>(Identifier.of(FramedFabric.MOD_ID, "wood_workbench_recipes"));

    public static final PacketCodec<RegistryByteBuf, WoodWorkbenchRecipesPayload> CODEC =
            PacketCodec.tuple(
                    PacketCodecs.INTEGER, WoodWorkbenchRecipesPayload::syncId,
                    ItemStack.PACKET_CODEC.collect(PacketCodecs.toList()), WoodWorkbenchRecipesPayload::recipes,
                    WoodWorkbenchRecipesPayload::new
            );

    @Override
    public Id<? extends CustomPayload> getId() {
        return ID;
    }
}