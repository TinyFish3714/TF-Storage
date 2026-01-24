package tf.storage.core;

import net.minecraftforge.fml.common.Loader;

/**
 * ModCompat - 外部模组兼容性检查
 */
public class ModCompat
{
    public static final String MODID_BAUBLES = "baubles";
    private static boolean baublesLoaded;

    public static void checkLoadedMods()
    {
        baublesLoaded = Loader.isModLoaded(MODID_BAUBLES);
    }

    public static boolean isBaublesLoaded()
    {
        return baublesLoaded;
    }
}
