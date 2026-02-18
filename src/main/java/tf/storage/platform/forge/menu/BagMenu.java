package tf.storage.platform.forge.menu;

import net.minecraft.network.FriendlyByteBuf;
import java.util.Optional;
import java.util.UUID;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.Container;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.inventory.ResultContainer;
import net.minecraft.world.inventory.TransientCraftingContainer;
import net.minecraft.world.inventory.SimpleContainerData;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.CraftingRecipe;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.level.Level;
import net.minecraftforge.items.IItemHandler;
import net.minecraftforge.items.wrapper.InvWrapper;
import net.minecraftforge.items.wrapper.PlayerArmorInvWrapper;
import tf.storage.core.inventory.MergeSlotRange;
import tf.storage.core.util.NBTHelper;
import tf.storage.platform.forge.ModRegistry;
import tf.storage.platform.forge.inventory.handler.ModularHandler;
import tf.storage.platform.forge.compat.CuriosCompat;
import tf.storage.platform.forge.inventory.slot.ArmorSlot;
import tf.storage.platform.forge.inventory.slot.CardSlot;
import tf.storage.platform.forge.inventory.slot.GenericSlot;
import tf.storage.platform.forge.inventory.slot.ResultSlot;
import tf.storage.platform.forge.item.TFBagItem;

/**
 * Menu for TF Bag item.
 */
public class BagMenu extends BaseMenu {

    private static final int PLAYER_INV_Y = 174;

    private final ModularHandler inventoryItemWithMemoryCards;
    private final TransientCraftingContainer craftMatrix;
    private final ResultContainer craftResult;
    private final IItemHandler craftMatrixWrapper;
    private int craftingSlot = -1;
    private final ContainerData data;
    private int selectedMemoryCard = -1;
    private ItemStack lastMemoryCardStack = ItemStack.EMPTY;
    private ItemStack modularStackLast = ItemStack.EMPTY;
    private final int bagTier;

    // Server constructor
    public BagMenu(int containerId, Inventory playerInventory, ItemStack bagStack) {
        super(ModRegistry.TF_BAG_MENU.get(), containerId, playerInventory.player,
            new ModularHandler(bagStack, playerInventory.player, true));

        this.inventoryItemWithMemoryCards = (ModularHandler) this.inventory;
        this.bagTier = resolveBagTier(bagStack);
        this.craftMatrix = new TransientCraftingContainer(this, 3, 3);
        this.craftResult = new ResultContainer();
        this.craftMatrixWrapper = new InvWrapper(this.craftMatrix);
        this.data = new SimpleContainerData(1);

        this.selectedMemoryCard = this.inventoryItemWithMemoryCards.getSelectedMemoryCardIndex();


        this.addCustomInventorySlots();
        this.addPlayerInventoryAndExtras(PLAYER_INV_Y);

        this.addDataSlots(this.data);

        if (!playerInventory.player.level().isClientSide) {
            this.data.set(0, this.selectedMemoryCard);
            updateCraftingResult();
        }
    }

    // Client constructor
    public BagMenu(int containerId, Inventory playerInventory, FriendlyByteBuf buf) {
        this(containerId, playerInventory, readBagStack(playerInventory, buf));
    }

    private static ItemStack readBagStack(Inventory playerInventory, FriendlyByteBuf buf) {
        ItemStack stack = buf.readItem();
        if (!stack.isEmpty() && stack.getItem() instanceof TFBagItem) {
            return stack;
        }
        ItemStack fromInv = TFBagItem.getOpenableBag(playerInventory.player);
        if (!fromInv.isEmpty()) {
            return fromInv;
        }
        return CuriosCompat.findOpenableBag(playerInventory.player);
    }

    private static int resolveBagTier(ItemStack stack) {
        if (!stack.isEmpty() && stack.getItem() instanceof TFBagItem bagItem) {
            return bagItem.getTier() == TFBagItem.Tier.TIER_2 ? 1 : 0;
        }
        return 0;
    }

    @Override
    protected void addCustomInventorySlots() {
        int customInvStart = this.slots.size();
        int xOff = 8;
        int yOff = 102;

        if (this.getBagTier() == 1) {
            xOff += 40;
        }

        // Main 3x9 grid (27 slots)
        for (int i = 0; i < 3; i++) {
            for (int j = 0; j < 9; j++) {
                this.addSlot(new GenericSlot(this.inventory, i * 9 + j, xOff + j * 18, yOff - 2 + i * 18));
            }
        }

        // Extra columns for large bag (tier 1)
        if (this.getBagTier() == 1) {
            int xOffXtra = 8;
            yOff = 102;

            for (int i = 0; i < 7; i++) {
                this.addSlot(new GenericSlot(this.inventory, 27 + i * 2, xOffXtra, yOff - 7 + i * 18));
                this.addSlot(new GenericSlot(this.inventory, 28 + i * 2, xOffXtra + 18, yOff - 7 + i * 18));
            }

            xOffXtra = 214;
            for (int i = 0; i < 7; i++) {
                this.addSlot(new GenericSlot(this.inventory, 41 + i * 2, xOffXtra, yOff - 7 + i * 18));
                this.addSlot(new GenericSlot(this.inventory, 42 + i * 2, xOffXtra + 18, yOff - 7 + i * 18));
            }
        }

        this.customInventorySlots = new MergeSlotRange(customInvStart, this.slots.size() - customInvStart);

        // Memory card slots
        int memCardPosX = xOff + 90;
        int memCardPosY = 69;
        IItemHandler memoryCardInv = this.inventoryItemWithMemoryCards.getMemoryCardInventory();
        int memoryCardSlots = memoryCardInv.getSlots();
        this.addMergeSlotRangePlayerToExt(this.slots.size(), memoryCardSlots);

        for (int i = 0; i < memoryCardSlots; i++) {
            this.addSlot(new CardSlot(memoryCardInv, i, memCardPosX + i * 18, memCardPosY));
        }
    }

    private void addPlayerInventoryAndExtras(int posY) {
        int posX = 8;
        if (this.getBagTier() == 1) {
            posX += 40;
        }

        // Main inventory + hotbar
        this.addPlayerInventorySlots(posX, posY - 7);

        // Armor slots (from top to bottom: helmet, chestplate, leggings, boots)
        int playerArmorStart = this.slots.size();
        IItemHandler armorInv = new PlayerArmorInvWrapper(this.player.getInventory());
        int armorPosY = 15;

        for (int i = 0; i < 4; i++) {
            // Reverse order: 3-i gives us helmet(3), chestplate(2), leggings(1), boots(0) from top to bottom
            int armorIndex = 3 - i;
            this.addSlot(new ArmorSlot(this.player, armorInv, armorIndex, armorIndex, posX, armorPosY + i * 18));
        }
        this.playerArmorSlots = new MergeSlotRange(playerArmorStart, 4);

        // Offhand
        this.addOffhandSlot(posX + 4 * 18, 69);

        // Crafting grid
        posX += 90;
        int craftPosY = 15;
        this.craftingSlot = this.slots.size();
        this.addSlot(new ResultSlot(this.craftMatrix, this.craftResult, 0, posX + 56, craftPosY + 18, this.player));

        for (int i = 0; i < 3; i++) {
            for (int j = 0; j < 3; j++) {
                this.addSlot(new GenericSlot(this.craftMatrixWrapper, j + i * 3, posX + j * 18, craftPosY + i * 18));
            }
        }
    }

    @Override
    public void slotsChanged(Container container) {
        super.slotsChanged(container);

        if (container == this.craftMatrix && !this.player.level().isClientSide) {
            updateCraftingResult();
        }
    }

    @Override
    public void removed(Player player) {
        super.removed(player);
        if (!player.level().isClientSide) {
            this.clearContainer(player, this.craftMatrix);
            this.craftResult.clearContent();
        }
    }

    private void updateCraftingResult() {
        if (!(this.player instanceof ServerPlayer serverPlayer)) {
            return;
        }

        Level level = serverPlayer.level();
        var recipe = level.getRecipeManager().getRecipeFor(RecipeType.CRAFTING, this.craftMatrix, level);

        ItemStack result = ItemStack.EMPTY;
        if (recipe.isPresent()) {
            result = recipe.get().assemble(this.craftMatrix, level.registryAccess());
        }

        this.craftResult.setItem(0, result);
        this.broadcastChanges();
    }

    public int getCraftingSlotStart() {
        return this.craftingSlot + 1;
    }

    public int getCraftingSlotCount() {
        return 9;
    }

    public int getPlayerInventorySlotStart() {
        return this.playerMainSlotsIncHotbar.first;
    }

    public int getPlayerInventorySlotCount() {
        return this.playerMainSlotsIncHotbar.lastExc - this.playerMainSlotsIncHotbar.first;
    }

    public int getCustomInventorySlotStart() {
        return this.customInventorySlots.first;
    }

    public int getCustomInventorySlotCount() {
        return this.customInventorySlots.lastExc - this.customInventorySlots.first;
    }

    @Override
    public void broadcastChanges() {
        if (!this.player.level().isClientSide) {
            int previousSelectedIndex = this.selectedMemoryCard;
            ItemStack modularStack = this.inventoryItemWithMemoryCards.getModularItemStack();
            boolean containerChanged = !ItemStack.matches(modularStack, this.modularStackLast);

            ItemStack currentCard = this.inventoryItemWithMemoryCards.getSelectedMemoryCardStack();
            int currentSelectedIndex = this.inventoryItemWithMemoryCards.getSelectedMemoryCardIndex();

            boolean cardChanged = !ItemStack.matches(currentCard, this.lastMemoryCardStack);
            boolean selectionChanged = previousSelectedIndex != currentSelectedIndex;

            if (containerChanged || cardChanged || selectionChanged) {
                if (containerChanged) {
                    this.inventoryItemWithMemoryCards.readFromContainerItemStack();
                    this.modularStackLast = modularStack.copy();
                } else if (selectionChanged) {
                    this.inventoryItemWithMemoryCards.readFromContainerItemStack();
                } else {
                    this.inventoryItemWithMemoryCards.readFromSelectedMemoryCardStack();
                }

                currentCard = this.inventoryItemWithMemoryCards.getSelectedMemoryCardStack();
                currentSelectedIndex = this.inventoryItemWithMemoryCards.getSelectedMemoryCardIndex();
                selectionChanged = previousSelectedIndex != currentSelectedIndex;

                this.lastMemoryCardStack = currentCard.copy();
                this.selectedMemoryCard = currentSelectedIndex;

                this.markAllSlotsDirty();
            }

            if (selectionChanged) {
                this.data.set(0, this.selectedMemoryCard);
                this.inventoryItemWithMemoryCards.cachedSelectedIndex = this.selectedMemoryCard;
            }
        }

        super.broadcastChanges();
    }

    @Override
    public void setData(int id, int value) {
        super.setData(id, value);
        if (id == 0) {
            this.selectedMemoryCard = value;
            this.inventoryItemWithMemoryCards.cachedSelectedIndex = value;
            this.inventoryItemWithMemoryCards.readFromContainerItemStack();
            this.markAllSlotsDirty();
        }
    }

    public int getBagTier() {
        return this.bagTier;
    }

    @Override
    protected boolean transferStackFromPlayerMainInventory(Player player, int slotNum) {
        ItemStack modularStack = this.inventoryItemWithMemoryCards.getModularItemStack();

        if (!modularStack.isEmpty() && TFBagItem.ShiftMode.getEffectiveMode(modularStack) == TFBagItem.ShiftMode.INV_HOTBAR) {
            if (this.playerHotbarSlots.contains(slotNum)) {
                return this.transferStackToSlotRange(player, slotNum, this.playerMainSlots, false);
            } else if (this.playerMainSlots.contains(slotNum)) {
                return this.transferStackToSlotRange(player, slotNum, this.playerHotbarSlots, false);
            }
        }

        return super.transferStackFromPlayerMainInventory(player, slotNum);
    }

    public ItemStack getContainerItem() {
        return this.inventoryItemWithMemoryCards.getModularItemStack();
    }

    public ModularHandler getInventoryItemWithMemoryCards() {
        return this.inventoryItemWithMemoryCards;
    }

    public int getSelectedMemoryCardIndex() {
        return this.inventoryItemWithMemoryCards.getSelectedMemoryCardIndex();
    }

    public ItemStack getMemoryCardStack(int index) {
        return this.inventoryItemWithMemoryCards.getMemoryCardInventory().getStackInSlot(index);
    }

    public int getBagMenuSlotIndex() {
        ItemStack bagStack = this.inventoryItemWithMemoryCards.getModularItemStack();
        if (bagStack.isEmpty()) {
            return -1;
        }

        UUID uuid = NBTHelper.getUUIDFromItemStack(bagStack, "UUID", false);
        if (uuid == null) {
            return -1;
        }

        Inventory inv = this.player.getInventory();
        int invSize = inv.getContainerSize();
        for (int slot = 0; slot < invSize; slot++) {
            ItemStack stack = inv.getItem(slot);
            if (!stack.isEmpty() && uuid.equals(NBTHelper.getUUIDFromItemStack(stack, "UUID", false))) {
                return getMenuSlotIndexForPlayerSlot(slot);
            }
        }

        return -1;
    }

    private int getMenuSlotIndexForPlayerSlot(int invSlot) {
        if (invSlot >= 9 && invSlot < 36) {
            return this.playerMainSlots.first + (invSlot - 9);
        }
        if (invSlot >= 0 && invSlot < 9) {
            return this.playerHotbarSlots.first + invSlot;
        }
        if (invSlot == 40) {
            return this.playerOffhandSlots.first;
        }
        return -1;
    }

    public void refreshAfterAction() {
        this.inventoryItemWithMemoryCards.readFromContainerItemStack();
        this.selectedMemoryCard = this.inventoryItemWithMemoryCards.getSelectedMemoryCardIndex();
        this.lastMemoryCardStack = this.inventoryItemWithMemoryCards.getSelectedMemoryCardStack().copy();
        this.markAllSlotsDirty();
    }
}
