package com.spider.framedfabric.registry;

import com.spider.framedfabric.FramedFabric;
import com.spider.framedfabric.recipe.WoodWorkbenchRecipe;
import com.spider.framedfabric.recipe.WoodWorkbenchRecipeSerializer;
import net.minecraft.recipe.RecipeSerializer;
import net.minecraft.recipe.RecipeType;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.util.Identifier;

public final class ModRecipeTypes {
    private ModRecipeTypes() {}

    public static final RecipeType<WoodWorkbenchRecipe> WOOD_WORKBENCH = Registry.register(
            Registries.RECIPE_TYPE,
            Identifier.of(FramedFabric.MOD_ID, "wood_workbench"),
            new RecipeType<WoodWorkbenchRecipe>() {
                @Override
                public String toString() {
                    return FramedFabric.MOD_ID + ":wood_workbench";
                }
            }
    );

    public static final RecipeSerializer<WoodWorkbenchRecipe> WOOD_WORKBENCH_SERIALIZER = Registry.register(
            Registries.RECIPE_SERIALIZER,
            Identifier.of(FramedFabric.MOD_ID, "wood_workbench"),
            new WoodWorkbenchRecipeSerializer()
    );

    public static void init() {}
}