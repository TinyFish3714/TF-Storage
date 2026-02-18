package tf.storage.core.util;

import net.minecraft.world.item.ItemStack;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.registries.ForgeRegistries;

/**
 * ItemType - 物品类型封装类
 * 用于物品排序和合并时的类型比较
 * 
 * 适配 Minecraft 1.20.1
 */
public class ItemType
{
    private final ItemStack stack;
    private final boolean checkNBT;
    private final int cachedHash;

    public ItemType(ItemStack stack)
    {
        this(stack, true);
    }

    public ItemType(ItemStack stack, boolean checkNBT)
    {
        this.stack = stack.isEmpty() ? ItemStack.EMPTY : stack.copy();
        
        if (!this.stack.isEmpty()) {
            this.stack.setCount(1);
        }
        
        this.checkNBT = checkNBT;
        this.cachedHash = calculateHashCode();
    }

    private int calculateHashCode()
    {
        final int prime = 31;
        int result = 1;
        
        if (!this.stack.isEmpty()) {
            ResourceLocation rl = ForgeRegistries.ITEMS.getKey(this.stack.getItem());
            result = prime * result + (rl != null ? rl.hashCode() : 0);
        }
        
        return result;
    }

    public ItemStack getStack()
    {
        return this.stack;
    }

    public boolean checkNBT()
    {
        return this.checkNBT;
    }

    @Override
    public int hashCode()
    {
        return this.cachedHash;
    }

    @Override
    public boolean equals(Object obj)
    {
        if (this == obj) return true;
        if (obj == null) return false;
        if (getClass() != obj.getClass()) return false;
        
        ItemType other = (ItemType) obj;
        
        if (this.cachedHash != other.cachedHash) return false;
        
        if (this.stack.isEmpty() || other.stack.isEmpty())
        {
            return this.stack.isEmpty() == other.stack.isEmpty();
        }

        if (this.stack.getItem() != other.stack.getItem()) return false;

        if (this.checkNBT())
        {
            return ItemStack.isSameItemSameTags(this.stack, other.stack);
        }

        return true;
    }

    @Override
    public String toString()
    {
        if (this.stack.isEmpty()) return "ItemType{EMPTY}";
        ResourceLocation rl = ForgeRegistries.ITEMS.getKey(this.stack.getItem());
        return rl != null ? rl.toString() : "unknown";
    }
}