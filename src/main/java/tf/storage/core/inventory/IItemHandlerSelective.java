package tf.storage.core.inventory;

import net.minecraft.world.item.ItemStack;

/**
 * Interface for item handlers that have selective slot validation.
 */
public interface IItemHandlerSelective {
    /**
     * Checks if an item is valid for a specific slot.
     * @param slot The slot index
     * @param stack The item stack to check
     * @return true if the item can be placed in the slot
     */
    boolean isItemValidForSlot(int slot, ItemStack stack);

    /**
     * Checks if items can be extracted from a specific slot.
     * @param slot The slot index
     * @return true if items can be extracted
     */
    boolean canExtractFromSlot(int slot);
}
