package tf.storage.core.inventory;

import net.minecraft.world.item.ItemStack;
import net.minecraftforge.items.IItemHandler;

/**
 * Interface for item handlers that support custom stack size limits.
 */
public interface IItemHandlerSize extends IItemHandler {
    /**
     * Gets the maximum stack size for this inventory.
     * @return The inventory-wide stack limit
     */
    int getInventoryStackLimit();

    /**
     * Gets the maximum stack size for a specific item in a specific slot.
     * @param slot The slot index
     * @param stack The item stack
     * @return The maximum stack size for this item in this slot
     */
    int getItemStackLimit(int slot, ItemStack stack);
}
