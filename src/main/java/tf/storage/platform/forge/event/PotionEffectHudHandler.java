package tf.storage.platform.forge.event;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.client.event.RenderGuiOverlayEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraft.resources.ResourceLocation;
import tf.storage.platform.forge.TFStorageMod;
import tf.storage.platform.forge.client.screen.BagScreen;

/**
 * PotionEffectHudHandler - 处理HUD药水效果显示
 * 当TFbag界面打开时隐藏原生的药水效果HUD
 */
@OnlyIn(Dist.CLIENT)
@Mod.EventBusSubscriber(modid = TFStorageMod.MOD_ID, bus = Mod.EventBusSubscriber.Bus.FORGE, value = Dist.CLIENT)
public class PotionEffectHudHandler {

    // 原生药水效果HUD的ID
    private static final ResourceLocation EFFECTS_OVERLAY_ID = ResourceLocation.parse("minecraft:effects");

    /**
     * 在TFbag界面打开时隐藏原生药水效果HUD
     */
    @SubscribeEvent
    public static void onRenderGuiOverlayPre(RenderGuiOverlayEvent.Pre event) {
        Minecraft mc = Minecraft.getInstance();
        Screen currentScreen = mc.screen;
        
        // 检查是否是TFbag界面
        if (currentScreen instanceof BagScreen) {
            // 取消渲染药水效果HUD
            if (event.getOverlay().id().equals(EFFECTS_OVERLAY_ID)) {
                event.setCanceled(true);
            }
        }
    }
}
