package com.spider.framedfabric.net.payload;

import com.spider.framedfabric.FramedFabric;
import com.spider.framedfabric.recipe.WoodWorkbenchRecipe;
import com.spider.framedfabric.recipe.WoodWorkbenchRecipeSerializer;
import com.spider.framedfabric.registry.ModRecipeTypes;
import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.recipe.RecipeManager;
import net.minecraft.util.Identifier;

import java.util.ArrayList;
import java.util.List;

public record WoodWorkbenchJeiRecipesPayload(List<Entry> recipes) implements CustomPayload {
    public static final Id<WoodWorkbenchJeiRecipesPayload> ID =
            new Id<>(Identifier.of(FramedFabric.MOD_ID, "wood_workbench_jei_recipes"));

    public static final PacketCodec<RegistryByteBuf, WoodWorkbenchJeiRecipesPayload> CODEC =
            PacketCodec.of(WoodWorkbenchJeiRecipesPayload::write, WoodWorkbenchJeiRecipesPayload::new);

    public WoodWorkbenchJeiRecipesPayload {
        recipes = List.copyOf(recipes);
    }

    private WoodWorkbenchJeiRecipesPayload(RegistryByteBuf buf) {
        this(readEntries(buf));
    }

    public static WoodWorkbenchJeiRecipesPayload fromRecipeManager(RecipeManager recipeManager) {
        return new WoodWorkbenchJeiRecipesPayload(
                recipeManager.getSynchronizedRecipes()
                        .getAllOfType(ModRecipeTypes.WOOD_WORKBENCH)
                        .stream()
                        .map(entry -> new Entry(entry.id().getValue(), entry.value()))
                        .toList()
        );
    }

    private void write(RegistryByteBuf buf) {
        buf.writeVarInt(this.recipes.size());
        for (Entry entry : this.recipes) {
            entry.write(buf);
        }
    }

    private static List<Entry> readEntries(RegistryByteBuf buf) {
        int size = buf.readVarInt();
        List<Entry> entries = new ArrayList<>(size);

        for (int i = 0; i < size; i++) {
            entries.add(new Entry(buf));
        }

        return entries;
    }

    @Override
    public Id<? extends CustomPayload> getId() {
        return ID;
    }

    public record Entry(Identifier id, WoodWorkbenchRecipe recipe) {
        private Entry(RegistryByteBuf buf) {
            this(buf.readIdentifier(), WoodWorkbenchRecipeSerializer.PACKET_CODEC.decode(buf));
        }

        private void write(RegistryByteBuf buf) {
            buf.writeIdentifier(this.id);
            WoodWorkbenchRecipeSerializer.PACKET_CODEC.encode(buf, this.recipe);
        }
    }
}
