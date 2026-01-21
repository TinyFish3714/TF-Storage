package tf.storage.util;

import java.util.List;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;
import net.minecraft.util.text.TextFormatting;
import tf.storage.item.TFUnit;
import tf.storage.core.ModItems;

public class CardHelper
{
    private static String getMemoryCardId()
    {
        net.minecraft.util.ResourceLocation rl = ModItems.MEMORY_CARD.getRegistryName();
        return rl != null ? rl.toString() : "";
    }

    public static int getInstalledMemoryCardCount(ItemStack containerStack)
    {
        NBTTagList nbtTagList = NBTHelper.getTagList(containerStack, null, "MemoryCards",
                net.minecraftforge.common.util.Constants.NBT.TAG_COMPOUND, false);
        if (nbtTagList == null)
        {
            return 0;
        }

        int count = 0;
        int listNumStacks = nbtTagList.tagCount();

        for (int i = 0; i < listNumStacks; i++)
        {
            NBTTagCompound tag = nbtTagList.getCompoundTagAt(i);
            String id = tag.getString("id");
            if (id.equals(getMemoryCardId()))
            {
                ItemStack moduleStack = NBTHelper.loadItemStackFromTag(tag);
                if (isTFUnit(moduleStack))
                {
                    count++;
                }
            }
        }

        return count;
    }

    public static ItemStack getSelectedMemoryCardStack(ItemStack containerStack)
    {
        NBTTagList nbtTagList = NBTHelper.getTagList(containerStack, null, "MemoryCards",
                net.minecraftforge.common.util.Constants.NBT.TAG_COMPOUND, false);

        if (nbtTagList == null)
        {
            return ItemStack.EMPTY;
        }

        int listNumStacks = nbtTagList.tagCount();
        int selected = getStoredMemoryCardSelection(containerStack);

        for (int i = 0; i < listNumStacks; ++i)
        {
            NBTTagCompound tag = nbtTagList.getCompoundTagAt(i);
            String id = tag.getString("id");

            if (id.equals(getMemoryCardId()))
            {
                if (tag.hasKey("Slot") && tag.getByte("Slot") == selected)
                {
                    ItemStack memoryCardStack = NBTHelper.loadItemStackFromTag(tag);
                    if (isTFUnit(memoryCardStack))
                    {
                        return memoryCardStack;
                    }
                }
            }
        }

        return ItemStack.EMPTY;
    }

    public static boolean setSelectedMemoryCardStack(ItemStack containerStack, ItemStack memoryCardStack)
    {
        NBTTagList nbtTagList = NBTHelper.getTagList(containerStack, null, "MemoryCards",
                net.minecraftforge.common.util.Constants.NBT.TAG_COMPOUND, true);

        int listNumStacks = nbtTagList.tagCount();
        int selected = getStoredMemoryCardSelection(containerStack);

        for (int i = 0; i < listNumStacks; ++i)
        {
            NBTTagCompound moduleTag = nbtTagList.getCompoundTagAt(i);
            String id = moduleTag.getString("id");

            if (id.equals(getMemoryCardId()))
            {
                if (moduleTag.hasKey("Slot") && moduleTag.getByte("Slot") == selected)
                {
                    moduleTag.removeTag("tag");
                    memoryCardStack.writeToNBT(moduleTag);
                    moduleTag.setByte("Slot", (byte)selected);
                    return true;
                }
            }
        }

        if (!memoryCardStack.isEmpty())
        {
            NBTTagCompound newTag = new NBTTagCompound();
            memoryCardStack.writeToNBT(newTag);
            newTag.setByte("Slot", (byte)selected);
            nbtTagList.appendTag(newTag);
            return true;
        }

        return false;
    }

    public static void setMemoryCardSelection(ItemStack containerStack, int index, int maxMemoryCards)
    {
        if (containerStack.isEmpty() == false)
        {
            NBTTagCompound nbt = NBTHelper.getCompoundTag(containerStack, null, true);

            index = Math.max(0, Math.min(index, maxMemoryCards - 1));

            nbt.setByte("SelectedMemoryCard", (byte) index);
        }
    }

    public static void setMemoryCardSelection(ItemStack containerStack, int index)
    {
        setMemoryCardSelection(containerStack, index, 8);
    }

    public static int getStoredMemoryCardSelection(ItemStack containerStack, int maxMemoryCards)
    {
        if (containerStack.isEmpty() || containerStack.getTagCompound() == null)
        {
            return 0;
        }

        int selected = containerStack.getTagCompound().getByte("SelectedMemoryCard");

        return Math.min(selected, maxMemoryCards - 1);
    }

    public static int getStoredMemoryCardSelection(ItemStack containerStack)
    {
        return getStoredMemoryCardSelection(containerStack, 8);
    }

    public static int getClampedMemoryCardSelection(ItemStack containerStack)
    {
        return getStoredMemoryCardSelection(containerStack);
    }

    public static boolean isTFUnit(ItemStack stack)
    {
        return !stack.isEmpty() &&
               stack.getItem() instanceof TFUnit &&
               ((TFUnit) stack.getItem()).getTFUnitTier(stack) >= TFUnit.TIER_6B &&
               ((TFUnit) stack.getItem()).getTFUnitTier(stack) <= TFUnit.TIER_12B;
    }

    public static int getFormattedItemListFromContainerItem(ItemStack containerStack, List<String> listLines, int maxLines)
    {
        int itemCount = 0;
        int overflow = 0;
        String preWhite = tf.storage.core.ModInfo.Colors.INFO;
        String rst = tf.storage.core.ModInfo.Colors.RESET + tf.storage.core.ModInfo.Colors.GRAY;
        NBTTagList nbtTagList = NBTHelper.getStoredItemsList(containerStack, false);

        if (nbtTagList != null && nbtTagList.tagCount() > 0)
        {
            int num = nbtTagList.tagCount();

            for (int i = 0; i < num; ++i)
            {
                NBTTagCompound tag = nbtTagList.getCompoundTagAt(i);
                String id = tag.getString("id");

                if (id.equals(getMemoryCardId()))
                {
                    ItemStack tmpStack = NBTHelper.loadItemStackFromTag(tag);
                    if (!tmpStack.isEmpty())
                    {
                        int stackSize = tmpStack.getCount();
                        itemCount += stackSize;

                        if (i < maxLines)
                        {
                            listLines.add("  " + preWhite + stackSize + rst + " " + tmpStack.getDisplayName());
                        }
                        else
                        {
                            overflow++;
                        }
                    }
                }
                else
                {
                    ItemStack tmpStack = NBTHelper.loadItemStackFromTag(tag);
                    if (!tmpStack.isEmpty())
                    {
                        int stackSize = tmpStack.getCount();
                        itemCount += stackSize;

                        if (i < maxLines)
                        {
                            listLines.add("  " + preWhite + stackSize + rst + " " + tmpStack.getDisplayName());
                        }
                        else
                        {
                            overflow++;
                        }
                    }
                }
            }
        }

        if (overflow > 0)
        {
            listLines.add("     ... 以及" + preWhite + overflow + rst + " 个更多堆栈未列出");
        }

        return itemCount;
    }
}
