package tf.storage.core.inventory;

import net.minecraftforge.items.IItemHandler;

/**
 * MergeSlotRange - 合并槽位范围管理类
 * 扩展SlotRange，增加existingOnly标志
 */
public class MergeSlotRange extends SlotRange
{
    public final boolean existingOnly;

    public MergeSlotRange(IItemHandler inv)
    {
        this(0, inv.getSlots());
    }

    public MergeSlotRange(int start, int numSlots)
    {
        this(start, numSlots, false);
    }

    public MergeSlotRange(int start, int numSlots, boolean existingOnly)
    {
        super(start, numSlots);
        this.existingOnly = existingOnly;
    }
}