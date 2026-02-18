package tf.storage.platform.forge.event;

import java.lang.ref.WeakReference;
import java.util.Iterator;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ArmorItem;
import net.minecraft.world.item.BowItem;
import net.minecraft.world.item.HoeItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ShearsItem;
import net.minecraft.world.item.SwordItem;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.event.entity.player.EntityItemPickupEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.LogicalSide;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.items.IItemHandler;
import net.minecraftforge.items.ItemHandlerHelper;
import tf.storage.platform.forge.TFStorageMod;
import tf.storage.platform.forge.ModRegistry;
import tf.storage.platform.forge.item.TFBagItem;
import tf.storage.platform.forge.item.TFBagItem.PickupMode;
import tf.storage.platform.forge.item.TFBagItem.RestockMode;
import tf.storage.platform.forge.inventory.handler.ModularHandler;
import tf.storage.core.util.StackHelper;

/**
 * PlayerEvents - 玩家事件处理
 * 主要功能：物品自动补货（从TF包补充到快捷栏）
 * 
 * 适配 Minecraft 1.20.1
 */
@Mod.EventBusSubscriber(modid = TFStorageMod.MOD_ID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public class PlayerEvents {

    /**
     * 使用WeakReference存储玩家引用，防止内存泄漏
     * 当玩家对象被垃圾回收时，对应的快照数据也会被自动清理
     */
    private static final Map<UUID, PlayerSnapshot> lastHotbarSnapshot = new ConcurrentHashMap<>();
    
    /**
     * 定期清理间隔（tick）
     * 每1200 tick（约60秒）清理一次无效的快照数据
     */
    private static final int CLEANUP_INTERVAL = 1200;
    
    /**
     * 上次清理的时间戳
     */
    private static long lastCleanupTick = 0;
    
    /**
     * 玩家快照数据，包含快捷栏数据和弱引用的玩家引用
     */
    private static class PlayerSnapshot {
        final WeakReference<Player> playerRef;
        ItemStack[] hotbarData;
        
        PlayerSnapshot(Player player, ItemStack[] data) {
            this.playerRef = new WeakReference<>(player);
            this.hotbarData = data;
        }
        
        /**
         * 检查玩家是否仍然有效（在线且未被垃圾回收）
         */
        boolean isValid() {
            Player player = playerRef.get();
            return player != null && player.isAlive() && !player.isRemoved();
        }
    }
    
    public static void clearSnapshots() {
        lastHotbarSnapshot.clear();
    }
    
    /**
     * 清理无效的快照数据（玩家已离线或被垃圾回收）
     */
    private static void cleanupInvalidSnapshots() {
        Iterator<Map.Entry<UUID, PlayerSnapshot>> iterator = lastHotbarSnapshot.entrySet().iterator();
        while (iterator.hasNext()) {
            Map.Entry<UUID, PlayerSnapshot> entry = iterator.next();
            if (!entry.getValue().isValid()) {
                iterator.remove();
            }
        }
    }

    @SubscribeEvent
    public static void onPlayerTick(TickEvent.PlayerTickEvent event) {
        if (event.side == LogicalSide.SERVER && event.phase == TickEvent.Phase.END) {
            Player player = event.player;

            if (player == null || player.isDeadOrDying() || player.getInventory() == null) {
                return;
            }
             
            UUID playerUUID = player.getUUID();
            if (playerUUID == null) {
                return;
            }

            // 每5 tick检查一次
            if (player.tickCount % 5 != 0) {
                return;
            }

            // 定期清理无效快照（防止内存泄漏）
            if (player.tickCount - lastCleanupTick > CLEANUP_INTERVAL) {
                lastCleanupTick = player.tickCount;
                cleanupInvalidSnapshots();
            }

            // 玩家必须在自己的背包界面（没有打开其他容器）
            if (player.containerMenu != player.inventoryMenu) {
                return;
            }

            // 玩家手上不能拿着物品
            if (!player.containerMenu.getCarried().isEmpty()) {
                return;
            }

            int snapshotSize = Inventory.getSelectionSize() + 1; // 快捷栏 + 副手
            
            // 获取或创建玩家快照
            PlayerSnapshot snapshot = lastHotbarSnapshot.computeIfAbsent(playerUUID, 
                k -> new PlayerSnapshot(player, new ItemStack[snapshotSize]));
            ItemStack[] lastHotbar = snapshot.hotbarData;

            ItemStack[] currentHotbar = new ItemStack[snapshotSize];

            // 记录当前快捷栏状态
            for (int i = 0; i < Inventory.getSelectionSize(); i++) {
                currentHotbar[i] = player.getInventory().getItem(i).copy();
            }
            currentHotbar[snapshotSize - 1] = player.getOffhandItem().copy();

            // 只检查当前选中的槽位
            for (int i = 0; i < Inventory.getSelectionSize(); i++) {
                if (i == player.getInventory().selected) {
                    checkAndReplenish(player, i, lastHotbar[i], currentHotbar[i]);
                }
            }

            // 检查副手槽位 (slot 40)
            checkAndReplenish(player, 40, lastHotbar[snapshotSize - 1], currentHotbar[snapshotSize - 1]);
            
            // 更新快照数据
            snapshot.hotbarData = currentHotbar;
        }
    }

    /**
     * 自动拾取：当玩家拾取地面物品时，尝试放入TF包
     */
    @SubscribeEvent
    public static void onEntityItemPickup(EntityItemPickupEvent event) {
        Player player = event.getEntity();
        ItemEntity entityItem = event.getItem();

        if (player == null || entityItem == null || player.level().isClientSide) {
            return;
        }

        ItemStack itemToPick = entityItem.getItem();
        if (itemToPick.isEmpty() || entityItem.isRemoved()) {
            return;
        }

        // 扫描玩家背包中的TF包
        Inventory playerInv = player.getInventory();
        for (int i = 0; i < playerInv.getContainerSize(); i++) {
            itemToPick = tryPickupIntoBag(playerInv.getItem(i), player, itemToPick, entityItem, event);
            if (itemToPick == null) {
                return;
            }
        }

        // 扫描Curios中的TF包
        if (tf.storage.platform.forge.compat.CuriosCompat.isLoaded()) {
            net.minecraftforge.items.IItemHandler curiosInv = tf.storage.platform.forge.compat.CuriosCompat.getEquippedCurios(player);
            if (curiosInv != null) {
                for (int i = 0; i < curiosInv.getSlots(); i++) {
                    itemToPick = tryPickupIntoBag(curiosInv.getStackInSlot(i), player, itemToPick, entityItem, event);
                    if (itemToPick == null) {
                        return;
                    }
                }
            }
        }
    }

    private static ItemStack tryPickupIntoBag(ItemStack bagStack, Player player, ItemStack itemToPick,
            ItemEntity entityItem, EntityItemPickupEvent event) {
        if (bagStack.isEmpty()) {
            return itemToPick;
        }

        if (bagStack.getItem() != ModRegistry.TF_BAG.get() &&
            bagStack.getItem() != ModRegistry.TF_BAG_LARGE.get()) {
            return itemToPick;
        }

        if (!TFBagItem.bagIsOpenable(bagStack)) {
            return itemToPick;
        }

        PickupMode mode = PickupMode.fromStack(bagStack);
        if (mode == PickupMode.NONE) {
            return itemToPick;
        }

        ModularHandler bagInv = getInventoryForBag(bagStack, player);
        if (bagInv == null) {
            return itemToPick;
        }

        boolean shouldPickup = mode == PickupMode.ALL;
        if (!shouldPickup && mode == PickupMode.MATCHING) {
            shouldPickup = StackHelper.getSlotOfFirstMatchingItemStack(bagInv, itemToPick) != -1;
        }

        if (!shouldPickup) {
            return itemToPick;
        }

        ItemStack originalStack = itemToPick.copy();
        ItemStack remainder = ItemHandlerHelper.insertItemStacked(bagInv, itemToPick, false);

        if (remainder.getCount() < originalStack.getCount()) {
            if (!entityItem.isSilent()) {
                player.level().playSound(null, player.getX(), player.getY(), player.getZ(),
                    SoundEvents.ITEM_PICKUP, SoundSource.PLAYERS, 0.2F,
                    ((player.getRandom().nextFloat() - player.getRandom().nextFloat()) * 0.7F + 1.0F) * 2.0F);
            }

            if (remainder.isEmpty()) {
                entityItem.discard();
                event.setCanceled(true);
                return null;
            }

            entityItem.setItem(remainder);
            return remainder;
        }

        return itemToPick;
    }
    
    private static void checkAndReplenish(Player player, int slotIndex, ItemStack lastStack, ItemStack currentStack) {
        if (lastStack != null && !lastStack.isEmpty() && currentStack.isEmpty()) {
            tryReplenishSlot(player, slotIndex, lastStack);
        }
    }

    /**
     * 监听玩家登出事件，清理内存中的快照数据，防止内存泄漏
     */
    @SubscribeEvent
    public static void onPlayerLogout(PlayerEvent.PlayerLoggedOutEvent event) {
        if (event.getEntity() != null) {
            UUID playerUUID = event.getEntity().getUUID();
            if (playerUUID != null) {
                lastHotbarSnapshot.remove(playerUUID);
            }
        }
    }

    /**
     * 尝试从TF包中补充物品到指定槽位
     */
    private static void tryReplenishSlot(Player player, int slot, ItemStack depletedStack) {
        Inventory playerInv = player.getInventory();

        // 检查玩家背包中的TF包
        for (int i = 0; i < playerInv.getContainerSize(); i++) {
            if (tryRestockFromBag(player, playerInv.getItem(i), depletedStack, slot)) {
                return;
            }
        }

        // 检查Curios中的TF包
        if (tf.storage.platform.forge.compat.CuriosCompat.isLoaded()) {
            net.minecraftforge.items.IItemHandler curiosInv = tf.storage.platform.forge.compat.CuriosCompat.getEquippedCurios(player);
            if (curiosInv != null) {
                for (int i = 0; i < curiosInv.getSlots(); i++) {
                    if (tryRestockFromBag(player, curiosInv.getStackInSlot(i), depletedStack, slot)) {
                        return;
                    }
                }
            }
        }
    }

    private static boolean tryRestockFromBag(Player player, ItemStack bagStack, ItemStack depletedStack, int slot) {
        if (bagStack.isEmpty() || (bagStack.getItem() != ModRegistry.TF_BAG.get() &&
            bagStack.getItem() != ModRegistry.TF_BAG_LARGE.get())) {
            return false;
        }

        RestockMode mode = RestockMode.fromStack(bagStack);
        if (mode != RestockMode.ON) {
            return false;
        }

        ModularHandler bagInv = getInventoryForBag(bagStack, player);
        if (bagInv == null) {
            return false;
        }

        int sourceSlot = findBestReplenishSlot(bagInv, depletedStack);
        if (sourceSlot == -1) {
            return false;
        }

        ItemStack stackToReplenish = bagInv.extractItem(sourceSlot, depletedStack.getMaxStackSize(), false);
        if (stackToReplenish.isEmpty()) {
            return false;
        }

        player.getInventory().setItem(slot, stackToReplenish);
        return true;
    }

    /**
     * 查找最佳补货槽位
     * 优先级：
     * 1. 完全匹配（包括NBT）
     * 2. 相同物品注册名（忽略NBT和耐久度）
     * 3. 等效物品（同类型工具/武器/护甲）
     */
    private static int findBestReplenishSlot(IItemHandler inv, ItemStack template) {
        // 1. 首先尝试找到完全匹配的物品（包括NBT）
        int exactMatch = StackHelper.getSlotOfFirstMatchingItemStack(inv, template);
        if (exactMatch != -1) {
            return exactMatch;
        }

        // 2. 尝试找相同物品注册名的物品（忽略NBT和耐久度）
        int sameItem = findSlotWithSameItem(inv, template);
        if (sameItem != -1) {
            return sameItem;
        }

        // 3. 尝试找等效物品（同类型工具/武器/护甲）
        return findSlotWithEquivalentItem(inv, template);
    }

    /**
     * 查找相同物品注册名的物品（忽略NBT和耐久度）
     */
    private static int findSlotWithSameItem(IItemHandler inv, ItemStack template) {
        if (template.isEmpty()) return -1;
        Item templateItem = template.getItem();

        for (int i = 0; i < inv.getSlots(); i++) {
            ItemStack stack = inv.getStackInSlot(i);
            if (!stack.isEmpty() && stack.getItem() == templateItem) {
                return i;
            }
        }
        return -1;
    }

    /**
     * 获取TF包的库存处理器
     */
    private static ModularHandler getInventoryForBag(ItemStack bagStack, Player player) {
        if (bagStack.isEmpty()) {
            return null;
        }

        if (!(bagStack.getItem() instanceof TFBagItem)) {
            return null;
        }

        return TFBagItem.getInventoryForBag(bagStack, player);
    }

    /**
     * 查找等效物品（同类型工具/武器/护甲）
     */
    private static int findSlotWithEquivalentItem(IItemHandler inv, ItemStack template) {
        if (template.isEmpty()) return -1;
        Item templateItem = template.getItem();

        // 护甲类
        if (templateItem instanceof ArmorItem armorItem) {
            EquipmentSlot slotType = armorItem.getEquipmentSlot();
            for (int i = 0; i < inv.getSlots(); i++) {
                ItemStack stack = inv.getStackInSlot(i);
                if (!stack.isEmpty() && stack.getItem() instanceof ArmorItem otherArmor) {
                    if (otherArmor.getEquipmentSlot() == slotType) {
                        return i;
                    }
                }
            }
        }
        
        // 工具类（通过类型判断）
        
        // 剑类
        if (templateItem instanceof SwordItem) {
            for (int i = 0; i < inv.getSlots(); i++) {
                ItemStack stack = inv.getStackInSlot(i);
                if (!stack.isEmpty() && stack.getItem() instanceof SwordItem) {
                    return i;
                }
            }
        }

        // 弓类
        if (templateItem instanceof BowItem) {
            for (int i = 0; i < inv.getSlots(); i++) {
                ItemStack stack = inv.getStackInSlot(i);
                if (!stack.isEmpty() && stack.getItem() instanceof BowItem) {
                    return i;
                }
            }
        }
        
        // 锄类
        if (templateItem instanceof HoeItem) {
            for (int i = 0; i < inv.getSlots(); i++) {
                ItemStack stack = inv.getStackInSlot(i);
                if (!stack.isEmpty() && stack.getItem() instanceof HoeItem) {
                    return i;
                }
            }
        }
        
        // 剪刀类
        if (templateItem instanceof ShearsItem) {
            for (int i = 0; i < inv.getSlots(); i++) {
                ItemStack stack = inv.getStackInSlot(i);
                if (!stack.isEmpty() && stack.getItem() instanceof ShearsItem) {
                    return i;
                }
            }
        }

        return -1;
    }
}
