package com.spider.framedfabric.recipe;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.world.item.ItemStackTemplate;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.RecipeSerializer;

public final class WoodWorkbenchRecipeSerializer {

    public static final MapCodec<WoodWorkbenchRecipe> CODEC = RecordCodecBuilder.mapCodec(instance -> instance.group(
            Ingredient.CODEC.fieldOf("ingredient").forGetter(WoodWorkbenchRecipe::getIngredient),
            Codec.INT.optionalFieldOf("input_count", 1).forGetter(WoodWorkbenchRecipe::getInputCount),
            ItemStackTemplate.CODEC.fieldOf("result").forGetter(WoodWorkbenchRecipe::getResultTemplate)
    ).apply(instance, WoodWorkbenchRecipe::new));

    public static final StreamCodec<RegistryFriendlyByteBuf, WoodWorkbenchRecipe> PACKET_CODEC =
            StreamCodec.composite(
                    Ingredient.CONTENTS_STREAM_CODEC, WoodWorkbenchRecipe::getIngredient,
                    ByteBufCodecs.INT, WoodWorkbenchRecipe::getInputCount,
                    ItemStackTemplate.STREAM_CODEC, WoodWorkbenchRecipe::getResultTemplate,
                    WoodWorkbenchRecipe::new
            );

    public static final RecipeSerializer<WoodWorkbenchRecipe> INSTANCE =
            new RecipeSerializer<>(CODEC, PACKET_CODEC);

    private WoodWorkbenchRecipeSerializer() {
    }
}
