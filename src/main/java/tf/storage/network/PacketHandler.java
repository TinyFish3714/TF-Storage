package tf.storage.network;

import net.minecraftforge.fml.common.network.NetworkRegistry;
import net.minecraftforge.fml.common.network.simpleimpl.SimpleNetworkWrapper;
import net.minecraftforge.fml.relauncher.Side;
import tf.storage.network.packet.ActionPacket;
import tf.storage.network.packet.OpenGuiPacket;
import tf.storage.network.packet.SyncSlotPacket;
import tf.storage.network.packet.SyncMultipleSlotsPacket;
import tf.storage.network.packet.SyncBagPacket;
import tf.storage.core.ModInfo;

public class PacketHandler
{
    public static final SimpleNetworkWrapper INSTANCE = NetworkRegistry.INSTANCE.newSimpleChannel(ModInfo.MOD_ID.toLowerCase());

    public static void init()
    {
        INSTANCE.registerMessage(ActionPacket.Handler.class,        ActionPacket.class,         0, Side.SERVER);
        INSTANCE.registerMessage(OpenGuiPacket.Handler.class,          OpenGuiPacket.class,           1, Side.SERVER);
        INSTANCE.registerMessage(SyncSlotPacket.Handler.class,         SyncSlotPacket.class,          2, Side.CLIENT);
        INSTANCE.registerMessage(SyncMultipleSlotsPacket.Handler.class, SyncMultipleSlotsPacket.class, 3, Side.CLIENT);
        INSTANCE.registerMessage(SyncBagPacket.Handler.class, SyncBagPacket.class, 4, Side.CLIENT);
    }
}
