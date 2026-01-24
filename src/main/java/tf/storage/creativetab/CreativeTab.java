package tf.storage.creativetab;

import net.minecraft.creativetab.CreativeTabs;
import net.minecraft.item.ItemStack;
import tf.storage.core.ModInfo;
import tf.storage.core.ModItems;

public class CreativeTab
{
    public static final CreativeTabs TF_STORAGE_TAB = new CreativeTabs(ModInfo.MOD_ID)
    {
        @Override
        public ItemStack createIcon()
        {
            // Changed to TF bag 0 as requested
            return new ItemStack(ModItems.TF_BAG, 1, 0);
        }

        @Override
        public String getTranslationKey()
        {
            return "itemGroup." + ModInfo.MOD_ID;
        }
    };
}
