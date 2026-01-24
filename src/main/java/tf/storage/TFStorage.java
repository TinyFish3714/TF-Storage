package tf.storage;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.Mod.Instance;
import net.minecraftforge.fml.common.SidedProxy;
import net.minecraftforge.fml.common.event.FMLPreInitializationEvent;
import net.minecraftforge.fml.common.event.FMLServerStartingEvent;
import net.minecraftforge.fml.common.event.FMLServerStoppingEvent;
import net.minecraftforge.fml.common.network.NetworkRegistry;

import tf.storage.config.ModConfig;
import tf.storage.event.PlayerEvents;
import tf.storage.gui.handler.GuiHandler;
import tf.storage.network.PacketHandler;
import tf.storage.proxy.CommonProxy;
import tf.storage.core.ModInfo;
import tf.storage.core.ModCompat;

/**
 * TFStorage - 模组主类
 */
@Mod(modid = ModInfo.MOD_ID, name = ModInfo.MOD_NAME, version = ModInfo.MOD_VERSION,
     guiFactory = "tf.storage.config.ModGuiFactory",
     acceptedMinecraftVersions = "[1.12.2]",
     dependencies = "required-after:forge@[14.23.5.2854,);")
public class TFStorage
{
    @Instance(ModInfo.MOD_ID)
    public static TFStorage instance;

    @SidedProxy(clientSide = ModInfo.PROXY_CLASS_CLIENT, serverSide = ModInfo.PROXY_CLASS_SERVER)
    public static CommonProxy proxy;

    public static final Logger logger = LogManager.getLogger(ModInfo.MOD_ID);

    @Mod.EventHandler
    public void preInit(FMLPreInitializationEvent event)
    {
        ModConfig.loadConfigsFromFile(event.getSuggestedConfigurationFile());
        ModCompat.checkLoadedMods();

        proxy.registerEventHandlers();
        proxy.registerRenderers();

        PacketHandler.init();
        NetworkRegistry.INSTANCE.registerGuiHandler(this, new GuiHandler());
    }

    @Mod.EventHandler
    public void onServerStartingEvent(FMLServerStartingEvent event)
    {
        // 配置文件已在预初始化阶段加载
    }

    @Mod.EventHandler
    public void onServerStoppingEvent(FMLServerStoppingEvent event)
    {
        PlayerEvents.clearSnapshots();
    }
}
