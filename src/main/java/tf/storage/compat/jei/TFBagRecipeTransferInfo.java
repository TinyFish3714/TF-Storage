package tf.storage.compat.jei;

import java.util.ArrayList;
import java.util.List;
import mezz.jei.api.recipe.VanillaRecipeCategoryUid;
import mezz.jei.api.recipe.transfer.IRecipeTransferInfo;
import net.minecraft.inventory.Slot;
import tf.storage.inventory.container.BagContainer;

public class TFBagRecipeTransferInfo implements IRecipeTransferInfo<BagContainer>
{
    @Override
    public Class<BagContainer> getContainerClass()
    {
        return BagContainer.class;
    }

    @Override
    public String getRecipeCategoryUid()
    {
        return VanillaRecipeCategoryUid.CRAFTING;
    }

    @Override
    public boolean canHandle(BagContainer container)
    {
        return true;
    }

    @Override
    public List<Slot> getRecipeSlots(BagContainer container)
    {
        List<Slot> slots = new ArrayList<>();
        // 手提袋槽位 + 存储卡槽位 + 玩家槽位 + 盔甲 + 副手 + 结果
        // Tier 0: 27 + 4 + 36 + 4 + 1 + 1 = 73 offset
        // Tier 1: 55 + 4 + 36 + 4 + 1 + 1 = 101 offset
        
        int offset;
        int tier = container.getBagTier();
        
        if (tier == 1)
        {
            offset = 55 + 4 + 36 + 4 + 1 + 1; // 101
        }
        else
        {
            offset = 27 + 4 + 36 + 4 + 1 + 1; // 73
        }

        for (int i = 0; i < 9; i++)
        {
            if (offset + i < container.inventorySlots.size())
            {
                slots.add(container.getSlot(offset + i));
            }
        }
        
        return slots;
    }

    @Override
    public List<Slot> getInventorySlots(BagContainer container)
    {
        List<Slot> slots = new ArrayList<>();
        int tier = container.getBagTier();
        
        // 1. 手提袋槽位
        int bagSlotsCount = (tier == 1) ? 55 : 27;
        for (int i = 0; i < bagSlotsCount; i++)
        {
            slots.add(container.getSlot(i));
        }
        
        // 2. 玩家库存（主物品栏 + 快捷栏）
        // 存储卡位于手提袋槽位之后：4 个槽位。
        // 玩家库存从存储卡之后开始。
        int playerStart = bagSlotsCount + 4;
        for (int i = 0; i < 36; i++)
        {
            slots.add(container.getSlot(playerStart + i));
        }
        
        return slots;
    }
}
