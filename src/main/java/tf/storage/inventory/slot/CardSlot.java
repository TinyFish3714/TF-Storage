package tf.storage.inventory.slot;

import net.minecraft.item.ItemStack;
import net.minecraftforge.items.IItemHandler;
import tf.storage.item.TFUnit;

/**
 * A slot specifically for memory cards
 * This class simplifies slot logic, specifically handling memory card related validation and restrictions
 */
public class CardSlot extends GenericSlot
{
    /**
     * Constructor, creates a memory card specific slot
     *
     * @param inventory The item handler
     * @param slot The slot index
     * @param posX The slot X coordinate
     * @param posY The slot Y coordinate
     */
    public CardSlot(IItemHandler inventory, int slot, int posX, int posY)
    {
        super(inventory, slot, posX, posY);
    }

    /**
     * Check if the given item stack can be placed in this slot
     * Only TFUnit type items can be placed in this slot
     *
     * @param stack The item stack to check
     * @return true if the item is of TFUnit type, false otherwise
     */
    @Override
    public boolean isItemValid(ItemStack stack)
    {
        if (stack.isEmpty())
        {
            return false;
        }

        // Only allow TFUnit type items
        return stack.getItem() instanceof TFUnit;
    }

    /**
     * Get the maximum stack limit for this slot
     * Each slot can only hold one memory card
     * 
     * @return Always returns 1
     */
    @Override
    public int getSlotStackLimit()
    {
        return 1;
    }

    /**
     * Get the maximum stack limit for the specified item stack in this slot
     * Memory card slots only allow placing one memory card
     *
     * @param stack The item stack
     * @return Always returns 1
     */
    @Override
    public int getItemStackLimit(ItemStack stack)
    {
        return 1;
    }

}
