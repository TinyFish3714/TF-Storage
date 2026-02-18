package tf.storage.core.inventory;

import net.minecraft.world.item.ItemStack;

/**
 * Interface for item handlers that support syncing stacks from server to client.
 */
public interface IItemHandlerSyncable {
    /**
     * Used to sync an ItemStack into a slot, even if the stack normally
     * wouldn't be allowed in that slot.
     * @param slot The slot index
     * @param stack The item stack to sync
     */
    void syncStackInSlot(int slot, ItemStack stack);
}
