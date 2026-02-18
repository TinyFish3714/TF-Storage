package tf.storage.platform.forge.menu;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.inventory.ContainerLevelAccess;
import net.minecraft.world.inventory.SimpleContainerData;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.items.IItemHandler;
import tf.storage.core.inventory.MergeSlotRange;
import tf.storage.platform.forge.ModRegistry;
import tf.storage.platform.forge.block.TFChestBlock;
import tf.storage.platform.forge.block.TFChestBlockEntity;
import tf.storage.platform.forge.inventory.slot.CardSlot;
import tf.storage.platform.forge.inventory.slot.GenericSlot;

/**
 * Menu for TF Chest block.
 */
public class ChestMenu extends BaseMenu {
    
    protected static final int[] PLAYER_INV_Y = new int[] { 104, 140, 140, 140 };
    
    private final TFChestBlockEntity blockEntity;
    private final ContainerLevelAccess access;
    private final ContainerData data;
    private int syncedTier = -1;
    
    public int selectedMemoryCard;

    // Server constructor
    public ChestMenu(int containerId, Inventory playerInventory, TFChestBlockEntity blockEntity) {
        super(ModRegistry.TF_CHEST_MENU.get(), containerId, playerInventory.player, blockEntity.getBaseItemHandler());
        this.blockEntity = blockEntity;
        this.access = ContainerLevelAccess.create(blockEntity.getLevel(), blockEntity.getBlockPos());
        this.data = new SimpleContainerData(2);
        this.syncedTier = -1;
        
        this.addCustomInventorySlots();
        
        int tier = blockEntity.getTier().getTierIndex();
        int y = tier >= 0 && tier <= 3 ? PLAYER_INV_Y[tier] : 145;
        this.addPlayerInventorySlots(8, y);
        
        this.addDataSlots(this.data);
    }

    // Client constructor
    public ChestMenu(int containerId, Inventory playerInventory, FriendlyByteBuf buf) {
        this(containerId, playerInventory, getBlockEntity(playerInventory, buf));
    }
    
    private static TFChestBlockEntity getBlockEntity(Inventory playerInventory, FriendlyByteBuf buf) {
        var pos = buf.readBlockPos();
        var level = playerInventory.player.level();
        var be = level.getBlockEntity(pos);
        if (be instanceof TFChestBlockEntity tfChest) {
            return tfChest;
        }
        throw new IllegalStateException("Block entity at " + pos + " is not a TFChestBlockEntity");
    }

    @Override
    protected void addCustomInventorySlots() {
        int customInvStart = this.slots.size();
        int tier = this.blockEntity.getTier().getTierIndex();

        int posX = 8;
        int posY = 37;
        
        int rows = switch (tier) {
            case 0 -> 3;
            case 1 -> 5;
            case 2 -> 8;
            default -> 12;
        };
        int columns = 9;

        // Main inventory slots
        IItemHandler mainInv = this.blockEntity.getItemHandler();
        
        // Define total rows based on tier (real rows in inventory)
        // Tier 0: 3 rows (27)
        // Tier 1: 5 rows (45)
        // Tier 2: 8 rows (72)
        // Tier 3: 12 rows (108)
        int realRows = switch (tier) {
            case 0 -> 3;
            case 1 -> 5;
            case 2 -> 8;
            default -> 12;
        };
        
        int slotIndex = 0;
        for (int row = 0; row < realRows; row++) {
            for (int col = 0; col < columns; col++) {
                // Initial Y position is relative to scroll; client updates it
                // For server sync, we just need valid slots
                this.addSlot(new GenericSlot(mainInv, slotIndex++, posX + col * 18, posY + row * 18));
            }
        }

        this.customInventorySlots = new MergeSlotRange(customInvStart, this.slots.size() - customInvStart);

        // Memory card slots
        this.addMergeSlotRangePlayerToExt(this.slots.size(), 4);

        int memCardPosX = 98;
        int memCardPosY = 8;

        IItemHandler memoryCardInv = this.blockEntity.getMemoryCardInventory();
        for (int i = 0; i < 4; i++) {
            this.addSlot(new CardSlot(memoryCardInv, i, memCardPosX + i * 18, memCardPosY));
        }
    }

    @Override
    public void broadcastChanges() {
        if (!this.player.level().isClientSide) {
            int currentSelection = this.blockEntity.getSelectedMemoryCardIndex();
            if (this.selectedMemoryCard != currentSelection) {
                this.data.set(0, currentSelection);
                this.selectedMemoryCard = currentSelection;
            }
            int tier = this.blockEntity.getTier().getTierIndex();
            if (this.syncedTier != tier) {
                this.data.set(1, tier);
                this.syncedTier = tier;
            }
        }
        super.broadcastChanges();
    }

    @Override
    public void setData(int id, int value) {
        super.setData(id, value);
        if (id == 0) {
            this.blockEntity.setSelectedMemoryCard(value);
        } else if (id == 1) {
            this.syncedTier = value;
        }
    }

    public void setSelectedMemoryCard(int index) {
        this.blockEntity.setSelectedMemoryCard(index);
    }

    public int getChestTier() {
        return this.syncedTier >= 0 ? this.syncedTier : this.blockEntity.getTier().getTierIndex();
    }

    @Override
    public boolean stillValid(Player player) {
        return this.blockEntity.isUseableByPlayer(player);
    }
    
    public TFChestBlockEntity getBlockEntity() {
        return this.blockEntity;
    }

    public IItemHandler getInventory() {
        return this.blockEntity.getItemHandler();
    }
    
    public int getTier() {
        return this.blockEntity.getTier().getTierIndex();
    }
}
