package tf.storage.event;

import org.lwjgl.input.Keyboard;
import net.minecraft.client.Minecraft;
import net.minecraft.client.settings.KeyBinding;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.util.math.BlockPos;
import net.minecraftforge.client.event.GuiScreenEvent;
import net.minecraftforge.fml.client.FMLClientHandler;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.InputEvent;
import tf.storage.config.ModConfig;
import tf.storage.gui.client.BagGui;
import tf.storage.item.TFBag;
import tf.storage.network.PacketHandler;
import tf.storage.network.packet.ActionPacket;
import tf.storage.core.ModInfo;
import it.unimi.dsi.fastutil.ints.Int2LongOpenHashMap;

public class InputEvents
{
    private static final Int2LongOpenHashMap KEY_PRESS_TIMES = new Int2LongOpenHashMap();
    public static int doubleTapLimit = 500;
    private final Minecraft mc;

    public InputEvents()
    {
        this.mc = Minecraft.getMinecraft();
    }

    @SubscribeEvent
    public void onKeyInputEvent(InputEvent.KeyInputEvent event)
    {
        if (this.mc.inGameHasFocus && this.mc.player != null)
        {
            int keyCode = Keyboard.getEventKey();
            boolean keyState = Keyboard.getEventKeyState();
            KeyBinding keyBindInventory = this.mc.gameSettings.keyBindInventory;

            if (keyBindInventory.isActiveAndMatches(keyCode))
            {
                EntityPlayer player = this.mc.player;
                boolean shouldOpen = keyState &&
                    (player.isSneaking() == ModConfig.ConfigValues.tfBagOpenRequiresSneak);

                GuiEvents.instance().setTFBagShouldOpen(shouldOpen);
            }
            else if (keyCode == Keyboard.KEY_ESCAPE)
            {
                GuiEvents.instance().setTFBagShouldOpen(false);
            }
        }
    }

    @SubscribeEvent
    public void onGuiKeyInputEventPre(GuiScreenEvent.KeyboardInputEvent.Pre event)
    {
        if (event.getGui() instanceof BagGui)
        {
            int key = Keyboard.getEventKey();
            if (Keyboard.getEventKeyState() && (key == Keyboard.KEY_LSHIFT || key == Keyboard.KEY_RSHIFT) && this.checkForDoubleTap(key))
            {
                PacketHandler.INSTANCE.sendToServer(new ActionPacket(0, new BlockPos(0, 0, 0),
                        ModInfo.GUI_ID_TF_BAG, TFBag.GUI_ACTION_TOGGLE_SHIFTCLICK_DOUBLETAP, 0));
            }
        }
    }

    private boolean checkForDoubleTap(int key)
    {
        long currentTime = System.currentTimeMillis();
        
        if (KEY_PRESS_TIMES.containsKey(key))
        {
            long lastTime = KEY_PRESS_TIMES.get(key);
            if (currentTime - lastTime > doubleTapLimit)
            {
                KEY_PRESS_TIMES.put(key, currentTime);
                return false;
            }
            else
            {
                KEY_PRESS_TIMES.remove(key);
                return true;
            }
        }
        else
        {
            KEY_PRESS_TIMES.put(key, currentTime);
            return false;
        }
    }
    
}
