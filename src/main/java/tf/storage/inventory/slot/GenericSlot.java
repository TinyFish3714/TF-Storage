package tf.storage.inventory.slot;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraftforge.items.IItemHandler;
import net.minecraftforge.items.IItemHandlerModifiable;
import net.minecraftforge.items.SlotItemHandler;
import tf.storage.TFStorage;
import tf.storage.inventory.IItemHandlerSelective;
import tf.storage.inventory.IItemHandlerSize;
import tf.storage.inventory.IItemHandlerSyncable;

public class GenericSlot extends SlotItemHandler
{
    public GenericSlot(IItemHandler itemHandler, int index, int xPosition, int yPosition)
    {
        super(itemHandler, index, xPosition, yPosition);
    }

    @Override
    public int getSlotStackLimit()
    {
        if (this.getItemHandler() instanceof IItemHandlerSize)
        {
            return ((IItemHandlerSize) this.getItemHandler()).getInventoryStackLimit();
        }

        return super.getSlotStackLimit();
    }

    @Override
    public int getItemStackLimit(ItemStack stack)
    {
        if (stack.isEmpty() == false && this.getItemHandler() instanceof IItemHandlerSize)
        {
            return ((IItemHandlerSize) this.getItemHandler()).getItemStackLimit(this.getSlotIndex(), stack);
        }

        return this.getSlotStackLimit();
    }

    @Override
    public ItemStack getStack()
    {
        return this.getItemHandler().getStackInSlot(this.getSlotIndex());
    }

    @Override
    public void putStack(ItemStack stack)
    {
        ((IItemHandlerModifiable) this.getItemHandler()).setStackInSlot(this.getSlotIndex(), stack);
        this.onSlotChanged();
    }

    public void syncStack(ItemStack stack)
    {
        if (this.getItemHandler() instanceof IItemHandlerSyncable)
        {
            ((IItemHandlerSyncable) this.getItemHandler()).syncStackInSlot(this.getSlotIndex(), stack);
        }
        else
        {
            this.putStack(stack);
        }
    }

    public ItemStack insertItem(ItemStack stack, boolean simulate)
    {
        return this.getItemHandler().insertItem(this.getSlotIndex(), stack, simulate);
    }

    @Override
    public ItemStack decrStackSize(int amount)
    {
        return this.getItemHandler().extractItem(this.getSlotIndex(), amount, false);
    }

    /**
     * Returns true if the item would be valid for an empty slot.
     */
    @Override
    public boolean isItemValid(ItemStack stack)
    {
        if (this.getItemHandler() instanceof IItemHandlerSelective)
        {
            return ((IItemHandlerSelective) this.getItemHandler()).isItemValidForSlot(this.getSlotIndex(), stack);
        }

        return true; // super.isItemValid(stack);
    }

    @Override
    public boolean canTakeStack(EntityPlayer player)
    {
        if (this.getItemHandler() instanceof IItemHandlerSelective)
        {
            return ((IItemHandlerSelective) this.getItemHandler()).canExtractFromSlot(this.getSlotIndex());
        }

        return true;
    }

}
