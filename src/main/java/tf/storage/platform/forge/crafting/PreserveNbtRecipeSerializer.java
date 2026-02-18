package tf.storage.platform.forge.crafting;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonSyntaxException;
import java.util.HashMap;
import java.util.Map;
import net.minecraft.core.NonNullList;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.GsonHelper;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.CraftingBookCategory;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.item.crafting.ShapedRecipe;
import net.minecraft.world.item.crafting.Ingredient;
import javax.annotation.Nullable;

public class PreserveNbtRecipeSerializer implements RecipeSerializer<PreserveNbtRecipe> {

    @Override
    public PreserveNbtRecipe fromJson(ResourceLocation id, JsonObject json) {
        String group = GsonHelper.getAsString(json, "group", "");
        CraftingBookCategory category = CraftingBookCategory.CODEC.byName(GsonHelper.getAsString(json, "category", "misc"), CraftingBookCategory.MISC);

        Map<Character, Ingredient> key = deserializeKey(GsonHelper.getAsJsonObject(json, "key"));
        String[] pattern = patternFromJson(GsonHelper.getAsJsonArray(json, "pattern"));

        int width = pattern[0].length();
        int height = pattern.length;
        NonNullList<Ingredient> ingredients = NonNullList.withSize(width * height, Ingredient.EMPTY);

        for (int row = 0; row < height; ++row) {
            for (int col = 0; col < width; ++col) {
                char symbol = pattern[row].charAt(col);
                Ingredient ingredient = key.get(symbol);
                if (ingredient == null) {
                    throw new JsonSyntaxException("Pattern references symbol '" + symbol + "' but it's not defined in the key");
                }
                ingredients.set(col + row * width, ingredient);
            }
        }

        ItemStack result = ShapedRecipe.itemStackFromJson(GsonHelper.getAsJsonObject(json, "result"));
        return new PreserveNbtRecipe(id, group, category, width, height, ingredients, result);
    }

    @Nullable
    @Override
    public PreserveNbtRecipe fromNetwork(ResourceLocation id, FriendlyByteBuf buffer) {
        String group = buffer.readUtf();
        CraftingBookCategory category = buffer.readEnum(CraftingBookCategory.class);
        int width = buffer.readVarInt();
        int height = buffer.readVarInt();
        NonNullList<Ingredient> ingredients = NonNullList.withSize(width * height, Ingredient.EMPTY);

        for (int i = 0; i < ingredients.size(); ++i) {
            ingredients.set(i, Ingredient.fromNetwork(buffer));
        }

        ItemStack result = buffer.readItem();
        return new PreserveNbtRecipe(id, group, category, width, height, ingredients, result);
    }

    @Override
    public void toNetwork(FriendlyByteBuf buffer, PreserveNbtRecipe recipe) {
        buffer.writeUtf(recipe.getGroup());
        buffer.writeEnum(recipe.category());
        buffer.writeVarInt(recipe.getWidth());
        buffer.writeVarInt(recipe.getHeight());

        for (Ingredient ingredient : recipe.getIngredients()) {
            ingredient.toNetwork(buffer);
        }

        buffer.writeItem(recipe.getResultItem(null));
    }

    private static Map<Character, Ingredient> deserializeKey(JsonObject json) {
        Map<Character, Ingredient> map = new HashMap<>();

        for (Map.Entry<String, JsonElement> entry : json.entrySet()) {
            String key = entry.getKey();
            if (key.length() != 1) {
                throw new JsonSyntaxException("Invalid key entry: '" + key + "' is an invalid symbol (must be 1 character only).");
            }
            if (" ".equals(key)) {
                throw new JsonSyntaxException("Invalid key entry: ' ' is a reserved symbol.");
            }
            map.put(key.charAt(0), Ingredient.fromJson(entry.getValue(), false));
        }

        map.put(' ', Ingredient.EMPTY);
        return map;
    }

    private static String[] patternFromJson(JsonArray patternArray) {
        if (patternArray.size() == 0) {
            throw new JsonSyntaxException("Invalid pattern: empty pattern not allowed");
        }

        String[] pattern = new String[patternArray.size()];
        for (int i = 0; i < pattern.length; ++i) {
            String line = GsonHelper.convertToString(patternArray.get(i), "pattern[" + i + "]");
            if (i > 0 && pattern[0].length() != line.length()) {
                throw new JsonSyntaxException("Invalid pattern: each row must be the same width");
            }
            pattern[i] = line;
        }

        return pattern;
    }
}
