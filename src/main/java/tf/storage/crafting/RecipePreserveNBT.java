package tf.storage.crafting;

import net.minecraft.inventory.InventoryCrafting;
import net.minecraft.item.ItemStack;
import net.minecraft.item.crafting.Ingredient;
import net.minecraft.item.crafting.ShapedRecipes;
import net.minecraft.util.NonNullList;

public class RecipePreserveNBT extends ShapedRecipes {

    public RecipePreserveNBT(String group, int width, int height, NonNullList<Ingredient> ingredients, ItemStack result) {
        super(group, width, height, ingredients, result);
    }

    @Override
    public ItemStack getCraftingResult(InventoryCrafting inv) {
        ItemStack result = super.getCraftingResult(inv); // 获取标准结果（副本）

        for (int i = 0; i < inv.getSizeInventory(); i++) {
            ItemStack stack = inv.getStackInSlot(i);
            if (!stack.isEmpty() && stack.getItem() == result.getItem()) {
                // 找到上一级物品
                if (stack.hasTagCompound()) {
                    result.setTagCompound(stack.getTagCompound().copy());
                }
                break;
            }
        }
        
        return result;
    }
}
