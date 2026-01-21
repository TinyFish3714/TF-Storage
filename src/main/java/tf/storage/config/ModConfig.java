package tf.storage.config;

import java.io.File;
import java.util.HashSet;
import java.util.Set;
import net.minecraftforge.common.config.Configuration;
import net.minecraftforge.common.config.Property;
import net.minecraftforge.fml.client.event.ConfigChangedEvent.OnConfigChangedEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import tf.storage.core.ModInfo;

public class ModConfig
{
    public static final String CATEGORY_CLIENT = "Client";
    public static final String CATEGORY_GENERIC = "Generic";
    private static File configurationFile;
    private static Configuration config;

    private static String currentCategory;
    private static boolean currentRequiresMcRestart;

    public static class ConfigValues
    {
        // Client
        public static boolean tfBagOpenRequiresSneak;
        
        // 私有构造函数，防止实例化
        private ConfigValues() {}
    }

    public static void loadConfigsFromFile(File configFile)
    {
        configurationFile = configFile;
        config = new Configuration(configurationFile, null, true);
        config.load();
        reLoadAllConfigs(false);
    }


    public static Configuration loadConfigsFromFile()
    {
        return config;
    }

    public static void reLoadAllConfigs(boolean reloadFromFile)
    {
        if (reloadFromFile)
        {
            config.load();
        }

        loadConfigGeneric(config);

        if (config.hasChanged())
        {
            config.save();
        }
    }

    public static File getConfigFile()
    {
        return configurationFile;
    }

    @SubscribeEvent
    public void onConfigChangedEvent(OnConfigChangedEvent event)
    {
        if (ModInfo.MOD_ID.equals(event.getModID()))
        {
            reLoadAllConfigs(false);
        }
    }

    private static void loadConfigGeneric(Configuration conf)
    {
        Property prop;
        currentCategory = CATEGORY_CLIENT;
        currentRequiresMcRestart = false;
        conf.addCustomCategoryComment(currentCategory, "客户端配置");

        prop = getProp("tfBagOpenRequiresSneak", false);
        prop.setComment("反转打开 TF 手提袋而不是常规库存时的潜行行为");
        ConfigValues.tfBagOpenRequiresSneak = prop.getBoolean();

    }


    private static Property getProp(String key, boolean defaultValue)
    {
        return getProp(currentCategory, key, defaultValue, currentRequiresMcRestart);
    }

    private static Property getProp(String category, String key, boolean defaultValue, boolean requiresMcRestart)
    {
        Property prop = config.get(category, key, defaultValue).setRequiresMcRestart(requiresMcRestart);
        return prop;
    }

}
