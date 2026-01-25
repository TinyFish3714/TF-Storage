package tf.storage.network.packet;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
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

public class SyncMultipleSlotsPacket implements IMessage
{
    private int windowId;
    private int[] slotIds;
    private ItemStack[] stacks;
    
    private static final int MAX_SLOTS_PER_PACKET = 64;
    
    public SyncMultipleSlotsPacket() {}
    
    public SyncMultipleSlotsPacket(int windowId, List<Integer> dirtySlots, List<ItemStack> newStacks)
    {
        this.windowId = windowId;
        
        int size = Math.min(dirtySlots.size(), MAX_SLOTS_PER_PACKET);
        this.slotIds = new int[size];
        this.stacks = new ItemStack[size];
        
        for (int i = 0; i < size; i++)
        {
            this.slotIds[i] = dirtySlots.get(i);
            this.stacks[i] = newStacks.get(i);
        }
    }
    
    @Override
    public void toBytes(ByteBuf buf)
    {
        buf.writeInt(windowId);
        buf.writeShort(slotIds.length);
        
        for (int i = 0; i < slotIds.length; i++)
        {
            buf.writeShort(slotIds[i]);
            ByteBufUtils.writeItemStackToBuffer(buf, stacks[i]);
        }
    }
    
    @Override
    public void fromBytes(ByteBuf buf)
    {
        try
        {
            this.windowId = buf.readInt();
            int count = buf.readShort();
            this.slotIds = new int[count];
            this.stacks = new ItemStack[count];
            
            for (int i = 0; i < count; i++)
            {
                this.slotIds[i] = buf.readShort();
                this.stacks[i] = ByteBufUtils.readItemStackFromBuffer(buf);
            }
        }
        catch (IOException e)
        {
            TFStorage.logger.warn("SyncMultipleSlotsPacket: Exception while reading data from buffer", e);
            this.slotIds = new int[0];
            this.stacks = new ItemStack[0];
        }
    }
    
    public static class Handler implements IMessageHandler<SyncMultipleSlotsPacket, IMessage>
    {
        @Override
        public IMessage onMessage(final SyncMultipleSlotsPacket message, MessageContext ctx)
        {
            if (ctx.side != Side.CLIENT)
            {
                TFStorage.logger.error("Wrong side in SyncMultipleSlotsPacket: " + ctx.side);
                return null;
            }
            
            handleClientSide(message, ctx);
            return null;
        }

        @SideOnly(Side.CLIENT)
        private void handleClientSide(final SyncMultipleSlotsPacket message, MessageContext ctx)
        {
            Minecraft mc = FMLClientHandler.instance().getClient();
            final EntityPlayer player = TFStorage.proxy.getPlayerFromMessageContext(ctx);
            
            if (mc == null || player == null)
            {
                TFStorage.logger.error("Minecraft or player was null in SyncMultipleSlotsPacket");
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
        protected void processMessage(final SyncMultipleSlotsPacket message, EntityPlayer player)
        {
            if (player.openContainer instanceof BaseContainer &&
                message.windowId == player.openContainer.windowId)
            {
                
                BaseContainer container = (BaseContainer) player.openContainer;
                
                for (int i = 0; i < message.slotIds.length; i++)
                {
                    container.syncStackInSlot(message.slotIds[i], message.stacks[i]);
                }
            }
        }
    }
}
