package tf.storage.compat.baubles;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.util.math.BlockPos;
import net.minecraftforge.fml.common.Loader;
import net.minecraftforge.fml.common.ModContainer;
import baubles.api.BaublesApi;
import baubles.api.cap.IBaublesItemHandler;
import net.minecraftforge.items.IItemHandler;
import tf.storage.core.ModCompat;
import tf.storage.core.ModItems;
import tf.storage.item.TFBag;
import tf.storage.TFStorage;

public class BaublesHelper {
    
    public static ItemStack getOpenableBag(EntityPlayer player) {
        try {
            IBaublesItemHandler baublesInv = BaublesApi.getBaublesHandler(player);
            for (int i = 0; i < baublesInv.getSlots(); i++) {
                ItemStack stack = baublesInv.getStackInSlot(i);
                if (!stack.isEmpty() && stack.getItem() == ModItems.TF_BAG) {
                    if (isBagOpenable(stack)) {
                        return stack;
                    }
                }
            }
        } catch (Exception e) {
            // Ignore
        }
        return ItemStack.EMPTY;
    }

    public static IItemHandler getBaublesInventory(EntityPlayer player) {
        try {
            return BaublesApi.getBaublesHandler(player);
        } catch (Exception e) {
            return null;
        }
    }

    private static boolean isBagOpenable(ItemStack stack) {
        if (stack.getTagCompound() == null) {
            return true;
        }
        if (stack.getTagCompound().getCompoundTag("TFBag").getBoolean("DisableOpen")) {
            return false;
        }
        return true;
    }

    public static void openBaublesGui(EntityPlayer player) {
        try {
            ModContainer baublesContainer = Loader.instance().getIndexedModList().get(ModCompat.MODID_BAUBLES);
            if (baublesContainer != null) {
                Object baubles = baublesContainer.getMod();
                BlockPos pos = player.getPosition();
                player.openGui(baubles, 0, player.getEntityWorld(), pos.getX(), pos.getY(), pos.getZ());
            }
        } catch (Exception e) {
            TFStorage.logger.warn("Failed to open the Baubles GUI from TF Bag", e);
        }
    }
}
