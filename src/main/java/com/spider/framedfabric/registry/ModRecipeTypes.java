package com.spider.framedfabric.registry;

import com.spider.framedfabric.FramedFabric;
import com.spider.framedfabric.recipe.WoodWorkbenchRecipe;
import com.spider.framedfabric.recipe.WoodWorkbenchRecipeSerializer;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.item.crafting.RecipeType;

public final class ModRecipeTypes {
    private ModRecipeTypes() {}

    public static final RecipeType<WoodWorkbenchRecipe> WOOD_WORKBENCH = Registry.register(
            BuiltInRegistries.RECIPE_TYPE,
            Identifier.fromNamespaceAndPath(FramedFabric.MOD_ID, "wood_workbench"),
            new RecipeType<WoodWorkbenchRecipe>() {
                @Override
                public String toString() {
                    return FramedFabric.MOD_ID + ":wood_workbench";
                }
            }
    );

    public static final RecipeSerializer<WoodWorkbenchRecipe> WOOD_WORKBENCH_SERIALIZER = Registry.register(
            BuiltInRegistries.RECIPE_SERIALIZER,
            Identifier.fromNamespaceAndPath(FramedFabric.MOD_ID, "wood_workbench"),
            new RecipeSerializer<>(WoodWorkbenchRecipeSerializer.CODEC, WoodWorkbenchRecipeSerializer.PACKET_CODEC)
    );

    public static void init() {}
}
