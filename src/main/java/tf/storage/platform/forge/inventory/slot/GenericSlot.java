package tf.storage.platform.forge.inventory.slot;

import javax.annotation.Nonnull;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.items.IItemHandler;
import net.minecraftforge.items.IItemHandlerModifiable;
import net.minecraftforge.items.SlotItemHandler;
import tf.storage.core.inventory.IItemHandlerSelective;
import tf.storage.core.inventory.IItemHandlerSize;
import tf.storage.core.inventory.IItemHandlerSyncable;

/**
 * Generic slot implementation with support for custom stack sizes and selective validation.
 */
public class GenericSlot extends SlotItemHandler {
    
    public GenericSlot(IItemHandler itemHandler, int index, int xPosition, int yPosition) {
        super(itemHandler, index, xPosition, yPosition);
    }


    @Override
    public int getMaxStackSize() {
        if (this.getItemHandler() instanceof IItemHandlerSize) {
            return ((IItemHandlerSize) this.getItemHandler()).getInventoryStackLimit();
        }

        return super.getMaxStackSize();
    }

    @Override
    public int getMaxStackSize(@Nonnull ItemStack stack) {
        if (!stack.isEmpty() && this.getItemHandler() instanceof IItemHandlerSize) {
            return ((IItemHandlerSize) this.getItemHandler()).getItemStackLimit(this.getSlotIndex(), stack);
        }

        return this.getMaxStackSize();
    }

    @Override
    @Nonnull
    public ItemStack getItem() {
        return this.getItemHandler().getStackInSlot(this.getSlotIndex());
    }

    @Override
    public void set(@Nonnull ItemStack stack) {
        ((IItemHandlerModifiable) this.getItemHandler()).setStackInSlot(this.getSlotIndex(), stack);
        this.setChanged();
    }

    /**
     * Sync a stack into this slot, even if it wouldn't normally be allowed.
     * Used for client-server synchronization.
     */
    public void syncStack(ItemStack stack) {
        if (this.getItemHandler() instanceof IItemHandlerSyncable) {
            ((IItemHandlerSyncable) this.getItemHandler()).syncStackInSlot(this.getSlotIndex(), stack);
        } else {
            this.set(stack);
        }
    }

    /**
     * Insert an item into this slot.
     */
    public ItemStack insertItem(ItemStack stack, boolean simulate) {
        return this.getItemHandler().insertItem(this.getSlotIndex(), stack, simulate);
    }

    @Override
    @Nonnull
    public ItemStack remove(int amount) {
        return this.getItemHandler().extractItem(this.getSlotIndex(), amount, false);
    }

    /**
     * Returns true if the item would be valid for an empty slot.
     */
    @Override
    public boolean mayPlace(@Nonnull ItemStack stack) {
        if (this.getItemHandler() instanceof IItemHandlerSelective) {
            return ((IItemHandlerSelective) this.getItemHandler()).isItemValidForSlot(this.getSlotIndex(), stack);
        }

        return true;
    }

    @Override
    public boolean mayPickup(Player player) {
        if (this.getItemHandler() instanceof IItemHandlerSelective) {
            return ((IItemHandlerSelective) this.getItemHandler()).canExtractFromSlot(this.getSlotIndex());
        }

        return true;
    }
}
