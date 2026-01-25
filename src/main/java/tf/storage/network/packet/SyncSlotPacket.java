package tf.storage.network.packet;

import java.io.IOException;
import net.minecraft.client.Minecraft;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraftforge.fml.client.FMLClientHandler;
import net.minecraftforge.fml.common.network.simpleimpl.IMessage;
import net.minecraftforge.fml.common.network.simpleimpl.IMessageHandler;
import net.minecraftforge.fml.common.network.simpleimpl.MessageContext;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;
import tf.storage.TFStorage;
import tf.storage.inventory.container.base.BaseContainer;
import io.netty.buffer.ByteBuf;

public class SyncSlotPacket implements IMessage
{
    private int windowId;
    private int slotNum;
    private ItemStack stack = ItemStack.EMPTY;

    public SyncSlotPacket()
    {
    }

    public SyncSlotPacket(int windowId, int slotNum, ItemStack stack)
    {
        this.windowId = windowId;
        this.slotNum = slotNum;
        this.stack = stack;
    }

    @Override
    public void fromBytes(ByteBuf buf)
    {
        try
        {
            this.windowId = buf.readInt();
            this.slotNum = buf.readShort();
            this.stack = ByteBufUtils.readItemStackFromBuffer(buf);
        }
        catch (IOException e)
        {
            TFStorage.logger.warn("SyncSlotPacket: Exception while reading data from buffer", e);
        }
    }

    @Override
    public void toBytes(ByteBuf buf)
    {
        buf.writeInt(this.windowId);
        buf.writeShort(this.slotNum);
        ByteBufUtils.writeItemStackToBuffer(buf, this.stack);
    }

    public static class Handler implements IMessageHandler<SyncSlotPacket, IMessage>
    {
        @Override
        public IMessage onMessage(final SyncSlotPacket message, MessageContext ctx)
        {
            if (ctx.side != Side.CLIENT)
            {
                TFStorage.logger.error("Wrong side in SyncSlotPacket: " + ctx.side);
                return null;
            }

            handleClientSide(message, ctx);
            return null;
        }
        
        @SideOnly(Side.CLIENT)
        private void handleClientSide(final SyncSlotPacket message, MessageContext ctx)
        {
            Minecraft mc = FMLClientHandler.instance().getClient();
            final EntityPlayer player = TFStorage.proxy.getPlayerFromMessageContext(ctx);

            if (mc == null || player == null)
            {
                TFStorage.logger.error("Minecraft or player was null in SyncSlotPacket");
                return;
            }

            mc.addScheduledTask(new Runnable()
            {
                public void run()
                {
                    processMessage(message, player);
                }
            });
        }

        @SideOnly(Side.CLIENT)
        protected void processMessage(final SyncSlotPacket message, EntityPlayer player)
        {
            if (player.openContainer instanceof BaseContainer && message.windowId == player.openContainer.windowId)
            {
                ((BaseContainer) player.openContainer).syncStackInSlot(message.slotNum, message.stack);
            }
        }
    }
}
