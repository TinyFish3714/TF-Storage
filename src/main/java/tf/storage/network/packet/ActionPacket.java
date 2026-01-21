package tf.storage.network.packet;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.minecraft.world.WorldServer;
import net.minecraftforge.fml.common.FMLCommonHandler;
import net.minecraftforge.fml.common.network.simpleimpl.IMessage;
import net.minecraftforge.fml.common.network.simpleimpl.IMessageHandler;
import net.minecraftforge.fml.common.network.simpleimpl.MessageContext;
import net.minecraftforge.fml.relauncher.Side;
import tf.storage.TFStorage;
import tf.storage.inventory.container.base.BaseContainer;
import tf.storage.item.TFBag;
import tf.storage.core.ModInfo;
import tf.storage.tile.TileChest;
import io.netty.buffer.ByteBuf;

public class ActionPacket implements IMessage
{
    private int guiId;
    private int action;
    private int elementId;
    private int dimension;
    private int posX;
    private int posY;
    private int posZ;

    public ActionPacket()
    {
    }

    public ActionPacket(int dim, BlockPos pos, int guiId, int action, int elementId)
    {
        this.dimension = dim;
        this.posX = pos.getX();
        this.posY = pos.getY();
        this.posZ = pos.getZ();
        this.guiId = guiId;
        this.action = action;
        this.elementId = elementId;
    }

    @Override
    public void fromBytes(ByteBuf buf)
    {
        this.dimension = buf.readInt();
        this.posX = buf.readInt();
        this.posY = buf.readInt();
        this.posZ = buf.readInt();
        this.guiId = buf.readInt();
        this.action = buf.readInt();
        this.elementId = buf.readInt();
    }

    @Override
    public void toBytes(ByteBuf buf)
    {
        buf.writeInt(this.dimension);
        buf.writeInt(this.posX);
        buf.writeInt(this.posY);
        buf.writeInt(this.posZ);
        buf.writeInt(this.guiId);
        buf.writeInt(this.action);
        buf.writeInt(this.elementId);
    }

    public static class Handler implements IMessageHandler<ActionPacket, IMessage>
    {
        @Override
        public IMessage onMessage(final ActionPacket message, MessageContext ctx)
        {
            if (ctx.side != Side.SERVER)
            {
                TFStorage.logger.error("ActionPacket 端错误: " + ctx.side);
                return null;
            }

            final EntityPlayerMP sendingPlayer = ctx.getServerHandler().player;
            if (sendingPlayer == null)
            {
                TFStorage.logger.error("ActionPacket 发送玩家为 null");
                return null;
            }

            final WorldServer playerWorldServer = sendingPlayer.getServerWorld();
            if (playerWorldServer == null)
            {
                TFStorage.logger.error("ActionPacket 世界为 null");
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

        protected void processMessage(final ActionPacket message, EntityPlayer player)
        {
            World world = FMLCommonHandler.instance().getMinecraftServerInstance().getWorld(message.dimension);

            if (world != null)
            {
                switch(message.guiId)
                {
                    case ModInfo.GUI_ID_TILE_ENTITY_GENERIC:
                        BlockPos pos = new BlockPos(message.posX, message.posY, message.posZ);

                        if (world.isBlockLoaded(pos))
                        {
                            TileEntity te = world.getTileEntity(pos);
                            if (te != null && te instanceof TileChest)
                            {
                                ((TileChest) te).performGuiAction(player, message.action, message.elementId);
                            }
                        }
                        break;

                    case ModInfo.GUI_ID_TF_BAG:
                        TFBag.performGuiAction(player, message.action, message.elementId);
                        break;

                    default:
                }
            }
        }
    }
}
