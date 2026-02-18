package tf.storage.platform.forge.network.packets;

import net.minecraft.client.Minecraft;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.entity.player.Player;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.network.NetworkEvent;
import tf.storage.platform.forge.TFStorageMod;
import tf.storage.platform.forge.menu.BaseMenu;
import tf.storage.core.util.NBTHelper;

import java.util.function.Supplier;

/**
 * Packet to sync a single slot from server to client.
 * Used for large stack sizes that vanilla can't handle.
 */
public class SyncSlotPacket {
    
    private final int containerId;
    private final int slotNum;
    private final ItemStack stack;
    
    public SyncSlotPacket(int containerId, int slotNum, ItemStack stack) {
        this.containerId = containerId;
        this.slotNum = slotNum;
        this.stack = stack;
    }
    
    public void encode(FriendlyByteBuf buf) {
        buf.writeInt(containerId);
        buf.writeShort(slotNum);
        if (stack.isEmpty()) {
            buf.writeNbt(null);
        } else {
            CompoundTag tag = NBTHelper.storeItemStackInTag(stack, new CompoundTag());
            buf.writeNbt(tag);
        }
    }
    
    public static SyncSlotPacket decode(FriendlyByteBuf buf) {
        int containerId = buf.readInt();
        int slotNum = buf.readShort();
        CompoundTag tag = buf.readNbt();
        ItemStack stack = tag != null ? NBTHelper.loadItemStackFromTag(tag) : ItemStack.EMPTY;
        return new SyncSlotPacket(containerId, slotNum, stack);
    }
    
    public void handle(Supplier<NetworkEvent.Context> ctx) {
        NetworkEvent.Context context = ctx.get();
        context.enqueueWork(() -> {
            handleClient();
        });
        context.setPacketHandled(true);
    }
    
    @OnlyIn(Dist.CLIENT)
    private void handleClient() {
        Minecraft mc = Minecraft.getInstance();
        Player player = mc.player;
        
        if (player == null) {
            TFStorageMod.LOGGER.error("SyncSlotPacket: player is null");
            return;
        }
        
        if (player.containerMenu instanceof BaseMenu menu && containerId == menu.containerId) {
            menu.syncStackInSlot(slotNum, stack);
        }
    }
}
