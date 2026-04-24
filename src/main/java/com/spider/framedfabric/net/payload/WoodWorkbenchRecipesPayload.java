package com.spider.framedfabric.net.payload;

import com.spider.framedfabric.FramedFabric;
import java.util.List;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.ItemStack;

public record WoodWorkbenchRecipesPayload(int syncId, List<ItemStack> recipes) implements CustomPacketPayload {
    public static final Type<WoodWorkbenchRecipesPayload> ID =
            new Type<>(Identifier.fromNamespaceAndPath(FramedFabric.MOD_ID, "wood_workbench_recipes"));

    public static final StreamCodec<RegistryFriendlyByteBuf, WoodWorkbenchRecipesPayload> CODEC =
            StreamCodec.composite(
                    ByteBufCodecs.INT, WoodWorkbenchRecipesPayload::syncId,
                    ItemStack.STREAM_CODEC.apply(ByteBufCodecs.list()), WoodWorkbenchRecipesPayload::recipes,
                    WoodWorkbenchRecipesPayload::new
            );

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return ID;
    }
}