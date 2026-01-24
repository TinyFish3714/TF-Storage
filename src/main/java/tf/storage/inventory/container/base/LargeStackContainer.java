package tf.storage.inventory.container.base;

import java.util.ArrayList;
import java.util.List;
import javax.annotation.Nullable;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.inventory.ClickType;
import net.minecraft.inventory.IContainerListener;
import net.minecraft.inventory.Slot;
import net.minecraft.item.ItemStack;
import net.minecraft.network.play.server.SPacketSetSlot;
import net.minecraftforge.items.IItemHandler;
import net.minecraftforge.items.SlotItemHandler;
import tf.storage.inventory.IItemHandlerSize;
import tf.storage.network.PacketHandler;
import tf.storage.network.packet.SyncMultipleSlotsPacket;
import tf.storage.tile.TileChest;

public class LargeStackContainer extends BaseContainer
{
    @Nullable
    protected final TileChest te;

    public LargeStackContainer(EntityPlayer player, IItemHandler inventory)
    {
        super(player, inventory);
        this.te = null;
    }

    public LargeStackContainer(EntityPlayer player, IItemHandler inventory, @Nullable TileChest te)
    {
        super(player, inventory);
        this.te = te;
    }

    @Override
    protected int getMaxStackSizeFromSlotAndStack(Slot slot, ItemStack stack)
    {
        // Our inventory
        if (slot instanceof SlotItemHandler && ((SlotItemHandler) slot).getItemHandler() == this.inventory)
        {
            return slot.getItemStackLimit(stack);
        }

        // Player inventory
        return super.getMaxStackSizeFromSlotAndStack(slot, stack);
    }

    @Override
    public ItemStack slotClick(int slotNum, int dragType, ClickType clickType, EntityPlayer player)
    {
        // Only handle special cases for stacks > 64 in custom inventory slots
        if (slotNum >= 0 && slotNum < this.inventorySlots.size() && clickType == ClickType.PICKUP)
        {
            Slot clickedSlot = this.getSlot(slotNum);
            
            // Only apply special handling to our custom inventory slots (slots that support large stacks)
            if (clickedSlot instanceof SlotItemHandler && 
                ((SlotItemHandler) clickedSlot).getItemHandler() == this.inventory)
            {
                ItemStack cursorStack = player.inventory.getItemStack();
                ItemStack slotStack = clickedSlot.getStack();
                
                // Case 1: Slot has more than 64 items
                if (!slotStack.isEmpty() && slotStack.getCount() > 64)
                {
                    // Case 1a: Empty hand, take from slot (max 64)
                    if (cursorStack.isEmpty())
                    {
                        int toTake = Math.min(slotStack.getCount(), 64);
                        if (dragType == 1) // Right click, take half (max 64)
                        {
                            toTake = Math.min((slotStack.getCount() + 1) / 2, 64);
                        }
                        
                        ItemStack taken = slotStack.copy();
                        taken.setCount(toTake);
                        slotStack.shrink(toTake);
                        
                        if (slotStack.isEmpty())
                        {
                            clickedSlot.putStack(ItemStack.EMPTY);
                        }
                        else
                        {
                            clickedSlot.putStack(slotStack);
                        }
                        
                        player.inventory.setItemStack(taken);
                        
                        this.syncSlotToClient(slotNum);
                        this.syncCursorStackToClient();
                        
                        return ItemStack.EMPTY;
                    }
                    // Case 1b: Same item in hand, merge to slot
                    else if (ItemStack.areItemsEqual(slotStack, cursorStack) &&
                             ItemStack.areItemStackTagsEqual(slotStack, cursorStack))
                    {
                        int slotLimit = clickedSlot.getItemStackLimit(cursorStack);
                        int spaceAvailable = slotLimit - slotStack.getCount();
                        
                        if (spaceAvailable > 0)
                        {
                            int toMerge;
                            if (dragType == 1) // Right click, put 1
                            {
                                toMerge = 1;
                            }
                            else // Left click, put all
                            {
                                toMerge = Math.min(spaceAvailable, cursorStack.getCount());
                            }
                            
                            cursorStack.shrink(toMerge);
                            if (cursorStack.isEmpty())
                            {
                                player.inventory.setItemStack(ItemStack.EMPTY);
                            }
                            
                            slotStack.grow(toMerge);
                            clickedSlot.putStack(slotStack);
                            
                            this.syncSlotToClient(slotNum);
                            this.syncCursorStackToClient();
                        }
                        
                        return ItemStack.EMPTY;
                    }
                    // Case 1c: Different item in hand, prevent swap (slot has > 64 items, cannot pick up)
                    else
                    {
                        // Do not allow swap, return directly
                        return ItemStack.EMPTY;
                    }
                }
                // Case 2: Slot has <= 64 items, but hand has items to put in (may need to merge to > 64 stack)
                else if (!cursorStack.isEmpty() && !slotStack.isEmpty() &&
                         ItemStack.areItemsEqual(slotStack, cursorStack) &&
                         ItemStack.areItemStackTagsEqual(slotStack, cursorStack))
                {
                    int slotLimit = clickedSlot.getItemStackLimit(cursorStack);
                    if (slotLimit > 64)
                    {
                        int spaceAvailable = slotLimit - slotStack.getCount();
                        
                        if (spaceAvailable > 0)
                        {
                            int toMerge;
                            if (dragType == 1) // Right click, put 1
                            {
                                toMerge = 1;
                            }
                            else // Left click, put all
                            {
                                toMerge = Math.min(spaceAvailable, cursorStack.getCount());
                            }
                            
                            cursorStack.shrink(toMerge);
                            if (cursorStack.isEmpty())
                            {
                                player.inventory.setItemStack(ItemStack.EMPTY);
                            }
                            
                            slotStack.grow(toMerge);
                            clickedSlot.putStack(slotStack);
                            
                            this.syncSlotToClient(slotNum);
                            this.syncCursorStackToClient();
                            
                            return ItemStack.EMPTY;
                        }
                    }
                }
            }
        }
        
        return super.slotClick(slotNum, dragType, clickType, player);
    }

    @Override
    public void addListener(IContainerListener listener)
    {
        if (this.listeners.contains(listener))
        {
            throw new IllegalArgumentException("Listener already listening");
        }
        else
        {
            this.listeners.add(listener);
            if (listener instanceof EntityPlayerMP)
            {
                EntityPlayerMP player = (EntityPlayerMP) listener;
                player.connection.sendPacket(new SPacketSetSlot(-1, -1, player.inventory.getItemStack()));
                this.syncAllSlots(player);
            }
            this.detectAndSendChanges();
        }
    }

    protected void syncAllSlots(EntityPlayerMP player)
    {
        List<Integer> slotsToSync = new ArrayList<>();
        List<ItemStack> stacksToSync = new ArrayList<>();
        
        for (int slot = 0; slot < this.inventorySlots.size(); slot++)
        {
            ItemStack stack = this.inventorySlots.get(slot).getStack();
            this.inventoryItemStacks.set(slot, stack.isEmpty() ? ItemStack.EMPTY : stack.copy());
            slotsToSync.add(slot);
            stacksToSync.add(stack);
        }

        final int MAX_SLOTS_PER_PACKET = 64;
        int totalSlots = slotsToSync.size();

        if (totalSlots > MAX_SLOTS_PER_PACKET)
        {
            for (int i = 0; i < totalSlots; i += MAX_SLOTS_PER_PACKET)
            {
                int end = Math.min(i + MAX_SLOTS_PER_PACKET, totalSlots);
                List<Integer> batchSlots = slotsToSync.subList(i, end);
                List<ItemStack> batchStacks = stacksToSync.subList(i, end);
                PacketHandler.INSTANCE.sendTo(
                    new SyncMultipleSlotsPacket(this.windowId, batchSlots, batchStacks),
                    player
                );
            }
        }
        else
        {
            PacketHandler.INSTANCE.sendTo(
                new SyncMultipleSlotsPacket(this.windowId, slotsToSync, stacksToSync),
                player
            );
        }
    }

    @Override
    public boolean canInteractWith(EntityPlayer player)
    {
        if (this.te != null)
        {
            return !this.te.isInvalid() && this.te.isUseableByPlayer(player);
        }
        return super.canInteractWith(player);
    }
    
}
