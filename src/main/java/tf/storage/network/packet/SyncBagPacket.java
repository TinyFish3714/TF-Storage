package tf.storage.network.packet;

import java.io.IOException;
import io.netty.buffer.ByteBuf;
import net.minecraft.client.Minecraft;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraftforge.fml.common.FMLCommonHandler;
import net.minecraftforge.fml.common.network.simpleimpl.IMessage;
import net.minecraftforge.fml.common.network.simpleimpl.IMessageHandler;
import net.minecraftforge.fml.common.network.simpleimpl.MessageContext;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;
import tf.storage.TFStorage;
import tf.storage.inventory.container.BagContainer;

/**
 * Used to sync container item stack NBT data over network.
 * When server-side item NBT data changes, this packet is sent to client to sync state.
 */
public class SyncBagPacket implements IMessage
{
    private ItemStack containerStack;
    private int windowId;
    private boolean syncInventory;

    public SyncBagPacket()
    {
    }

    public SyncBagPacket(int windowId, ItemStack containerStack)
    {
        this(windowId, containerStack, true);
    }

    public SyncBagPacket(int windowId, ItemStack containerStack, boolean syncInventory)
    {
        this.windowId = windowId;
        this.containerStack = containerStack;
        this.syncInventory = syncInventory;
    }

    @Override
    public void fromBytes(ByteBuf buf)
    {
        this.windowId = buf.readInt();
        this.syncInventory = buf.readBoolean();
        try {
            this.containerStack = ByteBufUtils.readItemStackFromBuffer(buf);
        } catch (IOException e) {
            this.containerStack = ItemStack.EMPTY;
        }
    }

    @Override
    public void toBytes(ByteBuf buf)
    {
        buf.writeInt(this.windowId);
        buf.writeBoolean(this.syncInventory);
        
        ItemStack stackToSend = this.containerStack;
        
        if (!this.syncInventory && !this.containerStack.isEmpty() && this.containerStack.hasTagCompound())
        {
            stackToSend = this.containerStack.copy();
            NBTTagCompound nbt = stackToSend.getTagCompound();
            nbt.removeTag("Items");
            nbt.removeTag("MemoryCards");
            nbt.removeTag("Inventory");
        }
        
        ByteBufUtils.writeItemStackToBuffer(buf, stackToSend);
    }

    public static class Handler implements IMessageHandler<SyncBagPacket, IMessage>
    {
        @Override
        public IMessage onMessage(final SyncBagPacket message, MessageContext ctx)
        {
            if (ctx.side != Side.CLIENT)
            {
                TFStorage.logger.error("Wrong side in SyncBagPacket: " + ctx.side);
                return null;
            }

            handleClientSide(message);
            return null;
        }

        @SideOnly(Side.CLIENT)
        private void handleClientSide(final SyncBagPacket message)
        {
            Minecraft.getMinecraft().addScheduledTask(new Runnable()
            {
                public void run()
                {
                    processMessage(message);
                }
            });
        }

        @SideOnly(Side.CLIENT)
        protected void processMessage(final SyncBagPacket message)
        {
            EntityPlayer player = Minecraft.getMinecraft().player;
            
            if (player != null && player.openContainer.windowId == message.windowId)
            {
                if (player.openContainer instanceof BagContainer)
                {
                    BagContainer container = (BagContainer) player.openContainer;
                    
                    if (!message.containerStack.isEmpty())
                    {
                        if (message.syncInventory)
                        {
                            container.inventoryItemWithMemoryCards.setModularItemStack(message.containerStack.copy());
                        }
                        else
                        {
                            ItemStack currentStack = container.inventoryItemWithMemoryCards.getModularItemStack();
                            if (!currentStack.isEmpty() && currentStack.hasTagCompound() && message.containerStack.hasTagCompound())
                            {
                                currentStack.getTagCompound().merge(message.containerStack.getTagCompound());
                            }
                        }
                        
                        container.inventoryItemWithMemoryCards.readFromContainerItemStack();
                        
                        container.inventoryItemWithMemoryCards.markDirty();
                    }

                }
            }
        }
    }
}
