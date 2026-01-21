package tf.storage.event;

import net.minecraft.client.gui.inventory.GuiInventory;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraftforge.client.event.GuiOpenEvent;
import net.minecraftforge.fml.client.FMLClientHandler;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import tf.storage.item.TFBag;
import tf.storage.network.PacketHandler;
import tf.storage.network.packet.OpenGuiPacket;
import tf.storage.core.ModInfo;

public class GuiEvents
{
    private static final GuiEvents INSTANCE = new GuiEvents();
    private boolean tfBagShouldOpen;

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

    @SubscribeEvent
    public void onGuiOpenEvent(GuiOpenEvent event)
    {
        // 打开玩家库存 GUI
        if (event.getGui() != null && event.getGui().getClass() == GuiInventory.class)
        {
            EntityPlayer player = FMLClientHandler.instance().getClientPlayerEntity();

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
}
