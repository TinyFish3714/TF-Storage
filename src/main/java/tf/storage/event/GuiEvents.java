package tf.storage.event;

import net.minecraft.client.gui.inventory.GuiInventory;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraftforge.client.event.GuiOpenEvent;
import net.minecraftforge.fml.client.FMLClientHandler;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;
import tf.storage.item.TFBag;
import tf.storage.network.PacketHandler;
import tf.storage.network.packet.OpenGuiPacket;
import tf.storage.core.ModInfo;
import baubles.client.gui.GuiPlayerExpanded;
import tf.storage.gui.client.GuiEmpty;

public class GuiEvents
{
    private static final GuiEvents INSTANCE = new GuiEvents();
    private boolean tfBagShouldOpen;
    private boolean openedBaublesFromBag;
    
    // Add a counter for delayed opening
    private int reopenBagDelay = -1;

    // 私有构造函数
    private GuiEvents() {}

    public static GuiEvents instance()
    {
        return INSTANCE;
    }

    public void setTFBagShouldOpen(boolean shouldOpen)
    {
        this.tfBagShouldOpen = shouldOpen;
    }

    public void setOpenedBaublesFromBag(boolean val)
    {
        this.openedBaublesFromBag = val;
    }

    @SubscribeEvent
    public void onGuiOpenEvent(GuiOpenEvent event)
    {
        net.minecraft.client.Minecraft mc = net.minecraft.client.Minecraft.getMinecraft();
        EntityPlayer player = FMLClientHandler.instance().getClientPlayerEntity();

        // Intercept Baubles closing to return to TFBag
        if (this.openedBaublesFromBag)
        {
            boolean isClosing = (event.getGui() == null);
            boolean isGoingToInv = (event.getGui() instanceof GuiInventory);
            
            boolean currentIsBaubles = false;
            if (mc.currentScreen != null)
            {
                 if (mc.currentScreen instanceof GuiPlayerExpanded || 
                     mc.currentScreen.getClass().getName().equals("baubles.client.gui.GuiPlayerExpanded")) {
                     currentIsBaubles = true;
                 }
            }

            if (currentIsBaubles && (isClosing || isGoingToInv))
            {
                this.openedBaublesFromBag = false;
                event.setGui(new GuiEmpty()); // Use placeholder to keep mouse position
                this.reopenBagDelay = 1;      // Schedule TFBag open
            }
        }

        // Open TFBag instead of Inventory if applicable
        if (event.getGui() != null && event.getGui().getClass() == GuiInventory.class)
        {
            if (this.tfBagShouldOpen && player != null && TFBag.getOpenableBag(player).isEmpty() == false)
            {
                if (event.isCancelable())
                {
                    event.setCanceled(true);
                }

                PacketHandler.INSTANCE.sendToServer(new OpenGuiPacket(player.dimension, ModInfo.GUI_ID_TF_BAG));
            }
        }
    }
    
    @SubscribeEvent
    public void onClientTick(TickEvent.ClientTickEvent event)
    {
        if (event.phase == TickEvent.Phase.END && this.reopenBagDelay >= 0)
        {
            this.reopenBagDelay--;
            
            if (this.reopenBagDelay == 0)
            {
                // Timer expired
                EntityPlayer player = FMLClientHandler.instance().getClientPlayerEntity();
                
                if (player != null)
                {
                    if (!TFBag.getOpenableBag(player).isEmpty())
                    {
                        PacketHandler.INSTANCE.sendToServer(new OpenGuiPacket(player.dimension, ModInfo.GUI_ID_TF_BAG));
                    }
                    else
                    {
                        net.minecraft.client.Minecraft.getMinecraft().displayGuiScreen(new GuiInventory(player));
                    }
                }
                
                // Reset timer
                this.reopenBagDelay = -1;
            }
        }
    }
}
