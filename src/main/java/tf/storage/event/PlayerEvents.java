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
    }

    @SubscribeEvent
    public static void onPlayerTick(PlayerTickEvent event) {
        if (event.side == Side.SERVER && event.phase == Phase.END) {
            EntityPlayer player = event.player;

            if (player == null || player.isDead || player.inventory == null) {
                return;
            }
             
            UUID playerUUID = player.getUniqueID();
            if (playerUUID == null) {
                return;
            }

            if (player.ticksExisted % 5 != 0) {
                return;
            }

            if (player.openContainer != player.inventoryContainer) {
                return;
            }

            if (!player.inventory.getItemStack().isEmpty()) {
                return;
            }

            int snapshotSize = InventoryPlayer.getHotbarSize() + 1;
            ItemStack[] lastHotbar = lastHotbarSnapshot.computeIfAbsent(playerUUID, k -> new ItemStack[snapshotSize]);

            ItemStack[] currentHotbar = new ItemStack[snapshotSize];

            for (int i = 0; i < InventoryPlayer.getHotbarSize(); i++) {
                currentHotbar[i] = player.inventory.getStackInSlot(i).copy();
            }
            currentHotbar[snapshotSize - 1] = player.getHeldItemOffhand().copy();

            for (int i = 0; i < InventoryPlayer.getHotbarSize(); i++) {
                if (i == player.inventory.currentItem) {
                    checkAndReplenish(player, i, lastHotbar[i], currentHotbar[i]);
                }
            }

            checkAndReplenish(player, 40, lastHotbar[snapshotSize - 1], currentHotbar[snapshotSize - 1]);
            
            lastHotbarSnapshot.put(playerUUID, currentHotbar);
        }
    }
    
    private static void checkAndReplenish(EntityPlayer player, int slotIndex, ItemStack lastStack, ItemStack currentStack) {
        if (lastStack != null && !lastStack.isEmpty() && currentStack.isEmpty()) {
            tryReplenishSlot(player, slotIndex, lastStack);
        }
    }

    /**
     * 监听玩家登出事件，用于清理内存中的快照数据，防止内存泄漏。
     */
    @SubscribeEvent
    public static void onPlayerLogout(PlayerEvent.PlayerLoggedOutEvent event) {
        if (event.player != null) {
            UUID playerUUID = event.player.getUniqueID();
            if (playerUUID != null) {
                lastHotbarSnapshot.remove(playerUUID);
            }
        }
    }

    private static void tryReplenishSlot(EntityPlayer player, int slot, ItemStack depletedStack) {
        // Check player inventory first
        for (int i = 0; i < player.inventory.getSizeInventory(); i++) {
            ItemStack bagStack = player.inventory.getStackInSlot(i);
            if (!bagStack.isEmpty() && bagStack.getItem() == ModItems.TF_BAG) {
                RestockMode mode = RestockMode.fromStack(bagStack);

                if (mode == RestockMode.ON) {
                    ModularHandler bagInv = TFBag.getInventoryForBag(bagStack, player);
                    if (bagInv != null) {
                        int sourceSlot = StackHelper.getSlotOfFirstMatchingItemStack(bagInv, depletedStack);

                        if (sourceSlot == -1 && depletedStack.isItemStackDamageable()) {
                            sourceSlot = findSlotWithSameItemIgnoreDamage(bagInv, depletedStack);
                            if (sourceSlot == -1) {
                                sourceSlot = findSlotWithEquivalentItem(bagInv, depletedStack);
                            }
                        }

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

        // Check Baubles inventory if loaded
        if (tf.storage.core.ModCompat.isBaublesLoaded()) {
            try {
                baubles.api.cap.IBaublesItemHandler baublesInv = baubles.api.BaublesApi.getBaublesHandler(player);
                for (int i = 0; i < baublesInv.getSlots(); i++) {
                    ItemStack bagStack = baublesInv.getStackInSlot(i);
                    if (!bagStack.isEmpty() && bagStack.getItem() == ModItems.TF_BAG) {
                        RestockMode mode = RestockMode.fromStack(bagStack);

                        if (mode == RestockMode.ON) {
                            ModularHandler bagInv = TFBag.getInventoryForBag(bagStack, player);
                            if (bagInv != null) {
                                int sourceSlot = StackHelper.getSlotOfFirstMatchingItemStack(bagInv, depletedStack);

                                if (sourceSlot == -1 && depletedStack.isItemStackDamageable()) {
                                    sourceSlot = findSlotWithSameItemIgnoreDamage(bagInv, depletedStack);
                                    if (sourceSlot == -1) {
                                        sourceSlot = findSlotWithEquivalentItem(bagInv, depletedStack);
                                    }
                                }

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
            } catch (Exception e) {
                // Ignore Baubles errors
            }
        }
    }

    private static int findSlotWithSameItemIgnoreDamage(net.minecraftforge.items.IItemHandler inv, ItemStack template) {
        for (int i = 0; i < inv.getSlots(); i++) {
            ItemStack stack = inv.getStackInSlot(i);
            if (!stack.isEmpty() && stack.getItem() == template.getItem()) {
                if (ItemStack.areItemStackTagsEqual(stack, template)) {
                    return i;
                }
            }
        }
        return -1;
    }

    private static int findSlotWithEquivalentItem(net.minecraftforge.items.IItemHandler inv, ItemStack template) {
        if (template.isEmpty()) return -1;
        net.minecraft.item.Item templateItem = template.getItem();

        if (templateItem instanceof net.minecraft.item.ItemArmor) {
            net.minecraft.inventory.EntityEquipmentSlot slotType = ((net.minecraft.item.ItemArmor) templateItem).armorType;
            for (int i = 0; i < inv.getSlots(); i++) {
                ItemStack stack = inv.getStackInSlot(i);
                if (!stack.isEmpty() && stack.getItem() instanceof net.minecraft.item.ItemArmor) {
                     if (((net.minecraft.item.ItemArmor) stack.getItem()).armorType == slotType) {
                         return i;
                     }
                }
            }
        }
        
        java.util.Set<String> templateToolClasses = templateItem.getToolClasses(template);
        if (!templateToolClasses.isEmpty()) {
             for (int i = 0; i < inv.getSlots(); i++) {
                ItemStack stack = inv.getStackInSlot(i);
                if (!stack.isEmpty()) {
                    java.util.Set<String> stackToolClasses = stack.getItem().getToolClasses(stack);
                    if (!java.util.Collections.disjoint(templateToolClasses, stackToolClasses)) {
                        return i;
                    }
                }
            }
        }

        if (templateItem instanceof net.minecraft.item.ItemSword) {
             for (int i = 0; i < inv.getSlots(); i++) {
                ItemStack stack = inv.getStackInSlot(i);
                if (!stack.isEmpty() && stack.getItem() instanceof net.minecraft.item.ItemSword) {
                    return i;
                }
            }
        }

        if (templateItem instanceof net.minecraft.item.ItemBow) {
             for (int i = 0; i < inv.getSlots(); i++) {
                ItemStack stack = inv.getStackInSlot(i);
                if (!stack.isEmpty() && stack.getItem() instanceof net.minecraft.item.ItemBow) {
                    return i;
                }
            }
        }
        
        if (templateItem instanceof net.minecraft.item.ItemHoe) {
             for (int i = 0; i < inv.getSlots(); i++) {
                ItemStack stack = inv.getStackInSlot(i);
                if (!stack.isEmpty() && stack.getItem() instanceof net.minecraft.item.ItemHoe) {
                    return i;
                }
            }
        }
        
        if (templateItem instanceof net.minecraft.item.ItemShears) {
             for (int i = 0; i < inv.getSlots(); i++) {
                ItemStack stack = inv.getStackInSlot(i);
                if (!stack.isEmpty() && stack.getItem() instanceof net.minecraft.item.ItemShears) {
                    return i;
                }
            }
        }

        return -1;
    }
}
