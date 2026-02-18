package tf.storage.core.util;

import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;

/**
 * EntityHelper - 实体操作工具类
 * 
 * 适配 Minecraft 1.20.1
 */
public class EntityHelper
{
    /**
     * Check if entity is holding a specific item in main or off hand
     */
    public static ItemStack getHeldItemOfType(LivingEntity entity, Item item)
    {
        if (entity == null) return ItemStack.EMPTY;
        
        ItemStack stack = entity.getMainHandItem();
        if (stack != null && !stack.isEmpty() && stack.getItem() == item)
        {
            return stack;
        }
        
        stack = entity.getOffhandItem();
        if (stack != null && !stack.isEmpty() && stack.getItem() == item)
        {
            return stack;
        }

        
        return ItemStack.EMPTY;
    }

    public static void dropItemStacksInWorld(Level level, BlockPos pos, ItemStack stack, boolean dropFullStacks)
    {
        if (level.isClientSide || stack == null || stack.isEmpty())
        {
            return;
        }

        double x = pos.getX() + 0.5;
        double y = pos.getY() + 0.5;
        double z = pos.getZ() + 0.5;
        
        ItemStack dropStack = stack.copy();
        ItemEntity entityItem = new ItemEntity(level, x, y, z, dropStack);
        
        entityItem.setDeltaMovement(0, 0, 0);
        entityItem.setPickUpDelay(10);
        level.addFreshEntity(entityItem);
    }
}