package com.spider.framedfabric.recipe;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.item.ItemStack;
import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.codec.PacketCodecs;
import net.minecraft.recipe.Ingredient;
import net.minecraft.recipe.RecipeSerializer;

public class WoodWorkbenchRecipeSerializer implements RecipeSerializer<WoodWorkbenchRecipe> {

    public static final MapCodec<WoodWorkbenchRecipe> CODEC = RecordCodecBuilder.mapCodec(instance -> instance.group(
            Ingredient.CODEC.fieldOf("ingredient").forGetter(WoodWorkbenchRecipe::getIngredient),
            Codec.INT.optionalFieldOf("input_count", 1).forGetter(WoodWorkbenchRecipe::getInputCount),
            ItemStack.VALIDATED_CODEC.fieldOf("result").forGetter(WoodWorkbenchRecipe::getResultStack)
    ).apply(instance, WoodWorkbenchRecipe::new));

    public static final PacketCodec<RegistryByteBuf, WoodWorkbenchRecipe> PACKET_CODEC =
            PacketCodec.tuple(
                    Ingredient.PACKET_CODEC, WoodWorkbenchRecipe::getIngredient,
                    PacketCodecs.INTEGER, WoodWorkbenchRecipe::getInputCount,
                    ItemStack.PACKET_CODEC, WoodWorkbenchRecipe::getResultStack,
                    WoodWorkbenchRecipe::new
            );

    @Override
    public MapCodec<WoodWorkbenchRecipe> codec() {
        return CODEC;
    }

    @Override
    public PacketCodec<RegistryByteBuf, WoodWorkbenchRecipe> packetCodec() {
        return PACKET_CODEC;
    }
}