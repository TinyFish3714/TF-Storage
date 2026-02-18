package tf.storage.platform.forge.network;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkDirection;
import net.minecraftforge.network.NetworkRegistry;
import net.minecraftforge.network.PacketDistributor;
import net.minecraftforge.network.simple.SimpleChannel;
import tf.storage.platform.forge.TFStorageMod;
import tf.storage.platform.forge.network.packets.ActionPacket;
import tf.storage.platform.forge.network.packets.OpenBagPacket;
import tf.storage.platform.forge.network.packets.SyncSlotPacket;
import tf.storage.platform.forge.network.packets.SyncMultipleSlotsPacket;

import java.util.Optional;

/**
 * Network handler for TF Storage mod.
 * Uses SimpleChannel for packet registration and handling.
 */
public class ModNetworking {
    
    private static final String PROTOCOL_VERSION = "1";
    
    public static final SimpleChannel CHANNEL = NetworkRegistry.newSimpleChannel(
        ResourceLocation.parse(TFStorageMod.MOD_ID + ":main"),
        () -> PROTOCOL_VERSION,
        PROTOCOL_VERSION::equals,
        PROTOCOL_VERSION::equals
    );
    
    private static int packetId = 0;
    
    private static int nextId() {
        return packetId++;
    }
    
    public static void register() {
        // Server-bound packets (client -> server)
        CHANNEL.messageBuilder(ActionPacket.class, nextId(), NetworkDirection.PLAY_TO_SERVER)
            .encoder(ActionPacket::encode)
            .decoder(ActionPacket::decode)
            .consumerMainThread(ActionPacket::handle)
            .add();
        
        CHANNEL.messageBuilder(OpenBagPacket.class, nextId(), NetworkDirection.PLAY_TO_SERVER)
            .encoder(OpenBagPacket::encode)
            .decoder(OpenBagPacket::decode)
            .consumerMainThread(OpenBagPacket::handle)
            .add();
        
        // Client-bound packets (server -> client)
        CHANNEL.messageBuilder(SyncSlotPacket.class, nextId(), NetworkDirection.PLAY_TO_CLIENT)
            .encoder(SyncSlotPacket::encode)
            .decoder(SyncSlotPacket::decode)
            .consumerMainThread(SyncSlotPacket::handle)
            .add();
        
        CHANNEL.messageBuilder(SyncMultipleSlotsPacket.class, nextId(), NetworkDirection.PLAY_TO_CLIENT)
            .encoder(SyncMultipleSlotsPacket::encode)
            .decoder(SyncMultipleSlotsPacket::decode)
            .consumerMainThread(SyncMultipleSlotsPacket::handle)
            .add();
        
        TFStorageMod.LOGGER.info("TF Storage network packets registered");
    }
    
    /**
     * Send a packet to a specific player.
     */
    public static <MSG> void sendToPlayer(MSG message, ServerPlayer player) {
        CHANNEL.send(PacketDistributor.PLAYER.with(() -> player), message);
    }
    
    /**
     * Send a packet to the server.
     */
    public static <MSG> void sendToServer(MSG message) {
        CHANNEL.sendToServer(message);
    }
    
    /**
     * Send a packet to all players.
     */
    public static <MSG> void sendToAll(MSG message) {
        CHANNEL.send(PacketDistributor.ALL.noArg(), message);
    }
}
