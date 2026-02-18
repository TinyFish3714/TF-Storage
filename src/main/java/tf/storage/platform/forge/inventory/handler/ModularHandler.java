package tf.storage.platform.forge.inventory.handler;

import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.common.capabilities.ForgeCapabilities;
import net.minecraftforge.items.IItemHandler;
import tf.storage.platform.forge.item.TFBagItem;
import tf.storage.core.util.StackHelper;
import tf.storage.core.util.NBTHelper;
import tf.storage.core.util.CardHelper;
import tf.storage.platform.forge.compat.CuriosCompat;

/**
 * ModularHandler is a specialized inventory class for TFBag.
 * This class extends ItemHandler and specifically handles memory card inventory management.
 *
 * <p>This class is designed to manage memory card functionality, providing clear and concise
 * memory card management features including card installation, selection, and switching.</p>
 */
public class ModularHandler extends ItemHandler {
    protected ItemStack modularItemStack = ItemStack.EMPTY;
    protected ItemHandler memoryCardInventory;

    public int cachedSelectedIndex = -1;

    public ModularHandler(ItemStack containerStack, Player player, boolean allowCustomStackSizes) {
        this(containerStack, player, ((TFBagItem) containerStack.getItem()).getSizeInventory(containerStack),
              allowCustomStackSizes, ((TFBagItem) containerStack.getItem()).getMaxMemoryCards(containerStack));
    }

    public ModularHandler(ItemStack containerStack, Player player, int mainInvSize,
                          boolean allowCustomStackSizes, int memoryCardInvSize) {
        super(containerStack, mainInvSize, 64, allowCustomStackSizes, "Items", player);

        this.modularItemStack = containerStack;
        this.containerUUID = NBTHelper.getUUIDFromItemStack(containerStack, "UUID", true);
        
        IItemHandler hostInv = player != null ?
                player.getCapability(ForgeCapabilities.ITEM_HANDLER).orElse(null) : null;
        
        if (this.containerUUID != null) {
            boolean hostSet = false;
            if (hostInv != null && !StackHelper.getItemStackByUUID(hostInv, this.containerUUID, "UUID").isEmpty()) {
                this.setHostInventory(hostInv, this.containerUUID);
                hostSet = true;
            }
            if (!hostSet && player != null) {
                IItemHandler curiosInv = CuriosCompat.getEquippedCurios(player);
                if (curiosInv != null && !StackHelper.getItemStackByUUID(curiosInv, this.containerUUID, "UUID").isEmpty()) {
                    this.setHostInventory(curiosInv, this.containerUUID);
                }
            }
        }

        this.memoryCardInventory = new ItemHandler(containerStack, memoryCardInvSize, 1, false, "MemoryCards") {
            @Override
            public boolean isItemValidForSlot(int slotNum, ItemStack stack) {
                if (!super.isItemValidForSlot(slotNum, stack) || stack.isEmpty()) {
                    return false;
                }
                return CardHelper.isTFUnit(stack);
            }
        };
        
        if (this.hostInventory != null && this.containerUUID != null) {
            this.memoryCardInventory.setHostInventory(this.hostInventory, this.containerUUID);
        }
        this.memoryCardInventory.readFromContainerItemStack();

        this.readFromContainerItemStack();
    }

    public ItemHandler getMemoryCardInventory() {
        return this.memoryCardInventory;
    }

    public ItemStack getModularItemStack() {
        if (this.containerUUID != null && this.hostInventory != null) {
            ItemStack stack = StackHelper.getItemStackByUUID(this.hostInventory, this.containerUUID, "UUID");
            if (!stack.isEmpty()) {
                return stack;
            }
        }

        return this.modularItemStack;
    }

    public void setModularItemStack(ItemStack stack) {
        this.modularItemStack = stack;
    }

    public int getSelectedMemoryCardIndex() {
        return this.cachedSelectedIndex;
    }

    public ItemStack getSelectedMemoryCardStack() {
        int index = this.getSelectedMemoryCardIndex();
        return index >= 0 && index < this.memoryCardInventory.getSlots() ?
               this.memoryCardInventory.getStackInSlot(index) : ItemStack.EMPTY;
    }

    public void readFromSelectedMemoryCardStack() {
        if (this.getSelectedMemoryCardStack().isEmpty()) {
            for (int i = 0; i < this.getSlots(); i++) {
                this.setStackInSlot(i, ItemStack.EMPTY);
            }
            return;
        }

        super.readFromContainerItemStack();
    }

    @Override
    public void readFromContainerItemStack() {
        if (this.getModularItemStack().isEmpty()) {
            this.cachedSelectedIndex = -1;
        } else {
            this.cachedSelectedIndex = CardHelper.getStoredMemoryCardSelection(
                this.getModularItemStack(),
                this.memoryCardInventory.getSlots()
            );
        }

        this.readFromSelectedMemoryCardStack();
    }

    @Override
    protected void writeToContainerItemStack() {
        super.writeToContainerItemStack();
        this.memoryCardInventory.writeToContainerItemStack();
    }

    public void writeDataToContainerItemStack() {
        this.writeToContainerItemStack();
    }

    @Override
    public ItemStack getContainerItemStack() {
        return this.getSelectedMemoryCardStack();
    }

    public boolean isSelectedMemoryCardValid() {
        ItemStack selectedCard = this.getSelectedMemoryCardStack();
        return !selectedCard.isEmpty();
    }

    @Override
    public int getInventoryStackLimit() {
        return this.getInventoryStackLimitFromContainerStack(this.getSelectedMemoryCardStack());
    }

    @Override
    public boolean isItemValidForSlot(int slotNum, ItemStack stack) {
        if (stack.isEmpty()) {
            return false;
        }

        ItemStack modularStack = this.getModularItemStack();

        if (!modularStack.isEmpty() && modularStack.getItem() == stack.getItem()) {
            return false;
        }

        return super.isItemValidForSlot(slotNum, stack);
    }

    public boolean isAccessibleBy(Player entity) {
        return !this.getModularItemStack().isEmpty() && !this.getSelectedMemoryCardStack().isEmpty();
    }

    public void markDirty() {
        this.writeDataToContainerItemStack();
        this.memoryCardInventory.writeToContainerItemStack();
    }
}
