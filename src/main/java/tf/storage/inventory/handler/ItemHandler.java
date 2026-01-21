package tf.storage.inventory.handler;

import java.util.UUID;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraftforge.items.IItemHandler;
import tf.storage.item.TFUnit;
import tf.storage.core.ModItems;
import tf.storage.util.StackHelper;
import tf.storage.util.NBTHelper;

public class ItemHandler extends BasicHandler
{
    protected ItemStack containerStack = ItemStack.EMPTY;
    protected boolean isRemote;

    public ItemHandler(ItemStack containerStack, int invSize, int stackLimit, boolean allowCustomStackSizes,
            String tagName, EntityPlayer player)
    {
        super(invSize, stackLimit, allowCustomStackSizes, tagName);

        this.containerStack = containerStack;
        this.isRemote = (player != null && player.getEntityWorld().isRemote);
    }

    public ItemHandler(ItemStack containerStack, int invSize, int stackLimit, boolean allowCustomStackSizes,
            String tagName)
    {
        this(containerStack, invSize, stackLimit, allowCustomStackSizes, tagName, null);
    }

    public ItemHandler(ItemStack containerStack, int invSize, int stackLimit, boolean allowCustomStackSizes)
    {
        this(containerStack, invSize, stackLimit, allowCustomStackSizes, "Items", null);
    }




    /**
     * Returns the ItemStack storing the contents of this inventory
     */
    public ItemStack getContainerItemStack()
    {
        return this.containerStack;
    }

    /**
     * Sets the ItemStack that stores the contents of this inventory.
     * NOTE: You MUST set it to null when the inventory is invalid/not accessible
     * ie. when the container ItemStack reference isn't valid anymore!!
     */
    public void setContainerItemStack(ItemStack stack)
    {
        this.containerStack = stack;
        this.readFromContainerItemStack();
    }

    /**
     * Read the inventory contents from the container ItemStack
     */
    public void readFromContainerItemStack()
    {
        // Only read the contents on the server side, they get synced to the client via the open Container
        if (this.isRemote == false)
        {
            this.items.clear();

            ItemStack stack = this.getContainerItemStack();

            if (stack.isEmpty() == false && stack.hasTagCompound() && this.isCurrentlyAccessible())
            {
                this.deserializeNBT(stack.getTagCompound());
            }
        }
    }

    /**
     * Writes the inventory contents to the container ItemStack
     */
    protected void writeToContainerItemStack()
    {
        if (this.isRemote == false)
        {
            ItemStack stack = this.getContainerItemStack();

            if (stack.isEmpty() == false && this.isCurrentlyAccessible())
            {
                NBTHelper.writeItemsToContainerItem(stack, this.items, this.getItemStorageTagName());
            }

        }
    }

    public boolean isCurrentlyAccessible()
    {
        return this.getContainerItemStack().isEmpty() == false;
    }

    @Override
    public int getInventoryStackLimit()
    {
        return this.getInventoryStackLimitFromContainerStack(this.getContainerItemStack());
    }

    public int getInventoryStackLimitFromContainerStack(ItemStack stack)
    {
        if (stack.isEmpty() == false && stack.getItem() == ModItems.MEMORY_CARD)
        {
            int tier = ((TFUnit) stack.getItem()).getTFUnitTier(stack);

            if (tier >= 6 && tier <= 12)
            {
                return (int)Math.pow(2, tier);
            }
        }

        return super.getInventoryStackLimit();
    }

    @Override
    public boolean isItemValidForSlot(int slot, ItemStack stack)
    {
        return this.getContainerItemStack().isEmpty() == false && this.isCurrentlyAccessible();
    }

    @Override
    public void onContentsChanged(int slot)
    {
        super.onContentsChanged(slot);

        if (this.isRemote == false)
        {
            this.writeToContainerItemStack();
        }
    }
}
