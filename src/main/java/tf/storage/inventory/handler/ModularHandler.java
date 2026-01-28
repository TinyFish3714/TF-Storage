package tf.storage.inventory.handler;

import net.minecraft.entity.Entity;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraftforge.items.CapabilityItemHandler;
import net.minecraftforge.items.IItemHandler;
import tf.storage.item.TFBag;
import tf.storage.item.TFUnit;
import tf.storage.util.StackHelper;
import tf.storage.util.NBTHelper;
import tf.storage.util.CardHelper;

    /**
     * ModularHandler 是一个专用的库存类
     * 该类继承自 ItemHandler，专门处理存储卡库存管理
     *
     * <p>此类设计用于专门管理存储卡功能，提供清晰简洁的存储卡管理功能，
     * 包括存储卡安装、选择和切换等核心功能。</p>
     *
     * @author TinyFish3714
     * @version 1.0
     */
    public class ModularHandler extends ItemHandler
    {
        protected ItemStack modularItemStack = ItemStack.EMPTY;
        protected ItemHandler memoryCardInventory;
        
        public int cachedSelectedIndex = -1;
        
        public ModularHandler(ItemStack containerStack, EntityPlayer player, boolean allowCustomStackSizes)
        {
            this(containerStack, player, ((TFBag) containerStack.getItem()).getSizeInventory(containerStack),
                  allowCustomStackSizes, ((TFBag) containerStack.getItem()).getMaxMemoryCards(containerStack));
        }
        
    public ModularHandler(ItemStack containerStack, EntityPlayer player, int mainInvSize,
                                       boolean allowCustomStackSizes, int memoryCardInvSize)
    {
        super(containerStack, mainInvSize, 64, allowCustomStackSizes, "Items", player);

        this.modularItemStack = containerStack;
        this.containerUUID = NBTHelper.getUUIDFromItemStack(containerStack, "UUID", true);
        IItemHandler hostInv = player != null ?
                player.getCapability(CapabilityItemHandler.ITEM_HANDLER_CAPABILITY, null) : null;
        if (hostInv != null && this.containerUUID != null)
        {
            this.setHostInventory(hostInv, this.containerUUID);
        }
            
            this.memoryCardInventory = new ItemHandler(containerStack, memoryCardInvSize, 1, false, "MemoryCards") {
                @Override
                public boolean isItemValidForSlot(int slotNum, ItemStack stack) {
                    if (super.isItemValidForSlot(slotNum, stack) == false || stack.isEmpty()) {
                        return false;
                    }
                    return CardHelper.isTFUnit(stack);
                }
            };
        if (hostInv != null && this.containerUUID != null)
        {
            this.memoryCardInventory.setHostInventory(hostInv, this.containerUUID);
        }
        this.memoryCardInventory.readFromContainerItemStack();
            
            this.readFromContainerItemStack();
        }
        
        public ItemHandler getMemoryCardInventory()
        {
            return this.memoryCardInventory;
        }
        
    public ItemStack getModularItemStack()
    {
        if (this.containerUUID != null && this.hostInventory != null)
        {
            return StackHelper.getItemStackByUUID(this.hostInventory, this.containerUUID, "UUID");
        }

        return this.modularItemStack;
    }
    
        
        public void setModularItemStack(ItemStack stack)
        {
            this.modularItemStack = stack;
        }
        
        public int getSelectedMemoryCardIndex()
        {
            return this.cachedSelectedIndex;
        }
        
        public ItemStack getSelectedMemoryCardStack()
        {
            int index = this.getSelectedMemoryCardIndex();
            return index >= 0 && index < this.memoryCardInventory.getSlots() ?
                   this.memoryCardInventory.getStackInSlot(index) : ItemStack.EMPTY;
        }
        
        public void readFromSelectedMemoryCardStack()
        {
            if (this.getSelectedMemoryCardStack().isEmpty())
            {
                for (int i = 0; i < this.getSlots(); i++)
                {
                    this.setStackInSlot(i, ItemStack.EMPTY);
                }
                return;
            }
            
            super.readFromContainerItemStack();
        }
        
        @Override
        public void readFromContainerItemStack()
        {
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
        protected void writeToContainerItemStack()
        {
            super.writeToContainerItemStack();
            this.memoryCardInventory.writeToContainerItemStack();
        }
        
        public void writeDataToContainerItemStack()
        {
            this.writeToContainerItemStack();
        }
        
        @Override
        public ItemStack getContainerItemStack()
        {
            return this.getSelectedMemoryCardStack();
        }
        
        public boolean isSelectedMemoryCardValid()
        {
            ItemStack selectedCard = this.getSelectedMemoryCardStack();
            return !selectedCard.isEmpty();
        }
        
        @Override
        public int getInventoryStackLimit()
        {
            return this.getInventoryStackLimitFromContainerStack(this.getSelectedMemoryCardStack());
        }
        
        @Override
        public boolean isItemValidForSlot(int slotNum, ItemStack stack)
        {
            if (stack.isEmpty())
            {
                return false;
            }
            
            ItemStack modularStack = this.getModularItemStack();
            
            if (modularStack.isEmpty() == false && modularStack.getItem() == stack.getItem())
            {
                return false;
            }
            
            return super.isItemValidForSlot(slotNum, stack);
        }
        
        public boolean isAccessibleBy(EntityPlayer entity)
        {
            return !this.getModularItemStack().isEmpty() && !this.getSelectedMemoryCardStack().isEmpty();
        }
    
        
        public void markDirty()
        {
            this.writeDataToContainerItemStack();
            this.memoryCardInventory.writeToContainerItemStack();
    }
}
