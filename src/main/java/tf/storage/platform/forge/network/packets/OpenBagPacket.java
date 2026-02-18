package tf.storage.platform.forge.network.packets;

import java.util.function.Supplier;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.network.NetworkEvent;
import net.minecraftforge.network.NetworkHooks;
import tf.storage.core.util.NBTHelper;
import tf.storage.platform.forge.TFStorageMod;
import tf.storage.platform.forge.item.TFBagItem;
import tf.storage.platform.forge.menu.provider.BagMenuProvider;

/**
 * Packet to request opening the TF Bag GUI.
 * Sent from client to server when player presses the keybind.
 */
public class OpenBagPacket {
    
    public OpenBagPacket() {
    }
    
    public void encode(FriendlyByteBuf buf) {
        // No data needed
    }
    
    public static OpenBagPacket decode(FriendlyByteBuf buf) {
        return new OpenBagPacket();
    }
    
    public void handle(Supplier<NetworkEvent.Context> ctx) {
        NetworkEvent.Context context = ctx.get();
        context.enqueueWork(() -> {
            ServerPlayer player = context.getSender();
            if (player == null) {
                TFStorageMod.LOGGER.error("OpenBagPacket: player is null");
                return;
            }
            
            ItemStack bagStack = TFBagItem.getOpenableBag(player);
            
            if (!bagStack.isEmpty()) {
                // Ensure UUID exists
                NBTHelper.getUUIDFromItemStack(bagStack, "UUID", true);
                
                // Sync container changes
                if (player.containerMenu != null) {
                    player.containerMenu.broadcastChanges();
                }
                
                // Open bag menu
                NetworkHooks.openScreen(player, new BagMenuProvider(bagStack), buf -> {
                    buf.writeItem(bagStack);
                });
                
                TFStorageMod.LOGGER.debug("OpenBagPacket: Opening bag GUI for player {}", player.getName().getString());
            }
        });
        context.setPacketHandled(true);
    }
}
