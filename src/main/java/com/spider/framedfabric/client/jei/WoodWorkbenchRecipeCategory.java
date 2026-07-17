package com.spider.framedfabric.client.jei;

import com.spider.framedfabric.recipe.WoodWorkbenchRecipe;
import com.spider.framedfabric.registry.ModBlocks;
import mezz.jei.api.constants.VanillaTypes;
import mezz.jei.api.gui.builder.IRecipeLayoutBuilder;
import mezz.jei.api.gui.drawable.IDrawable;
import mezz.jei.api.gui.ingredient.IRecipeSlotsView;
import mezz.jei.api.helpers.IGuiHelper;
import mezz.jei.api.recipe.IFocusGroup;
import mezz.jei.api.recipe.category.IRecipeCategory;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.core.Holder;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.RecipeHolder;
import org.jetbrains.annotations.Nullable;

import java.util.List;

public final class WoodWorkbenchRecipeCategory implements IRecipeCategory<RecipeHolder<WoodWorkbenchRecipe>> {
    private static final Component TITLE = Component.translatable("jei.framedfabric.category.wood_workbench");
    private static final int WIDTH = 82;
    private static final int HEIGHT = 54;

    private final IDrawable icon;
    private final IDrawable arrow;

    public WoodWorkbenchRecipeCategory(IGuiHelper guiHelper) {
        this.icon = guiHelper.createDrawableIngredient(VanillaTypes.ITEM_STACK, new ItemStack(ModBlocks.WOOD_WORKBENCH));
        this.arrow = guiHelper.getRecipeArrow();
    }

    @Override
    public mezz.jei.api.recipe.types.IRecipeType<RecipeHolder<WoodWorkbenchRecipe>> getRecipeType() {
        return FramedJeiPlugin.WOOD_WORKBENCH;
    }

    @Override
    public Component getTitle() {
        return TITLE;
    }

    @Override
    public int getWidth() {
        return WIDTH;
    }

    @Override
    public int getHeight() {
        return HEIGHT;
    }

    @Override
    public @Nullable IDrawable getIcon() {
        return icon;
    }

    @Override
    public void setRecipe(IRecipeLayoutBuilder builder, RecipeHolder<WoodWorkbenchRecipe> entry, IFocusGroup focuses) {
        WoodWorkbenchRecipe recipe = entry.value();

        builder.addInputSlot(1, 18)
                .setStandardSlotBackground()
                .addItemStacks(getInputStacks(recipe));

        builder.addOutputSlot(61, 18)
                .setOutputSlotBackground()
                .add(recipe.getResultStack());
    }

    @Override
    public void draw(RecipeHolder<WoodWorkbenchRecipe> recipe, IRecipeSlotsView recipeSlotsView, GuiGraphicsExtractor graphics, double mouseX, double mouseY) {
        arrow.draw(graphics, 29, 18);
    }

    @Override
    public @Nullable Identifier getIdentifier(RecipeHolder<WoodWorkbenchRecipe> recipe) {
        return recipe.id().identifier();
    }

    private static List<ItemStack> getInputStacks(WoodWorkbenchRecipe recipe) {
        int count = Math.max(1, recipe.getInputCount());
        return recipe.getIngredient()
                .items()
                .map(Holder::value)
                .map(item -> new ItemStack(item, count))
                .toList();
    }
}
