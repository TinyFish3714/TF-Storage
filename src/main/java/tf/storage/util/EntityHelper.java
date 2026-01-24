package tf.storage.util;

import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.item.EntityItem;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

public class EntityHelper
{
    /**
     * Check if entity is holding a specific item in main or off hand
     */
    public static ItemStack getHeldItemOfType(EntityLivingBase entity, Item item)
    {
        if (entity == null) return ItemStack.EMPTY;
        
        ItemStack stack = entity.getHeldItemMainhand();
        if (stack != null && !stack.isEmpty() && stack.getItem() == item)
        {
            return stack;
        }
        
        stack = entity.getHeldItemOffhand();
        if (stack != null && !stack.isEmpty() && stack.getItem() == item)
        {
            return stack;
        }

        
        return ItemStack.EMPTY;
    }

    public static void dropItemStacksInWorld(World world, BlockPos pos, ItemStack stack, boolean dropFullStacks)
    {
        if (world.isRemote || stack == null || stack.isEmpty())
        {
            return;
        }

        double x = pos.getX() + 0.5;
        double y = pos.getY() + 0.5;
        double z = pos.getZ() + 0.5;
        
        ItemStack dropStack = stack.copy();
        EntityItem entityItem = new EntityItem(world, x, y, z, dropStack);
        
        entityItem.motionX = 0;
        entityItem.motionY = 0;
        entityItem.motionZ = 0;
        entityItem.setPickupDelay(10);
        world.spawnEntity(entityItem);
    }

}
