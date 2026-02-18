package tf.storage.platform.forge.crafting;

import net.minecraft.core.NonNullList;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.inventory.CraftingContainer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.CraftingBookCategory;
import net.minecraft.world.item.crafting.CraftingRecipe;
import net.minecraft.world.item.crafting.ShapedRecipe;
import net.minecraft.world.level.Level;
import tf.storage.platform.forge.item.TFUnitItem;

public class PreserveNbtRecipe extends ShapedRecipe implements CraftingRecipe {

    public PreserveNbtRecipe(ResourceLocation id, String group, CraftingBookCategory category, int width, int height,
                             NonNullList<net.minecraft.world.item.crafting.Ingredient> ingredients, ItemStack result) {
        super(id, group, category, width, height, ingredients, result);
    }

    @Override
    public ItemStack assemble(CraftingContainer container, net.minecraft.core.RegistryAccess registryAccess) {
        ItemStack result = super.assemble(container, registryAccess);
        if (result.isEmpty()) {
            return result;
        }

        for (int i = 0; i < container.getContainerSize(); i++) {
            ItemStack stack = container.getItem(i);
            // 检查输入物品和输出物品是否都是TFUnitItem类型，并且输入物品有NBT数据
            // 这样可以支持TFUnitItem在不同等级之间升级时继承NBT数据
            if (!stack.isEmpty() && stack.hasTag() && 
                stack.getItem() instanceof TFUnitItem && result.getItem() instanceof TFUnitItem) {
                result.setTag(stack.getTag().copy());
                break;
            }
        }
        return result;
    }

    @Override
    public boolean matches(CraftingContainer container, Level level) {
        return super.matches(container, level);
    }
}
