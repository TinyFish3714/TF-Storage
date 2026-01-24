package tf.storage.crafting;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonSyntaxException;
import java.util.Map;
import java.util.HashMap;
import net.minecraft.item.ItemStack;
import net.minecraft.item.crafting.IRecipe;
import net.minecraft.item.crafting.Ingredient;
import net.minecraft.util.JsonUtils;
import net.minecraft.util.NonNullList;
import net.minecraftforge.common.crafting.CraftingHelper;
import net.minecraftforge.common.crafting.IRecipeFactory;
import net.minecraftforge.common.crafting.JsonContext;

public class RecipePreserveNBTFactory implements IRecipeFactory {
    @Override
    public IRecipe parse(JsonContext context, JsonObject json) {
        String group = JsonUtils.getString(json, "group", "");
        
        // Manual parsing to avoid mysterious "Invalid shaped recipe" errors from CraftingHelper.parseShaped
        
        // 1. Parse Keys
        Map<Character, Ingredient> ingMap = new HashMap<>();
        JsonObject keys = JsonUtils.getJsonObject(json, "key");
        
        for (Map.Entry<String, JsonElement> entry : keys.entrySet()) {
            if (entry.getKey().length() != 1)
                throw new JsonSyntaxException("Invalid key entry: '" + entry.getKey() + "' is an invalid symbol (must be 1 character only).");
            if (" ".equals(entry.getKey()))
                throw new JsonSyntaxException("Invalid key entry: ' ' is a reserved symbol.");

            ingMap.put(entry.getKey().charAt(0), CraftingHelper.getIngredient(entry.getValue(), context));
        }
        
        ingMap.put(' ', Ingredient.EMPTY);

        // 2. Parse Pattern
        JsonArray patternJ = JsonUtils.getJsonArray(json, "pattern");
        if (patternJ.size() == 0)
            throw new JsonSyntaxException("Invalid shaped recipe: " + json);

        String[] pattern = new String[patternJ.size()];
        for (int x = 0; x < pattern.length; ++x) {
            String line = JsonUtils.getString(patternJ.get(x), "pattern[" + x + "]");
            if (x > 0 && pattern[0].length() != line.length())
                throw new JsonSyntaxException("Invalid pattern: each row must be the same width");
            pattern[x] = line;
        }

        int width = pattern[0].length();
        int height = pattern.length;
        
        // 3. Convert Pattern to Ingredients
        NonNullList<Ingredient> input = NonNullList.withSize(width * height, Ingredient.EMPTY);
        for (int r = 0; r < height; ++r) {
            for (int c = 0; c < width; ++c) {
                char key = pattern[r].charAt(c);
                Ingredient ingredient = ingMap.get(key);
                if (ingredient == null)
                    throw new JsonSyntaxException("Pattern references symbol '" + key + "' but it's not defined in the key");
                input.set(r * width + c, ingredient);
            }
        }

        // 4. Parse Result
        ItemStack result = CraftingHelper.getItemStack(JsonUtils.getJsonObject(json, "result"), context);
        
        return new RecipePreserveNBT(group, width, height, input, result);
    }
}
