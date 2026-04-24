package com.spider.framedfabric.recipe;

import com.spider.framedfabric.registry.ModRecipeTypes;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.PlacementInfo;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.item.crafting.RecipeBookCategories;
import net.minecraft.world.item.crafting.RecipeBookCategory;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.item.crafting.SingleRecipeInput;
import net.minecraft.world.level.Level;

public class WoodWorkbenchRecipe implements Recipe<SingleRecipeInput> {
    private final Ingredient ingredient;
    private final int inputCount;
    private final Item resultItem;
    private final int resultCount;

    public WoodWorkbenchRecipe(Ingredient ingredient, int inputCount, Item resultItem, int resultCount) {
        this.ingredient = ingredient;
        this.inputCount = inputCount;
        this.resultItem = resultItem;
        this.resultCount = resultCount;
    }

    public WoodWorkbenchRecipe(Ingredient ingredient, int inputCount, ItemStack result) {
        this(ingredient, inputCount, result.getItem(), result.getCount());
    }

    public Ingredient getIngredient() {
        return ingredient;
    }

    public int getInputCount() {
        return inputCount;
    }

    public Item getResultItem() {
        return resultItem;
    }

    public int getResultCount() {
        return resultCount;
    }

    public ItemStack getResultStack() {
        return resultItem.getDefaultInstance().copyWithCount(resultCount);
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
        return getResultStack();
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
