package tf.storage.proxy;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraftforge.common.ForgeChunkManager;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.network.simpleimpl.MessageContext;
import tf.storage.TFStorage;
import tf.storage.item.TFBag;
import tf.storage.event.PlayerEvents;

public class CommonProxy
{
    public String format(String key, Object... args)
    {
        // 在服务端直接返回key，避免使用已废弃的I18n方法
        // 服务端不应该进行翻译，翻译应该由客户端处理
        return key;
    }

    public EntityPlayer getClientPlayer()
    {
        return null;
    }

    public EntityPlayer getPlayerFromMessageContext(MessageContext ctx)
    {
        switch (ctx.side)
        {
            case SERVER:
                return ctx.getServerHandler().player;
            default:
                TFStorage.logger.warn("Invalid side in getPlayerFromMessageContext(): " + ctx.side);
                return null;
        }
    }


    public void registerEventHandlers()
    {
        MinecraftForge.EVENT_BUS.register(TFBag.class);
        MinecraftForge.EVENT_BUS.register(this);
        MinecraftForge.EVENT_BUS.register(PlayerEvents.class);
    }

    public void registerRenderers() { }

    public boolean isShiftKeyDown()
    {
        return false;
    }

    public boolean isControlKeyDown()
    {
        return false;
    }

    public boolean isAltKeyDown()
    {
        return false;
    }
}
