package tf.storage.inventory.container;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.inventory.ClickType;
import net.minecraft.inventory.IContainerListener;
import net.minecraft.item.ItemStack;
import net.minecraft.util.math.MathHelper;
import tf.storage.inventory.container.base.LargeStackContainer;
import tf.storage.inventory.container.base.MergeSlotRange;
import tf.storage.inventory.container.base.SlotRange;
import tf.storage.inventory.slot.GenericSlot;
import tf.storage.inventory.slot.CardSlot;
import tf.storage.tile.TileChest;

public class ChestContainer extends LargeStackContainer
{
    protected static final int[] PLAYER_INV_Y = new int[] { 104, 140, 140, 140 };
    protected TileChest tetfc;
    protected SlotRange cardSlots;
    public int selectedMemoryCard;


    public ChestContainer(EntityPlayer player, TileChest te)
    {
        super(player, te.getWrappedInventoryForContainer(player), te);
        this.tetfc = te;

        this.addCustomInventorySlots();

        int tier = te.getStorageTier();
        int y = tier >= 0 && tier <= 3 ? PLAYER_INV_Y[tier] : 145;
        this.addPlayerInventorySlots(8, y);
    }

    @Override
    protected void addCustomInventorySlots()
    {
        int customInvStart = this.inventorySlots.size();
        int tier = MathHelper.clamp(this.tetfc.getStorageTier(), 0, 3);

        int posX = 8;
        int posY = 37;
        
        int rows = (tier == 0) ? 3 : (tier == 1) ? 5 : (tier == 2) ? 8 : 12;
        int columns = 9;

        for (int row = 0; row < rows; row++)
        {
            for (int col = 0; col < columns; col++)
            {
                this.addSlotToContainer(new GenericSlot(this.inventory, row * columns + col, posX + col * 18, posY + row * 18));
            }
        }

        this.customInventorySlots = new MergeSlotRange(customInvStart, this.inventorySlots.size() - customInvStart);

        this.addMergeSlotRangePlayerToExt(this.inventorySlots.size(), 4);
        this.cardSlots = new SlotRange(this.inventorySlots.size(), 4);

        int memCardPosX = 98;
        int memCardPosY = 8;

        for (int i = 0; i < 4; i++)
        {
            // 已恢复为标准 CardSlot，移除了带锁定检查的匿名类
            this.addSlotToContainer(new CardSlot(this.tetfc.getMemoryCardInventory(), i,
                    memCardPosX + i * 18, memCardPosY));
        }
    }

    @Override
    public void addListener(IContainerListener listener)

    {
        super.addListener(listener);

        listener.sendWindowProperty(this, 0, this.tetfc.getSelectedMemoryCardIndex());
    }


    @Override
    public void detectAndSendChanges()
    {
        if (this.tetfc.getWorld().isRemote)
        {
            return;
        }

        for (int i = 0; i < this.listeners.size(); ++i)
        {
            IContainerListener listener = this.listeners.get(i);

            if (this.selectedMemoryCard != this.tetfc.getSelectedMemoryCardIndex())
            {
                listener.sendWindowProperty(this, 0, this.tetfc.getSelectedMemoryCardIndex());
            }
        }

        this.selectedMemoryCard = this.tetfc.getSelectedMemoryCardIndex();

        super.detectAndSendChanges();
    }


    @Override
    public void updateProgressBar(int var, int val)
    {
        super.updateProgressBar(var, val);

        switch (var)
        {
            case 0:
                this.tetfc.setSelectedMemoryCard(val);
                break;
            default:

        }
    }

    @Override
    public ItemStack slotClick(int slotNum, int dragType, ClickType clickType, EntityPlayer player)
    {
        return super.slotClick(slotNum, dragType, clickType, player);
    }

}
