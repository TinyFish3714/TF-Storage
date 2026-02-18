package tf.storage.platform.forge.menu;

import java.util.ArrayList;
import java.util.BitSet;
import java.util.List;
import javax.annotation.Nullable;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ClickType;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.items.IItemHandler;
import net.minecraftforge.items.IItemHandlerModifiable;
import net.minecraftforge.items.SlotItemHandler;
import net.minecraftforge.items.wrapper.CombinedInvWrapper;
import net.minecraftforge.items.wrapper.InvWrapper;
import net.minecraftforge.items.wrapper.PlayerArmorInvWrapper;
import net.minecraftforge.items.wrapper.PlayerOffhandInvWrapper;
import net.minecraftforge.items.wrapper.RangedWrapper;
import tf.storage.platform.forge.network.ModNetworking;
import tf.storage.platform.forge.network.packets.SyncSlotPacket;
import tf.storage.platform.forge.network.packets.SyncMultipleSlotsPacket;
import tf.storage.core.inventory.IItemHandlerSize;
import tf.storage.core.inventory.MergeSlotRange;
import tf.storage.platform.forge.inventory.slot.GenericSlot;
import tf.storage.platform.forge.inventory.slot.ResultSlot;

/**
 * Base container menu for TF Storage.
 * Provides common functionality for all TF Storage menus.
 */
public abstract class BaseMenu extends AbstractContainerMenu {
    
    public final Player player;
    protected final boolean isClient;
    protected final Inventory inventoryPlayer;
    protected final IItemHandlerModifiable playerInv;
    public final IItemHandler inventory;
    
    protected MergeSlotRange customInventorySlots;
    protected MergeSlotRange playerMainSlots;
    protected MergeSlotRange playerHotbarSlots;
    protected MergeSlotRange playerMainSlotsIncHotbar;
    protected MergeSlotRange playerOffhandSlots;
    protected MergeSlotRange playerArmorSlots;
    protected List<MergeSlotRange> mergeSlotRangesPlayerToExt;
    
    private BitSet dirtySlots;
    private boolean hasDirtySlots;
    private boolean forceCustomSlotsSync;

    protected int selectedSlot = -1;

    protected BaseMenu(@Nullable MenuType<?> menuType, int containerId, Player player, IItemHandler inventory) {
        super(menuType, containerId);
        this.player = player;
        this.isClient = player.level().isClientSide;
        this.inventoryPlayer = player.getInventory();
        this.playerInv = new CombinedInvWrapper(
            new PlayerMainInvWrapperNoSync(player.getInventory()),
            new PlayerArmorInvWrapper(player.getInventory()),
            new PlayerOffhandInvWrapper(player.getInventory())
        );
        this.inventory = inventory;
        this.mergeSlotRangesPlayerToExt = new ArrayList<>();

        this.customInventorySlots = new MergeSlotRange(0, 0);
        this.playerMainSlotsIncHotbar = new MergeSlotRange(0, 0);
        this.playerMainSlots = new MergeSlotRange(0, 0);
        this.playerHotbarSlots = new MergeSlotRange(0, 0);
        this.playerOffhandSlots = new MergeSlotRange(0, 0);
        this.playerArmorSlots = new MergeSlotRange(0, 0);
        
        this.dirtySlots = new BitSet(256);
        this.hasDirtySlots = false;
        this.forceCustomSlotsSync = false;
    }

    protected abstract void addCustomInventorySlots();
    
    protected void markSlotDirty(int slotId) {
        if (slotId >= 0 && slotId < this.slots.size()) {
            this.dirtySlots.set(slotId);
            this.hasDirtySlots = true;
        }
    }
    
    private void clearDirtySlots() {
        if (this.hasDirtySlots) {
            this.dirtySlots.clear();
            this.hasDirtySlots = false;
        }
    }
    
    protected void markAllSlotsDirty() {
        this.dirtySlots.set(0, this.slots.size());
        this.hasDirtySlots = true;
        this.forceCustomSlotsSync = true;
    }

    protected void addPlayerInventorySlots(int posX, int posY) {
        int playerInvStart = this.slots.size();

        // Main inventory (27 slots)
        for (int i = 0; i < 3; i++) {
            for (int j = 0; j < 9; j++) {
                this.addSlot(new GenericSlot(this.playerInv, i * 9 + j + 9, posX + j * 18, posY + i * 18));
            }
        }

        this.playerMainSlots = new MergeSlotRange(playerInvStart, 27);
        int playerHotbarStart = this.slots.size();

        // Hotbar (9 slots)
        for (int i = 0; i < 9; i++) {
            this.addSlot(new GenericSlot(this.playerInv, i, posX + i * 18, posY + 58));
        }

        this.playerMainSlotsIncHotbar = new MergeSlotRange(playerInvStart, 36);
        this.playerHotbarSlots = new MergeSlotRange(playerHotbarStart, 9);
    }

    protected void addOffhandSlot(int posX, int posY) {
        this.playerOffhandSlots = new MergeSlotRange(this.slots.size(), 1);
        this.addSlot(new GenericSlot(this.playerInv, 40, posX, posY));
    }

    public Player getPlayer() {
        return this.player;
    }

    @Override
    public boolean stillValid(Player player) {
        return true;
    }

    @Override
    public boolean canTakeItemForPickAll(ItemStack stack, Slot slot) {
        return (slot instanceof SlotItemHandler) &&
               !(slot instanceof ResultSlot) &&
               !this.getCarried().isEmpty();
    }

    @Nullable
    public Slot getSlotOrNull(int slotId) {
        return slotId >= 0 && slotId < this.slots.size() ? this.slots.get(slotId) : null;
    }

    public GenericSlot getSlotItemHandler(int slotId) {
        Slot slot = this.getSlotOrNull(slotId);
        return (slot instanceof GenericSlot) ? (GenericSlot) slot : null;
    }

    @Override
    public void broadcastChanges() {
        super.broadcastChanges();
        if (!this.player.level().isClientSide) {
            syncDirtySlots();
            syncCustomSlotsIfLargeStacks();
        }
    }

    private void syncDirtySlots() {
        if (!this.hasDirtySlots) {
            return;
        }
        List<Integer> slotIds = new ArrayList<>();
        List<ItemStack> stacks = new ArrayList<>();
        for (int i = this.dirtySlots.nextSetBit(0); i >= 0; i = this.dirtySlots.nextSetBit(i + 1)) {
            Slot slot = this.getSlotOrNull(i);
            if (slot != null) {
                slotIds.add(i);
                stacks.add(slot.getItem().copy());
            }
        }
        clearDirtySlots();
        if (!slotIds.isEmpty() && this.player instanceof ServerPlayer serverPlayer) {
            for (SyncMultipleSlotsPacket packet : SyncMultipleSlotsPacket.splitPackets(this.containerId, slotIds, stacks)) {
                ModNetworking.sendToPlayer(packet, serverPlayer);
            }
        }
    }

    @Override
    public ItemStack quickMoveStack(Player player, int slotNum) {
        this.transferStackFromSlot(player, slotNum);
        return ItemStack.EMPTY;
    }

    private void syncCustomSlotsIfLargeStacks() {
        if (!this.hasDirtySlots && !this.forceCustomSlotsSync) {
            return;
        }
        if (!(this.inventory instanceof IItemHandlerSize sizeHandler)) {
            return;
        }
        if (sizeHandler.getInventoryStackLimit() <= 64) {
            return;
        }
        if (!(this.player instanceof ServerPlayer serverPlayer)) {
            return;
        }

        List<Integer> slotIds = new ArrayList<>();
        List<ItemStack> stacks = new ArrayList<>();

        int start = this.customInventorySlots.first;
        int end = this.customInventorySlots.lastExc;

        for (int i = start; i < end; i++) {
            Slot slot = this.getSlotOrNull(i);
            if (slot != null) {
                slotIds.add(i);
                stacks.add(slot.getItem().copy());
            }
        }

        if (!slotIds.isEmpty()) {
            for (SyncMultipleSlotsPacket packet : SyncMultipleSlotsPacket.splitPackets(this.containerId, slotIds, stacks)) {
                ModNetworking.sendToPlayer(packet, serverPlayer);
            }
        }

        this.forceCustomSlotsSync = false;
    }

    @Override
    public void clicked(int slotNum, int dragType, ClickType clickType, Player player) {
        if (clickType == ClickType.CLONE) {
            handleMiddleClick(slotNum, player);
            return;
        }
        if (slotNum >= 0 && slotNum < this.slots.size() && clickType == ClickType.PICKUP) {
            Slot clickedSlot = this.getSlot(slotNum);

            if (clickedSlot instanceof SlotItemHandler handlerSlot && handlerSlot.getItemHandler() == this.inventory) {
                ItemStack cursorStack = this.getCarried();
                ItemStack slotStack = clickedSlot.getItem();

                if (!slotStack.isEmpty() && slotStack.getCount() > slotStack.getMaxStackSize()) {
                    if (cursorStack.isEmpty()) {
                        int itemMaxStack = slotStack.getMaxStackSize();
                        int toTake = Math.min(slotStack.getCount(), itemMaxStack);
                        if (dragType == 1) {
                            toTake = Math.min((slotStack.getCount() + 1) / 2, itemMaxStack);
                        }

                        ItemStack taken = slotStack.copy();
                        taken.setCount(toTake);
                        slotStack.shrink(toTake);

                        clickedSlot.set(slotStack.isEmpty() ? ItemStack.EMPTY : slotStack);
                        this.setCarried(taken);
                        this.markSlotDirty(slotNum);
                        syncSlotToClient(slotNum);
                        syncCursorStackToClient();
                        return;
                    } else if (ItemStack.isSameItemSameTags(slotStack, cursorStack)) {
                        int slotLimit = clickedSlot.getMaxStackSize(cursorStack);
                        int spaceAvailable = slotLimit - slotStack.getCount();

                        if (spaceAvailable > 0) {
                            int toMerge = (dragType == 1) ? 1 : Math.min(spaceAvailable, cursorStack.getCount());
                            cursorStack.shrink(toMerge);
                            if (cursorStack.isEmpty()) {
                                this.setCarried(ItemStack.EMPTY);
                            }
                            slotStack.grow(toMerge);
                            clickedSlot.set(slotStack);
                            this.markSlotDirty(slotNum);
                            syncSlotToClient(slotNum);
                            syncCursorStackToClient();
                        }
                        return;
                    } else {
                        return;
                    }
                } else if (!cursorStack.isEmpty() && !slotStack.isEmpty() && ItemStack.isSameItemSameTags(slotStack, cursorStack)) {
                    int slotLimit = clickedSlot.getMaxStackSize(cursorStack);
                    if (slotLimit > cursorStack.getMaxStackSize()) {
                        int spaceAvailable = slotLimit - slotStack.getCount();
                        if (spaceAvailable > 0) {
                            int toMerge = (dragType == 1) ? 1 : Math.min(spaceAvailable, cursorStack.getCount());
                            cursorStack.shrink(toMerge);
                            if (cursorStack.isEmpty()) {
                                this.setCarried(ItemStack.EMPTY);
                            }
                            slotStack.grow(toMerge);
                            clickedSlot.set(slotStack);
                            this.markSlotDirty(slotNum);
                            syncSlotToClient(slotNum);
                            syncCursorStackToClient();
                            return;
                        }
                    }
                }
            }
        }

        super.clicked(slotNum, dragType, clickType, player);
    }

    private void handleMiddleClick(int slotNum, Player player) {
        if (slotNum < 0 || slotNum >= this.slots.size()) {
            return;
        }

        Slot slot = this.getSlot(slotNum);
        if (!(slot instanceof SlotItemHandler handlerSlot) || handlerSlot.getItemHandler() != this.inventory) {
            return;
        }

        // Creative clone behavior
        if (player.isCreative() && this.getCarried().isEmpty() && slot.hasItem()) {
            ItemStack stack = slot.getItem().copy();
            stack.setCount(stack.getMaxStackSize());
            this.setCarried(stack);
            syncCursorStackToClient();
            return;
        }

        if (this.selectedSlot >= 0 && this.selectedSlot < this.slots.size()) {
            if (this.selectedSlot != slotNum) {
                Slot slot2 = this.getSlot(this.selectedSlot);
                if (slot2 instanceof SlotItemHandler handlerSlot2 && handlerSlot2.getItemHandler() == this.inventory) {
                    swapSlots(handlerSlot, handlerSlot2, slotNum, this.selectedSlot, player);
                }
            }
            this.selectedSlot = -1;
        } else {
            this.selectedSlot = slotNum;
        }
    }

    private void swapSlots(SlotItemHandler slot1, SlotItemHandler slot2, int slotNum1, int slotNum2, Player player) {
        if (!slot1.mayPickup(player) || !slot2.mayPickup(player)) {
            return;
        }

        ItemStack stack1 = slot1.getItem();
        ItemStack stack2 = slot2.getItem();

        if ((!stack1.isEmpty() && !slot2.mayPlace(stack1)) || (!stack2.isEmpty() && !slot1.mayPlace(stack2))) {
            return;
        }

        if (!stack1.isEmpty()) {
            slot1.onTake(player, stack1);
        }
        if (!stack2.isEmpty()) {
            slot2.onTake(player, stack2);
        }

        slot1.set(stack2.isEmpty() ? ItemStack.EMPTY : stack2.copy());
        slot2.set(stack1.isEmpty() ? ItemStack.EMPTY : stack1.copy());

        this.markSlotDirty(slotNum1);
        this.markSlotDirty(slotNum2);
        syncSlotToClient(slotNum1);
        syncSlotToClient(slotNum2);
    }

    protected void syncSlotToClient(int slotId) {
        if (this.player instanceof ServerPlayer serverPlayer) {
            Slot slot = this.getSlotOrNull(slotId);
            if (slot != null) {
                ModNetworking.sendToPlayer(new SyncSlotPacket(this.containerId, slotId, slot.getItem().copy()), serverPlayer);
            }
        }
    }

    protected void syncCursorStackToClient() {
        if (this.player instanceof ServerPlayer serverPlayer) {
            ModNetworking.sendToPlayer(new SyncSlotPacket(this.containerId, -1, this.getCarried().copy()), serverPlayer);
        }
    }

    protected boolean transferStackFromSlot(Player player, int slotNum) {
        Slot slot = this.getSlotOrNull(slotNum);

        if (slot == null || !slot.hasItem() || !slot.mayPickup(player)) {
            return false;
        }

        // Handle ResultSlot (crafting output) using vanilla logic
        if (slot instanceof ResultSlot) {
            boolean craftedAny = false;

            while (true) {
                ItemStack resultStack = slot.getItem();
                if (resultStack.isEmpty()) {
                    break;
                }

                ItemStack toMove = resultStack.copy();
                boolean moved = this.moveItemStackTo(toMove, this.playerMainSlotsIncHotbar.first, this.playerMainSlotsIncHotbar.lastExc, false);
                if (!moved) {
                    moved = this.moveItemStackTo(toMove, this.customInventorySlots.first, this.customInventorySlots.lastExc, false);
                }

                if (!moved || !toMove.isEmpty()) {
                    break;
                }

                slot.onTake(player, resultStack);
                craftedAny = true;
            }

            return craftedAny;
        }

        if (this.playerArmorSlots.contains(slotNum) || this.playerOffhandSlots.contains(slotNum)) {
            return this.transferStackToSlotRange(player, slotNum, this.playerMainSlotsIncHotbar, false);
        } else if (this.playerMainSlotsIncHotbar.contains(slotNum)) {
            return this.transferStackFromPlayerMainInventory(player, slotNum);
        }

        return this.transferStackToSlotRange(player, slotNum, this.playerMainSlotsIncHotbar, true);
    }

    protected boolean transferStackFromPlayerMainInventory(Player player, int slotNum) {
        if (this.transferStackToSlotRange(player, slotNum, this.playerArmorSlots, false)) {
            return true;
        }

        if (this.transferStackToPrioritySlots(player, slotNum, false)) {
            return true;
        }

        return this.transferStackToSlotRange(player, slotNum, this.customInventorySlots, false);
    }

    protected boolean transferStackToPrioritySlots(Player player, int slotNum, boolean reverse) {
        boolean ret = false;

        for (MergeSlotRange slotRange : this.mergeSlotRangesPlayerToExt) {
            ret |= this.transferStackToSlotRange(player, slotNum, slotRange, reverse);
        }

        return ret;
    }

    protected boolean transferStackToSlotRange(Player player, int slotNum, MergeSlotRange slotRange, boolean reverse) {
        GenericSlot slot = this.getSlotItemHandler(slotNum);

        if (slot == null || !slot.hasItem() || !slot.mayPickup(player)) {
            return false;
        }

        ItemStack stack = slot.getItem().copy();
        int amount = Math.min(stack.getCount(), stack.getMaxStackSize());
        stack.setCount(amount);

        stack = this.mergeItemStackToRange(stack, slotRange, reverse, true);

        if (!stack.isEmpty()) {
            if (!slot.mayPlace(stack) || stack.getCount() == amount) {
                return false;
            }

            amount -= stack.getCount();
        }

        stack = slot.remove(amount);
        slot.onTake(player, stack);

        this.markSlotDirty(slotNum);

        stack = this.mergeItemStackToRange(stack, slotRange, reverse, false);

        if (!stack.isEmpty()) {
            slot.insertItem(stack, false);
        }

        return true;
    }

    protected ItemStack mergeItemStackToRange(ItemStack stack, MergeSlotRange slotRange, boolean reverse, boolean simulate) {
        int slotStart = slotRange.first;
        int slotEndExclusive = slotRange.lastExc;
        int slotIndex = reverse ? slotEndExclusive - 1 : slotStart;

        // First pass: merge with existing stacks
        while (!stack.isEmpty() && slotIndex >= slotStart && slotIndex < slotEndExclusive) {
            GenericSlot slot = this.getSlotItemHandler(slotIndex);

            if (slot != null && slot.hasItem() && slot.mayPlace(stack)) {
                ItemStack before = slot.getItem().copy();
                stack = slot.insertItem(stack, simulate);
                 
                if (!simulate && !ItemStack.matches(before, slot.getItem())) {
                    this.markSlotDirty(slotIndex);
                }
            }

            slotIndex = reverse ? slotIndex - 1 : slotIndex + 1;
        }

        // Second pass: insert into empty slots
        if (!stack.isEmpty() && !slotRange.existingOnly) {
            slotIndex = reverse ? slotEndExclusive - 1 : slotStart;

            while (!stack.isEmpty() && slotIndex >= slotStart && slotIndex < slotEndExclusive) {
                GenericSlot slot = this.getSlotItemHandler(slotIndex);

                if (slot != null && !slot.hasItem() && slot.mayPlace(stack)) {
                    ItemStack before = slot.getItem().copy();
                    stack = slot.insertItem(stack, simulate);
                     
                    if (!simulate && !ItemStack.matches(before, slot.getItem())) {
                        this.markSlotDirty(slotIndex);
                    }
                }

                slotIndex = reverse ? slotIndex - 1 : slotIndex + 1;
            }
        }

        return stack;
    }

    protected void addMergeSlotRangePlayerToExt(int start, int numSlots) {
        this.addMergeSlotRangePlayerToExt(start, numSlots, false);
    }

    protected void addMergeSlotRangePlayerToExt(int start, int numSlots, boolean existingOnly) {
        this.mergeSlotRangesPlayerToExt.add(new MergeSlotRange(start, numSlots, existingOnly));
    }

    /**
     * Sync a stack into a slot from the server.
     * Used for large stack sizes that vanilla can't handle.
     */
    public void syncStackInSlot(int slotId, ItemStack stack) {
        if (slotId == -1) {
            // Cursor stack
            this.setCarried(stack);
        } else {
            Slot slot = this.getSlotOrNull(slotId);
            if (slot instanceof GenericSlot genericSlot) {
                genericSlot.syncStack(stack);
            } else if (slot != null) {
                slot.set(stack);
            }
            this.markSlotDirty(slotId);
        }
    }

    /**
     * Player main inventory wrapper that doesn't sync on insert.
     */
    private static class PlayerMainInvWrapperNoSync extends RangedWrapper {
        private final Inventory inventoryPlayer;

        public PlayerMainInvWrapperNoSync(Inventory inv) {
            super(new InvWrapper(inv), 0, inv.items.size());
            this.inventoryPlayer = inv;
        }

        @Override
        public ItemStack insertItem(int slot, ItemStack stack, boolean simulate) {
            ItemStack stackRemaining = super.insertItem(slot, stack, simulate);

            if (!stackRemaining.isEmpty() && stackRemaining.getCount() != stack.getCount()) {
                ItemStack stackSlot = this.getStackInSlot(slot);
                if (!stackSlot.isEmpty() && this.inventoryPlayer.player.level().isClientSide) {
                    stackSlot.setPopTime(5);
                }
            }

            return stackRemaining;
        }
    }
}
