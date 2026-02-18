package tf.storage.platform.forge.event;

import com.mojang.blaze3d.platform.InputConstants;
import it.unimi.dsi.fastutil.ints.Int2LongOpenHashMap;
import net.minecraft.client.Minecraft;
import net.minecraft.client.KeyMapping;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.client.event.InputEvent;
import net.minecraftforge.client.event.ScreenEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import tf.storage.platform.forge.TFStorageMod;
import tf.storage.platform.forge.client.screen.BagScreen;
import tf.storage.platform.forge.config.TFStorageConfig;
import tf.storage.platform.forge.item.TFBagItem;
import tf.storage.platform.forge.network.GuiActions;
import tf.storage.platform.forge.network.ModNetworking;
import tf.storage.platform.forge.network.packets.ActionPacket;

/**
 * InputEvents - 客户端输入事件处理
 * 主要功能：快捷键打开TF包、双击Shift切换模式
 * 
 * 适配 Minecraft 1.20.1
 * 注意：这是纯客户端类，使用 @OnlyIn(Dist.CLIENT)
 */
@OnlyIn(Dist.CLIENT)
@Mod.EventBusSubscriber(modid = TFStorageMod.MOD_ID, bus = Mod.EventBusSubscriber.Bus.FORGE, value = Dist.CLIENT)
public class InputEvents {
    
    private static final Int2LongOpenHashMap KEY_PRESS_TIMES = new Int2LongOpenHashMap();
    private static final Int2LongOpenHashMap KEY_RELEASE_TIMES = new Int2LongOpenHashMap();
    public static int doubleTapLimit = 500; // 双击时间限制（毫秒）
    
    /**
     * 监听键盘输入事件
     * 当按下背包键时，根据配置决定是否打开TF包
     */
    @SubscribeEvent
    public static void onKeyInputEvent(InputEvent.Key event) {
        Minecraft mc = Minecraft.getInstance();
        
        if (mc.screen == null && mc.player != null) {
            int keyCode = event.getKey();
            int action = event.getAction(); // GLFW.GLFW_PRESS = 1, GLFW.GLFW_RELEASE = 0
            KeyMapping keyBindInventory = mc.options.keyInventory;
            
            // 检查是否按下了背包键
            if (keyBindInventory.matches(keyCode, event.getScanCode())) {
                Player player = mc.player;
                boolean keyPressed = (action == InputConstants.PRESS);
                
                boolean requiresSneak = TFStorageConfig.CLIENT.tfBagOpenRequiresSneak.get();
                boolean shouldOpen = keyPressed && (player.isShiftKeyDown() == requiresSneak);
                
                GuiEvents.instance().setTFBagShouldOpen(shouldOpen);
            }
            else if (keyCode == InputConstants.KEY_ESCAPE) {
                GuiEvents.instance().setTFBagShouldOpen(false);
            }
        }
    }

    /**
     * 监听GUI中的键盘输入事件
     * 用于在TF包GUI中检测双击Shift
     * 只有在按键释放后才能再次触发双击检测，避免长按触发
     */
    @SubscribeEvent
    public static void onGuiKeyInputEventPre(ScreenEvent.KeyPressed.Pre event) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.screen instanceof BagScreen) {
            int key = event.getKeyCode();
            if ((key == InputConstants.KEY_LSHIFT || key == InputConstants.KEY_RSHIFT)
                    && checkForDoubleTap(key)) {
                ModNetworking.sendToServer(ActionPacket.forBag(
                    GuiActions.Bag.TOGGLE_SHIFTCLICK_DOUBLETAP, 0));
            }
        }
    }

    /**
     * 监听GUI中的键盘释放事件
     * 用于记录按键释放时间，确保双击检测正确
     */
    @SubscribeEvent
    public static void onGuiKeyInputEventPost(ScreenEvent.KeyReleased.Post event) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.screen instanceof BagScreen) {
            int key = event.getKeyCode();
            if (key == InputConstants.KEY_LSHIFT || key == InputConstants.KEY_RSHIFT) {
                KEY_RELEASE_TIMES.put(key, System.currentTimeMillis());
            }
        }
    }

    /**
     * 检查是否为双击
     * 只有在按键释放后才能再次触发双击检测，避免长按触发
     * @param key 按键代码
     * @return 如果是双击返回true
     */
    private static boolean checkForDoubleTap(int key) {
        long currentTime = System.currentTimeMillis();
        
        // 检查按键是否已经释放
        if (!KEY_RELEASE_TIMES.containsKey(key)) {
            return false;
        }
        
        long releaseTime = KEY_RELEASE_TIMES.get(key);
        
        // 如果按键释放时间太长，重置
        if (currentTime - releaseTime > doubleTapLimit) {
            KEY_RELEASE_TIMES.remove(key);
            KEY_PRESS_TIMES.remove(key);
            return false;
        }
        
        if (KEY_PRESS_TIMES.containsKey(key)) {
            long lastTime = KEY_PRESS_TIMES.get(key);
            if (currentTime - lastTime > doubleTapLimit) {
                KEY_PRESS_TIMES.put(key, currentTime);
                return false;
            } else {
                KEY_PRESS_TIMES.remove(key);
                KEY_RELEASE_TIMES.remove(key);
                return true;
            }
        } else {
            KEY_PRESS_TIMES.put(key, currentTime);
            return false;
        }
    }
}
