package tf.storage.compat.jei;

import mezz.jei.api.IModPlugin;
import mezz.jei.api.IModRegistry;
import mezz.jei.api.JEIPlugin;
import mezz.jei.api.recipe.VanillaRecipeCategoryUid;

@JEIPlugin
public class TFStorageJEIPlugin implements IModPlugin
{
    @Override
    public void register(IModRegistry registry)
    {
        registry.getRecipeTransferRegistry().addRecipeTransferHandler(new TFBagRecipeTransferInfo());
    }
}
