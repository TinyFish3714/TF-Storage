package tf.storage.platform.forge.network.packets;

import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraftforge.network.NetworkEvent;
import tf.storage.platform.forge.TFStorageMod;
import tf.storage.platform.forge.block.TFChestBlockEntity;
import tf.storage.platform.forge.item.TFBagItem;
import tf.storage.platform.forge.menu.BagMenu;
import tf.storage.platform.forge.network.GuiActions;
import tf.storage.platform.forge.network.ModNetworking;
import tf.storage.platform.forge.network.packets.SyncSlotPacket;

import java.util.function.Supplier;

/**
 * Packet for GUI actions (button clicks, etc.)
 * Sent from client to server.
 */
public class ActionPacket {
    
    private final int guiType;
    private final int action;
    private final int elementId;
    private final BlockPos pos;
    
    public ActionPacket(int guiType, int action, int elementId, BlockPos pos) {
        this.guiType = guiType;
        this.action = action;
        this.elementId = elementId;
        this.pos = pos;
    }
    
    /**
     * Create an action packet for TF Bag.
     */
    public static ActionPacket forBag(int action, int elementId) {
        return new ActionPacket(GuiActions.GuiType.BAG, action, elementId, BlockPos.ZERO);
    }
    
    /**
     * Create an action packet for TF Chest.
     */
    public static ActionPacket forChest(int action, int elementId, BlockPos pos) {
        return new ActionPacket(GuiActions.GuiType.CHEST, action, elementId, pos);
    }
    
    public void encode(FriendlyByteBuf buf) {
        buf.writeInt(guiType);
        buf.writeInt(action);
        buf.writeInt(elementId);
        buf.writeBlockPos(pos);
    }
    
    public static ActionPacket decode(FriendlyByteBuf buf) {
        int guiType = buf.readInt();
        int action = buf.readInt();
        int elementId = buf.readInt();
        BlockPos pos = buf.readBlockPos();
        return new ActionPacket(guiType, action, elementId, pos);
    }
    
    public void handle(Supplier<NetworkEvent.Context> ctx) {
        NetworkEvent.Context context = ctx.get();
        context.enqueueWork(() -> {
            ServerPlayer player = context.getSender();
            if (player == null) {
                TFStorageMod.LOGGER.error("ActionPacket: player is null");
                return;
            }
            
            ServerLevel level = player.serverLevel();
            
            switch (guiType) {
                case GuiActions.GuiType.CHEST -> {
                    if (level.isLoaded(pos)) {
                        BlockEntity be = level.getBlockEntity(pos);
                        if (be instanceof TFChestBlockEntity chest) {
                            chest.performGuiAction(player, action, elementId);
                            if (player.containerMenu instanceof tf.storage.platform.forge.menu.ChestMenu menu) {
                                menu.broadcastChanges();
                            }
                        }
                    }
                }
                case GuiActions.GuiType.BAG -> {
                    TFBagItem.performGuiAction(player, action, elementId);
                    if (player.containerMenu instanceof BagMenu bagMenu) {
                        bagMenu.refreshAfterAction();
                        bagMenu.broadcastChanges();
                        int slotIndex = bagMenu.getBagMenuSlotIndex();
                        if (slotIndex >= 0) {
                            ModNetworking.sendToPlayer(new SyncSlotPacket(bagMenu.containerId, slotIndex, bagMenu.getContainerItem()), player);
                        }
                    }
                }
            }
        });
        context.setPacketHandled(true);
    }
}
