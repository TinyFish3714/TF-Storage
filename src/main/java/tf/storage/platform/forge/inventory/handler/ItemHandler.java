package tf.storage.platform.forge.inventory.handler;

import java.util.UUID;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.items.IItemHandler;
import tf.storage.platform.forge.item.TFUnitItem;
import tf.storage.platform.forge.ModRegistry;
import tf.storage.core.util.StackHelper;
import tf.storage.core.util.NBTHelper;

/**
 * Item handler that stores its contents in a container ItemStack's NBT.
 */
public class ItemHandler extends BasicHandler {
    protected ItemStack containerStack = ItemStack.EMPTY;
    protected boolean isRemote;
    protected UUID containerUUID;
    protected IItemHandler hostInventory;

    public ItemHandler(ItemStack containerStack, int invSize, int stackLimit, boolean allowCustomStackSizes,
            String tagName, Player player) {
        super(invSize, stackLimit, allowCustomStackSizes, tagName);

        this.containerStack = containerStack;
        this.isRemote = (player != null && player.level().isClientSide);
        this.containerUUID = null;
        this.hostInventory = null;
    }

    public ItemHandler(ItemStack containerStack, int invSize, int stackLimit, boolean allowCustomStackSizes,
            String tagName) {
        this(containerStack, invSize, stackLimit, allowCustomStackSizes, tagName, null);
    }

    public ItemHandler(ItemStack containerStack, int invSize, int stackLimit, boolean allowCustomStackSizes) {
        this(containerStack, invSize, stackLimit, allowCustomStackSizes, "Items", null);
    }

    /**
     * Returns the ItemStack storing the contents of this inventory
     */
    public ItemStack getContainerItemStack() {
        if (this.containerUUID != null && this.hostInventory != null) {
            ItemStack stack = StackHelper.getItemStackByUUID(this.hostInventory, this.containerUUID, "UUID");
            if (!stack.isEmpty()) {
                return stack;
            }
        }

        return this.containerStack;
    }

    /**
     * Sets the host inventory and the UUID of the container ItemStack, so that the correct
     * container ItemStack can be fetched from the host inventory.
     */
    public void setHostInventory(IItemHandler inv, UUID uuid) {
        this.hostInventory = inv;
        this.containerUUID = uuid;
    }

    /**
     * Sets the ItemStack that stores the contents of this inventory.
     * NOTE: You MUST set it to null when the inventory is invalid/not accessible
     * ie. when the container ItemStack reference isn't valid anymore!!
     */
    public void setContainerItemStack(ItemStack stack) {
        this.containerStack = stack;
        this.readFromContainerItemStack();
    }

    /**
     * Read the inventory contents from the container ItemStack
     */
    public void readFromContainerItemStack() {
        // Only read the contents on the server side, they get synced to the client via the open Container
        if (!this.isRemote) {
            this.items.clear();

            ItemStack stack = this.getContainerItemStack();

            if (!stack.isEmpty() && stack.hasTag() && this.isCurrentlyAccessible()) {
                this.deserializeNBT(stack.getTag());
            }
        }
    }

    /**
     * Writes the inventory contents to the container ItemStack
     */
    protected void writeToContainerItemStack() {
        if (!this.isRemote) {
            ItemStack stack = this.getContainerItemStack();

            if (!stack.isEmpty() && this.isCurrentlyAccessible()) {
                NBTHelper.writeItemsToContainerItem(stack, this.items, this.getItemStorageTagName());
            }
        }
    }

    public boolean isCurrentlyAccessible() {
        return !this.getContainerItemStack().isEmpty();
    }

    public boolean isAccessibleBy(Entity entity) {
        return this.isCurrentlyAccessible();
    }

    @Override
    public int getInventoryStackLimit() {
        return this.getInventoryStackLimitFromContainerStack(this.getContainerItemStack());
    }

    @Override
    public ItemStack getStackInSlot(int slot) {
        if (!this.isCurrentlyAccessible()) {
            return ItemStack.EMPTY;
        }

        return super.getStackInSlot(slot);
    }

    public int getInventoryStackLimitFromContainerStack(ItemStack stack) {
        if (!stack.isEmpty() && stack.getItem() instanceof TFUnitItem) {
            int tier = ((TFUnitItem) stack.getItem()).getTFUnitTier(stack);

            if (tier >= 6 && tier <= 12) {
                return (int) Math.pow(2, tier);
            }
        }

        return super.getInventoryStackLimit();
    }

    @Override
    public boolean isItemValidForSlot(int slot, ItemStack stack) {
        return this.isCurrentlyAccessible();
    }

    @Override
    public boolean canExtractFromSlot(int slot) {
        return this.isCurrentlyAccessible();
    }

    @Override
    public void onContentsChanged(int slot) {
        super.onContentsChanged(slot);

        if (!this.isRemote) {
            this.writeToContainerItemStack();
        }
    }
}
