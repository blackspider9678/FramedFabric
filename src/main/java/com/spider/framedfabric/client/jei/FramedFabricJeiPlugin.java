package com.spider.framedfabric.client.jei;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.spider.framedfabric.FramedFabric;
import com.spider.framedfabric.client.screen.WoodWorkbenchScreen;
import com.spider.framedfabric.recipe.WoodWorkbenchRecipe;
import com.spider.framedfabric.registry.ModBlocks;
import com.spider.framedfabric.registry.ModScreenHandlers;
import com.spider.framedfabric.screen.WoodWorkbenchScreenHandler;
import mezz.jei.api.IModPlugin;
import mezz.jei.api.JeiPlugin;
import mezz.jei.api.registration.IGuiHandlerRegistration;
import mezz.jei.api.registration.IRecipeCatalystRegistration;
import mezz.jei.api.registration.IRecipeCategoryRegistration;
import mezz.jei.api.registration.IRecipeRegistration;
import mezz.jei.api.registration.IRecipeTransferRegistration;
import mezz.jei.api.runtime.IJeiRuntime;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.MinecraftClient;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Item;
import net.minecraft.item.Items;
import net.minecraft.recipe.Ingredient;
import net.minecraft.registry.Registries;
import net.minecraft.resource.Resource;
import net.minecraft.util.Identifier;

import java.io.BufferedReader;
import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

@JeiPlugin
@Environment(EnvType.CLIENT)
public final class FramedFabricJeiPlugin implements IModPlugin {
    private static final Identifier PLUGIN_UID = Identifier.of(FramedFabric.MOD_ID, "jei_plugin");
    private static List<WoodWorkbenchJeiRecipeCategory.DisplayRecipe> syncedRecipes = List.of();
    private static List<WoodWorkbenchJeiRecipeCategory.DisplayRecipe> registeredRecipes = List.of();
    private static IJeiRuntime jeiRuntime;
    private static boolean hasReceivedSync;

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
        List<WoodWorkbenchJeiRecipeCategory.DisplayRecipe> initialRecipes = syncedRecipes.isEmpty()
                ? loadRecipesFromResources()
                : syncedRecipes;

        if (initialRecipes.isEmpty()) {
            return;
        }

        registration.addRecipes(WoodWorkbenchJeiRecipeCategory.RECIPE_TYPE, initialRecipes);
        registeredRecipes = List.copyOf(initialRecipes);
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
        registration.addRecipeCatalyst(
                ModBlocks.WOOD_WORKBENCH,
                WoodWorkbenchJeiRecipeCategory.RECIPE_TYPE
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

    @Override
    public void onRuntimeAvailable(IJeiRuntime jeiRuntime) {
        FramedFabricJeiPlugin.jeiRuntime = jeiRuntime;
        refreshRuntimeRecipes();
    }

    @Override
    public void onRuntimeUnavailable() {
        jeiRuntime = null;
        syncedRecipes = List.of();
        registeredRecipes = List.of();
        hasReceivedSync = false;
    }

    public static void updateSyncedRecipes(List<WoodWorkbenchJeiRecipeCategory.DisplayRecipe> recipes) {
        syncedRecipes = List.copyOf(recipes);
        hasReceivedSync = true;
        refreshRuntimeRecipes();
    }

    private static void refreshRuntimeRecipes() {
        if (jeiRuntime == null) {
            return;
        }

        if (!hasReceivedSync) {
            return;
        }

        if (!registeredRecipes.isEmpty()) {
            if (haveSameRecipeIds(registeredRecipes, syncedRecipes)) {
                return;
            }

            jeiRuntime.getRecipeManager().hideRecipes(WoodWorkbenchJeiRecipeCategory.RECIPE_TYPE, registeredRecipes);
        }

        registeredRecipes = List.copyOf(syncedRecipes);

        if (!registeredRecipes.isEmpty()) {
            jeiRuntime.getRecipeManager().addRecipes(WoodWorkbenchJeiRecipeCategory.RECIPE_TYPE, registeredRecipes);
            jeiRuntime.getRecipeManager().unhideRecipes(WoodWorkbenchJeiRecipeCategory.RECIPE_TYPE, registeredRecipes);
        }
    }

    private static boolean haveSameRecipeIds(
            List<WoodWorkbenchJeiRecipeCategory.DisplayRecipe> left,
            List<WoodWorkbenchJeiRecipeCategory.DisplayRecipe> right
    ) {
        if (left.size() != right.size()) {
            return false;
        }

        Set<Identifier> leftIds = left.stream()
                .map(WoodWorkbenchJeiRecipeCategory.DisplayRecipe::id)
                .collect(java.util.stream.Collectors.toUnmodifiableSet());
        Set<Identifier> rightIds = right.stream()
                .map(WoodWorkbenchJeiRecipeCategory.DisplayRecipe::id)
                .collect(java.util.stream.Collectors.toUnmodifiableSet());

        return leftIds.equals(rightIds);
    }

    private static List<WoodWorkbenchJeiRecipeCategory.DisplayRecipe> loadRecipesFromResources() {
        MinecraftClient client = MinecraftClient.getInstance();
        if (client == null) {
            return List.of();
        }

        Map<Identifier, Resource> resources = client.getResourceManager().findResources(
                "recipe/wood_workbench",
                id -> id.getNamespace().equals(FramedFabric.MOD_ID) && id.getPath().endsWith(".json")
        );

        return resources.entrySet()
                .stream()
                .sorted(Map.Entry.comparingByKey())
                .map(entry -> parseDisplayRecipe(entry.getKey(), entry.getValue()))
                .flatMap(Optional::stream)
                .toList();
    }

    private static Optional<WoodWorkbenchJeiRecipeCategory.DisplayRecipe> parseDisplayRecipe(Identifier resourceId, Resource resource) {
        try (BufferedReader reader = resource.getReader()) {
            JsonObject json = JsonParser.parseReader(reader).getAsJsonObject();
            if (!areLoadConditionsSatisfied(json)) {
                return Optional.empty();
            }

            String ingredientId = json.get("ingredient").getAsString();
            JsonObject resultJson = json.getAsJsonObject("result");
            String resultId = resultJson.get("id").getAsString();

            Item ingredientItem = getItem(ingredientId);
            Item resultItem = getItem(resultId);

            if (ingredientItem == null || resultItem == null) {
                return Optional.empty();
            }

            int inputCount = json.has("input_count") ? json.get("input_count").getAsInt() : 1;
            int resultCount = resultJson.has("count") ? resultJson.get("count").getAsInt() : 1;

            Identifier recipeId = Identifier.of(
                    resourceId.getNamespace(),
                    resourceId.getPath().substring("recipe/".length(), resourceId.getPath().length() - ".json".length())
            );

            WoodWorkbenchRecipe recipe = new WoodWorkbenchRecipe(
                    Ingredient.ofItems(ingredientItem),
                    inputCount,
                    new ItemStack(resultItem, resultCount)
            );

            return Optional.of(new WoodWorkbenchJeiRecipeCategory.DisplayRecipe(recipeId, recipe));
        } catch (IOException | RuntimeException e) {
            FramedFabric.LOGGER.warn("Failed to load JEI wood workbench recipe from {}", resourceId, e);
            return Optional.empty();
        }
    }

    private static Item getItem(String itemId) {
        Identifier id = Identifier.tryParse(itemId);
        if (id == null) {
            return null;
        }

        Item item = Registries.ITEM.get(id);
        if (item == Items.AIR && !id.equals(Identifier.ofVanilla("air"))) {
            return null;
        }

        return item;
    }

    private static boolean areLoadConditionsSatisfied(JsonObject json) {
        if (!json.has("fabric:load_conditions")) {
            return true;
        }

        JsonArray conditions = json.getAsJsonArray("fabric:load_conditions");
        for (JsonElement condition : conditions) {
            if (!testCondition(condition.getAsJsonObject())) {
                return false;
            }
        }

        return true;
    }

    private static boolean testCondition(JsonObject condition) {
        String type = condition.get("condition").getAsString();

        return switch (type) {
            case "fabric:true" -> true;
            case "fabric:all_mods_loaded" -> allModsLoaded(condition.getAsJsonArray("values"));
            case "fabric:any_mods_loaded" -> anyModsLoaded(condition.getAsJsonArray("values"));
            case "fabric:not" -> !testCondition(condition.getAsJsonObject("value"));
            case "fabric:and" -> testAll(condition.getAsJsonArray("values"));
            case "fabric:or" -> testAny(condition.getAsJsonArray("values"));
            default -> true;
        };
    }

    private static boolean allModsLoaded(JsonArray values) {
        for (JsonElement value : values) {
            if (!FabricLoader.getInstance().isModLoaded(value.getAsString())) {
                return false;
            }
        }

        return true;
    }

    private static boolean anyModsLoaded(JsonArray values) {
        for (JsonElement value : values) {
            if (FabricLoader.getInstance().isModLoaded(value.getAsString())) {
                return true;
            }
        }

        return false;
    }

    private static boolean testAll(JsonArray values) {
        for (JsonElement value : values) {
            if (!testCondition(value.getAsJsonObject())) {
                return false;
            }
        }

        return true;
    }

    private static boolean testAny(JsonArray values) {
        for (JsonElement value : values) {
            if (testCondition(value.getAsJsonObject())) {
                return true;
            }
        }

        return false;
    }
}
