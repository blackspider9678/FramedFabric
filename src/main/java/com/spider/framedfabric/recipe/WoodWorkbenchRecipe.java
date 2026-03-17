package com.spider.framedfabric.recipe;

import com.spider.framedfabric.registry.ModRecipeTypes;
import net.minecraft.item.ItemStack;
import net.minecraft.recipe.Ingredient;
import net.minecraft.recipe.IngredientPlacement;
import net.minecraft.recipe.Recipe;
import net.minecraft.recipe.RecipeSerializer;
import net.minecraft.recipe.RecipeType;
import net.minecraft.recipe.book.RecipeBookCategories;
import net.minecraft.recipe.book.RecipeBookCategory;
import net.minecraft.recipe.input.SingleStackRecipeInput;
import net.minecraft.registry.RegistryWrapper;
import net.minecraft.world.World;

public class WoodWorkbenchRecipe implements Recipe<SingleStackRecipeInput> {
    private final Ingredient ingredient;
    private final int inputCount;
    private final ItemStack result;

    public WoodWorkbenchRecipe(Ingredient ingredient, int inputCount, ItemStack result) {
        this.ingredient = ingredient;
        this.inputCount = inputCount;
        this.result = result.copy();
    }

    public Ingredient getIngredient() {
        return ingredient;
    }

    public int getInputCount() {
        return inputCount;
    }

    public ItemStack getResultStack() {
        return result.copy();
    }

    public boolean matchesStack(ItemStack stack) {
        return !stack.isEmpty() && ingredient.test(stack) && stack.getCount() >= inputCount;
    }

    @Override
    public boolean matches(SingleStackRecipeInput input, World world) {
        return matchesStack(input.item());
    }

    @Override
    public ItemStack craft(SingleStackRecipeInput input, RegistryWrapper.WrapperLookup lookup) {
        return result.copy();
    }

    @Override
    public RecipeSerializer<WoodWorkbenchRecipe> getSerializer() {
        return ModRecipeTypes.WOOD_WORKBENCH_SERIALIZER;
    }

    @Override
    public RecipeType<WoodWorkbenchRecipe> getType() {
        return ModRecipeTypes.WOOD_WORKBENCH;
    }

    @Override
    public IngredientPlacement getIngredientPlacement() {
        return IngredientPlacement.forSingleSlot(this.ingredient);
    }

    @Override
    public RecipeBookCategory getRecipeBookCategory() {
        return RecipeBookCategories.STONECUTTER;
    }
}