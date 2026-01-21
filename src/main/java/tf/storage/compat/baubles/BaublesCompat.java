package tf.storage.compat.baubles;

import baubles.api.BaubleType;
import baubles.api.IBauble;
import net.minecraft.item.ItemStack;

public class BaublesCompat {
    
    public static BaubleType getBaubleType(ItemStack stack) {
        return BaubleType.BODY; // Backpack fits best in BODY slot (usually back slot)
    }
}
