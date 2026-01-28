package tf.storage.util;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import com.google.common.collect.ImmutableMap;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.init.Items;
import net.minecraft.item.EnumDyeColor;
import net.minecraft.item.Item;
import net.minecraft.item.ItemBlock;
import net.minecraft.item.ItemStack;
import net.minecraft.util.NonNullList;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.minecraftforge.items.CapabilityItemHandler;
import net.minecraftforge.items.IItemHandler;
import net.minecraftforge.items.IItemHandlerModifiable;
import net.minecraftforge.items.ItemHandlerHelper;
import net.minecraftforge.oredict.OreDictionary;
import tf.storage.inventory.IItemHandlerSize;
import tf.storage.inventory.handler.BasicHandler;
import tf.storage.inventory.container.base.SlotRange;

public class StackHelper
{
    public static final BasicHandler NULL_INV = new BasicHandler(0);

    public static void dropInventoryContentsInWorld(World world, BlockPos pos, IItemHandler inv)
    {
        int slots = inv.getSlots();
        for (int i = 0; i < slots; i++) {
            ItemStack stack = inv.getStackInSlot(i);
            if (!stack.isEmpty()) {
                EntityHelper.dropItemStacksInWorld(world, pos, stack, true);
            }
        }
    }

    public static InvResult tryMoveAllItems(IItemHandler invSrc, IItemHandler invDst)
    {
        return tryMoveAllItemsWithinSlotRange(invSrc, invDst, new SlotRange(invSrc), new SlotRange(invDst));
    }

    public static InvResult tryMoveAllItemsWithinSlotRange(IItemHandler invSrc, IItemHandler invDst, SlotRange slotsSrc, SlotRange slotsDst)
    {
        boolean movedAll = true;
        boolean movedSome = false;
        final int lastSlot = Math.min(slotsSrc.lastInc, invSrc.getSlots() - 1);

        for (int slot = slotsSrc.first; slot <= lastSlot; slot++)
        {
            ItemStack stackInSrc = invSrc.getStackInSlot(slot);
            
            if (stackInSrc.isEmpty()) {
                continue;
            }

            ItemStack remainder = tryInsertItemStackToInventoryWithinSlotRange(invDst, stackInSrc, slotsDst, true);

            int amountToMove = stackInSrc.getCount() - remainder.getCount();

            if (amountToMove > 0)
            {
                ItemStack movedStack = invSrc.extractItem(slot, amountToMove, false);
                
                ItemStack leftover = tryInsertItemStackToInventoryWithinSlotRange(invDst, movedStack, slotsDst, false);
                
                if (!leftover.isEmpty())
                {
                    invSrc.insertItem(slot, leftover, false);
                }

                movedSome = true;
                
                if (amountToMove < stackInSrc.getCount()) {
                    movedAll = false;
                }
            }
            else
            {
                movedAll = false;
            }
        }

        return movedAll ? InvResult.MOVED_ALL : (movedSome ? InvResult.MOVED_SOME : InvResult.MOVED_NOTHING);
    }

    public static InvResult tryMoveMatchingItems(IItemHandler invSrc, IItemHandler invDst)
    {
        return tryMoveMatchingItemsWithinSlotRange(invSrc, invDst, new SlotRange(invSrc), new SlotRange(invDst));
    }

    public static InvResult tryMoveMatchingItemsWithinSlotRange(IItemHandler invSrc, IItemHandler invDst, SlotRange slotsSrc, SlotRange slotsDst)
    {
        boolean movedAll = true;
        boolean movedSome = false;
        InvResult result = InvResult.MOVED_NOTHING;
        final int lastSlot = Math.min(slotsSrc.lastInc, invSrc.getSlots() - 1);

        for (int slot = slotsSrc.first; slot <= lastSlot; slot++)
        {
            ItemStack stack = invSrc.getStackInSlot(slot);

            if (stack.isEmpty() == false)
            {
                if (getSlotOfFirstMatchingItemStackWithinSlotRange(invDst, stack, slotsDst) != -1)
                {
                    result = tryMoveAllItemsWithinSlotRange(invSrc, invDst, new SlotRange(slot, 1), slotsDst);
                }

                if (result != InvResult.MOVED_NOTHING)
                {
                    movedSome = true;
                }
                else
                {
                    movedAll = false;
                }
            }
        }

        return movedAll ? InvResult.MOVED_ALL : (movedSome ? InvResult.MOVED_SOME : InvResult.MOVED_NOTHING);
    }

    public static InvResult fillStacksOfMatchingItems(IItemHandler invSrc, IItemHandler invDst)
    {
        return fillStacksOfMatchingItemsWithinSlotRange(invSrc, invDst, new SlotRange(invSrc), new SlotRange(invDst));
    }

    public static InvResult fillStacksOfMatchingItemsWithinSlotRange(IItemHandler invSrc, IItemHandler invDst, SlotRange slotsSrc, SlotRange slotsDst)
    {
        boolean movedAll = true;
        boolean movedSome = false;
        InvResult result = InvResult.MOVED_NOTHING;
        final int lastSlot = Math.min(slotsSrc.lastInc, invSrc.getSlots() - 1);

        for (int slot = slotsSrc.first; slot <= lastSlot; slot++)
        {
            ItemStack stack = invSrc.getStackInSlot(slot);

            if (stack.isEmpty() == false)
            {
                List<Integer> matchingSlots = getSlotNumbersOfMatchingStacksWithinSlotRange(invDst, stack, slotsDst);

                for (int dstSlot : matchingSlots)
                {
                    result = tryMoveAllItemsWithinSlotRange(invSrc, invDst, new SlotRange(slot, 1), new SlotRange(dstSlot, 1));

                    if (result != InvResult.MOVED_NOTHING)
                    {
                        movedSome = true;
                    }
                    else
                    {
                        movedAll = false;
                    }
                }
            }
        }

        return movedAll ? InvResult.MOVED_ALL : (movedSome ? InvResult.MOVED_SOME : InvResult.MOVED_NOTHING);
    }

    /**
     * Get the ItemStack that has the given UUID stored in its NBT.
     * If containerTagName is not null, then the UUID is read from a compound tag by that name.
     */
    public static ItemStack getItemStackByUUID(IItemHandler inv, UUID uuid, @Nullable String containerTagName)
    {
        final int invSize = inv.getSlots();

        for (int slot = 0; slot < invSize; slot++)
        {
            ItemStack stack = inv.getStackInSlot(slot);

            if (stack.isEmpty() == false && uuid.equals(NBTHelper.getUUIDFromItemStack(stack, containerTagName, false)))
            {
                return stack;
            }
        }

        return ItemStack.EMPTY;
    }



    public static ItemStack tryInsertItemStackToInventoryWithinSlotRange(IItemHandler inv, @Nonnull ItemStack stackIn, SlotRange slotRange, boolean simulate)
    {
        if (stackIn.isEmpty() || inv == null)
        {
            return stackIn;
        }

        final int lastSlot = Math.min(slotRange.lastInc, inv.getSlots() - 1);
        
        for (int slot = slotRange.first; slot <= lastSlot; slot++)
        {
            if (inv.getStackInSlot(slot).isEmpty() == false)
            {
                stackIn = inv.insertItem(slot, stackIn, simulate);

                if (stackIn.isEmpty())
                {
                    return ItemStack.EMPTY;
                }
            }
        }

        for (int slot = slotRange.first; slot <= lastSlot; slot++)
        {
            stackIn = inv.insertItem(slot, stackIn, simulate);

            if (stackIn.isEmpty())
            {
                return ItemStack.EMPTY;
            }
        }

        return stackIn;
    }

    public static ItemStack tryInsertItemStackToInventoryWithinSlotRange(IItemHandler inv, @Nonnull ItemStack stackIn, SlotRange slotRange)
    {
        return tryInsertItemStackToInventoryWithinSlotRange(inv, stackIn, slotRange, false);
    }

    public static ItemStack tryInsertItemStackToExistingStacksInInventory(IItemHandler inv, @Nonnull ItemStack stackIn)
    {
        List<Integer> slots = getSlotNumbersOfMatchingStacks(inv, stackIn);

        for (int slot : slots)
        {
            stackIn = inv.insertItem(slot, stackIn, false);

            if (stackIn.isEmpty())
            {
                return ItemStack.EMPTY;
            }
        }

        return stackIn;
    }




    public static int getFirstNonEmptySlot(IItemHandler inv)
    {
        for (int i = 0; i < inv.getSlots(); i++)
        {
            if (inv.getStackInSlot(i).isEmpty() == false)
            {
                return i;
            }
        }

        return -1;
    }

    public static int getSlotOfFirstMatchingItemStack(IItemHandler inv, @Nonnull ItemStack stackIn)
    {
        return getSlotOfFirstMatchingItemStackWithinSlotRange(inv, stackIn, new SlotRange(inv));
    }

    public static int getSlotOfFirstMatchingItemStackWithinSlotRange(IItemHandler inv, @Nonnull ItemStack stackIn, SlotRange slotRange)
    {
        final int lastSlot = Math.min(inv.getSlots() - 1, slotRange.lastInc);

        for (int slot = slotRange.first; slot <= lastSlot; slot++)
        {
            ItemStack stack = inv.getStackInSlot(slot);

            if (isSameItem(stack, stackIn))
            {
                return slot;
            }
        }

        return -1;
    }


    public static List<Integer> getSlotNumbersOfMatchingItems(IItemHandler inv, Item item)
    {
        return getSlotNumbersOfMatchingItems(inv, item, OreDictionary.WILDCARD_VALUE);
    }

    public static List<Integer> getSlotNumbersOfMatchingItems(IItemHandler inv, Item item, int meta)
    {
        List<Integer> slots = new ArrayList<Integer>();
        final int invSize = inv.getSlots();

        for (int slot = 0; slot < invSize; ++slot)
        {
            ItemStack stack = inv.getStackInSlot(slot);

            if (stack.isEmpty() == false && stack.getItem() == item &&
                (stack.getMetadata() == meta || meta == OreDictionary.WILDCARD_VALUE))
            {
                slots.add(Integer.valueOf(slot));
            }
        }

        return slots;
    }

    public static List<Integer> getSlotNumbersOfMatchingStacks(IItemHandler inv, @Nonnull ItemStack stackIn)
    {
        return getSlotNumbersOfMatchingStacksWithinSlotRange(inv, stackIn, new SlotRange(inv));
    }

    public static List<Integer> getSlotNumbersOfMatchingStacksWithinSlotRange(IItemHandler inv, @Nonnull ItemStack stackIn, SlotRange slotRange)
    {
        List<Integer> slots = new ArrayList<Integer>();
        final int lastSlot = Math.min(inv.getSlots() - 1, slotRange.lastInc);

        for (int slot = slotRange.first; slot <= lastSlot; slot++)
        {
            ItemStack stack = inv.getStackInSlot(slot);

            if (isSameItem(stack, stackIn))
            {
                slots.add(Integer.valueOf(slot));
            }
        }

        return slots;
    }




    public static ItemStack extractItemsFromSlot(IItemHandler inv, int slot, int amount)
    {
        ItemStack stackExtract = inv.extractItem(slot, amount, false);

        if (stackExtract.isEmpty())
        {
            return ItemStack.EMPTY;
        }

        while (stackExtract.getCount() < amount)
        {
            ItemStack stackTmp = inv.extractItem(slot, amount - stackExtract.getCount(), false);

            if (stackTmp.isEmpty())
            {
                break;
            }

            stackExtract.grow(stackTmp.getCount());
        }

        return stackExtract;
    }

    private static ItemStack collectItemsFromInventory(IItemHandler inv, @Nonnull ItemStack stackTemplate, int maxAmount, boolean reverse)
    {
        return collectItemsFromInventoryFromSlotRange(inv, stackTemplate, new SlotRange(inv), maxAmount, reverse);
    }

    private static ItemStack collectItemsFromInventoryFromSlotRange(IItemHandler inv, @Nonnull ItemStack stackTemplate,
            SlotRange range, int amount, boolean reverse)
    {
        if (range.first >= inv.getSlots())
        {
            return ItemStack.EMPTY;
        }

        int inc = reverse ? -1 : 1;
        final int lastSlot = Math.min(range.lastInc, inv.getSlots() - 1);
        final int start = reverse ? lastSlot : range.first;
        ItemStack stack = stackTemplate.copy();
        stack.setCount(0);

        for (int slot = start; slot >= range.first && slot <= lastSlot && stack.getCount() < amount; slot += inc)
        {
            ItemStack stackTmp = inv.getStackInSlot(slot);

            if (stackTmp.isEmpty())
            {
                continue;
            }

            if (isSameItem(stackTmp, stackTemplate))
            {
                stackTmp = extractItemsFromSlot(inv, slot, amount - stack.getCount());

                if (stackTmp.isEmpty() == false)
                {
                    stack.grow(stackTmp.getCount());
                }
            }
        }

        return stack.getCount() > 0 ? stack : ItemStack.EMPTY;
    }


    public static void leaveOneFullStackOfEveryItem(IItemHandler invTarget, IItemHandler invStorage, boolean reverse)
    {
        leaveOneFullStackOfEveryItemWithinSlotRange(invTarget, invStorage, new SlotRange(invTarget), reverse);
    }

    public static void leaveOneFullStackOfEveryItemWithinSlotRange(IItemHandler invTarget, IItemHandler invStorage, SlotRange slotRange, boolean reverse)
    {
        final int inc = (reverse ? -1 : 1);
        final int start = (reverse ? Math.min(slotRange.lastInc, invTarget.getSlots() - 1) : slotRange.first);
        final int end = (reverse ? slotRange.first : Math.min(slotRange.lastInc, invTarget.getSlots() - 1));

        for (int slot = start; (reverse ? slot >= end : slot <= end); slot += inc)
        {
            ItemStack stack = invTarget.getStackInSlot(slot);

            if (stack.isEmpty())
            {
                continue;
            }

            int maxSize = invTarget.getSlotLimit(slot);

            List<Integer> matchingSlots = getSlotNumbersOfMatchingStacksWithinSlotRange(invTarget, stack, slotRange);

            if (matchingSlots.size() > 1)
            {
                for (int tmp : matchingSlots)
                {
                    if (tmp == slot)
                    {
                        continue;
                    }

                    while (true)
                    {
                        stack = invTarget.extractItem(tmp, maxSize, false);

                        if (stack.isEmpty())
                        {
                            break;
                        }

                        stack = invTarget.insertItem(slot, stack, false);

                        if (stack.isEmpty() == false)
                        {
                            break;
                        }
                    }

                    while (stack.isEmpty() == false)
                    {
                        stack = tryInsertItemStackToInventoryWithinSlotRange(invStorage, stack, new SlotRange(invStorage), false);

                        if (stack.isEmpty() == false)
                        {
                            tryInsertItemStackToInventoryWithinSlotRange(invTarget, stack, new SlotRange(invTarget), false);
                            return;
                        }

                        stack = invTarget.extractItem(tmp, maxSize, false);
                    }
                }
            }

            stack = invTarget.getStackInSlot(slot);

            if (stack.isEmpty() == false)
            {
                maxSize = stack.getMaxStackSize();

                if (stack.getCount() < maxSize)
                {
                    ItemStack stackTmp = collectItemsFromInventory(invStorage, stack, maxSize - stack.getCount(), true);

                    if (stackTmp.isEmpty() == false)
                    {
                        stackTmp = invTarget.insertItem(slot, stackTmp, false);

                        if (stackTmp.isEmpty() == false)
                        {
                            tryInsertItemStackToInventoryWithinSlotRange(invStorage, stackTmp, new SlotRange(invStorage), false);
                        }
                    }
                }
            }
        }
    }



    public static void sortInventoryWithinRange(IItemHandlerModifiable inv, SlotRange range)
    {
        sortInventoryInternal(inv, range);
    }

    private static void sortInventoryInternal(IItemHandlerModifiable inv, SlotRange range)
    {
        final int lastSlot = Math.min(range.lastInc, inv.getSlots() - 1);
        
        List<ItemStack> backupSnapshot = new ArrayList<>();
        for (int i = 0; i < inv.getSlots(); i++)
        {
            ItemStack stack = inv.getStackInSlot(i);
            backupSnapshot.add(stack.isEmpty() ? ItemStack.EMPTY : stack.copy());
        }

        try
        {
            java.util.Map<ItemType, Integer> countMap = new java.util.LinkedHashMap<>();

            for (int i = range.first; i <= lastSlot; i++)
            {
                ItemStack stack = inv.getStackInSlot(i);
                if (!stack.isEmpty())
                {
                    ItemType type = new ItemType(stack);
                    int currentCount = countMap.getOrDefault(type, 0);
                    countMap.put(type, currentCount + stack.getCount());
                }
            }

            List<ItemType> blocks = new ArrayList<>();
            List<ItemType> items = new ArrayList<>();
            
            for (ItemType type : countMap.keySet())
            {
                if (type.getStack().getItem() instanceof net.minecraft.item.ItemBlock) blocks.add(type);
                else items.add(type);
            }
            
            java.util.Comparator<ItemType> comparator = (t1, t2) -> {
                net.minecraft.util.ResourceLocation r1 = t1.getStack().getItem().getRegistryName();
                net.minecraft.util.ResourceLocation r2 = t2.getStack().getItem().getRegistryName();
                String s1 = r1 != null ? r1.toString() : "";
                String s2 = r2 != null ? r2.toString() : "";
                int c = s1.compareTo(s2);
                if (c != 0) return c;
                return Integer.compare(t1.getStack().getMetadata(), t2.getStack().getMetadata());
            };
            blocks.sort(comparator);
            items.sort(comparator);

            for (int i = range.first; i <= lastSlot; i++)
            {
                inv.setStackInSlot(i, ItemStack.EMPTY);
            }

            int currentSlot = range.first;
            currentSlot = fillSortedItems(inv, blocks, countMap, currentSlot, lastSlot);
            fillSortedItems(inv, items, countMap, currentSlot, lastSlot);
        }
        catch (Exception e)
        {
            tf.storage.TFStorage.logger.error("库存排序时发生严重错误！正在恢复备份...", e);
            
            for (int i = 0; i < inv.getSlots(); i++)
            {
                inv.setStackInSlot(i, backupSnapshot.get(i));
            }
        }
    }

    private static int fillSortedItems(IItemHandlerModifiable inv, List<ItemType> types, java.util.Map<ItemType, Integer> countMap, int startSlot, int endSlot)
    {
        int currentSlot = startSlot;
        
        boolean isLargeStackInv = (inv instanceof IItemHandlerSize);

        for (ItemType type : types)
        {
            int totalCount = countMap.get(type);
            ItemStack template = type.getStack();
            
            int itemMaxStack = template.getMaxStackSize();
            
            int effectiveStackLimit;
            
            if (isLargeStackInv) {
                effectiveStackLimit = ((IItemHandlerSize) inv).getItemStackLimit(currentSlot, template);
            } else {
                effectiveStackLimit = Math.min(itemMaxStack, inv.getSlotLimit(currentSlot));
            }

            while (totalCount > 0)
            {
                if (currentSlot > endSlot)
                {
                    tf.storage.TFStorage.logger.warn("StackHelper: 排序溢出！丢失了 " + totalCount + " 个 " + template.getDisplayName());
                    break;
                }

                int currentSlotLimit = inv.getSlotLimit(currentSlot);
                
                int toAdd = Math.min(totalCount, Math.min(effectiveStackLimit, currentSlotLimit));

                ItemStack newStack = template.copy();
                newStack.setCount(toAdd);
                inv.setStackInSlot(currentSlot, newStack);

                totalCount -= toAdd;
                currentSlot++;
            }
        }
        return currentSlot;
    }

    public static boolean isSameItem(ItemStack stack1, ItemStack stack2)
    {
        return ItemHandlerHelper.canItemStacksStack(stack1, stack2);
    }


    public static enum InvResult
    {
        MOVED_NOTHING,
        MOVED_SOME,
        MOVED_ALL;
    }
}
