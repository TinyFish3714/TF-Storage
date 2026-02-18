package tf.storage.platform.forge.block;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.world.Nameable;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.capabilities.ForgeCapabilities;
import net.minecraftforge.common.util.LazyOptional;
import net.minecraftforge.items.IItemHandler;
import net.minecraftforge.items.ItemStackHandler;
import tf.storage.platform.forge.inventory.handler.ItemHandler;
import tf.storage.platform.forge.ModRegistry;
import tf.storage.platform.forge.network.GuiActions;

/**
 * TFChestBlockEntity - TF箱子方块实体
 * 管理存储卡槽位和物品存储
 * 
 * 适配 Minecraft 1.20.1
 */
public class TFChestBlockEntity extends BlockEntity implements Nameable, MenuProvider {
    
    public static final int MAX_MEMORY_CARDS = 4;
    public static final int GUI_ACTION_SELECT_MEMORY_CARD = GuiActions.Chest.SELECT_MEMORY_CARD;
    public static final int GUI_ACTION_MOVE_ITEMS = GuiActions.Chest.MOVE_ITEMS;
    public static final int GUI_ACTION_SORT_ITEMS = GuiActions.Chest.SORT_ITEMS;
    
    private TFChestBlock.Tier tier;
    private Direction facing = Direction.NORTH;
    private Component customName;
    
    // 存储卡槽位
    private final ItemStackHandler memoryCardHandler;
    private int selectedMemoryCard = 0;
    
    // 物品存储 - 基于选中的存储卡
    private ItemHandler itemHandler;
    private LazyOptional<IItemHandler> itemHandlerLazy = LazyOptional.empty();
    
    public TFChestBlockEntity(BlockPos pos, BlockState state) {
        this(pos, state, TFChestBlock.Tier.TIER_0);
    }
    
    public TFChestBlockEntity(BlockPos pos, BlockState state, TFChestBlock.Tier tier) {
        super(ModRegistry.TF_CHEST_BE.get(), pos, state);
        this.tier = resolveTier(state, tier);
        
        this.memoryCardHandler = new ItemStackHandler(MAX_MEMORY_CARDS) {
            @Override
            public boolean isItemValid(int slot, @Nonnull ItemStack stack) {
                return stack.getItem() instanceof tf.storage.platform.forge.item.TFUnitItem;
            }
            
            @Override
            public int getSlotLimit(int slot) {
                return 1;
            }
            
            @Override
            protected void onContentsChanged(int slot) {
                setChanged();
                if (slot == selectedMemoryCard) {
                    updateItemHandler();
                }
            }
        };
        
        initItemHandler();
    }

    private TFChestBlock.Tier resolveTier(BlockState state, TFChestBlock.Tier fallback) {
        if (state.getBlock() instanceof TFChestBlock block) {
            return block.getTier();
        }
        return fallback != null ? fallback : TFChestBlock.Tier.TIER_0;
    }

    private void initItemHandler() {
        this.itemHandler = new ItemHandler(ItemStack.EMPTY, this.tier.getInvSize(), 64, true, "Items") {
            @Override
            public void onContentsChanged(int slot) {
                super.onContentsChanged(slot);
                setChanged();
                if (level != null && !level.isClientSide) {
                    level.updateNeighbourForOutputSignal(worldPosition, getBlockState().getBlock());
                }
            }
        };
        this.itemHandlerLazy = LazyOptional.of(() -> itemHandler);
        updateItemHandler();
    }
    
    public TFChestBlock.Tier getTier() {
        return tier;
    }
    
    public Direction getFacing() {
        return facing;
    }
    
    public void setFacing(Direction facing) {
        this.facing = facing;
        setChanged();
        if (level != null && !level.isClientSide) {
            level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), 3);
        }
    }
    
    public void setCustomName(Component name) {
        this.customName = name;
    }
    
    @Override
    public Component getName() {
        return customName != null ? customName : Component.translatable("container.tfstorage.tf_chest");
    }
    
    @Override
    public Component getDisplayName() {
        return getName();
    }

    @Override
    public AbstractContainerMenu createMenu(int containerId, Inventory playerInventory, Player player) {
        return new tf.storage.platform.forge.menu.ChestMenu(containerId, playerInventory, this);
    }
    
    @Nullable
    @Override
    public Component getCustomName() {
        return customName;
    }
    
    public boolean isUseableByPlayer(Player player) {
        if (level == null || level.getBlockEntity(worldPosition) != this) {
            return false;
        }
        return player.distanceToSqr(worldPosition.getX() + 0.5, worldPosition.getY() + 0.5, worldPosition.getZ() + 0.5) <= 64.0;
    }
    
    // ==================== 存储卡管理 ====================
    
    public IItemHandler getMemoryCardInventory() {
        return memoryCardHandler;
    }
    
    public int getSelectedMemoryCardIndex() {
        return selectedMemoryCard;
    }

    public boolean isInventoryAccessible(Player player) {
        return !getSelectedMemoryCard().isEmpty();
    }
    
    public void setSelectedMemoryCard(int index) {
        this.selectedMemoryCard = Math.max(0, Math.min(index, MAX_MEMORY_CARDS - 1));
        updateItemHandler();
        setChanged();
        if (level != null && !level.isClientSide) {
            level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), 3);
        }
    }
    
    public ItemStack getSelectedMemoryCard() {
        return memoryCardHandler.getStackInSlot(selectedMemoryCard);
    }
    
    public IItemHandler getBaseItemHandler() {
        return memoryCardHandler;
    }

    public IItemHandler getItemHandler() {
        return itemHandler;
    }
    
    /**
     * Called when inventory contents change.
     * Used by TileHandler to notify the block entity.
     */
    public void inventoryChanged(int inventoryId, int slot) {
        // Inventory changes are persisted by ItemHandler
    }
    
    /**
     * Perform a GUI action from a network packet.
     * @param player The player performing the action
     * @param action The action ID
     * @param elementId The element ID (e.g., button index)
     */
    public void performGuiAction(Player player, int action, int elementId) {
        if (action == GUI_ACTION_SELECT_MEMORY_CARD && elementId >= 0 && elementId < MAX_MEMORY_CARDS) {
            setSelectedMemoryCard(elementId);
        } else if (action == GUI_ACTION_MOVE_ITEMS) {
            int op = elementId & 0x7FFF;
            boolean shift = (elementId & 0x8000) != 0;

            if (op >= 0 && op < 6) {
                IItemHandler inventory = this.itemHandler;
                net.minecraftforge.items.IItemHandlerModifiable playerMainInv = new net.minecraftforge.items.wrapper.RangedWrapper(
                    new net.minecraftforge.items.wrapper.InvWrapper(player.getInventory()), 0, player.getInventory().items.size());
                net.minecraftforge.items.IItemHandlerModifiable offhandInv = new net.minecraftforge.items.wrapper.PlayerOffhandInvWrapper(player.getInventory());
                net.minecraftforge.items.IItemHandler playerInv = new net.minecraftforge.items.wrapper.CombinedInvWrapper(playerMainInv, offhandInv);

                tf.storage.core.inventory.SlotRange chestSlotRange = new tf.storage.core.inventory.SlotRange(inventory);
                tf.storage.core.inventory.SlotRange playerSlotRange = new tf.storage.core.inventory.SlotRange(9, 27);

                switch (op) {
                    case 0 -> {
                        if (shift) {
                            tf.storage.core.util.StackHelper.tryMoveAllItems(playerInv, inventory);
                        } else {
                            tf.storage.core.util.StackHelper.tryMoveAllItemsWithinSlotRange(playerInv, inventory, playerSlotRange, chestSlotRange);
                        }
                    }
                    case 1 -> {
                        if (shift) {
                            tf.storage.core.util.StackHelper.tryMoveMatchingItems(playerInv, inventory);
                        } else {
                            tf.storage.core.util.StackHelper.tryMoveMatchingItemsWithinSlotRange(playerInv, inventory, playerSlotRange, chestSlotRange);
                        }
                    }
                    case 2 -> tf.storage.core.util.StackHelper.leaveOneFullStackOfEveryItem(playerInv, inventory, true);
                    case 3 -> tf.storage.core.util.StackHelper.fillStacksOfMatchingItems(inventory, playerInv);
                    case 4 -> tf.storage.core.util.StackHelper.tryMoveMatchingItems(inventory, playerInv);
                    case 5 -> tf.storage.core.util.StackHelper.tryMoveAllItems(inventory, playerInv);
                    default -> {
                    }
                }

                setChanged();
            }
        } else if (action == GUI_ACTION_SORT_ITEMS && elementId >= 0 && elementId <= 1) {
            if (elementId == 0) {
                tf.storage.core.util.StackHelper.sortInventoryWithinRange(this.itemHandler, new tf.storage.core.inventory.SlotRange(0, this.itemHandler.getSlots()));
            } else {
                net.minecraftforge.items.IItemHandlerModifiable inv = new net.minecraftforge.items.wrapper.RangedWrapper(
                    new net.minecraftforge.items.wrapper.InvWrapper(player.getInventory()), 0, player.getInventory().items.size());
                tf.storage.core.util.StackHelper.sortInventoryWithinRange(inv, new tf.storage.core.inventory.SlotRange(9, 27));
            }
        }
    }
    
    private void updateItemHandler() {
        ItemStack card = getSelectedMemoryCard();
        this.itemHandler.setContainerItemStack(card);
    }
    
    // ==================== NBT序列化 ====================
    
    @Override
    protected void saveAdditional(CompoundTag nbt) {
        super.saveAdditional(nbt);
        
        nbt.putByte("Facing", (byte) facing.get3DDataValue());
        nbt.putByte("Tier", (byte) tier.getTierIndex());
        nbt.putByte("SelectedCard", (byte) selectedMemoryCard);
        nbt.put("MemoryCards", memoryCardHandler.serializeNBT());
        
        if (customName != null) {
            nbt.putString("CustomName", Component.Serializer.toJson(customName));
        }
    }
    
    @Override
    public void load(CompoundTag nbt) {
        super.load(nbt);
        
        if (nbt.contains("Facing")) {
            facing = Direction.from3DDataValue(nbt.getByte("Facing"));
        }
        if (nbt.contains("Tier")) {
            int index = nbt.getByte("Tier");
            this.tier = TFChestBlock.Tier.values()[Math.max(0, Math.min(index, TFChestBlock.Tier.values().length - 1))];
        } else if (nbt.contains("ChestTier")) {
            int index = nbt.getByte("ChestTier");
            this.tier = TFChestBlock.Tier.values()[Math.max(0, Math.min(index, TFChestBlock.Tier.values().length - 1))];
        } else {
            this.tier = resolveTier(getBlockState(), this.tier);
        }
        if (nbt.contains("SelectedCard")) {
            selectedMemoryCard = nbt.getByte("SelectedCard");
        } else if (nbt.contains("SelMemoryCard")) {
            selectedMemoryCard = nbt.getByte("SelMemoryCard");
        }
        if (nbt.contains("MemoryCards")) {
            memoryCardHandler.deserializeNBT(nbt.getCompound("MemoryCards"));
        }
        if (nbt.contains("CustomName")) {
            customName = Component.Serializer.fromJson(nbt.getString("CustomName"));
        }
        
        initItemHandler();
    }
    
    @Override
    public CompoundTag getUpdateTag() {
        CompoundTag nbt = super.getUpdateTag();
        nbt.putByte("Facing", (byte) facing.get3DDataValue());
        nbt.putByte("Tier", (byte) tier.getTierIndex());
        nbt.putByte("SelectedCard", (byte) selectedMemoryCard);
        return nbt;
    }
    
    @Nullable
    @Override
    public Packet<ClientGamePacketListener> getUpdatePacket() {
        return ClientboundBlockEntityDataPacket.create(this);
    }
    
    // ==================== Capability ====================
    
    @Nonnull
    @Override
    public <T> LazyOptional<T> getCapability(@Nonnull Capability<T> cap, @Nullable Direction side) {
        if (cap == ForgeCapabilities.ITEM_HANDLER) {
            return itemHandlerLazy.cast();
        }
        return super.getCapability(cap, side);
    }
    
    @Override
    public void invalidateCaps() {
        super.invalidateCaps();
        itemHandlerLazy.invalidate();
    }
    
    @Override
    public void reviveCaps() {
        super.reviveCaps();
        itemHandlerLazy = LazyOptional.of(() -> itemHandler);
    }
}
