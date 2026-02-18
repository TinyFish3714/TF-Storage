package tf.storage.platform.forge.item;

import java.util.ArrayList;
import java.util.List;
import javax.annotation.Nullable;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import net.minecraft.nbt.CompoundTag;
import net.minecraftforge.items.IItemHandler;
import net.minecraftforge.items.IItemHandlerModifiable;
import net.minecraftforge.items.wrapper.CombinedInvWrapper;
import net.minecraftforge.items.wrapper.EmptyHandler;
import net.minecraftforge.items.wrapper.InvWrapper;
import net.minecraftforge.items.wrapper.PlayerOffhandInvWrapper;
import net.minecraftforge.items.wrapper.RangedWrapper;
import net.minecraftforge.common.capabilities.ForgeCapabilities;
import tf.storage.core.util.NBTHelper;
import tf.storage.core.util.CardHelper;
import tf.storage.core.util.StackHelper;
import tf.storage.core.inventory.SlotRange;
import tf.storage.platform.forge.ModRegistry;
import tf.storage.platform.forge.inventory.handler.ModularHandler;
import tf.storage.platform.forge.menu.BagMenu;
import tf.storage.platform.forge.compat.CuriosCompat;
import tf.storage.platform.forge.network.GuiActions;

/**
 * TFBagItem - TF包物品
 * 支持2种等级，包含存储卡槽位、拾取/补货模式等功能
 * 
 * 适配 Minecraft 1.20.1
 */
public class TFBagItem extends Item {
    
    private final Tier tier;
    
    public enum Tier {
        TIER_1(27, 4),   // 27格，4个存储卡槽
        TIER_2(55, 4);   // 55格，4个存储卡槽
        
        private final int invSize;
        private final int memoryCardSlots;
        
        Tier(int invSize, int memoryCardSlots) {
            this.invSize = invSize;
            this.memoryCardSlots = memoryCardSlots;
        }
        
        public int getInvSize() { return invSize; }
        public int getMemoryCardSlots() { return memoryCardSlots; }
    }
    
    // GUI操作常量
    public static final int GUI_ACTION_SELECT_MODULE = GuiActions.Bag.SELECT_MODULE;
    public static final int GUI_ACTION_MOVE_ITEMS = GuiActions.Bag.MOVE_ITEMS;
    public static final int GUI_ACTION_SORT_ITEMS = GuiActions.Bag.SORT_ITEMS;
    public static final int GUI_ACTION_TOGGLE_REGION_LOCK = GuiActions.Bag.TOGGLE_REGION_LOCK;
    public static final int GUI_ACTION_TOGGLE_MODES = GuiActions.Bag.TOGGLE_MODES;
    public static final int GUI_ACTION_TOGGLE_SHIFTCLICK = GuiActions.Bag.TOGGLE_SHIFTCLICK;
    public static final int GUI_ACTION_TOGGLE_SHIFTCLICK_DOUBLETAP = GuiActions.Bag.TOGGLE_SHIFTCLICK_DOUBLETAP;
    
    public TFBagItem(Tier tier, Properties properties) {
        super(properties);
        this.tier = tier;
    }
    
    public Tier getTier() {
        return tier;
    }
    
    public int getSizeInventory(ItemStack containerStack) {
        return tier.getInvSize();
    }
    
    public int getMaxMemoryCards(ItemStack containerStack) {
        return tier.getMemoryCardSlots();
    }
    
    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (level.isClientSide) {
            return InteractionResultHolder.success(stack);
        }

        if (!stack.isEmpty()) {
            // 确保有UUID
            NBTHelper.getUUIDFromItemStack(stack, "UUID", true);

            if (player instanceof ServerPlayer serverPlayer) {
                net.minecraftforge.network.NetworkHooks.openScreen(
                    serverPlayer,
                    new tf.storage.platform.forge.menu.provider.BagMenuProvider(stack),
                    buf -> buf.writeItem(stack)
                );
            }
        }

        return InteractionResultHolder.success(stack);
    }
    
    @Override
    public void inventoryTick(ItemStack stack, Level level, Entity entity, int slot, boolean selected) {
        // 可以在这里实现自动拾取等功能
    }
    
    @Override
    public void onCraftedBy(ItemStack stack, Level level, Player player) {
        super.onCraftedBy(stack, level, player);
        NBTHelper.getUUIDFromItemStack(stack, "UUID", true);
    }
    
    @Override
    public Component getName(ItemStack stack) {
        ItemStack memoryCardStack = CardHelper.getSelectedMemoryCardStack(stack);
        
        if (!memoryCardStack.isEmpty() && memoryCardStack.getTag() != null) {
            Component itemName = super.getName(stack);
            
            if (memoryCardStack.hasCustomHoverName()) {
                return Component.literal(itemName.getString() + " ")
                    .append(Component.literal("\u00A7a\u00A7o" + memoryCardStack.getHoverName().getString() + "\u00A7r"));
            }
            
            return itemName;
        }
        
        return super.getName(stack);
    }
    
    @Override
    public void appendHoverText(ItemStack containerStack, @Nullable Level level, List<Component> tooltip, TooltipFlag flag) {
        super.appendHoverText(containerStack, level, tooltip, flag);
        
        if (containerStack.getTag() == null) {
            return;
        }
        
        String preGreen = "\u00A7a";
        String preRed = "\u00A7c";
        String preWhite = "\u00A7f";
        String rst = "\u00A7r\u00A77";
        
        // 拾取模式
        PickupMode pickupMode = PickupMode.fromStack(containerStack);
        String strPickupMode = Component.translatable("tfstorage.tooltip.item.pickupmode.short").getString() + ": ";
        if (pickupMode == PickupMode.NONE) strPickupMode += preRed;
        else if (pickupMode == PickupMode.MATCHING) strPickupMode += "\u00A7e"; // Yellow
        else if (pickupMode == PickupMode.ALL) strPickupMode += preGreen;
        strPickupMode += pickupMode.getDisplayName() + rst;
        
        // 补货模式
        RestockMode restockMode = RestockMode.fromStack(containerStack);
        String strRestockMode = Component.translatable("tfstorage.tooltip.item.restockmode.short").getString() + ": ";
        if (restockMode == RestockMode.OFF) strRestockMode += preRed;
        else strRestockMode += preGreen;
        strRestockMode += restockMode.getDisplayName() + rst;
        
        tooltip.add(Component.literal(strPickupMode + " / " + strRestockMode));
        
        // 启用状态
        String str;
        if (bagIsOpenable(containerStack)) {
            str = Component.translatable("tfstorage.tooltip.item.enabled").getString() + ": " +
                    preGreen + Component.translatable("tfstorage.tooltip.item.yes").getString();
        } else {
            str = Component.translatable("tfstorage.tooltip.item.enabled").getString() + ": " +
                    preRed + Component.translatable("tfstorage.tooltip.item.no").getString();
        }
        tooltip.add(Component.literal(str));
        
        // 存储卡信息
        int installed = CardHelper.getInstalledMemoryCardCount(containerStack);
        if (installed > 0) {
            int slotNum = CardHelper.getStoredMemoryCardSelection(containerStack);
            String preBlue = "\u00A79";
            ItemStack memoryCardStack = CardHelper.getSelectedMemoryCardStack(containerStack);
            int max = getMaxMemoryCards(containerStack);
            
            if (!memoryCardStack.isEmpty()) {
                String dName = memoryCardStack.hasCustomHoverName() ? 
                    preWhite + "\u00A7o" + memoryCardStack.getHoverName().getString() + rst + " " : "";
                tooltip.add(Component.literal(String.format("%s %s(%s%d%s / %s%d%s)", 
                    Component.translatable("tfstorage.tooltip.item.selectedmemorycard.short").getString(),
                    dName, preBlue, slotNum + 1, rst, preBlue, max, rst)));
                
                // 添加存储卡内容提示
                if (memoryCardStack.getItem() instanceof TFUnitItem tfUnit) {
                    ArrayList<String> lines = new ArrayList<>();
                    int itemCount = CardHelper.getFormattedItemListFromContainerItem(memoryCardStack, lines, 20);
                    if (!lines.isEmpty()) {
                        for (String line : lines) {
                            tooltip.add(Component.literal(line));
                        }
                    }
                }
            } else {
                tooltip.add(Component.literal(String.format("%s %s (%s%d%s / %s%d%s)",
                    Component.translatable("tfstorage.tooltip.item.selectedmemorycard.short").getString(),
                    Component.translatable("tfstorage.tooltip.item.selectedmemorycard.notinstalled").getString(),
                    preBlue, slotNum + 1, rst, preBlue, max, rst)));
            }
        } else {
            tooltip.add(Component.translatable("tfstorage.tooltip.item.nomemorycards"));
        }
    }
    
    /**
     * 检查TF包是否可以打开
     */
    public static boolean bagIsOpenable(ItemStack stack) {
        if (stack.getTag() == null) {
            return true;
        }
        
        CompoundTag tfBagTag = stack.getTag().getCompound("TFBag");
        if (tfBagTag.getBoolean("DisableOpen")) {
            return false;
        }
        
        return true;
    }

    /**
     * 是否允许通过背包键自动打开
     */
    public static boolean bagIsAutoOpenable(ItemStack stack) {
        return bagIsOpenable(stack);
    }
    
    /**
     * 获取玩家身上第一个可打开的TF包
     * 检测顺序：Curios饰品栏 -> 快捷栏 -> 物品栏 -> 副手
     */
    public static ItemStack getOpenableBag(Player player) {
        if (player == null) {
            return ItemStack.EMPTY;
        }

        // 1. 先检测Curios饰品栏的TF槽
        ItemStack curiosStack = CuriosCompat.findOpenableBag(player);
        if (!curiosStack.isEmpty()) {
            return curiosStack;
        }

        net.minecraft.world.entity.player.Inventory inv = player.getInventory();

        // 2. 再检测快捷栏（slot 0-8）
        for (int slot = 0; slot < 9; slot++) {
            ItemStack stack = inv.getItem(slot);
            if (isBagStackOpenable(stack)) {
                return stack;
            }
        }

        // 3. 再检测物品栏（slot 9-35）
        for (int slot = 9; slot < 36; slot++) {
            ItemStack stack = inv.getItem(slot);
            if (isBagStackOpenable(stack)) {
                return stack;
            }
        }

        // 4. 最后检测副手（slot 40）
        ItemStack offhand = inv.getItem(40);
        if (isBagStackOpenable(offhand)) {
            return offhand;
        }

        return ItemStack.EMPTY;
    }

    private static boolean isBagStackOpenable(ItemStack stack) {
        if (stack.isEmpty()) {
            return false;
        }

        if (stack.getItem() == ModRegistry.TF_BAG.get() || stack.getItem() == ModRegistry.TF_BAG_LARGE.get()) {
            return bagIsAutoOpenable(stack);
        }

        return false;
    }

    public static ModularHandler getInventoryForBag(ItemStack bagStack, Player player) {
        ModularHandler bagInv;

        if (player.containerMenu instanceof BagMenu bagMenu &&
            bagMenu.getInventoryItemWithMemoryCards().getModularItemStack() == bagStack) {
            bagInv = bagMenu.getInventoryItemWithMemoryCards();
        } else {
            bagInv = new ModularHandler(bagStack, player, true);
        }

        if (!bagInv.isAccessibleBy(player)) {
            return null;
        }

        return bagInv;
    }
    
    // ==================== 拾取模式 ====================
    
    public enum PickupMode {
        NONE(0, "tfstorage.tooltip.item.disabled"),
        MATCHING(1, "tfstorage.tooltip.item.matching"),
        ALL(2, "tfstorage.tooltip.item.all");
        
        private final int id;
        private final String translationKey;
        
        PickupMode(int id, String translationKey) {
            this.id = id;
            this.translationKey = translationKey;
        }
        
        public String getDisplayName() {
            return Component.translatable(translationKey).getString();
        }

        public String getDisplayNameKey() {
            return translationKey;
        }




        
        public static PickupMode fromStack(ItemStack bagStack) {
            int id = getModeId(bagStack);
            return (id >= 0 && id < values().length) ? values()[id] : NONE;
        }
        
        public static void cycleMode(ItemStack bagStack, Player player, boolean reverse) {
            int id = getModeId(bagStack) + (reverse ? -1 : 1);
            if (id < 0) id = values().length - 1;
            else if (id >= values().length) id = 0;
            setModeId(bagStack, id);
        }
        
        private static int getModeId(ItemStack bagStack) {
            if (bagStack.isEmpty()) return NONE.ordinal();
            return NBTHelper.getByte(bagStack, null, "PickupMode");
        }
        
        private static void setModeId(ItemStack bagStack, int id) {
            if (!bagStack.isEmpty()) {
                NBTHelper.setByte(bagStack, null, "PickupMode", (byte) id);
            }
        }
    }
    
    // ==================== 补货模式 ====================
    
    public enum RestockMode {
        OFF("off"),
        ON("on");
        
        private final String name;
        
        RestockMode(String name) {
            this.name = name;
        }
        
        public String getDisplayName() {
            return Component.translatable("tfstorage.tooltip.item." + name).getString();
        }
        
        public static RestockMode fromStack(ItemStack bagStack) {
            return getModeId(bagStack) == 1 ? ON : OFF;
        }
        
        public static void cycleMode(ItemStack bagStack, Player player, boolean reverse) {
            int currentId = getModeId(bagStack);
            setModeId(bagStack, currentId == 0 ? 1 : 0);
        }
        
        private static int getModeId(ItemStack bagStack) {
            if (bagStack.isEmpty()) return OFF.ordinal();
            return NBTHelper.getByte(bagStack, null, "RestockMode");
        }
        
        private static void setModeId(ItemStack bagStack, int id) {
            if (!bagStack.isEmpty()) {
                NBTHelper.setByte(bagStack, null, "RestockMode", (byte) id);
            }
        }
    }

    public boolean canEquip(String identifier, net.minecraft.world.entity.LivingEntity livingEntity, ItemStack stack) {
        return "tf".equals(identifier);
    }
    
    // ==================== Shift点击模式 ====================
    
    public enum ShiftMode {
        TO_BAG("tfstorage.gui.label.tfbag.shiftclick.tobag"),
        INV_HOTBAR("tfstorage.gui.label.tfbag.shiftclick.invhotbar"),
        DOUBLE_TAP("tfstorage.gui.label.tfbag.shiftclick.doubletapshift");
        
        private final String translationKey;
        
        ShiftMode(String translationKey) {
            this.translationKey = translationKey;
        }
        
        public String getDisplayName() {
            return Component.translatable(translationKey).getString();
        }

        public String getDisplayNameKey() {
            return translationKey;
        }

        
        public static ShiftMode fromStack(ItemStack bagStack) {
            return fromId(getModeId(bagStack) & 0x03);
        }
        
        public static ShiftMode fromId(int id) {
            return (id >= 0 && id < values().length) ? values()[id] : TO_BAG;
        }
        
        public static void cycleMode(ItemStack bagStack, boolean reverse) {
            int rawMode = getModeId(bagStack);
            int id = (rawMode & 0x03) + (reverse ? -1 : 1);
            if (id < 0) id = values().length - 1;
            else if (id >= values().length) id = 0;
            rawMode = (rawMode & 0x80) | id;
            setModeId(bagStack, rawMode);
        }
        
        public static void toggleDoubleTapEffectiveMode(ItemStack bagStack) {
            byte rawMode = (byte) (getModeId(bagStack) ^ 0x80);
            setModeId(bagStack, rawMode);
        }

        public static ShiftMode getEffectiveMode(ItemStack bagStack) {
            int rawMode = getModeId(bagStack);
            ShiftMode mode = fromId(rawMode & 0x03);
            if (mode == DOUBLE_TAP) {
                return (rawMode & 0x80) != 0 ? INV_HOTBAR : TO_BAG;
            }
            return mode;
        }
        
        private static int getModeId(ItemStack bagStack) {
            return NBTHelper.getByte(bagStack, "TFBag", "ShiftMode");
        }
        
        private static void setModeId(ItemStack bagStack, int id) {
            NBTHelper.setByte(bagStack, "TFBag", "ShiftMode", (byte) id);
        }
    }

    public static void performGuiAction(Player player, int action, int element) {
        if (!(player.containerMenu instanceof BagMenu)) {
            return;
        }

        BagMenu container = (BagMenu) player.containerMenu;
        ModularHandler inv = container.getInventoryItemWithMemoryCards();
        ItemStack stack = inv.getModularItemStack();

        if (!stack.isEmpty() && stack.getItem() instanceof TFBagItem) {
            int max = ((TFBagItem) stack.getItem()).getMaxMemoryCards(stack);

            if (action == GUI_ACTION_SELECT_MODULE && element >= 0 && element < max) {
                CardHelper.setMemoryCardSelection(stack, element, max);
                inv.readFromContainerItemStack();
                player.containerMenu.broadcastChanges();
            } else if (action == GUI_ACTION_MOVE_ITEMS) {
                IItemHandlerModifiable playerMainInv = new RangedWrapper(new InvWrapper(player.getInventory()), 0, player.getInventory().items.size());
                IItemHandlerModifiable offhandInv = new PlayerOffhandInvWrapper(player.getInventory());
                IItemHandler playerInv = new CombinedInvWrapper(playerMainInv, offhandInv);
                IItemHandler wrappedBagInv = getWrappedEnabledInv(stack, inv);

                switch (element & 0x7FFF) {
                    case 0 -> {
                        if ((element & 0x8000) != 0) {
                            StackHelper.tryMoveAllItems(playerInv, wrappedBagInv);
                        } else {
                            StackHelper.tryMoveAllItemsWithinSlotRange(playerInv, wrappedBagInv, new SlotRange(9, 27), new SlotRange(wrappedBagInv));
                        }
                    }
                    case 1 -> {
                        if ((element & 0x8000) != 0) {
                            StackHelper.tryMoveMatchingItems(playerInv, wrappedBagInv);
                        } else {
                            StackHelper.tryMoveMatchingItemsWithinSlotRange(playerInv, wrappedBagInv, new SlotRange(9, 27), new SlotRange(wrappedBagInv));
                        }
                    }
                    case 2 -> StackHelper.leaveOneFullStackOfEveryItem(playerInv, wrappedBagInv, true);
                    case 3 -> StackHelper.fillStacksOfMatchingItems(wrappedBagInv, playerInv);
                    case 4 -> StackHelper.tryMoveMatchingItems(wrappedBagInv, playerInv);
                    case 5 -> StackHelper.tryMoveAllItems(wrappedBagInv, playerInv);
                    default -> {
                    }
                }
            } else if (action == GUI_ACTION_SORT_ITEMS && element >= 0 && element <= 3) {
                if (element == 3) {
                    IItemHandlerModifiable playerMainInv = new RangedWrapper(new InvWrapper(player.getInventory()), 0, player.getInventory().items.size());
                    StackHelper.sortInventoryWithinRange(playerMainInv, new SlotRange(9, 27));
                    player.containerMenu.broadcastChanges();
                    return;
                }

                if (element > 0 && ((TFBagItem) stack.getItem()).getTier() != Tier.TIER_2) {
                    return;
                }

                StackHelper.sortInventoryWithinRange(inv, getSlotRangeForSection(element));
            } else if (action == GUI_ACTION_TOGGLE_REGION_LOCK && element >= 0 && element <= 2) {
                setSlotMask(inv, stack, element, "LockMask");
                inv.writeDataToContainerItemStack();
                player.containerMenu.broadcastChanges();
            } else if (action == GUI_ACTION_TOGGLE_MODES && (element & 0x03) <= 2) {
                switch (element & 0x03) {
                    case 0 -> NBTHelper.toggleBoolean(stack, "TFBag", "DisableOpen");
                    case 1 -> PickupMode.cycleMode(stack, player, (element & 0x8000) != 0);
                    case 2 -> RestockMode.cycleMode(stack, player, (element & 0x8000) != 0);
                    default -> {
                    }
                }

                inv.writeDataToContainerItemStack();
                player.containerMenu.broadcastChanges();
            } else if (action == GUI_ACTION_TOGGLE_SHIFTCLICK) {
                ShiftMode.cycleMode(stack, element != 0);
                inv.writeDataToContainerItemStack();
                player.containerMenu.broadcastChanges();
            } else if (action == GUI_ACTION_TOGGLE_SHIFTCLICK_DOUBLETAP) {
                // 只有在 DOUBLE_TAP 模式下才切换有效模式
                if (ShiftMode.fromStack(stack) == ShiftMode.DOUBLE_TAP) {
                    ShiftMode.toggleDoubleTapEffectiveMode(stack);
                    inv.writeDataToContainerItemStack();
                    player.containerMenu.broadcastChanges();
                }
            }
        }
    }

    private static void setSlotMask(ModularHandler inv, ItemStack bagStack, int bagSection, String tagName) {
        int slot = inv.getSelectedMemoryCardIndex();

        if (slot >= 0) {
            ItemStack cardStack = inv.getMemoryCardInventory().getStackInSlot(slot);
            if (!cardStack.isEmpty()) {
                long[] masks = new long[] { 0x1FFFFFFL, 0x1FFF8000000L, 0x7FFE0000000000L };
                long mask = NBTHelper.getLong(cardStack, "TFBag", tagName);
                mask ^= masks[bagSection];
                NBTHelper.setLong(cardStack, "TFBag", tagName, mask);
                CardHelper.setSelectedMemoryCardStack(bagStack, cardStack);
                inv.writeDataToContainerItemStack();
            }
        }
    }

    private static SlotRange getSlotRangeForSection(int section) {
        if (section == 0) {
            return new SlotRange(0, 27);
        } else if (section == 1) {
            return new SlotRange(27, 14);
        }

        return new SlotRange(41, 14);
    }

    private static IItemHandler getWrappedEnabledInv(ItemStack stack, IItemHandlerModifiable baseInv) {
        ItemStack cardStack = CardHelper.getSelectedMemoryCardStack(stack);
        if (cardStack.isEmpty()) {
            return EmptyHandler.INSTANCE;
        }

        if (!(stack.getItem() instanceof TFBagItem bagItem) || bagItem.getTier() != Tier.TIER_2) {
            return baseInv;
        }

        long[] masks = new long[] { 0x1FFFFFFL, 0x1FFF8000000L, 0x7FFE0000000000L };
        long lockMask = NBTHelper.getLong(cardStack, "TFBag", "LockMask");

        IItemHandlerModifiable inv = null;
        for (int i = 0; i < 3; i++) {
            if ((lockMask & masks[i]) == 0) {
                SlotRange range = getSlotRangeForSection(i);
                if (inv == null) {
                    inv = new RangedWrapper(baseInv, range.first, range.lastExc);
                } else {
                    inv = new CombinedInvWrapper(inv, new RangedWrapper(baseInv, range.first, range.lastExc));
                }
            }
        }

        return inv != null ? inv : EmptyHandler.INSTANCE;
    }
}
    
