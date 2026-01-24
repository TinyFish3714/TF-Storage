package tf.storage.network.packet;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.item.ItemStack;
import net.minecraft.world.WorldServer;
import net.minecraftforge.fml.common.network.simpleimpl.IMessage;
import net.minecraftforge.fml.common.network.simpleimpl.IMessageHandler;
import net.minecraftforge.fml.common.network.simpleimpl.MessageContext;
import net.minecraftforge.fml.relauncher.Side;
import tf.storage.TFStorage;
import tf.storage.item.TFBag;
import tf.storage.core.ModInfo;
import tf.storage.util.NBTHelper;
import io.netty.buffer.ByteBuf;

public class OpenGuiPacket implements IMessage
{
    private int dimension;
    private int guiId;

    public OpenGuiPacket()
    {
    }

    public OpenGuiPacket(int dim, int guiId)
    {
        this.dimension = dim;
        this.guiId = guiId;
    }

    @Override
    public void fromBytes(ByteBuf buf)
    {
        this.dimension = buf.readInt();
        this.guiId = buf.readInt();
    }

    @Override
    public void toBytes(ByteBuf buf)
    {
        buf.writeInt(this.dimension);
        buf.writeInt(this.guiId);
    }

    public static class Handler implements IMessageHandler<OpenGuiPacket, IMessage>
    {
        @Override
        public IMessage onMessage(final OpenGuiPacket message, MessageContext ctx)
        {
            if (ctx.side != Side.SERVER)
            {
                TFStorage.logger.error("Wrong side in OpenGuiPacket: " + ctx.side);
                return null;
            }

            final EntityPlayerMP sendingPlayer = ctx.getServerHandler().player;
            if (sendingPlayer == null)
            {
                TFStorage.logger.error("player was null in OpenGuiPacket");
                return null;
            }

            final WorldServer playerWorldServer = sendingPlayer.getServerWorld();
            if (playerWorldServer == null)
            {
                TFStorage.logger.error("World was null in OpenGuiPacket");
                return null;
            }

            playerWorldServer.addScheduledTask(new Runnable()
            {
                public void run()
                {
                    processMessage(message, sendingPlayer);
                }
            });

            return null;
        }

        protected void processMessage(final OpenGuiPacket message, EntityPlayer player)
        {
            switch (message.guiId)
            {
                case ModInfo.GUI_ID_TF_BAG:
                    ItemStack stack = TFBag.getOpenableBag(player);

                    if (stack.isEmpty() == false)
                    {
                        // These two lines are to fix the UUID being missing the first time the GUI opens,
                        // if the item is grabbed from the creative inventory or from JEI or from /give
                        NBTHelper.getUUIDFromItemStack(stack, "UUID", true);
                        player.openContainer.detectAndSendChanges();
                        player.openGui(TFStorage.instance, message.guiId, player.getEntityWorld(),
                                (int)player.posX, (int)player.posY, (int)player.posZ);
                    }
                    break;
                default:
            }
        }
    }
}
