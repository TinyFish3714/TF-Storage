package tf.storage.platform.forge.network.packets;

import io.netty.buffer.Unpooled;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;
import net.minecraft.client.Minecraft;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.network.NetworkEvent;
import tf.storage.core.util.NBTHelper;
import tf.storage.platform.forge.TFStorageMod;
import tf.storage.platform.forge.menu.BaseMenu;

/**
 * Packet to sync multiple slots from server to client.
 * More efficient than sending individual SyncSlotPackets.
 */
public class SyncMultipleSlotsPacket {
    
    private static final int MAX_SLOTS_PER_PACKET = 64;
    private static final int MAX_PACKET_BYTES = 32 * 1024;
    private static final int BASE_PACKET_BYTES = 6;
    
    private final int containerId;
    private final int[] slotIds;
    private final ItemStack[] stacks;
    
    public SyncMultipleSlotsPacket(int containerId, List<Integer> dirtySlots, List<ItemStack> newStacks) {
        this.containerId = containerId;

        int size = dirtySlots.size();
        this.slotIds = new int[size];
        this.stacks = new ItemStack[size];

        for (int i = 0; i < size; i++) {
            this.slotIds[i] = dirtySlots.get(i);
            this.stacks[i] = newStacks.get(i);
        }
    }
    
    private SyncMultipleSlotsPacket(int containerId, int[] slotIds, ItemStack[] stacks) {
        this.containerId = containerId;
        this.slotIds = slotIds;
        this.stacks = stacks;
    }

    public static List<SyncMultipleSlotsPacket> splitPackets(int containerId, List<Integer> slotIds, List<ItemStack> stacks) {
        List<SyncMultipleSlotsPacket> packets = new ArrayList<>();
        if (slotIds.isEmpty()) {
            return packets;
        }

        List<Integer> currentSlotIds = new ArrayList<>();
        List<ItemStack> currentStacks = new ArrayList<>();
        int currentBytes = BASE_PACKET_BYTES;

        for (int i = 0; i < slotIds.size(); i++) {
            int entryBytes = estimateEntryBytes(stacks.get(i));

            boolean wouldOverflow = currentBytes + entryBytes > MAX_PACKET_BYTES;
            boolean slotLimitReached = currentSlotIds.size() >= MAX_SLOTS_PER_PACKET;

            if (!currentSlotIds.isEmpty() && (wouldOverflow || slotLimitReached)) {
                packets.add(buildPacket(containerId, currentSlotIds, currentStacks));
                currentSlotIds = new ArrayList<>();
                currentStacks = new ArrayList<>();
                currentBytes = BASE_PACKET_BYTES;
            }

            currentSlotIds.add(slotIds.get(i));
            currentStacks.add(stacks.get(i));
            currentBytes += entryBytes;
        }

        if (!currentSlotIds.isEmpty()) {
            packets.add(buildPacket(containerId, currentSlotIds, currentStacks));
        }

        return packets;
    }

    private static SyncMultipleSlotsPacket buildPacket(int containerId, List<Integer> slotIds, List<ItemStack> stacks) {
        int[] slotIdsArray = new int[slotIds.size()];
        ItemStack[] stacksArray = new ItemStack[slotIds.size()];

        for (int i = 0; i < slotIds.size(); i++) {
            slotIdsArray[i] = slotIds.get(i);
            stacksArray[i] = stacks.get(i);
        }

        return new SyncMultipleSlotsPacket(containerId, slotIdsArray, stacksArray);
    }

    private static int estimateEntryBytes(ItemStack stack) {
        return 2 + estimateNbtBytes(stack);
    }

    private static int estimateNbtBytes(ItemStack stack) {
        if (stack.isEmpty()) {
            return 1;
        }
        FriendlyByteBuf buf = new FriendlyByteBuf(Unpooled.buffer());
        try {
            CompoundTag tag = NBTHelper.storeItemStackInTag(stack, new CompoundTag());
            buf.writeNbt(tag);
            return buf.readableBytes();
        } finally {
            buf.release();
        }
    }
    
    public void encode(FriendlyByteBuf buf) {
        buf.writeInt(containerId);
        buf.writeShort(slotIds.length);

        for (int i = 0; i < slotIds.length; i++) {
            buf.writeShort(slotIds[i]);
            ItemStack stack = stacks[i];
            if (stack.isEmpty()) {
                buf.writeNbt(null);
            } else {
                CompoundTag tag = NBTHelper.storeItemStackInTag(stack, new CompoundTag());
                buf.writeNbt(tag);
            }
        }
    }
    
    public static SyncMultipleSlotsPacket decode(FriendlyByteBuf buf) {
        int containerId = buf.readInt();
        int count = buf.readShort();
        int[] slotIds = new int[count];
        ItemStack[] stacks = new ItemStack[count];

        for (int i = 0; i < count; i++) {
            slotIds[i] = buf.readShort();
            CompoundTag tag = buf.readNbt();
            stacks[i] = tag != null ? NBTHelper.loadItemStackFromTag(tag) : ItemStack.EMPTY;
        }

        return new SyncMultipleSlotsPacket(containerId, slotIds, stacks);
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
            TFStorageMod.LOGGER.error("SyncMultipleSlotsPacket: player is null");
            return;
        }
        
        if (player.containerMenu instanceof BaseMenu menu && containerId == menu.containerId) {
            for (int i = 0; i < slotIds.length; i++) {
                menu.syncStackInSlot(slotIds[i], stacks[i]);
            }
        }
    }
}
