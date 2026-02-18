package tf.storage.platform.forge.compat.jei;

import mezz.jei.api.IModPlugin;
import mezz.jei.api.JeiPlugin;
import mezz.jei.api.registration.IRecipeTransferRegistration;
import net.minecraft.resources.ResourceLocation;
import tf.storage.platform.forge.TFStorageMod;

@JeiPlugin
public class JeiTfStoragePlugin implements IModPlugin {
    private static final ResourceLocation UID = ResourceLocation.parse(TFStorageMod.MOD_ID + ":jei_plugin");

    @Override
    public ResourceLocation getPluginUid() {
        return UID;
    }

    @Override
    public void registerRecipeTransferHandlers(IRecipeTransferRegistration registration) {
        registration.addRecipeTransferHandler(new BagMenuRecipeTransferInfo());
    }
}
