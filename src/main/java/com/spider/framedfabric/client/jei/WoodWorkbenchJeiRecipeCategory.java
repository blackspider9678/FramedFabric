package com.spider.framedfabric.client.jei;

import com.spider.framedfabric.FramedFabric;
import com.spider.framedfabric.recipe.WoodWorkbenchRecipe;
import com.spider.framedfabric.registry.ModBlocks;
import mezz.jei.api.gui.builder.IRecipeLayoutBuilder;
import mezz.jei.api.gui.drawable.IDrawable;
import mezz.jei.api.gui.ingredient.IRecipeSlotsView;
import mezz.jei.api.helpers.IGuiHelper;
import mezz.jei.api.recipe.IFocusGroup;
import mezz.jei.api.recipe.category.IRecipeCategory;
import mezz.jei.api.recipe.types.IRecipeType;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;

@Environment(EnvType.CLIENT)
public final class WoodWorkbenchJeiRecipeCategory implements IRecipeCategory<WoodWorkbenchJeiRecipeCategory.DisplayRecipe> {
    public static final Identifier UID = Identifier.of(FramedFabric.MOD_ID, "wood_workbench");
    public static final IRecipeType<DisplayRecipe> RECIPE_TYPE = IRecipeType.create(UID, DisplayRecipe.class);

    private static final Text TITLE = Text.translatable("block." + FramedFabric.MOD_ID + ".wood_workbench");

    private final IDrawable icon;
    private final IDrawable arrow;

    public WoodWorkbenchJeiRecipeCategory(IGuiHelper guiHelper) {
        this.icon = guiHelper.createDrawableItemLike(ModBlocks.WOOD_WORKBENCH);
        this.arrow = guiHelper.getRecipeArrow();
    }

    @Override
    public IRecipeType<DisplayRecipe> getRecipeType() {
        return RECIPE_TYPE;
    }

    @Override
    public Text getTitle() {
        return TITLE;
    }

    @Override
    public int getWidth() {
        return 82;
    }

    @Override
    public int getHeight() {
        return 34;
    }

    @Override
    public IDrawable getIcon() {
        return this.icon;
    }

    @Override
    public void setRecipe(IRecipeLayoutBuilder builder, DisplayRecipe recipe, IFocusGroup focuses) {
        builder.addInputSlot(1, 9)
                .add(recipe.recipe().getIngredient())
                .setStandardSlotBackground();

        builder.addOutputSlot(61, 5)
                .add(recipe.recipe().getResultStack())
                .setOutputSlotBackground();
    }

    @Override
    public void draw(DisplayRecipe recipe, IRecipeSlotsView recipeSlotsView, DrawContext context, double mouseX, double mouseY) {
        this.arrow.draw(context, 24, 10);

        int inputCount = recipe.recipe().getInputCount();
        if (inputCount > 1) {
            context.drawText(
                    MinecraftClient.getInstance().textRenderer,
                    Text.literal("x" + inputCount),
                    1,
                    1,
                    0x404040,
                    false
            );
        }
    }

    @Override
    public Identifier getIdentifier(DisplayRecipe recipe) {
        return recipe.id();
    }

    public record DisplayRecipe(Identifier id, WoodWorkbenchRecipe recipe) {}
}
