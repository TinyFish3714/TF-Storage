package tf.storage.inventory.container.base;

import java.util.ArrayList;
import java.util.BitSet;
import java.util.List;
import javax.annotation.Nullable;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.entity.player.InventoryPlayer;
import net.minecraft.inventory.Container;
import net.minecraft.inventory.IContainerListener;
import net.minecraft.inventory.Slot;
import net.minecraft.item.ItemStack;
import net.minecraft.util.NonNullList;
import net.minecraftforge.items.IItemHandler;
import net.minecraftforge.items.IItemHandlerModifiable;
import net.minecraftforge.items.SlotItemHandler;
import tf.storage.TFStorage;
import tf.storage.inventory.slot.ResultSlot;
import tf.storage.inventory.slot.GenericSlot;
import tf.storage.network.PacketHandler;
import net.minecraftforge.items.wrapper.CombinedInvWrapper;
import net.minecraftforge.items.wrapper.PlayerOffhandInvWrapper;
import net.minecraftforge.items.wrapper.PlayerArmorInvWrapper;
import net.minecraftforge.items.wrapper.InvWrapper;
import net.minecraftforge.items.wrapper.RangedWrapper;
import tf.storage.network.packet.SyncSlotPacket;
import tf.storage.network.packet.SyncMultipleSlotsPacket;
import tf.storage.util.StackHelper;

public class BaseContainer extends Container
{
    public final EntityPlayer player;
    protected final boolean isClient;
    protected final InventoryPlayer inventoryPlayer;
    protected final IItemHandlerModifiable playerInv;
    public final IItemHandler inventory;
    protected MergeSlotRange customInventorySlots;
    protected MergeSlotRange playerMainSlots;
    protected MergeSlotRange playerHotbarSlots;
    protected MergeSlotRange playerMainSlotsIncHotbar;
    protected MergeSlotRange playerOffhandSlots;
    protected MergeSlotRange playerArmorSlots;
    protected List<MergeSlotRange> mergeSlotRangesPlayerToExt;
    
    private BitSet dirtySlots;
    private boolean hasDirtySlots;

    public BaseContainer(EntityPlayer player, IItemHandler inventory)
    {
        this.player = player;
        this.isClient = player.getEntityWorld().isRemote;
        this.inventoryPlayer = player.inventory;
        this.playerInv = new CombinedInvWrapper(
            new PlayerMainInvWrapperNoSync(player.inventory),
            new PlayerArmorInvWrapper(player.inventory),
            new PlayerOffhandInvWrapper(player.inventory)
        );
        this.inventory = inventory;
        this.mergeSlotRangesPlayerToExt = new ArrayList<MergeSlotRange>();

        this.customInventorySlots       = new MergeSlotRange(0, 0);
        this.playerMainSlotsIncHotbar   = new MergeSlotRange(0, 0);
        this.playerMainSlots            = new MergeSlotRange(0, 0);
        this.playerHotbarSlots          = new MergeSlotRange(0, 0);
        this.playerOffhandSlots         = new MergeSlotRange(0, 0);
        this.playerArmorSlots           = new MergeSlotRange(0, 0);
        
        this.dirtySlots = new BitSet(64);
        this.hasDirtySlots = false;
    }

    protected void addCustomInventorySlots()
    {
    }
    
    protected void markSlotDirty(int slotId)
    {
        if (slotId >= 0 && slotId < this.inventorySlots.size())
        {
            this.dirtySlots.set(slotId);
            this.hasDirtySlots = true;
        }
    }
    
    private void clearDirtySlots()
    {
        if (this.hasDirtySlots)
        {
            this.dirtySlots.clear();
            this.hasDirtySlots = false;
        }
    }
    
    protected void markAllSlotsDirty() {
        this.dirtySlots.set(0, this.inventorySlots.size());
        this.hasDirtySlots = true;
    }

    protected void addPlayerInventorySlots(int posX, int posY)
    {
        int playerInvStart = this.inventorySlots.size();

        for (int i = 0; i < 3; i++)
        {
            for (int j = 0; j < 9; j++)
            {
                this.addSlotToContainer(new GenericSlot(this.playerInv, i * 9 + j + 9, posX + j * 18, posY + i * 18));
            }
        }

        this.playerMainSlots = new MergeSlotRange(playerInvStart, 27);
        int playerHotbarStart = this.inventorySlots.size();

        for (int i = 0; i < 9; i++)
        {
            this.addSlotToContainer(new GenericSlot(this.playerInv, i, posX + i * 18, posY + 58));
        }

        this.playerMainSlotsIncHotbar = new MergeSlotRange(playerInvStart, 36);
        this.playerHotbarSlots = new MergeSlotRange(playerHotbarStart, 9);
    }

    protected void addOffhandSlot(int posX, int posY)
    {
        this.playerOffhandSlots = new MergeSlotRange(this.inventorySlots.size(), 1);

        this.addSlotToContainer(new GenericSlot(this.playerInv, 40, posX, posY)
        {
            @Override
            public String getSlotTexture()
            {
                return "minecraft:items/empty_armor_slot_shield";
            }
        });
    }

    public EntityPlayer getPlayer()
    {
        return this.player;
    }

    public SlotRange getPlayerMainInventorySlotRange()
    {
        return this.playerMainSlotsIncHotbar;
    }

    @Override
    public boolean canInteractWith(EntityPlayer player)
    {
        return true;
    }

    @Override
    public boolean canMergeSlot(ItemStack stack, Slot slot)
    {
        return (slot instanceof SlotItemHandler) &&
                (slot instanceof ResultSlot) == false &&
                this.inventoryPlayer.getItemStack().isEmpty() == false;
    }

    @Override
    @Nullable
    public Slot getSlot(int slotId)
    {
        return slotId >= 0 && slotId < this.inventorySlots.size() ? super.getSlot(slotId) : null;
    }

    public GenericSlot getSlotItemHandler(int slotId)
    {
        Slot slot = this.getSlot(slotId);

        return (slot instanceof GenericSlot) ? (GenericSlot) slot : null;
    }

    protected void syncCursorStackToClient()
    {
        this.syncStackToClient(-1, this.player.inventory.getItemStack());
    }

    protected void syncSlotToClient(int slotNum)
    {
        if (slotNum >= 0 && slotNum < this.inventorySlots.size())
        {
            this.syncStackToClient(slotNum, this.getSlot(slotNum).getStack());
        }
    }

    protected void syncStackToClient(int slotNum, ItemStack stack)
    {
        for (int i = 0; i < this.listeners.size(); i++)
        {
            IContainerListener listener = this.listeners.get(i);

            if (listener instanceof EntityPlayerMP)
            {
                PacketHandler.INSTANCE.sendTo(new SyncSlotPacket(this.windowId, slotNum, stack), (EntityPlayerMP) listener);
            }
        }
    }

    public void syncStackInSlot(int slotId, ItemStack stack)
    {
        if (slotId == -1)
        {
            this.player.inventory.setItemStack(stack);
        }
        else
        {
            Slot slot = this.getSlot(slotId);

            if (slot instanceof GenericSlot)
            {
                ((GenericSlot) slot).syncStack(stack);
            }
            else
            {
                this.putStackInSlot(slotId, stack);
            }
            
            this.markSlotDirty(slotId);
        }
    }

    @Override
    public void detectAndSendChanges()
    {
        if (this.isClient == false)
        {
            this.syncSlotChanges();
        }
    }
    
    private void syncSlotChanges()
    {
        boolean useOptimization = this.hasDirtySlots;
        
        List<Integer> dirtySlotsList = new ArrayList<Integer>();
        List<ItemStack> newStacks = new ArrayList<ItemStack>();
        
        for (int slot = 0; slot < this.inventorySlots.size(); slot++)
        {
            if (useOptimization && !this.dirtySlots.get(slot)) continue;
            
            ItemStack currentStack = this.inventorySlots.get(slot).getStack();
            ItemStack prevStack = this.inventoryItemStacks.get(slot);

            if (!ItemStack.areItemStacksEqual(prevStack, currentStack))
            {
                dirtySlotsList.add(slot);
                ItemStack stackToSync = currentStack.isEmpty() ? ItemStack.EMPTY : currentStack.copy();
                newStacks.add(stackToSync);
                this.inventoryItemStacks.set(slot, stackToSync);
            }
        }
        
        if (dirtySlotsList.size() > 5)
        {
            sendBatchSlotUpdates(dirtySlotsList, newStacks);
        }
        else
        {
            for (int i = 0; i < dirtySlotsList.size(); i++)
            {
                sendSlotToClient(dirtySlotsList.get(i), newStacks.get(i));
            }
        }
        
        this.clearDirtySlots();
    }
    
    private void sendBatchSlotUpdates(List<Integer> dirtySlots, List<ItemStack> newStacks)
    {
        final int MAX_SLOTS_PER_PACKET = 64;
        int totalSlots = dirtySlots.size();
        
        if (totalSlots > MAX_SLOTS_PER_PACKET)
        {
            for (int i = 0; i < totalSlots; i += MAX_SLOTS_PER_PACKET)
            {
                int end = Math.min(i + MAX_SLOTS_PER_PACKET, totalSlots);
                List<Integer> batchSlots = dirtySlots.subList(i, end);
                List<ItemStack> batchStacks = newStacks.subList(i, end);
                
                sendBatchPacket(batchSlots, batchStacks);
            }
        }
        else
        {
            sendBatchPacket(dirtySlots, newStacks);
        }
    }
    
    private void sendBatchPacket(List<Integer> slotIds, List<ItemStack> stacks)
    {
        for (int i = 0; i < this.listeners.size(); i++)
        {
            IContainerListener listener = this.listeners.get(i);

            if (listener instanceof EntityPlayerMP)
            {
                PacketHandler.INSTANCE.sendTo(new SyncMultipleSlotsPacket(this.windowId, slotIds, stacks),
                                            (EntityPlayerMP) listener);
            }
        }
    }
    
    private void sendSlotToClient(int slot, ItemStack stack)
    {
        for (int i = 0; i < this.listeners.size(); i++)
        {
            IContainerListener listener = this.listeners.get(i);

            if (listener instanceof EntityPlayerMP)
            {
                PacketHandler.INSTANCE.sendTo(new SyncSlotPacket(this.windowId, slot, stack), (EntityPlayerMP) listener);
            }
        }
    }

    @Override
    public ItemStack transferStackInSlot(EntityPlayer player, int slotNum)
    {
        this.transferStackFromSlot(player, slotNum);
        return ItemStack.EMPTY;
    }

    protected boolean transferStackFromSlot(EntityPlayer player, int slotNum)
    {
        Slot slot = this.getSlot(slotNum);

        if (slot == null || slot.getHasStack() == false || slot.canTakeStack(player) == false)
        {
            return false;
        }

        if (this.playerArmorSlots.contains(slotNum) || this.playerOffhandSlots.contains(slotNum))
        {
            return this.transferStackToSlotRange(player, slotNum, this.playerMainSlotsIncHotbar, false);
        }
        else if (this.playerMainSlotsIncHotbar.contains(slotNum))
        {
            return this.transferStackFromPlayerMainInventory(player, slotNum);
        }

        return this.transferStackToSlotRange(player, slotNum, this.playerMainSlotsIncHotbar, true);
    }

    protected boolean transferStackFromPlayerMainInventory(EntityPlayer player, int slotNum)
    {
        if (this.transferStackToSlotRange(player, slotNum, this.playerArmorSlots, false))
        {
            return true;
        }

        if (this.transferStackToPrioritySlots(player, slotNum, false))
        {
            return true;
        }

        return this.transferStackToSlotRange(player, slotNum, this.customInventorySlots, false);
    }

    protected boolean transferStackToPrioritySlots(EntityPlayer player, int slotNum, boolean reverse)
    {
        boolean ret = false;

        for (MergeSlotRange slotRange : this.mergeSlotRangesPlayerToExt)
        {
            ret |= this.transferStackToSlotRange(player, slotNum, slotRange, reverse);
        }

        return ret;
    }

    protected boolean transferStackToSlotRange(EntityPlayer player, int slotNum, MergeSlotRange slotRange, boolean reverse)
    {
        GenericSlot slot = this.getSlotItemHandler(slotNum);

        if (slot == null || slot.getHasStack() == false || slot.canTakeStack(player) == false)
        {
            return false;
        }

        ItemStack stack = slot.getStack().copy();
        int amount = Math.min(stack.getCount(), stack.getMaxStackSize());
        stack.setCount(amount);

        stack = this.mergeItemStack(stack, slotRange, reverse, true);

        if (stack.isEmpty() == false)
        {
            if (slot.isItemValid(stack) == false || stack.getCount() == amount)
            {
                return false;
            }

            amount -= stack.getCount();
        }

        stack = slot.decrStackSize(amount);
        slot.onTake(player, stack);

        this.markSlotDirty(slotNum);

        stack = this.mergeItemStack(stack, slotRange, reverse, false);

        if (stack.isEmpty() == false)
        {
            slot.insertItem(stack, false);

                TFStorage.logger.warn("在 '{}' 中合并所有物品失败。这不应该发生，请报告。",
                    this.getClass().getSimpleName());
        }

        return true;
    }

    protected int getMaxStackSizeFromSlotAndStack(Slot slot, ItemStack stack)
    {
        return stack.isEmpty() == false ? Math.min(slot.getItemStackLimit(stack), stack.getMaxStackSize()) : slot.getSlotStackLimit();
    }


    protected ItemStack mergeItemStack(ItemStack stack, MergeSlotRange slotRange, boolean reverse, boolean simulate)
    {
        int slotStart = slotRange.first;
        int slotEndExclusive = slotRange.lastExc;
        int slotIndex = (reverse ? slotEndExclusive - 1 : slotStart);

        while (stack.isEmpty() == false && slotIndex >= slotStart && slotIndex < slotEndExclusive)
        {
            GenericSlot slot = this.getSlotItemHandler(slotIndex);

            if (slot != null && slot.getHasStack() && slot.isItemValid(stack))
            {
                ItemStack before = slot.getStack().copy();
                stack = slot.insertItem(stack, simulate);
                 
                if (!simulate && !ItemStack.areItemStacksEqual(before, slot.getStack()))
                {
                    this.markSlotDirty(slotIndex);
                }
            }

            slotIndex = (reverse ? slotIndex - 1 : slotIndex + 1);
        }

        if (stack.isEmpty() == false && slotRange.existingOnly == false)
        {
            slotIndex = (reverse ? slotEndExclusive - 1 : slotStart);

            while (stack.isEmpty() == false && slotIndex >= slotStart && slotIndex < slotEndExclusive)
            {
                GenericSlot slot = this.getSlotItemHandler(slotIndex);

                if (slot != null && slot.getHasStack() == false && slot.isItemValid(stack))
                {
                    ItemStack before = slot.getStack().copy();
                    stack = slot.insertItem(stack, simulate);
                     
                    if (!simulate && !ItemStack.areItemStacksEqual(before, slot.getStack()))
                    {
                        this.markSlotDirty(slotIndex);
                    }
                }

                slotIndex = (reverse ? slotIndex - 1 : slotIndex + 1);
            }
        }

        return stack;
    }

    protected void addMergeSlotRangePlayerToExt(int start, int numSlots)
    {
        this.addMergeSlotRangePlayerToExt(start, numSlots, false);
    }

    protected void addMergeSlotRangePlayerToExt(int start, int numSlots, boolean existingOnly)
    {
        this.mergeSlotRangesPlayerToExt.add(new MergeSlotRange(start, numSlots, existingOnly));
    }

    private static class PlayerMainInvWrapperNoSync extends RangedWrapper
    {
        private final InventoryPlayer inventoryPlayer;

        public PlayerMainInvWrapperNoSync(InventoryPlayer inv)
        {
            super(new InvWrapper(inv), 0, inv.mainInventory.size());
            this.inventoryPlayer = inv;
        }

        @Override
        public ItemStack insertItem(int slot, ItemStack stack, boolean simulate)
        {
            ItemStack stackRemaining = super.insertItem(slot, stack, simulate);

            if (!stackRemaining.isEmpty() && stackRemaining.getCount() != stack.getCount())
            {
                ItemStack stackSlot = this.getStackInSlot(slot);
                if (!stackSlot.isEmpty() && this.inventoryPlayer.player.getEntityWorld().isRemote)
                {
                    stackSlot.setAnimationsToGo(5);
                }
            }

            return stackRemaining;
        }
    }
    

}
