package com.spider.framedfabric.recipe;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.crafting.Ingredient;

public final class WoodWorkbenchRecipeSerializer {
    private WoodWorkbenchRecipeSerializer() {}

    public static final MapCodec<WoodWorkbenchRecipe> CODEC = RecordCodecBuilder.mapCodec(instance -> instance.group(
            Ingredient.CODEC.fieldOf("ingredient").forGetter(WoodWorkbenchRecipe::getIngredient),
            Codec.INT.optionalFieldOf("input_count", 1).forGetter(WoodWorkbenchRecipe::getInputCount),
            ResultDefinition.CODEC.fieldOf("result").forGetter(ResultDefinition::of)
    ).apply(instance, (ingredient, inputCount, result) -> new WoodWorkbenchRecipe(ingredient, inputCount, result.item(), result.count())));

    public static final StreamCodec<RegistryFriendlyByteBuf, WoodWorkbenchRecipe> PACKET_CODEC =
            StreamCodec.composite(
                    Ingredient.CONTENTS_STREAM_CODEC, WoodWorkbenchRecipe::getIngredient,
                    ByteBufCodecs.INT, WoodWorkbenchRecipe::getInputCount,
                    ResultDefinition.STREAM_CODEC, ResultDefinition::of,
                    (ingredient, inputCount, result) -> new WoodWorkbenchRecipe(ingredient, inputCount, result.item(), result.count())
            );

    private record ResultDefinition(Item item, int count) {
        private static final Codec<ResultDefinition> CODEC = RecordCodecBuilder.create(instance -> instance.group(
                BuiltInRegistries.ITEM.byNameCodec().fieldOf("id").forGetter(ResultDefinition::item),
                Codec.INT.optionalFieldOf("count", 1).forGetter(ResultDefinition::count)
        ).apply(instance, ResultDefinition::new));

        private static final StreamCodec<RegistryFriendlyByteBuf, ResultDefinition> STREAM_CODEC =
                StreamCodec.composite(
                        Identifier.STREAM_CODEC, result -> BuiltInRegistries.ITEM.getKey(result.item()),
                        ByteBufCodecs.INT, ResultDefinition::count,
                        (id, count) -> new ResultDefinition(BuiltInRegistries.ITEM.getValue(id), count)
                );

        private static ResultDefinition of(WoodWorkbenchRecipe recipe) {
            return new ResultDefinition(recipe.getResultItem(), recipe.getResultCount());
        }
    }
}
