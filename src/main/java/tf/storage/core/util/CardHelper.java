package tf.storage.core.util;

import java.util.List;
import net.minecraft.world.item.ItemStack;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.registries.ForgeRegistries;

/**
 * CardHelper - 存储卡操作工具类
 * 
 * 适配 Minecraft 1.20.1
 */
public class CardHelper
{
    // 存储卡物品的注册名，需要在物品注册后设置
    private static String memoryCardId = "tfstorage:tfunit";
    
    public static void setMemoryCardId(String id) {
        memoryCardId = id;
    }
    
    private static String getMemoryCardId()
    {
        return memoryCardId;
    }

    public static int getInstalledMemoryCardCount(ItemStack containerStack)
    {
        ListTag nbtTagList = NBTHelper.getTagList(containerStack, null, "MemoryCards",
                Tag.TAG_COMPOUND, false);
        if (nbtTagList == null)
        {
            return 0;
        }

        int count = 0;
        int listNumStacks = nbtTagList.size();

        for (int i = 0; i < listNumStacks; i++)
        {
            CompoundTag tag = nbtTagList.getCompound(i);
            ItemStack moduleStack = NBTHelper.loadItemStackFromTag(tag);
            if (isTFUnit(moduleStack))
            {
                count++;
            }
        }

        return count;
    }

    public static ItemStack getSelectedMemoryCardStack(ItemStack containerStack)
    {
        ListTag nbtTagList = NBTHelper.getTagList(containerStack, null, "MemoryCards",
                Tag.TAG_COMPOUND, false);

        if (nbtTagList == null)
        {
            return ItemStack.EMPTY;
        }

        int listNumStacks = nbtTagList.size();
        int selected = getStoredMemoryCardSelection(containerStack);

        for (int i = 0; i < listNumStacks; ++i)
        {
            CompoundTag tag = nbtTagList.getCompound(i);
            if (tag.contains("Slot") && tag.getByte("Slot") == selected)
            {
                ItemStack memoryCardStack = NBTHelper.loadItemStackFromTag(tag);
                if (isTFUnit(memoryCardStack))
                {
                    return memoryCardStack;
                }
            }
        }

        return ItemStack.EMPTY;
    }

    public static boolean setSelectedMemoryCardStack(ItemStack containerStack, ItemStack memoryCardStack)
    {
        ListTag nbtTagList = NBTHelper.getTagList(containerStack, null, "MemoryCards",
                Tag.TAG_COMPOUND, true);

        int listNumStacks = nbtTagList.size();
        int selected = getStoredMemoryCardSelection(containerStack);

        for (int i = 0; i < listNumStacks; ++i)
        {
            CompoundTag moduleTag = nbtTagList.getCompound(i);
            if (moduleTag.contains("Slot") && moduleTag.getByte("Slot") == selected)
            {
                ItemStack existing = NBTHelper.loadItemStackFromTag(moduleTag);
                if (isTFUnit(existing))
                {
                    moduleTag.remove("tag");
                    memoryCardStack.save(moduleTag);
                    moduleTag.putByte("Slot", (byte)selected);
                    return true;
                }
            }
        }

        if (!memoryCardStack.isEmpty())
        {
            CompoundTag newTag = new CompoundTag();
            memoryCardStack.save(newTag);
            newTag.putByte("Slot", (byte)selected);
            nbtTagList.add(newTag);
            return true;
        }

        return false;
    }

    public static void setMemoryCardSelection(ItemStack containerStack, int index, int maxMemoryCards)
    {
        if (containerStack.isEmpty() == false)
        {
            CompoundTag nbt = NBTHelper.getCompoundTag(containerStack, null, true);

            index = Math.max(0, Math.min(index, maxMemoryCards - 1));

            nbt.putByte("SelectedMemoryCard", (byte) index);
        }
    }

    public static void setMemoryCardSelection(ItemStack containerStack, int index)
    {
        setMemoryCardSelection(containerStack, index, 8);
    }

    public static int getStoredMemoryCardSelection(ItemStack containerStack, int maxMemoryCards)
    {
        if (containerStack.isEmpty() || containerStack.getTag() == null)
        {
            return 0;
        }

        int selected = containerStack.getTag().getByte("SelectedMemoryCard");

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

    /**
     * 检查物品是否为有效的TF单元存储卡
     * 注意：此方法需要在物品系统迁移后更新以使用正确的类型检查
     */
    public static boolean isTFUnit(ItemStack stack)
    {
        if (stack.isEmpty()) return false;
        
        ResourceLocation rl = ForgeRegistries.ITEMS.getKey(stack.getItem());
        if (rl == null) return false;

        // 允许所有 tfunit_* 作为存储卡
        return "tfstorage".equals(rl.getNamespace()) && rl.getPath().startsWith("tfunit_");
    }

    public static int getFormattedItemListFromContainerItem(ItemStack containerStack, List<String> listLines, int maxLines)
    {
        int itemCount = 0;
        int overflow = 0;
        String preWhite = "\u00A7f"; // White color
        String rst = "\u00A7r\u00A77"; // Reset + Gray
        ListTag nbtTagList = NBTHelper.getStoredItemsList(containerStack, false);

        if (nbtTagList != null && nbtTagList.size() > 0)
        {
            int num = nbtTagList.size();

            for (int i = 0; i < num; ++i)
            {
                CompoundTag tag = nbtTagList.getCompound(i);
                ItemStack tmpStack = NBTHelper.loadItemStackFromTag(tag);
                
                if (!tmpStack.isEmpty())
                {
                    int stackSize = tmpStack.getCount();
                    itemCount += stackSize;

                    if (i < maxLines)
                    {
                        listLines.add("  " + preWhite + stackSize + rst + " " + tmpStack.getHoverName().getString());
                    }
                    else
                    {
                        overflow++;
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
