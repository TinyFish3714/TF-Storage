package tf.storage.event;

import java.util.UUID;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.InventoryPlayer;
import net.minecraft.item.ItemStack;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.PlayerEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent.Phase;
import net.minecraftforge.fml.common.gameevent.TickEvent.PlayerTickEvent;
import net.minecraftforge.fml.relauncher.Side;
import tf.storage.inventory.handler.ModularHandler;
import tf.storage.item.TFBag;
import tf.storage.item.TFBag.RestockMode;
import tf.storage.core.ModInfo;
import tf.storage.core.ModItems;
import tf.storage.util.StackHelper;

@Mod.EventBusSubscriber(modid = ModInfo.MOD_ID)
public class PlayerEvents {

    private static final java.util.Map<java.util.UUID, ItemStack[]> lastHotbarSnapshot = new java.util.HashMap<>();
    
    public static void clearSnapshots() {
        lastHotbarSnapshot.clear();
        // lastTossTick.clear(); // 如果重新启用，也需要清理
    }

    @SubscribeEvent
    public static void onPlayerTick(PlayerTickEvent event) {
        // 只在服务器端、tick结束时执行
        if (event.side == Side.SERVER && event.phase == Phase.END) {
            EntityPlayer player = event.player;

            // 1. 检查玩家对象、背包是否有效
            if (player == null || player.isDead || player.inventory == null) {
                return;
            }
            
            // 2. 检查玩家UUID是否已加载（这是导致崩溃的直接原因）
            UUID playerUUID = player.getUniqueID();
            if (playerUUID == null) {
                // 如果UUID尚未加载，我们不能执行任何操作，因为无法在Map中存储/检索数据。
                // 直接返回，等待下一个tick。
                return;
            }

            // 性能优化：每 5 ticks (0.25秒) 检查一次，减少 GC 压力
            // 补货不需要达到 tick 级的精度
            if (player.ticksExisted % 5 != 0) {
                return;
            }

            // 防误触机制 1: 如果玩家打开了外部容器（箱子等），禁止自动补货，防止整理背包时误触发
            // player.inventoryContainer 是玩家自带的背包界面，始终存在，所以不拦截它
            if (player.openContainer != player.inventoryContainer) {
                return;
            }

            // 防误触机制 2: 如果玩家鼠标抓着物品（说明正在移动物品），禁止自动补货
            if (!player.inventory.getItemStack().isEmpty()) {
                return;
            }

            // 扩充快照大小以支持副手：0-8为主手快捷栏，9为副手

            int snapshotSize = InventoryPlayer.getHotbarSize() + 1;
            ItemStack[] lastHotbar = lastHotbarSnapshot.computeIfAbsent(playerUUID, k -> new ItemStack[snapshotSize]);

            ItemStack[] currentHotbar = new ItemStack[snapshotSize];

            // 复制主手快捷栏
            for (int i = 0; i < InventoryPlayer.getHotbarSize(); i++) {
                currentHotbar[i] = player.inventory.getStackInSlot(i).copy();
            }
            // 复制副手
            currentHotbar[snapshotSize - 1] = player.getHeldItemOffhand().copy();

            // 检查主手快捷栏
            for (int i = 0; i < InventoryPlayer.getHotbarSize(); i++) {
                // 仅当该槽位是玩家当前选中的主手槽位时，才尝试补货
                // 这避免了整理背包时，非当前手持的物品被意外补货
                if (i == player.inventory.currentItem) {
                    checkAndReplenish(player, i, lastHotbar[i], currentHotbar[i]);
                }
            }

            // 检查副手
            int offhandIndex = snapshotSize - 1;
            // 始终检查副手
            checkAndReplenish(player, 40, lastHotbar[offhandIndex], currentHotbar[offhandIndex]); // 40 is offhand slot index in InventoryPlayer
            
            lastHotbarSnapshot.put(playerUUID, currentHotbar);
        }
    }
    
    private static void checkAndReplenish(EntityPlayer player, int slotIndex, ItemStack lastStack, ItemStack currentStack) {
        // 检查 lastStack 是否为 null (初始化时)
        if (lastStack != null && !lastStack.isEmpty() && currentStack.isEmpty()) {
            tryReplenishSlot(player, slotIndex, lastStack);
        }
    }

    /**
     * 监听玩家登出事件，用于清理内存中的快照数据，防止内存泄漏。
     */
    @SubscribeEvent
    public static void onPlayerLogout(PlayerEvent.PlayerLoggedOutEvent event) {
        // 同样进行安全检查
        if (event.player != null) {
            UUID playerUUID = event.player.getUniqueID();
            if (playerUUID != null) {
                lastHotbarSnapshot.remove(playerUUID);
                // lastTossTick.remove(playerUUID);
            }

        }
    }

    private static void tryReplenishSlot(EntityPlayer player, int slot, ItemStack depletedStack) {
        // 遍历玩家整个背包，寻找开启了补货模式的便利袋
        for (int i = 0; i < player.inventory.getSizeInventory(); i++) {
            ItemStack bagStack = player.inventory.getStackInSlot(i);
            if (!bagStack.isEmpty() && bagStack.getItem() == ModItems.TF_BAG) {
                RestockMode mode = RestockMode.fromStack(bagStack);

                if (mode == RestockMode.ON) {
                    ModularHandler bagInv = TFBag.getInventoryForBag(bagStack, player);
                    if (bagInv != null) {
                        int sourceSlot = StackHelper.getSlotOfFirstMatchingItemStack(bagInv, depletedStack);
                        if (sourceSlot != -1) {
                            ItemStack stackToReplenish = bagInv.extractItem(sourceSlot, depletedStack.getMaxStackSize(), false);
                            if (!stackToReplenish.isEmpty()) {
                                player.inventory.setInventorySlotContents(slot, stackToReplenish);
                                return;
                            }
                        }
                    }
                }
            }
        }
    }
}
