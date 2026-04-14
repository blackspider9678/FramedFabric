package com.spider.framedfabric.client.jei;

import com.spider.framedfabric.FramedFabric;
import com.spider.framedfabric.client.screen.WoodWorkbenchScreen;
import com.spider.framedfabric.recipe.WoodWorkbenchRecipe;
import com.spider.framedfabric.registry.ModBlocks;
import com.spider.framedfabric.registry.ModRecipeTypes;
import com.spider.framedfabric.registry.ModScreenHandlers;
import com.spider.framedfabric.screen.WoodWorkbenchScreenHandler;
import mezz.jei.api.IModPlugin;
import mezz.jei.api.JeiPlugin;
import mezz.jei.api.registration.IGuiHandlerRegistration;
import mezz.jei.api.registration.IRecipeCatalystRegistration;
import mezz.jei.api.registration.IRecipeCategoryRegistration;
import mezz.jei.api.registration.IRecipeRegistration;
import mezz.jei.api.registration.IRecipeTransferRegistration;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.MinecraftClient;
import net.minecraft.recipe.RecipeEntry;
import net.minecraft.util.Identifier;

import java.util.List;

@JeiPlugin
@Environment(EnvType.CLIENT)
public final class FramedFabricJeiPlugin implements IModPlugin {
    private static final Identifier PLUGIN_UID = Identifier.of(FramedFabric.MOD_ID, "jei_plugin");

    @Override
    public Identifier getPluginUid() {
        return PLUGIN_UID;
    }

    @Override
    public void registerCategories(IRecipeCategoryRegistration registration) {
        registration.addRecipeCategories(
                new WoodWorkbenchJeiRecipeCategory(registration.getJeiHelpers().getGuiHelper())
        );
    }

    @Override
    public void registerRecipes(IRecipeRegistration registration) {
        registration.addRecipes(
                WoodWorkbenchJeiRecipeCategory.RECIPE_TYPE,
                getWoodWorkbenchRecipes()
        );
    }

    @Override
    public void registerRecipeTransferHandlers(IRecipeTransferRegistration registration) {
        registration.addRecipeTransferHandler(
                WoodWorkbenchScreenHandler.class,
                ModScreenHandlers.WOOD_WORKBENCH,
                WoodWorkbenchJeiRecipeCategory.RECIPE_TYPE,
                0,
                1,
                2,
                36
        );
    }

    @Override
    public void registerRecipeCatalysts(IRecipeCatalystRegistration registration) {
        registration.addCraftingStation(
                WoodWorkbenchJeiRecipeCategory.RECIPE_TYPE,
                ModBlocks.WOOD_WORKBENCH
        );
    }

    @Override
    public void registerGuiHandlers(IGuiHandlerRegistration registration) {
        registration.addRecipeClickArea(
                WoodWorkbenchScreen.class,
                121,
                33,
                20,
                17,
                WoodWorkbenchJeiRecipeCategory.RECIPE_TYPE
        );
    }

    private static List<WoodWorkbenchJeiRecipeCategory.DisplayRecipe> getWoodWorkbenchRecipes() {
        MinecraftClient client = MinecraftClient.getInstance();
        if (client.world == null) {
            return List.of();
        }

        return client.world.getRecipeManager()
                .getSynchronizedRecipes()
                .getAllOfType(ModRecipeTypes.WOOD_WORKBENCH)
                .stream()
                .map(FramedFabricJeiPlugin::toDisplayRecipe)
                .toList();
    }

    private static WoodWorkbenchJeiRecipeCategory.DisplayRecipe toDisplayRecipe(RecipeEntry<WoodWorkbenchRecipe> entry) {
        return new WoodWorkbenchJeiRecipeCategory.DisplayRecipe(entry.id().getValue(), entry.value());
    }
}
