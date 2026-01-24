package tf.storage.util;

import net.minecraft.item.ItemStack;

/**
 * [Performance Optimized] ItemType
 * Removed NBT.toString() hash calculation for improved sorting and merging performance
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
        
        result = prime * result + (this.stack.isEmpty() ? 0 : this.stack.getItem().getRegistryName().hashCode());
        result = prime * result + (this.stack.isEmpty() ? 0 : this.stack.getMetadata());
        
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
        if (this.stack.getMetadata() != other.stack.getMetadata()) return false;

        if (this.checkNBT())
        {
            return ItemStack.areItemStackTagsEqual(this.stack, other.stack);
        }

        return true;
    }

    @Override
    public String toString()
    {
        if (this.stack.isEmpty()) return "ItemType{EMPTY}";
        return this.stack.getItem().getRegistryName() + "@" + this.stack.getMetadata();
    }
}
