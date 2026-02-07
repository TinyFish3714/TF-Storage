package tf.storage.item;

import java.util.Iterator;
import java.util.List;
import net.minecraft.client.renderer.block.model.ModelResourceLocation;
import net.minecraft.client.resources.I18n;
import net.minecraft.creativetab.CreativeTabs;
import net.minecraft.entity.Entity;
import net.minecraft.entity.item.EntityItem;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.init.SoundEvents;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.ActionResult;
import net.minecraft.util.EnumActionResult;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.EnumHand;
import net.minecraft.util.NonNullList;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.SoundCategory;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.text.TextFormatting;
import net.minecraft.world.World;
import net.minecraftforge.event.entity.player.EntityItemPickupEvent;
import net.minecraftforge.fml.common.FMLCommonHandler;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.eventhandler.Event.Result;
import net.minecraftforge.fml.common.Loader;
import net.minecraftforge.fml.common.ModContainer;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;
import net.minecraftforge.items.CapabilityItemHandler;
import net.minecraftforge.items.IItemHandler;
import net.minecraftforge.items.IItemHandlerModifiable;
import net.minecraftforge.items.wrapper.CombinedInvWrapper;
import net.minecraftforge.items.wrapper.InvWrapper;
import net.minecraftforge.items.wrapper.PlayerOffhandInvWrapper;
import net.minecraftforge.items.wrapper.RangedWrapper;
import tf.storage.TFStorage;
import tf.storage.item.base.BaseItem;
import tf.storage.inventory.container.BagContainer;
import tf.storage.inventory.container.base.SlotRange;
import tf.storage.inventory.handler.ModularHandler;
import tf.storage.item.TFUnit;
import tf.storage.network.PacketHandler;
import tf.storage.network.packet.ActionPacket;
import tf.storage.network.packet.SyncBagPacket;
import tf.storage.core.ModInfo;
import tf.storage.core.ModItems;
import tf.storage.core.ModCompat;
import tf.storage.util.TextFormatter;
import tf.storage.util.StackHelper;
import tf.storage.util.NBTHelper;
import tf.storage.util.CardHelper;
import tf.storage.config.ModConfig;
import static tf.storage.config.ModConfig.ConfigValues;
import net.minecraftforge.fml.common.Optional;
import baubles.api.IBauble;
import baubles.api.BaubleType;
import tf.storage.compat.baubles.BaublesHelper;

@Optional.Interface(iface = "baubles.api.IBauble", modid = ModCompat.MODID_BAUBLES)
public class TFBag extends BaseItem implements IBauble
{
    public static final int META_TIER_1 = 0;
    public static final int META_TIER_2 = 1;

    public static final int INV_SIZE_TIER_1 = 27;
    public static final int INV_SIZE_TIER_2 = 55;

    public static final int GUI_ACTION_SELECT_MODULE = 0;
    public static final int GUI_ACTION_MOVE_ITEMS    = 1;
    public static final int GUI_ACTION_SORT_ITEMS    = 2;
    public static final int GUI_ACTION_TOGGLE_REGION_LOCK = 3;
    public static final int GUI_ACTION_TOGGLE_MODES  = 5;
    public static final int GUI_ACTION_TOGGLE_SHIFTCLICK            = 6;
    public static final int GUI_ACTION_TOGGLE_SHIFTCLICK_DOUBLETAP  = 7;
    public static final int GUI_ACTION_OPEN_BAUBLES  = 100;

    public TFBag(String name)
    {
        super(name);

        this.setMaxStackSize(1);
        this.setMaxDamage(0);
        this.setHasSubtypes(true);
        this.commonTooltip = "item.tfstorage.tfbag.tooltips";
    }

    @Override
    @Optional.Method(modid = ModCompat.MODID_BAUBLES)
    public BaubleType getBaubleType(ItemStack itemstack)
    {
        return BaubleType.BODY;
    }

    @Override
    public void onUpdate(ItemStack stack, World world, Entity entity, int slot, boolean isCurrent)
    {
    }


    @Override
    public ActionResult<ItemStack> onItemRightClick(World world, EntityPlayer player, EnumHand hand)
    {
        ItemStack stack = player.getHeldItem(hand);

        if (world.isRemote == false)
        {
            NBTHelper.getUUIDFromItemStack(stack, "UUID", true);
            player.openContainer.detectAndSendChanges();

            player.openGui(TFStorage.instance, ModInfo.GUI_ID_TF_BAG_RIGHT_CLICK, world,
                    (int)player.posX, (int)player.posY, (int)player.posZ);
        }

        return new ActionResult<ItemStack>(EnumActionResult.SUCCESS, stack);
    }

    @Override
    public void onCreated(ItemStack stack, World world, EntityPlayer player)
    {
        super.onCreated(stack, world, player);
        NBTHelper.getUUIDFromItemStack(stack, "UUID", true);
    }

    @Override
    public String getTranslationKey(ItemStack stack)
    {
        return super.getTranslationKey() + "_" + stack.getMetadata();
    }

    @Override
    public String getItemStackDisplayName(ItemStack stack)
    {
        ItemStack memoryCardStack = CardHelper.getSelectedMemoryCardStack(stack);

        if (memoryCardStack.isEmpty() == false && memoryCardStack.getTagCompound() != null)
        {
            String itemName = super.getItemStackDisplayName(stack);
            String rst = ModInfo.Colors.RESET + ModInfo.Colors.INFO;

            if (memoryCardStack.hasDisplayName())
            {
                String pre = ModInfo.Colors.SUCCESS + ModInfo.Colors.ITALIC;

                if (itemName.length() >= 14)
                {
                    return TextFormatter.getInitialsWithDots(itemName) + " " + pre + memoryCardStack.getDisplayName() + rst;
                }

                return itemName + " " + pre + memoryCardStack.getDisplayName() + rst;
            }

            return itemName;
        }

        return super.getItemStackDisplayName(stack);
    }

    @Override
    @SideOnly(Side.CLIENT)
    public void addTooltipLines(ItemStack containerStack, EntityPlayer player, List<String> list, boolean verbose)
    {
        if (containerStack.getTagCompound() == null)
        {
            return;
        }

        String preGreen = ModInfo.Colors.SUCCESS;
        String preRed = ModInfo.Colors.ERROR;
        String preWhite = ModInfo.Colors.INFO;
        String rst = ModInfo.Colors.RESET + ModInfo.Colors.GRAY;

        String strPickupMode = I18n.format("tfstorage.tooltip.item.pickupmode" + (verbose ? "" : ".short")) + ": ";
        String strRestockMode = I18n.format("tfstorage.tooltip.item.restockmode" + (verbose ? "" : ".short")) + ": ";

        PickupMode pickupMode = PickupMode.fromStack(containerStack);
        if (pickupMode == PickupMode.NONE) strPickupMode += preRed;
        else if (pickupMode == PickupMode.MATCHING) strPickupMode += ModInfo.Colors.WARNING;
        else if (pickupMode == PickupMode.ALL) strPickupMode += preGreen;
        strPickupMode += pickupMode.getDisplayName() + rst;

        RestockMode restockMode = RestockMode.fromStack(containerStack);
        if (restockMode == RestockMode.OFF) strRestockMode += preRed;
        else strRestockMode += preGreen;

        strRestockMode += restockMode.getDisplayName() + rst;

        if (verbose)
        {
            list.add(strPickupMode);
            list.add(strRestockMode);
        }
        else
        {
            list.add(strPickupMode + " / " + strRestockMode);
        }

        String str;

        if (bagIsOpenable(containerStack))
        {
            str = I18n.format("tfstorage.tooltip.item.enabled") + ": " +
                    preGreen + I18n.format("tfstorage.tooltip.item.yes");
        }
        else
        {
            str = I18n.format("tfstorage.tooltip.item.enabled") + ": " +
                    preRed + I18n.format("tfstorage.tooltip.item.no");
        }

        list.add(str);

        int installed = CardHelper.getInstalledMemoryCardCount(containerStack);

        if (installed > 0)
        {
            int slotNum = CardHelper.getStoredMemoryCardSelection(containerStack);
            String preBlue = ModInfo.Colors.HIGHLIGHT;
            String preWhiteIta = preWhite + ModInfo.Colors.ITALIC;
            String strShort = I18n.format("tfstorage.tooltip.item.selectedmemorycard.short");
            ItemStack memoryCardStack = CardHelper.getSelectedMemoryCardStack(containerStack);
            int max = this.getMaxMemoryCards(containerStack);

            if (memoryCardStack.isEmpty() == false && memoryCardStack.getItem() == ModItems.MEMORY_CARD)
            {
                String dName = (memoryCardStack.hasDisplayName() ? preWhiteIta + memoryCardStack.getDisplayName() + rst + " " : "");
                list.add(String.format("%s %s(%s%d%s / %s%d%s)", strShort, dName, preBlue, slotNum + 1, rst, preBlue, max, rst));

                ((TFUnit) memoryCardStack.getItem()).addTooltipLines(memoryCardStack, player, list, false);
                return;
            }
            else
            {
                String strNo = I18n.format("tfstorage.tooltip.item.selectedmemorycard.notinstalled");
                list.add(String.format("%s %s (%s%d%s / %s%d%s)", strShort, strNo, preBlue, slotNum + 1, rst, preBlue, max, rst));
            }
        }
        else
        {
            list.add(I18n.format("tfstorage.tooltip.item.nomemorycards"));
        }
    }

    public static ModularHandler getInventoryForBag(ItemStack bagStack, EntityPlayer player)
    {
        ModularHandler bagInv = null;

        if (player.openContainer instanceof BagContainer &&
            ((BagContainer) player.openContainer).inventoryItemWithMemoryCards.getModularItemStack() == bagStack)
        {
            bagInv = ((BagContainer) player.openContainer).inventoryItemWithMemoryCards;
        }
        else
        {
            bagInv = new ModularHandler(bagStack, player, true);
        }

        if (bagInv.isAccessibleBy(player) == false)
        {
            return null;
        }

        return bagInv;
    }



    @SubscribeEvent
    public static void onEntityItemPickupEvent(EntityItemPickupEvent event) {
        EntityPlayer player = event.getEntityPlayer();
        EntityItem entityItem = event.getItem();
        ItemStack itemToPick = entityItem.getItem();

        if (player == null || player.world.isRemote || itemToPick.isEmpty() || entityItem.isDead) {
            return;
        }

        // Check player inventory first
        for (int i = 0; i < player.inventory.getSizeInventory(); i++) {
            ItemStack bagStack = player.inventory.getStackInSlot(i);
            
            if (!bagStack.isEmpty() && bagStack.getItem() == ModItems.TF_BAG && TFBag.bagIsOpenable(bagStack)) {
                PickupMode mode = PickupMode.fromStack(bagStack);
                
                if (mode == PickupMode.NONE) {
                    continue;
                }

                ModularHandler bagInv = getInventoryForBag(bagStack, player);
                if (bagInv == null) {
                    continue;
                }

                boolean shouldPickup = false;
                if (mode == PickupMode.ALL) {
                    shouldPickup = true;
                } else if (mode == PickupMode.MATCHING) {
                    if (StackHelper.getSlotOfFirstMatchingItemStack(bagInv, itemToPick) != -1) {
                        shouldPickup = true;
                    }
                }

                if (shouldPickup) {
                    ItemStack originalStack = itemToPick.copy();
                    
                    ItemStack remainder = net.minecraftforge.items.ItemHandlerHelper.insertItemStacked(bagInv, itemToPick, false);
                    
                    if (remainder.getCount() < originalStack.getCount()) {
                        
                        int pickedUpAmount = originalStack.getCount() - remainder.getCount();
                        
                        player.onItemPickup(entityItem, pickedUpAmount);
                        if (entityItem.isSilent() == false) {
                             player.world.playSound(null, player.posX, player.posY, player.posZ, SoundEvents.ENTITY_ITEM_PICKUP, SoundCategory.PLAYERS, 0.2F, ((player.getRNG().nextFloat() - player.getRNG().nextFloat()) * 0.7F + 1.0F) * 2.0F);
                        }

                        if (remainder.isEmpty()) {
                            entityItem.setDead();
                            event.setCanceled(true);
                            return;
                        } else {
                            entityItem.setItem(remainder);
                            itemToPick = remainder;
                        }
                    }
                }
            }
        }

        // Check Baubles inventory if loaded
        if (ModCompat.isBaublesLoaded()) {
            try {
                baubles.api.cap.IBaublesItemHandler baublesInv = baubles.api.BaublesApi.getBaublesHandler(player);
                for (int i = 0; i < baublesInv.getSlots(); i++) {
                    ItemStack bagStack = baublesInv.getStackInSlot(i);
                    
                    if (!bagStack.isEmpty() && bagStack.getItem() == ModItems.TF_BAG && TFBag.bagIsOpenable(bagStack)) {
                        PickupMode mode = PickupMode.fromStack(bagStack);
                        
                        if (mode == PickupMode.NONE) {
                            continue;
                        }

                        ModularHandler bagInv = getInventoryForBag(bagStack, player);
                        if (bagInv == null) {
                            continue;
                        }

                        boolean shouldPickup = false;
                        if (mode == PickupMode.ALL) {
                            shouldPickup = true;
                        } else if (mode == PickupMode.MATCHING) {
                            if (StackHelper.getSlotOfFirstMatchingItemStack(bagInv, itemToPick) != -1) {
                                shouldPickup = true;
                            }
                        }

                        if (shouldPickup) {
                            ItemStack originalStack = itemToPick.copy();
                            
                            ItemStack remainder = net.minecraftforge.items.ItemHandlerHelper.insertItemStacked(bagInv, itemToPick, false);
                            
                            if (remainder.getCount() < originalStack.getCount()) {
                                
                                int pickedUpAmount = originalStack.getCount() - remainder.getCount();
                                
                                player.onItemPickup(entityItem, pickedUpAmount);
                                if (entityItem.isSilent() == false) {
                                     player.world.playSound(null, player.posX, player.posY, player.posZ, SoundEvents.ENTITY_ITEM_PICKUP, SoundCategory.PLAYERS, 0.2F, ((player.getRNG().nextFloat() - player.getRNG().nextFloat()) * 0.7F + 1.0F) * 2.0F);
                                }

                                if (remainder.isEmpty()) {
                                    entityItem.setDead();
                                    event.setCanceled(true);
                                    return;
                                } else {
                                    entityItem.setItem(remainder);
                                    itemToPick = remainder;
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

    public static boolean bagIsOpenable(ItemStack stack)
    {
        if (stack.getTagCompound() == null)
        {
            return true;
        }

        if (stack.getTagCompound().getCompoundTag("TFBag").getBoolean("DisableOpen"))
        {
            return false;
        }

        return true;
    }

    public static ItemStack getOpenableBag(EntityPlayer player)
    {
        if (ModCompat.isBaublesLoaded())
        {
            ItemStack baubleStack = BaublesHelper.getOpenableBag(player);
            if (!baubleStack.isEmpty()) {
                return baubleStack;
            }
        }

        IItemHandler playerInv = player.getCapability(CapabilityItemHandler.ITEM_HANDLER_CAPABILITY, null);
        List<Integer> slots = StackHelper.getSlotNumbersOfMatchingItems(playerInv, ModItems.TF_BAG);

        for (int slot : slots)
        {
            ItemStack stack = playerInv.getStackInSlot(slot);

            if (bagIsOpenable(stack))
            {
                return stack;
            }
        }
        
        return ItemStack.EMPTY;
    }

    public int getSizeInventory(ItemStack containerStack)
    {
        return containerStack.getMetadata() == META_TIER_2 ? INV_SIZE_TIER_2 : INV_SIZE_TIER_1;
    }

    public static void performGuiAction(EntityPlayer player, int action, int element)
    {
        if (player.openContainer instanceof BagContainer)
        {
            BagContainer container = (BagContainer)player.openContainer;
            ModularHandler inv = container.inventoryItemWithMemoryCards;
            ItemStack stack = inv.getModularItemStack();

            if (stack.isEmpty() == false && stack.getItem() == ModItems.TF_BAG)
            {
                int max = ((TFBag)stack.getItem()).getMaxMemoryCards(stack);

                if (action == GUI_ACTION_SELECT_MODULE && element >= 0 && element < max)
                {
                    CardHelper.setMemoryCardSelection(stack, element, max);
                    inv.readFromContainerItemStack();
                    
                    player.openContainer.detectAndSendChanges();
                }
                else if (action == GUI_ACTION_MOVE_ITEMS)
                {
                    IItemHandlerModifiable playerMainInv = (IItemHandlerModifiable) player.getCapability(CapabilityItemHandler.ITEM_HANDLER_CAPABILITY, EnumFacing.UP);
                    IItemHandlerModifiable offhandInv = new PlayerOffhandInvWrapper(player.inventory);
                    IItemHandler playerInv = new CombinedInvWrapper(playerMainInv, offhandInv);
                    IItemHandler wrappedBagInv = getWrappedEnabledInv(stack, inv);

                    switch (element & 0x7FFF)
                    {
                        case 0:
                            if ((element & 0x8000) != 0)
                            {
                                StackHelper.tryMoveAllItems(playerInv, wrappedBagInv);
                            }
                            else
                            {
                                StackHelper.tryMoveAllItemsWithinSlotRange(playerInv, wrappedBagInv, new SlotRange(9, 27), new SlotRange(wrappedBagInv));
                            }
                            break;

                        case 1:
                            if ((element & 0x8000) != 0)
                            {
                                StackHelper.tryMoveMatchingItems(playerInv, wrappedBagInv);
                            }
                            else
                            {
                                StackHelper.tryMoveMatchingItemsWithinSlotRange(playerInv, wrappedBagInv, new SlotRange(9, 27), new SlotRange(wrappedBagInv));
                            }
                            break;

                        case 2:
                            StackHelper.leaveOneFullStackOfEveryItem(playerInv, wrappedBagInv, true);
                            break;

                        case 3:
                            StackHelper.fillStacksOfMatchingItems(wrappedBagInv, playerInv);
                            break;

                        case 4:
                            StackHelper.tryMoveMatchingItems(wrappedBagInv, playerInv);
                            break;

                        case 5:
                            StackHelper.tryMoveAllItems(wrappedBagInv, playerInv);
                            break;
                        default:
                            break;
                    }
                }
                else if (action == GUI_ACTION_SORT_ITEMS && element >= 0 && element <= 3)
                {
                    if (element == 3)
                    {
                        IItemHandlerModifiable playerMainInv = (IItemHandlerModifiable) player.getCapability(CapabilityItemHandler.ITEM_HANDLER_CAPABILITY, EnumFacing.UP);
                        StackHelper.sortInventoryWithinRange(playerMainInv, new SlotRange(9, 27));
                        player.openContainer.detectAndSendChanges();
                        return;
                    }

                    if (element > 0 && stack.getMetadata() == 0)
                    {
                        return;
                    }

                    StackHelper.sortInventoryWithinRange(inv, getSlotRangeForSection(element));
                }
                else if (action == GUI_ACTION_TOGGLE_REGION_LOCK && element >= 0 && element <= 2)
                {
                    setSlotMask(inv, stack, element, "LockMask");
                    inv.writeDataToContainerItemStack();
                    player.openContainer.detectAndSendChanges();
                    
                    // 优化：仅同步配置部分NBT，减少网络流量
                    PacketHandler.INSTANCE.sendTo(new SyncBagPacket(player.openContainer.windowId, stack.copy(), false), (EntityPlayerMP) player);
                }
                else if (action == GUI_ACTION_TOGGLE_MODES && (element & 0x03) <= 2)
                {
                    switch (element & 0x03)
                    {
                        case 0:
                            NBTHelper.toggleBoolean(stack, "TFBag", "DisableOpen");
                            break;
                        case 1:
                            PickupMode.cycleMode(stack, player, (element & 0x8000) != 0);
                            break;
                        case 2:
                            RestockMode.cycleMode(stack, player, (element & 0x8000) != 0);
                            break;
                        default:
                            break;
                    }
                    
                    inv.writeDataToContainerItemStack();
                    player.openContainer.detectAndSendChanges();
                    
                    // 优化：仅同步配置部分NBT
                    PacketHandler.INSTANCE.sendTo(new SyncBagPacket(player.openContainer.windowId, stack.copy(), false), (EntityPlayerMP) player);
                }
                else if (action == GUI_ACTION_TOGGLE_SHIFTCLICK)
                {
                    ShiftMode.cycleMode(stack, element != 0);
                    inv.writeDataToContainerItemStack();
                    player.openContainer.detectAndSendChanges();
                    
                    // 优化：仅同步配置部分NBT
                    PacketHandler.INSTANCE.sendTo(new SyncBagPacket(player.openContainer.windowId, stack.copy(), false), (EntityPlayerMP) player);
                }
                else if (action == GUI_ACTION_TOGGLE_SHIFTCLICK_DOUBLETAP)
                {
                    if (ShiftMode.fromStack(stack) == ShiftMode.DOUBLE_TAP)
                    {
                        ShiftMode.toggleDoubleTapEffectiveMode(stack);
                    }
                    inv.writeDataToContainerItemStack();
                    player.openContainer.detectAndSendChanges();
                    
                    // 优化：仅同步配置部分NBT
                    PacketHandler.INSTANCE.sendTo(new SyncBagPacket(player.openContainer.windowId, stack.copy(), false), (EntityPlayerMP) player);
                }
                else if (action == GUI_ACTION_OPEN_BAUBLES && ModCompat.isBaublesLoaded())
                {
                    BaublesHelper.openBaublesGui(player);
                }
            }
        }
    }

    private static void setSlotMask(ModularHandler inv, ItemStack bagStack, int bagSection, String tagName)
    {
        int slot = inv.getSelectedMemoryCardIndex();

        if (slot >= 0)
        {
            ItemStack cardStack = inv.getMemoryCardInventory().getStackInSlot(slot);

            if (cardStack.isEmpty() == false)
            {
                long[] masks = new long[] { 0x1FFFFFFL, 0x1FFF8000000L, 0x7FFE0000000000L };
                long mask = NBTHelper.getLong(cardStack, "TFBag", tagName);
                mask ^= masks[bagSection];
                NBTHelper.setLong(cardStack, "TFBag", tagName, mask);
                CardHelper.setSelectedMemoryCardStack(bagStack, cardStack);
                
                inv.writeDataToContainerItemStack();
            }
        }
    }

    private static SlotRange getSlotRangeForSection(int section)
    {
        if (section == 0)
        {
            return new SlotRange(0, 27);
        }
        else if (section == 1)
        {
            return new SlotRange(27, 14);
        }

        return new SlotRange(41, 14);
    }

    private static IItemHandler getWrappedEnabledInv(ItemStack stack, IItemHandlerModifiable baseInv)
    {
        ItemStack cardStack = CardHelper.getSelectedMemoryCardStack(stack);
        if (cardStack.isEmpty())
        {
            return StackHelper.NULL_INV;
        }

        if (stack.getMetadata() != 1)
        {
            return baseInv;
        }

        long[] masks = new long[] { 0x1FFFFFFL, 0x1FFF8000000L, 0x7FFE0000000000L };

        long lockMask = NBTHelper.getLong(cardStack, "TFBag", "LockMask");

        IItemHandlerModifiable inv = null;

        for (int i = 0; i < 3; i++)
        {
            if ((lockMask & masks[i]) == 0)
            {
                SlotRange range = getSlotRangeForSection(i);

                if (inv == null)
                {
                    inv = new RangedWrapper(baseInv, range.first, range.lastExc);
                }
                else
                {
                    inv = new CombinedInvWrapper(inv, new RangedWrapper(baseInv, range.first, range.lastExc));
                }
            }
        }

        return inv != null ? inv : StackHelper.NULL_INV;
    }


    public int getMaxMemoryCards(ItemStack containerStack)
    {
        return 4;
    }

    @Override
    public void getSubItemsCustom(CreativeTabs creativeTab, NonNullList<ItemStack> list)
    {
        list.add(new ItemStack(this, 1, 0));
        list.add(new ItemStack(this, 1, 1));
    }

    @Override
    @SideOnly(Side.CLIENT)
    public ResourceLocation[] getItemVariants()
    {
        String rl = ModInfo.MOD_ID + ":" + this.name;
        
        ResourceLocation[] variants = new ResourceLocation[4];
        int i = 0;

        for (String strL : new String[] { "false", "true" })
        {
            for (String strT : new String[] { "0", "1" })
            {
                String variant = String.format("locked=%s,tier=%s", strL, strT);
                variants[i++] = new ModelResourceLocation(rl, variant);
            }
        }

        return variants;
    }

    @Override
    @SideOnly(Side.CLIENT)
    public ModelResourceLocation getModelLocation(ItemStack stack)
    {
        String variant = "locked=" + (bagIsOpenable(stack) ? "false" : "true") +
                       ",tier=" + MathHelper.clamp(stack.getMetadata(), 0, 1);
                       
        return new ModelResourceLocation(ModInfo.MOD_ID + ":" + this.name, variant);
    }


    public enum PickupMode
    {
        NONE     (0, "tfstorage.tooltip.item.disabled", "none"),
        MATCHING (1, "tfstorage.tooltip.item.matching", "matching"),
        ALL      (2, "tfstorage.tooltip.item.all",      "all");

        private final String displayName;
        private final String variantName;

        private PickupMode (int id, String displayName, String variantName)
        {
            this.displayName = displayName;
            this.variantName = variantName;
        }

        @SideOnly(Side.CLIENT)
        public String getDisplayName()
        {
            return I18n.format(this.displayName);
        }

        public String getVariantName()
        {
            return this.variantName;
        }

        public static PickupMode fromStack(ItemStack bagStack)
        {
            int id = getModeId(bagStack);
            return (id >= 0 && id < values().length) ? values()[id] : NONE;
        }

        public static void cycleMode(ItemStack bagStack, EntityPlayer player, boolean reverse)
        {
            int id = getModeId(bagStack) + (reverse ? -1 : 1);

            if (id < 0)
            {
                id = values().length - 1;
            }
            else if (id >= values().length)
            {
                id = 0;
            }

            setModeId(bagStack, player, id);
        }

        private static int getModeId(ItemStack bagStack)
        {
            if (bagStack.isEmpty())
            {
                return PickupMode.NONE.ordinal();
            }
            return NBTHelper.getByte(bagStack, null, "PickupMode");
        }

        private static void setModeId(ItemStack bagStack, EntityPlayer player, int id)
        {
            if (bagStack.isEmpty() == false)
            {
                NBTHelper.setByte(bagStack, null, "PickupMode", (byte) id);
            }
        }
    }

    public enum RestockMode
    {
        OFF ("off"),
        ON  ("on");

        private final String name;

        private RestockMode (String name)
        {
            this.name = name;
        }

        public String getName()
        {
            return this.name;
        }

        @SideOnly(Side.CLIENT)
        public String getDisplayName()
        {
            return I18n.format("tfstorage.tooltip.item." + this.getName());
        }

        public static RestockMode fromStack(ItemStack bagStack)
        {
            return getModeId(bagStack) == 1 ? ON : OFF;
        }

        public static void cycleMode(ItemStack bagStack, EntityPlayer player, boolean reverse)
        {
            int currentId = getModeId(bagStack);
            setModeId(bagStack, player, currentId == 0 ? 1 : 0);
        }

        private static int getModeId(ItemStack bagStack)
        {
            if (bagStack.isEmpty())
            {
                return RestockMode.OFF.ordinal();
            }
            return NBTHelper.getByte(bagStack, null, "RestockMode");
        }

        private static void setModeId(ItemStack bagStack, EntityPlayer player, int id)
        {
            if (bagStack.isEmpty() == false)
            {
                NBTHelper.setByte(bagStack, null, "RestockMode", (byte) id);
            }
        }
    }

    public enum ShiftMode
    {
        TO_BAG      ("tfstorage.gui.label.tfbag.shiftclick.tobag"),
        INV_HOTBAR  ("tfstorage.gui.label.tfbag.shiftclick.invhotbar"),
        DOUBLE_TAP  ("tfstorage.gui.label.tfbag.shiftclick.doubletapshift");

        private final String unlocName;

        private ShiftMode (String unlocName)
        {
            this.unlocName = unlocName;
        }

        public String getUnlocName()
        {
            return this.unlocName;
        }

        @SideOnly(Side.CLIENT)
        public String getDisplayName()
        {
            return I18n.format(this.getUnlocName());
        }

        public static ShiftMode fromId(int id)
        {
            return (id >= 0 && id < values().length) ? values()[id] : TO_BAG;
        }

        public static ShiftMode fromStack(ItemStack bagStack)
        {
            return fromId(getModeId(bagStack) & 0x03);
        }

        public static void cycleMode(ItemStack bagStack, boolean reverse)
        {
            int rawMode = getModeId(bagStack);
            int id = (rawMode & 0x03) + (reverse ? -1 : 1);

            if (id < 0)
            {
                id = values().length - 1;
            }
            else if (id >= values().length)
            {
                id = 0;
            }

            rawMode = (rawMode & 0x80) | id;
            setModeId(bagStack, rawMode);
        }

        public static void toggleDoubleTapEffectiveMode(ItemStack bagStack)
        {
            byte rawMode = (byte) (getModeId(bagStack) ^ 0x80);
            setModeId(bagStack, rawMode);
        }

        public static ShiftMode getEffectiveMode(ItemStack bagStack)
        {
            int rawMode = getModeId(bagStack);
            ShiftMode mode = fromId(rawMode & 0x03);

            if (mode == ShiftMode.DOUBLE_TAP)
            {
                return (rawMode & 0x80) != 0 ? ShiftMode.INV_HOTBAR : ShiftMode.TO_BAG;
            }
            else
            {
                return mode;
            }
        }

        private static int getModeId(ItemStack bagStack)
        {
            return NBTHelper.getByte(bagStack, "TFBag", "ShiftMode");
        }

        private static void setModeId(ItemStack bagStack, int id)
        {
            NBTHelper.setByte(bagStack, "TFBag", "ShiftMode", (byte) id);
        }
        
        @SubscribeEvent
        public static void onEntityItemPickupEvent(EntityItemPickupEvent event)
        {
            TFBag.onEntityItemPickupEvent(event);
        }
    
    }
}
