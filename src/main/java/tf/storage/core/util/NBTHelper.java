package tf.storage.core.util;

import java.util.List;
import java.util.ArrayList;
import java.util.Set;
import java.util.UUID;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import com.google.common.collect.Sets;
import net.minecraft.world.item.ItemStack;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.core.NonNullList;
import net.minecraft.nbt.NbtUtils;

/**
 * NBTHelper - 统一的 NBT 操作管理器
 * 提供统一的 ItemStack 和 CompoundTag 操作接口
 * 
 * 适配 Minecraft 1.20.1
 */
public class NBTHelper
{
    @Nullable
    public static CompoundTag writeTagToNBT(@Nullable CompoundTag nbt, @Nonnull String name, @Nullable Tag tag)
    {
        if (nbt == null)
        {
            if (tag == null)
            {
                return nbt;
            }

            nbt = new CompoundTag();
        }

        if (tag == null)
        {
            nbt.remove(name);
        }
        else
        {
            nbt.put(name, tag);
        }

        return nbt;
    }

    /**
     * 设置 ItemStack 的根复合标签。空复合标签将被完全移除。
     */
    @Nonnull
    public static ItemStack setRootCompoundTag(@Nonnull ItemStack stack, @Nullable CompoundTag nbt)
    {
        if (nbt != null && nbt.isEmpty())
        {
            nbt = null;
        }

        stack.setTag(nbt);
        return stack;
    }

    /**
     * 获取 ItemStack 的根复合标签。
     * 如果不存在，并且 <b>create</b> 为 true，则会创建并添加，否则返回 null。
     */
    @Nullable
    public static CompoundTag getRootCompoundTag(@Nonnull ItemStack stack, boolean create)
    {
        CompoundTag nbt = stack.getTag();

        if (create == false)
        {
            return nbt;
        }

        if (nbt == null)

        {
            nbt = new CompoundTag();
            stack.setTag(nbt);
        }

        return nbt;
    }

    /**
     * 通过给定的名称从另一个复合标签 <b>nbt</b> 中获取复合标签 <b>tagName</b>。
     * 如果不存在，并且 <b>create</b> 为 true，则会创建并添加，否则返回 null。
     */
    @Nullable
    public static CompoundTag getCompoundTag(@Nullable CompoundTag nbt, @Nonnull String tagName, boolean create)
    {
        if (nbt == null)
        {
            return null;
        }

        if (create == false)
        {
            return nbt.contains(tagName, Tag.TAG_COMPOUND) ? nbt.getCompound(tagName) : null;
        }

        if (nbt.contains(tagName, Tag.TAG_COMPOUND) == false)

        {
            nbt.put(tagName, new CompoundTag());
        }

        return nbt.getCompound(tagName);
    }

    /**
     * 通过给定的名称 <b>tagName</b> 返回复合标签。如果 <b>tagName</b> 为 null，
     * 则返回根复合标签。如果 <b>create</b> 为 <b>false</b>
     * 且标签不存在，则返回 null 且不创建标签。
     * 如果 <b>create</b> 为 <b>true</b>，则在必要时创建并添加标签。
     */
    @Nullable
    public static CompoundTag getCompoundTag(@Nonnull ItemStack stack, @Nullable String tagName, boolean create)
    {
        CompoundTag nbt = getRootCompoundTag(stack, create);

        if (tagName != null)
        {
            nbt = getCompoundTag(nbt, tagName, create);
        }

        return nbt;
    }

    /**
     * 从另一个复合标签 <b>containerTagName</b> 中获取名为 <b>tagName</b> 的嵌套复合标签。
     * 如果某些标签不存在，并且 <b>create</b> 为 true，则会创建并添加，否则返回 null。
     */
    @Nullable
    public static CompoundTag getCompoundTag(@Nonnull ItemStack stack, @Nullable String containerTagName,
            @Nonnull String tagName, boolean create)
    {
        CompoundTag nbt = getRootCompoundTag(stack, create);

        if (containerTagName != null)
        {
            nbt = getCompoundTag(nbt, containerTagName, create);
        }

        return getCompoundTag(nbt, tagName, create);
    }



    /**
     * 从给定的 ItemStack 中获取存储的 UUID。如果 <b>containerTagName</b> 不为 null，
     * 则从该名称的复合标签中读取 UUID。
     * 如果 <b>create</b> 为 true 且未找到 UUID，则会创建并添加一个新的随机 UUID。
     * 如果 <b>create</b> 为 false 且未找到 UUID，则返回 null。
     */
    @Nullable
    public static UUID getUUIDFromItemStack(@Nonnull ItemStack stack, @Nullable String containerTagName, boolean create)
    {
        CompoundTag nbt = getCompoundTag(stack, containerTagName, create);
        if (nbt == null) return null;

        UUID uuid = getUUIDFromNBT(nbt);

        if (uuid == null && create)
        {
            uuid = UUID.randomUUID();
            nbt.putUUID("UUID", uuid);
        }

        return uuid;
    }

    /**
     * 从给定的复合标签中获取存储的 UUID。如果未找到，则返回 null。
     */
    @Nullable
    public static UUID getUUIDFromNBT(@Nullable CompoundTag nbt)
    {
        if (nbt != null && nbt.hasUUID("UUID"))
        {
            return nbt.getUUID("UUID");
        }

        return null;
    }

    /**
     * 将给定的 UUID 存储到给定的 ItemStack 中。如果 <b>containerTagName</b> 不为 null，
     * 则 UUID 存储在该名称的复合标签内。否则直接存储在根复合标签内。
     */
    public static void setUUID(@Nonnull ItemStack stack, @Nullable String containerTagName, @Nonnull UUID uuid)
    {
        CompoundTag nbt = getCompoundTag(stack, containerTagName, true);
        nbt.putUUID("UUID", uuid);
    }


    /**
     * 返回标签 <b>tagName</b> 中的布尔值，如果不存在则返回 false。
     * 如果 <b>containerTagName</b> 不为 null，则从该名称的复合标签中获取值。
     */
    public static boolean getBoolean(@Nonnull ItemStack stack, @Nullable String containerTagName, @Nonnull String tagName)
    {
        CompoundTag nbt = getCompoundTag(stack, containerTagName, false);
        return nbt != null ? nbt.getBoolean(tagName) : false;
    }

    public static void setBoolean(@Nonnull ItemStack stack, @Nullable String containerTagName, @Nonnull String tagName, boolean value)
    {
        getCompoundTag(stack, containerTagName, true).putBoolean(tagName, value);
    }

    public static void toggleBoolean(@Nonnull CompoundTag nbt, @Nonnull String tagName)
    {
        nbt.putBoolean(tagName, ! nbt.getBoolean(tagName));
    }

    /**
     * 切换给定 ItemStack NBT 中标签 <b>tagName</b> 的布尔值。如果 <b>containerTagName</b>
     * 不为 null，则该值存储在该名称的复合标签内。
     */
    public static void toggleBoolean(@Nonnull ItemStack stack, @Nullable String containerTagName, @Nonnull String tagName)
    {
        CompoundTag nbt = getCompoundTag(stack, containerTagName, true);
        toggleBoolean(nbt, tagName);
    }

    /**
     * 返回标签 <b>tagName</b> 中的字节值，如果不存在则返回 0。
     * 如果 <b>containerTagName</b> 不为 null，则从该名称的复合标签中获取值。
     */
    public static byte getByte(@Nonnull ItemStack stack, @Nullable String containerTagName, @Nonnull String tagName)
    {
        CompoundTag nbt = getCompoundTag(stack, containerTagName, false);
        return nbt != null ? nbt.getByte(tagName) : 0;
    }

    /**
     * 在给定 ItemStack NBT 的标签 <b>tagName</b> 中设置字节值。如果 <b>containerTagName</b>
     * 不为 null，则该值存储在该名称的复合标签内。
     */
    public static void setByte(@Nonnull ItemStack stack, @Nullable String containerTagName, @Nonnull String tagName, byte value)
    {
        CompoundTag nbt = getCompoundTag(stack, containerTagName, true);
        nbt.putByte(tagName, value);
    }


    /**
     * 循环给定 ItemStack NBT 中标签 <b>tagName</b> 的字节值。如果 <b>containerTagName</b>
     * 不为 null，则该值存储在该名称的复合标签内。
     */
    public static void cycleByteValue(@Nonnull ItemStack stack, @Nullable String containerTagName,
            @Nonnull String tagName, int minValue, int maxValue, boolean reverse)
    {
        CompoundTag nbt = getCompoundTag(stack, containerTagName, true);
        byte value = nbt.getByte(tagName);

        if (reverse)
        {
            if (--value < minValue)
            {
                value = (byte)maxValue;
            }
        }
        else
        {
            if (++value > maxValue)
            {
                value = (byte)minValue;
            }
        }

        nbt.putByte(tagName, value);
    }

    /**
     * 循环给定 ItemStack NBT 中标签 <b>tagName</b> 的字节值。如果 <b>containerTagName</b>
     * 不为 null，则该值存储在该名称的复合标签内。
     * 范围的低端为 0。
     */
    public static void cycleByteValue(@Nonnull ItemStack stack, @Nullable String containerTagName,
            @Nonnull String tagName, int maxValue, boolean reverse)
    {
        cycleByteValue(stack, containerTagName, tagName, 0, maxValue, reverse);
    }

    /**
     * 循环给定 ItemStack NBT 中标签 <b>tagName</b> 的字节值。如果 <b>containerTagName</b>
     * 不为 null，则该值存储在该名称的复合标签内。
     * 范围的低端为 0。
     */
    public static void cycleByteValue(@Nonnull ItemStack stack, @Nullable String containerTagName,
            @Nonnull String tagName, int maxValue)
    {
        cycleByteValue(stack, containerTagName, tagName, maxValue, false);
    }

    /**
     * 返回标签 <b>tagName</b> 中的短整型值，如果不存在则返回 0。
     * 如果 <b>containerTagName</b> 不为 null，则从该名称的复合标签中获取值。
     */
    public static short getShort(@Nonnull ItemStack stack, @Nullable String containerTagName, @Nonnull String tagName)
    {
        CompoundTag nbt = getCompoundTag(stack, containerTagName, false);
        return nbt != null ? nbt.getShort(tagName) : 0;
    }

    /**
     * 在给定 ItemStack NBT 的标签 <b>tagName</b> 中设置短整型值。如果 <b>containerTagName</b>
     * 不为 null，则该值存储在该名称的复合标签内。
     */
    public static void setShort(@Nonnull ItemStack stack, @Nullable String containerTagName, @Nonnull String tagName, short value)
    {
        CompoundTag nbt = getCompoundTag(stack, containerTagName, true);
        nbt.putShort(tagName, value);
    }

    /**
     * 返回标签 <b>tagName</b> 中的整型值，如果不存在则返回 0。
     * 如果 <b>containerTagName</b> 不为 null，则从该名称的复合标签中获取值。
     */
    public static int getInteger(@Nonnull ItemStack stack, @Nullable String containerTagName, @Nonnull String tagName)
    {
        CompoundTag nbt = getCompoundTag(stack, containerTagName, false);
        return nbt != null ? nbt.getInt(tagName) : 0;
    }

    /**
     * 在给定 ItemStack NBT 的标签 <b>tagName</b> 中设置整型值。如果 <b>containerTagName</b>
     * 不为 null，则该值存储在该名称的复合标签内。
     */
    public static void setInteger(@Nonnull ItemStack stack, @Nullable String containerTagName, @Nonnull String tagName, int value)
    {
        CompoundTag nbt = getCompoundTag(stack, containerTagName, true);
        nbt.putInt(tagName, value);
    }

    /**
     * 返回标签 <b>tagName</b> 中的长整型值，如果不存在则返回 0。
     * 如果 <b>containerTagName</b> 不为 null，则从该名称的复合标签中获取值。
     */
    public static long getLong(@Nonnull ItemStack stack, @Nullable String containerTagName, @Nonnull String tagName)
    {
        CompoundTag nbt = getCompoundTag(stack, containerTagName, false);
        return nbt != null ? nbt.getLong(tagName) : 0;
    }

    /**
     * 在给定 ItemStack NBT 的标签 <b>tagName</b> 中设置短整型值。如果 <b>containerTagName</b>
     * 不为 null，则该值存储在该名称的复合标签内。
     */
    public static void setLong(@Nonnull ItemStack stack, @Nullable String containerTagName, @Nonnull String tagName, long value)
    {
        CompoundTag nbt = getCompoundTag(stack, containerTagName, true);
        nbt.putLong(tagName, value);
    }

    /**
     * 返回键 <b>tagName</b> 的 ListTag，如果未找到则创建并添加它。
     * 如果 <b>containerTagName</b> 不为 null，则从该名称的复合标签中获取。
     * @param containerStack
     * @param containerTagName 包含 ListTag 的复合标签名称，如果直接在根复合标签内则为 null
     * @param tagName ListTag 的名称/键
     * @param tagType 列表包含的标签类型
     * @param create true = 如果未找到则创建标签，false = 不创建标签
     * @return 请求的 ListTag（如果 <b>create</b> 为 true 则在必要时创建并添加），或 null（如果 <b>create</b> 为 false）
     */
    @Nullable
    public static ListTag getTagList(@Nonnull ItemStack containerStack, @Nullable String containerTagName,
            @Nonnull String tagName, int tagType, boolean create)
    {
        CompoundTag nbt = getCompoundTag(containerStack, containerTagName, create);

        if (create && nbt.contains(tagName, Tag.TAG_LIST) == false)
        {
            nbt.put(tagName, new ListTag());
        }

        return nbt != null ? nbt.getList(tagName, tagType) : null;
    }

    /**
     * 将给定的 <b>tagList</b> 写入 ItemStack containerStack。
     * 如有必要，创建复合标签。
     */
    public static void setTagList(@Nonnull ItemStack containerStack, @Nullable String containerTagName,
            @Nonnull String tagName, @Nonnull ListTag tagList)
    {
        CompoundTag nbt = getCompoundTag(containerStack, containerTagName, true);
        nbt.put(tagName, tagList);
    }

    /**
     * 返回包含 containerStack 中所有存储 ItemStack 的 ListTag。
     * 如果 ListTag 不存在且 <b>create</b> 为 true，则创建并添加该标签。
     * @param containerStack
     * @return 包含存储物品的 ListTag，如果不存在且 <b>create</b> 为 false 则返回 null
     */
    @Nullable
    public static ListTag getStoredItemsList(@Nonnull ItemStack containerStack, boolean create)
    {
        return getTagList(containerStack, null, "Items", Tag.TAG_COMPOUND, create);
    }


    /**
     * 从给定的复合标签读取 ItemStack，包括自定义堆叠大小。
     * @param tag
     * @return
     */
    @Nonnull
    public static ItemStack loadItemStackFromTag(@Nonnull CompoundTag tag)
    {
        ItemStack stack = ItemStack.of(tag);

        if (tag.contains("ActualCount", Tag.TAG_INT))
        {
            stack.setCount(tag.getInt("ActualCount"));
        }

        return stack.isEmpty() ? ItemStack.EMPTY : stack;
    }

    @Nonnull
    public static CompoundTag storeItemStackInTag(@Nonnull ItemStack stack, @Nonnull CompoundTag tag)
    {
        if (stack.isEmpty() == false)
        {
            stack.save(tag);

            if (stack.getCount() > 127)
            {
                tag.putByte("Count", (byte) (stack.getCount() & 0x7F));
                tag.putInt("ActualCount", stack.getCount());
            }
        }

        return tag;
    }

    /**
     * 从提供的 CompoundTag 读取存储的物品，从名为 <b>tagName</b> 的 ListTag 中读取
     * 并将它们写入提供的 ItemStack 列表 <b>items</b>。<br>
     * <b>注意：</b> 列表应初始化为足够大以容纳所有要读取的堆叠！
     * @param tag
     * @param items
     * @param tagName
     */
    public static void readStoredItemsFromTag(@Nonnull CompoundTag nbt, NonNullList<ItemStack> items, @Nonnull String tagName)
    {
        if (nbt.contains(tagName, Tag.TAG_LIST) == false)
        {
            return;
        }

        ListTag nbtTagList = nbt.getList(tagName, Tag.TAG_COMPOUND);
        int num = nbtTagList.size();
        int listSize = items.size();

        for (int i = 0; i < num; ++i)
        {
            CompoundTag tag = nbtTagList.getCompound(i);
            int slotNum = tag.getShort("Slot");

            if (slotNum >= 0 && slotNum < listSize)
            {
                items.set(slotNum, loadItemStackFromTag(tag));
            }
        }
    }


    /**
     * 将 <b>items</b> 中的 ItemStack 写入新的 ListTag 并返回该列表。
     * @param items
     */
    @Nonnull
    public static ListTag createTagListForItems(NonNullList<ItemStack> items)
    {
        ListTag nbtTagList = new ListTag();
        final int invSlots = items.size();

        for (int slotNum = 0; slotNum < invSlots; slotNum++)
        {
            ItemStack stack = items.get(slotNum);

            if (stack.isEmpty() == false)
            {
                CompoundTag tag = storeItemStackInTag(stack, new CompoundTag());

                if (invSlots <= 127)
                {
                    tag.putByte("Slot", (byte) slotNum);
                }
                else
                {
                    tag.putShort("Slot", (short) slotNum);
                }

                nbtTagList.add(tag);
            }
        }

        return nbtTagList;
    }

    /**
     * 将 <b>items</b> 中的 ItemStack 写入 CompoundTag <b>nbt</b>
     * 中名为 <b>tagName</b> 的 ListTag。
     * @param nbt
     * @param items
     * @param tagName 物品将写入的 ListTag 标签名称
     */
    @Nonnull
    public static CompoundTag writeItemsToTag(@Nonnull CompoundTag nbt, NonNullList<ItemStack> items,
            @Nonnull String tagName)
    {
        ListTag nbtTagList = createTagListForItems(items);

        if (nbtTagList.size() > 0)
        {
            nbt.put(tagName, nbtTagList);
        }
        else
        {
            nbt.remove(tagName);
        }

        return nbt;
    }

    /**
     * 将 <b>items</b> 中的 ItemStack 写入容器 ItemStack <b>containerStack</b>
     * 中名为 <b>tagName</b> 的 ListTag。
     * @param containerStack
     * @param items
     * @param tagName 物品将写入的 ListTag 标签名称
     */
    public static void writeItemsToContainerItem(@Nonnull ItemStack containerStack, NonNullList<ItemStack> items,
            @Nonnull String tagName)
    {
        CompoundTag nbt = getRootCompoundTag(containerStack, true);
        writeItemsToTag(nbt, items, tagName);

        setRootCompoundTag(containerStack, nbt);
    }

}