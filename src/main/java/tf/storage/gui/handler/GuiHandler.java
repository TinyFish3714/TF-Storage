package tf.storage.gui.handler;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.minecraftforge.fml.common.network.IGuiHandler;
import tf.storage.gui.client.BagGui;
import tf.storage.inventory.container.BagContainer;
import tf.storage.item.TFBag;
import tf.storage.core.ModInfo;
import tf.storage.core.ModItems;
import tf.storage.tile.TileChest;
import tf.storage.util.EntityHelper;

public class GuiHandler implements IGuiHandler
{

    @Override
    public Object getServerGuiElement(int id, EntityPlayer player, World world, int x, int y, int z)
    {
        if (player == null || world == null)
        {
            return null;
        }

        ItemStack stack;

        switch (id)
        {
            case ModInfo.GUI_ID_TILE_ENTITY_GENERIC:
                TileEntity te = world.getTileEntity(new BlockPos(x, y, z));

                if (te != null && te instanceof TileChest)
                {
                    return ((TileChest) te).getContainer(player);
                }
                break;

            case ModInfo.GUI_ID_TF_BAG:
                stack = TFBag.getOpenableBag(player);
                if (stack.isEmpty() == false)
                {
                    return new BagContainer(player, stack);
                }
                break;

            case ModInfo.GUI_ID_TF_BAG_RIGHT_CLICK:
                stack = EntityHelper.getHeldItemOfType(player, ModItems.TF_BAG);
                if (stack.isEmpty() == false)
                {
                    return new BagContainer(player, stack);
                }
                break;

            default:
        }

        return null;
    }

    @Override
    public Object getClientGuiElement(int id, EntityPlayer player, World world, int x, int y, int z)
    {
        if (player == null || world == null)
        {
            return null;
        }

        switch (id)
        {
            case ModInfo.GUI_ID_TILE_ENTITY_GENERIC:
                TileEntity te = world.getTileEntity(new BlockPos(x, y, z));

                if (te != null && te instanceof TileChest)
                {
                    return ((TileChest) te).getGui(player);
                }
                break;

            case ModInfo.GUI_ID_TF_BAG:
                ItemStack stack = TFBag.getOpenableBag(player);
                if (stack.isEmpty() == false)
                {
                    return new BagGui(new BagContainer(player, stack));
                }
                break;

            case ModInfo.GUI_ID_TF_BAG_RIGHT_CLICK:
                stack = EntityHelper.getHeldItemOfType(player, ModItems.TF_BAG);
                if (stack.isEmpty() == false)
                {
                    return new BagGui(new BagContainer(player, stack));
                }
                break;

            default:
        }

        return null;
    }

}
