package com.spider.framedfabric.recipe;

import com.spider.framedfabric.registry.ModRecipeTypes;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ItemStackTemplate;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.PlacementInfo;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.item.crafting.RecipeBookCategories;
import net.minecraft.world.item.crafting.RecipeBookCategory;
import net.minecraft.world.item.crafting.SingleRecipeInput;
import net.minecraft.world.level.Level;

public class WoodWorkbenchRecipe implements Recipe<SingleRecipeInput> {
    private final Ingredient ingredient;
    private final int inputCount;
    private final ItemStackTemplate result;

    public WoodWorkbenchRecipe(Ingredient ingredient, int inputCount, ItemStackTemplate result) {
        this.ingredient = ingredient;
        this.inputCount = inputCount;
        this.result = result;
    }

    public Ingredient getIngredient() {
        return ingredient;
    }

    public int getInputCount() {
        return inputCount;
    }

    public ItemStackTemplate getResultTemplate() {
        return result;
    }

    public ItemStack getResultStack() {
        return result.create();
    }

    public boolean matchesStack(ItemStack stack) {
        return !stack.isEmpty() && ingredient.test(stack) && stack.getCount() >= inputCount;
    }

    @Override
    public boolean matches(SingleRecipeInput input, Level world) {
        return matchesStack(input.item());
    }

    @Override
    public ItemStack assemble(SingleRecipeInput input) {
        return result.create();
    }

    @Override
    public boolean showNotification() {
        return false;
    }

    @Override
    public String group() {
        return "";
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
    public PlacementInfo placementInfo() {
        return PlacementInfo.create(this.ingredient);
    }

    @Override
    public RecipeBookCategory recipeBookCategory() {
        return RecipeBookCategories.STONECUTTER;
    }
}
