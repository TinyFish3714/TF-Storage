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
import tf.storage.inventory.slot.GenericSlot;
import tf.storage.network.PacketHandler;
import tf.storage.network.packet.SyncMultipleSlotsPacket;
import tf.storage.tile.TileChest;

public class LargeStackContainer extends BaseContainer
{
    @Nullable
    protected final TileChest te;
    
    public int selectedSlot = -1;
    private int selectedSlotLast = -1;

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
        // 处理中键点击（交换物品）
        if (clickType == ClickType.CLONE)
        {
            if (player.capabilities.isCreativeMode && this.inventoryPlayer.getItemStack().isEmpty() && slotNum >= 0 && slotNum < this.inventorySlots.size())
            {
                Slot slot = this.getSlot(slotNum);
                if (slot != null && slot.getHasStack())
                {
                    ItemStack stack = slot.getStack().copy();
                    stack.setCount(stack.getMaxStackSize());
                    this.inventoryPlayer.setItemStack(stack);
                    return ItemStack.EMPTY;
                }
            }
        
            this.middleClickSlot(slotNum, player);
            return ItemStack.EMPTY;
        }

        // 仅在自定义库存槽位中处理超过最大堆叠数的特殊情况
        if (slotNum >= 0 && slotNum < this.inventorySlots.size() && clickType == ClickType.PICKUP)
        {
            Slot clickedSlot = this.getSlot(slotNum);
            
            // 仅对我们的自定义库存槽位应用特殊处理（支持大堆叠的槽位）
            if (clickedSlot instanceof SlotItemHandler && 
                ((SlotItemHandler) clickedSlot).getItemHandler() == this.inventory)
            {
                ItemStack cursorStack = player.inventory.getItemStack();
                ItemStack slotStack = clickedSlot.getStack();
                
                // 情况 1: 槽位中的物品数量超过最大堆叠数
                if (!slotStack.isEmpty() && slotStack.getCount() > slotStack.getMaxStackSize())
                {
                    // 情况 1a: 空手，从槽位取物（取最大堆叠数）
                    if (cursorStack.isEmpty())
                    {
                        int itemMaxStack = slotStack.getMaxStackSize();
                        int toTake = Math.min(slotStack.getCount(), itemMaxStack);
                        if (dragType == 1) // 右键点击，取一半（最大堆叠数）
                        {
                            toTake = Math.min((slotStack.getCount() + 1) / 2, itemMaxStack);
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
                    // 情况 1b: 手中持有相同物品，合并到槽位
                    else if (ItemStack.areItemsEqual(slotStack, cursorStack) &&
                             ItemStack.areItemStackTagsEqual(slotStack, cursorStack))
                    {
                        int slotLimit = clickedSlot.getItemStackLimit(cursorStack);
                        int spaceAvailable = slotLimit - slotStack.getCount();
                        
                        if (spaceAvailable > 0)
                        {
                            int toMerge;
                            if (dragType == 1) // 右键点击，放 1 个
                            {
                                toMerge = 1;
                            }
                            else // 左键点击，放全部
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
                    // 情况 1c: 手中持有不同物品，防止交换（槽位物品 > 最大堆叠数，无法拿起）
                    else
                    {
                        // 不允许交换，直接返回
                        return ItemStack.EMPTY;
                    }
                }
                // 情况 2: 槽位物品 <= 最大堆叠数，但手中有物品要放入（可能需要合并为 > 最大堆叠数）
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
                            if (dragType == 1) // 右键点击，放 1 个
                            {
                                toMerge = 1;
                            }
                            else // 左键点击，放全部
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
            listener.sendWindowProperty(this, 1, this.selectedSlot); // 同步初始选中的槽位
            if (listener instanceof EntityPlayerMP)
            {
                EntityPlayerMP player = (EntityPlayerMP) listener;
                player.connection.sendPacket(new SPacketSetSlot(-1, -1, player.inventory.getItemStack()));
                this.syncAllSlots(player);
            }
            this.detectAndSendChanges();
        }
    }
    
    @Override
    public void detectAndSendChanges()
    {
        if (this.listeners != null) {
            for (int i = 0; i < this.listeners.size(); i++)
            {
                IContainerListener listener = this.listeners.get(i);
                if (this.selectedSlot != this.selectedSlotLast)
                {
                    // 使用 ID 1 作为选中槽位的同步 ID。ID 0 被子类用于同步内存卡选择。
                    listener.sendWindowProperty(this, 1, this.selectedSlot);
                }
            }
            this.selectedSlotLast = this.selectedSlot;
        }
        super.detectAndSendChanges();
    }
    
    @Override
    public void updateProgressBar(int id, int data)
    {
        if (id == 1)
        {
            this.selectedSlot = data;
        }
        super.updateProgressBar(id, data);
    }
    
    protected void middleClickSlot(int slotNum, EntityPlayer player)
    {
        this.swapSlots(slotNum, player);
    }

    protected void swapSlots(int slotNum, EntityPlayer player)
    {
        Slot slot1 = this.getSlot(slotNum);
        
        // 仅允许在我们的自定义库存槽位中进行交换
        if (slot1 instanceof SlotItemHandler && ((SlotItemHandler) slot1).getItemHandler() == this.inventory)
        {
             if (this.selectedSlot >= 0 && this.selectedSlot < this.inventorySlots.size())
             {
                 if (this.selectedSlot != slotNum)
                 {
                     Slot slot2 = this.getSlot(this.selectedSlot);
                     if (slot2 instanceof SlotItemHandler && ((SlotItemHandler) slot2).getItemHandler() == this.inventory)
                     {
                         this.swapSlots((SlotItemHandler)slot1, (SlotItemHandler)slot2);
                     }
                 }
                 this.selectedSlot = -1;
             }
             else
             {
                 this.selectedSlot = slotNum;
             }
             
             // 立即同步更改
             this.detectAndSendChanges();
        }
    }
    
    protected boolean swapSlots(SlotItemHandler slot1, SlotItemHandler slot2)
    {
        if (!slot1.canTakeStack(this.player) || !slot2.canTakeStack(this.player))
        {
            return false;
        }
        
        ItemStack stack1 = slot1.getStack();
        ItemStack stack2 = slot2.getStack();
        
        // 验证物品是否有效
        if ((!stack1.isEmpty() && !slot2.isItemValid(stack1)) || (!stack2.isEmpty() && !slot1.isItemValid(stack2)))
        {
            return false;
        }
        
        if (stack1.isEmpty() == false)
        {
            slot1.onTake(this.player, stack1);
        }

        if (stack2.isEmpty() == false)
        {
            slot2.onTake(this.player, stack2);
        }

        slot1.putStack(stack2.isEmpty() ? ItemStack.EMPTY : stack2.copy());
        slot2.putStack(stack1.isEmpty() ? ItemStack.EMPTY : stack1.copy());
        
        return true;
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
