package tf.storage.platform.forge;

import net.minecraft.client.gui.screens.MenuScreens;
import tf.storage.platform.forge.client.screen.BagScreen;
import tf.storage.platform.forge.client.screen.ChestScreen;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.fml.ModLoadingContext;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.config.ModConfig;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import tf.storage.core.util.CardHelper;
import tf.storage.platform.forge.config.TFStorageConfig;
import tf.storage.platform.forge.network.ModNetworking;

/**
 * TF Storage - 主模组类
 * 
 * @author TinyFish3714
 * @version 1.20.1-1.0.0
 */
@Mod(TFStorageMod.MOD_ID)
public class TFStorageMod {
    
    public static final String MOD_ID = "tfstorage";
    public static final Logger LOGGER = LogManager.getLogger(MOD_ID);
    
    @SuppressWarnings({"deprecation", "removal"})
    public TFStorageMod() {
        IEventBus modEventBus = FMLJavaModLoadingContext.get().getModEventBus();
        ModLoadingContext.get().registerConfig(ModConfig.Type.CLIENT, TFStorageConfig.CLIENT_SPEC);
        
        // 注册所有内容
        ModRegistry.register(modEventBus);
        
        // 注册生命周期事件
        modEventBus.addListener(this::commonSetup);
        modEventBus.addListener(this::clientSetup);
        
        // 注册Forge事件总线
        MinecraftForge.EVENT_BUS.register(this);
        
        LOGGER.info("TF Storage mod initialized!");
    }
    
    private void commonSetup(final FMLCommonSetupEvent event) {
        event.enqueueWork(() -> {
            // 注册网络包
            ModNetworking.register();
            
            // 设置存储卡ID供CardHelper使用
            CardHelper.setMemoryCardId(MOD_ID + ":tfunit_6b");
            
            LOGGER.info("TF Storage common setup complete");
        });
    }
    
    private void clientSetup(final FMLClientSetupEvent event) {
        event.enqueueWork(() -> {
            MenuScreens.register(ModRegistry.TF_CHEST_MENU.get(), ChestScreen::new);
            MenuScreens.register(ModRegistry.TF_BAG_MENU.get(), BagScreen::new);
            
            LOGGER.info("TF Storage client setup complete");
        });
    }
}
