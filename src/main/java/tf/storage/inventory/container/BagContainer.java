package tf.storage.inventory.container;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.inventory.ClickType;
import net.minecraft.inventory.EntityEquipmentSlot;
import net.minecraft.inventory.IContainerListener;
import net.minecraft.inventory.Slot;
import net.minecraft.item.ItemStack;
import net.minecraftforge.items.IItemHandler;
import net.minecraftforge.items.wrapper.InvWrapper;
import tf.storage.inventory.handler.BasicHandler;
import tf.storage.inventory.container.base.LargeStackContainer;
import tf.storage.inventory.container.base.MergeSlotRange;
import tf.storage.inventory.handler.ModularHandler;
import tf.storage.inventory.slot.ArmorSlot;
import tf.storage.inventory.slot.ResultSlot;
import tf.storage.inventory.slot.GenericSlot;
import tf.storage.inventory.slot.CardSlot;
import tf.storage.inventory.wrapper.CraftingWrapper;
import tf.storage.inventory.wrapper.ResultWrapper;
import tf.storage.item.TFBag.ShiftMode;
import tf.storage.util.StackHelper;
import tf.storage.util.CardHelper;

public class BagContainer extends LargeStackContainer
{
    public static final EntityEquipmentSlot[] EQUIPMENT_SLOT_TYPES = new EntityEquipmentSlot[]
    {
        EntityEquipmentSlot.HEAD,
        EntityEquipmentSlot.CHEST,
        EntityEquipmentSlot.LEGS,
        EntityEquipmentSlot.FEET
    };
    public final ModularHandler inventoryItemWithMemoryCards;
    private final CraftingWrapper craftMatrix;
    private final IItemHandler craftMatrixWrapper;
    private final ResultWrapper craftResult = new ResultWrapper();
    private int craftingSlot = 0;
    
    private ItemStack lastMemoryCardStack = ItemStack.EMPTY;
    
    public int selectedMemoryCard = -1;
    
    private int bagSlotIndex = -1;

    public BagContainer(EntityPlayer player, ItemStack containerStack)
    {
        super(player, new ModularHandler(containerStack, player, true));
        this.inventoryItemWithMemoryCards = (ModularHandler) this.inventory;
        this.craftMatrix = new CraftingWrapper(3, 3, new BasicHandler(9), this.craftResult, player, this);
        this.craftMatrixWrapper = new InvWrapper(this.craftMatrix);

        this.selectedMemoryCard = this.inventoryItemWithMemoryCards.getSelectedMemoryCardIndex();

        this.addCustomInventorySlots();
        this.addPlayerInventorySlots(8, 174);
        
        this.findBagSlotIndex(containerStack);
    }
    
    private void findBagSlotIndex(ItemStack bagStack)
    {
        for (int i = 0; i < this.inventorySlots.size(); ++i)
        {
            if (this.inventorySlots.get(i).getStack() == bagStack)
            {
                this.bagSlotIndex = i;
                break;
            }
        }
    }

    @Override
    protected void addPlayerInventorySlots(int posX, int posY)
    {
        if (this.getBagTier() == 1)
        {
            posX += 40;
        }

        super.addPlayerInventorySlots(posX, posY - 7);

        int playerArmorStart = this.inventorySlots.size();

        posY = 15;

        for (int i = 0; i < 4; i++)
        {
            this.addSlotToContainer(new ArmorSlot(this, this.playerInv, i, 39 - i, posX, posY + i * 18));
        }

        this.playerArmorSlots = new MergeSlotRange(playerArmorStart, 4);

        this.addOffhandSlot(posX + 4 * 18, 69);

        posX += 90;
        posY = 15;
        this.craftingSlot = this.inventorySlots.size();
        this.addSlotToContainer(new ResultSlot(this.craftMatrix, this.craftResult, 0, posX + 56, posY + 18, this.player));

        for (int i = 0; i < 3; ++i)
        {
            for (int j = 0; j < 3; ++j)
            {
                this.addSlotToContainer(new GenericSlot(this.craftMatrixWrapper, j + i * 3, posX + j * 18, posY + i * 18));
            }
        }
    }

    @Override
    protected void addCustomInventorySlots()
    {
        int customInvStart = this.inventorySlots.size();
        int xOff = 8;
        int yOff = 102;

        if (this.getBagTier() == 1)
        {
            xOff += 40;
        }

        for (int i = 0; i < 3; i++)
        {
            for (int j = 0; j < 9; j++)
            {
                this.addSlotToContainer(new GenericSlot(this.inventory, i * 9 + j, xOff + j * 18, yOff - 2 + i * 18));
            }
        }

        // TF包（大）
        if (this.getBagTier() == 1)
        {
            int xOffXtra = 8;
            yOff = 102;

            for(int i = 0; i < 7; i++)
            {
                this.addSlotToContainer(new GenericSlot(this.inventory, 27 + i * 2, xOffXtra +  0, yOff - 7 + i * 18));
                this.addSlotToContainer(new GenericSlot(this.inventory, 28 + i * 2, xOffXtra + 18, yOff - 7 + i * 18));
            }

            xOffXtra = 214;

            for(int i = 0; i < 7; i++)
            {
                this.addSlotToContainer(new GenericSlot(this.inventory, 41 + i * 2, xOffXtra +  0, yOff - 7 + i * 18));
                this.addSlotToContainer(new GenericSlot(this.inventory, 42 + i * 2, xOffXtra + 18, yOff - 7 + i * 18));
            }
        }

        this.customInventorySlots = new MergeSlotRange(customInvStart, this.inventorySlots.size() - customInvStart);

        xOff += 90;
        yOff = 69;
        int memoryCardSlots = this.inventoryItemWithMemoryCards.getMemoryCardInventory().getSlots();
        this.addMergeSlotRangePlayerToExt(this.inventorySlots.size(), memoryCardSlots);

        for (int i = 0; i < memoryCardSlots; i++)
        {
            this.addSlotToContainer(new CardSlot(this.inventoryItemWithMemoryCards.getMemoryCardInventory(), i, xOff + i * 18, yOff));
        }
    }

    public ItemStack getContainerItem()
    {
        return this.inventoryItemWithMemoryCards.getModularItemStack();
    }

    public void dropCraftingGridContents()
    {
        for (int i = 0; i < 9; ++i)
        {
            ItemStack stack = this.craftMatrix.removeStackFromSlot(i);

            if (stack.isEmpty() == false)
            {
                this.player.dropItem(stack, true);
            }
        }

        this.craftResult.setStackInSlot(0, ItemStack.EMPTY);
    }

    public int getBagTier()
    {
        if (this.inventoryItemWithMemoryCards.getModularItemStack().isEmpty() == false)
        {
            return this.inventoryItemWithMemoryCards.getModularItemStack().getMetadata() == 1 ? 1 : 0;
        }

        return 0;
    }

    @Override
    public void onContainerClosed(EntityPlayer player)
    {
        super.onContainerClosed(player);

        this.dropCraftingGridContents();
    }
    
    @Override
    public boolean canInteractWith(EntityPlayer player)
    {
        if (this.bagSlotIndex != -1)
        {
            if (this.bagSlotIndex < this.inventorySlots.size())
            {
                Slot slot = this.inventorySlots.get(this.bagSlotIndex);
                if (slot.getStack() != this.inventoryItemWithMemoryCards.getModularItemStack())
                {
                    return false;
                }
            }
        }
        return super.canInteractWith(player);
    }

    @Override
    public ItemStack slotClick(int slotNum, int dragType, ClickType clickType, EntityPlayer player)
    {
        if (slotNum >= 0 && slotNum == this.bagSlotIndex)
        {
            return ItemStack.EMPTY;
        }
        
        if (clickType == ClickType.SWAP && dragType >= 0 && dragType < 9)
        {
             ItemStack stackInHotbar = player.inventory.getStackInSlot(dragType);
             if (stackInHotbar == this.inventoryItemWithMemoryCards.getModularItemStack())
             {
                 return ItemStack.EMPTY;
             }
        }

        super.slotClick(slotNum, dragType, clickType, player);

        if (this.isClient == false && slotNum == this.craftingSlot)
        {
            this.syncSlotToClient(this.craftingSlot);
            this.syncCursorStackToClient();
        }

        return ItemStack.EMPTY;
    }

    @Override
    protected boolean transferStackFromPlayerMainInventory(EntityPlayer player, int slotNum)
    {
        ItemStack modularStack = this.inventoryItemWithMemoryCards.getModularItemStack();

        if (modularStack.isEmpty() == false && ShiftMode.getEffectiveMode(modularStack) == ShiftMode.INV_HOTBAR)
        {
            if (this.playerHotbarSlots.contains(slotNum))
            {
                return this.transferStackToSlotRange(player, slotNum, this.playerMainSlots, false);
            }
            else if (this.playerMainSlots.contains(slotNum))
            {
                return this.transferStackToSlotRange(player, slotNum, this.playerHotbarSlots, false);
            }
        }

        return super.transferStackFromPlayerMainInventory(player, slotNum);
    }

    @Override
    public void addListener(IContainerListener listener)
    {
        super.addListener(listener);
        
        listener.sendWindowProperty(this, 0, this.selectedMemoryCard);
    }

    @Override
    public void detectAndSendChanges()
    {
        if (!this.player.getEntityWorld().isRemote)
        {
            ItemStack modularStack = this.inventoryItemWithMemoryCards.getModularItemStack();
            ItemStack currentCard = this.inventoryItemWithMemoryCards.getSelectedMemoryCardStack();
            int currentSelectedIndex = this.inventoryItemWithMemoryCards.getSelectedMemoryCardIndex();

            boolean containerChanged = !ItemStack.areItemStacksEqual(modularStack, this.getContainerItem());
            boolean cardChanged = !ItemStack.areItemStacksEqual(currentCard, this.lastMemoryCardStack);
            boolean selectionChanged = this.selectedMemoryCard != currentSelectedIndex;

            if (containerChanged || cardChanged || selectionChanged)
            {
                if (containerChanged) {
                    this.inventoryItemWithMemoryCards.readFromContainerItemStack();
                } else {
                    this.inventoryItemWithMemoryCards.readFromSelectedMemoryCardStack();
                }
                
                this.lastMemoryCardStack = currentCard.copy();
                this.selectedMemoryCard = currentSelectedIndex;

                this.markAllSlotsDirty();
            }
            
            if (selectionChanged)
            {
                for (int i = 0; i < this.listeners.size(); ++i)
                {
                    IContainerListener listener = this.listeners.get(i);
                    listener.sendWindowProperty(this, 0, this.selectedMemoryCard);
                }
            }
        }
        super.detectAndSendChanges();
    }

    @Override
    public void updateProgressBar(int var, int val)
    {
        super.updateProgressBar(var, val);
        
        switch (var)
        {
            case 0:
                this.selectedMemoryCard = val;
                this.inventoryItemWithMemoryCards.cachedSelectedIndex = val;
                this.inventoryItemWithMemoryCards.readFromSelectedMemoryCardStack();
                this.markAllSlotsDirty();
                break;
            default:
        }
    }
}
