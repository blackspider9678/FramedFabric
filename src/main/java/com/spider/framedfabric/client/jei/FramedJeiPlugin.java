package com.spider.framedfabric.client.jei;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.spider.framedfabric.FramedFabric;
import com.spider.framedfabric.recipe.WoodWorkbenchRecipe;
import com.spider.framedfabric.registry.ModBlocks;
import com.spider.framedfabric.registry.ModRecipeTypes;
import com.spider.framedfabric.registry.ModScreenHandlers;
import com.spider.framedfabric.screen.WoodWorkbenchScreenHandler;
import mezz.jei.api.IModPlugin;
import mezz.jei.api.JeiPlugin;
import mezz.jei.api.recipe.types.IRecipeHolderType;
import mezz.jei.api.recipe.types.IRecipeType;
import mezz.jei.api.registration.IRecipeCatalystRegistration;
import mezz.jei.api.registration.IRecipeCategoryRegistration;
import mezz.jei.api.registration.IRecipeRegistration;
import mezz.jei.api.registration.IRecipeTransferRegistration;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.item.crafting.RecipeHolder;

import java.io.IOException;
import java.io.Reader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.stream.Stream;

@JeiPlugin
public final class FramedJeiPlugin implements IModPlugin {
    public static final IRecipeHolderType<WoodWorkbenchRecipe> WOOD_WORKBENCH =
            IRecipeType.create(ModRecipeTypes.WOOD_WORKBENCH);

    private static final Identifier UID = Identifier.fromNamespaceAndPath(FramedFabric.MOD_ID, "jei_plugin");
    private static final Identifier WOOD_WORKBENCH_RECIPE_TYPE = Identifier.fromNamespaceAndPath(FramedFabric.MOD_ID, "wood_workbench");
    private static final String RECIPE_ROOT = "data/" + FramedFabric.MOD_ID + "/recipe/wood_workbench";

    @Override
    public Identifier getPluginUid() {
        return UID;
    }

    @Override
    public void registerCategories(IRecipeCategoryRegistration registration) {
        registration.addRecipeCategories(new WoodWorkbenchRecipeCategory(registration.getJeiHelpers().getGuiHelper()));
    }

    @Override
    public void registerRecipes(IRecipeRegistration registration) {
        registration.addRecipes(WOOD_WORKBENCH, loadWorkbenchRecipes());
    }

    @Override
    public void registerRecipeCatalysts(IRecipeCatalystRegistration registration) {
        registration.addCraftingStation(WOOD_WORKBENCH, ModBlocks.WOOD_WORKBENCH);
    }

    @Override
    public void registerRecipeTransferHandlers(IRecipeTransferRegistration registration) {
        registration.addRecipeTransferHandler(
                WoodWorkbenchScreenHandler.class,
                ModScreenHandlers.WOOD_WORKBENCH,
                WOOD_WORKBENCH,
                0,
                1,
                2,
                36
        );
    }

    private static List<RecipeHolder<WoodWorkbenchRecipe>> loadWorkbenchRecipes() {
        Optional<Path> root = FabricLoader.getInstance()
                .getModContainer(FramedFabric.MOD_ID)
                .flatMap(container -> container.findPath(RECIPE_ROOT));

        if (root.isEmpty()) {
            return List.of();
        }

        try (Stream<Path> paths = Files.walk(root.get())) {
            return paths
                    .filter(Files::isRegularFile)
                    .filter(path -> path.getFileName().toString().endsWith(".json"))
                    .map(path -> readWorkbenchRecipe(root.get(), path))
                    .flatMap(Optional::stream)
                    .sorted(Comparator.comparing(entry -> entry.id().identifier().toString()))
                    .toList();
        } catch (IOException e) {
            FramedFabric.LOGGER.warn("Failed to load Wood Workbench recipes for JEI", e);
            return List.of();
        }
    }

    private static Optional<RecipeHolder<WoodWorkbenchRecipe>> readWorkbenchRecipe(Path root, Path path) {
        try (Reader reader = Files.newBufferedReader(path)) {
            JsonObject json = JsonParser.parseReader(reader).getAsJsonObject();
            Identifier type = Identifier.tryParse(getString(json, "type"));
            if (!WOOD_WORKBENCH_RECIPE_TYPE.equals(type) || !loadConditionsPass(json)) {
                return Optional.empty();
            }

            Identifier ingredientId = Identifier.tryParse(getString(json, "ingredient"));
            JsonObject resultJson = getObject(json, "result");
            Identifier resultId = Identifier.tryParse(getString(resultJson, "id"));

            Optional<Item> inputItem = getItem(ingredientId);
            Optional<Item> resultItem = getItem(resultId);
            if (inputItem.isEmpty() || resultItem.isEmpty()) {
                return Optional.empty();
            }

            int inputCount = Math.max(1, getInt(json, "input_count", 1));
            int resultCount = Math.max(1, getInt(resultJson, "count", 1));

            Identifier recipeId = getRecipeId(root, path);
            ResourceKey<Recipe<?>> key = ResourceKey.create(Registries.RECIPE, recipeId);
            WoodWorkbenchRecipe recipe = new WoodWorkbenchRecipe(
                    Ingredient.of(inputItem.get()),
                    inputCount,
                    new ItemStack(resultItem.get(), resultCount)
            );

            return Optional.of(new RecipeHolder<>(key, recipe));
        } catch (RuntimeException | IOException e) {
            FramedFabric.LOGGER.warn("Skipping invalid Wood Workbench JEI recipe {}", path, e);
            return Optional.empty();
        }
    }

    private static boolean loadConditionsPass(JsonObject json) {
        JsonElement conditionsElement = json.get("fabric:load_conditions");
        if (conditionsElement == null || !conditionsElement.isJsonArray()) {
            return true;
        }

        for (JsonElement element : conditionsElement.getAsJsonArray()) {
            JsonObject condition = element.getAsJsonObject();
            String conditionType = getString(condition, "condition");
            if ("fabric:all_mods_loaded".equals(conditionType)) {
                JsonArray values = condition.getAsJsonArray("values");
                if (values == null) {
                    return false;
                }

                for (JsonElement value : values) {
                    if (!FabricLoader.getInstance().isModLoaded(value.getAsString())) {
                        return false;
                    }
                }
            } else if ("fabric:registry_contains".equals(conditionType)) {
                String registry = getString(condition, "registry");
                if (!registry.isEmpty() && !"minecraft:item".equals(registry)) {
                    return false;
                }

                JsonArray values = condition.getAsJsonArray("values");
                if (values == null) {
                    return false;
                }

                for (JsonElement value : values) {
                    Identifier id = Identifier.tryParse(value.getAsString());
                    if (getItem(id).isEmpty()) {
                        return false;
                    }
                }
            } else {
                return false;
            }
        }

        return true;
    }

    private static Optional<Item> getItem(Identifier id) {
        return id == null ? Optional.empty() : BuiltInRegistries.ITEM.getOptional(id);
    }

    private static Identifier getRecipeId(Path root, Path path) {
        String relativePath = root.relativize(path).toString().replace('\\', '/');
        if (relativePath.endsWith(".json")) {
            relativePath = relativePath.substring(0, relativePath.length() - ".json".length());
        }
        return Identifier.fromNamespaceAndPath(FramedFabric.MOD_ID, "wood_workbench/" + relativePath);
    }

    private static JsonObject getObject(JsonObject object, String key) {
        JsonElement element = object.get(key);
        return element != null && element.isJsonObject() ? element.getAsJsonObject() : new JsonObject();
    }

    private static String getString(JsonObject object, String key) {
        JsonElement element = object.get(key);
        return element != null && element.isJsonPrimitive() ? element.getAsString() : "";
    }

    private static int getInt(JsonObject object, String key, int fallback) {
        JsonElement element = object.get(key);
        return element != null && element.isJsonPrimitive() ? element.getAsInt() : fallback;
    }
}
