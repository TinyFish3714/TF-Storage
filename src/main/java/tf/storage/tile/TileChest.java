package tf.storage.tile;

import java.util.UUID;
import javax.annotation.Nullable;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.network.NetworkManager;
import net.minecraft.network.play.server.SPacketUpdateTileEntity;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.MathHelper;
import net.minecraft.world.World;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.util.Constants;
import net.minecraftforge.items.CapabilityItemHandler;
import net.minecraftforge.items.IItemHandler;
import tf.storage.gui.client.ChestGui;
import tf.storage.inventory.IMemoryCardHolder;
import tf.storage.inventory.handler.BasicHandler;
import tf.storage.inventory.handler.TileHandler;
import tf.storage.inventory.container.ChestContainer;
import tf.storage.core.ModInfo;
import tf.storage.util.CardHelper;

public class TileChest extends TileEntity implements IMemoryCardHolder
{
    public static final int GUI_ACTION_SELECT_MEMORY_CARD = 0;
    public static final int GUI_ACTION_MOVE_ITEMS         = 2;

    public static final int GUI_ACTION_SORT_ITEMS         = 3;

    public static final int INV_ID_MEMORY_CARDS = 0;
    public static final int MAX_TIER            = 3;
    public static final int[] INV_SIZES = new int[] { 27, 45, 72, 108 };
    public static final int MAX_MEMORY_CARDS = 4;

    protected String tileEntityName;
    protected EnumFacing facing;
    
    protected TileHandler itemHandlerBase;
    protected IItemHandler itemHandlerExternal;
    protected String customInventoryName;
    protected boolean useWrapperHandlerForContainerExtract;
    
    private final IItemHandler itemHandlerMemoryCards;
    protected InventoryItemCallback itemInventory;
    protected int selectedMemoryCard;
    protected int chestTier;
    protected int invSize;


    public TileChest()
    {
        this.facing = EnumFacing.NORTH;
        this.tileEntityName = ModInfo.NAME_TILE_ENTITY_TF_CHEST;
        this.itemHandlerBase = new TileHandler(INV_ID_MEMORY_CARDS, MAX_MEMORY_CARDS, 1, false, "Items", this);
        this.itemHandlerMemoryCards = this.getBaseItemHandler();
        this.selectedMemoryCard = 0;
    }


    public String getTEName()
    {
        return this.tileEntityName;
    }

    public void setFacing(EnumFacing facing)
    {
        this.facing = facing;
        this.markDirty();

        if (this.getWorld() != null && this.getWorld().isRemote == false)
        {
            this.notifyBlockUpdate(this.getPos());
        }
    }

    public EnumFacing getFacing()
    {
        return this.facing;
    }

    @Override
    public void mirror(net.minecraft.util.Mirror mirrorIn)
    {
        this.rotate(mirrorIn.toRotation(this.facing));
    }

    @Override
    public void rotate(net.minecraft.util.Rotation rotationIn)
    {
        this.setFacing(rotationIn.rotate(this.getFacing()));
    }

    public boolean isUseableByPlayer(EntityPlayer player)
    {
        return this.getWorld().getTileEntity(this.getPos()) == this &&
               player.getDistanceSq(this.getPos().getX() + 0.5D, this.getPos().getY() + 0.5D, this.getPos().getZ() + 0.5D) <= 64.0D;
    }

    public boolean isMovableBy(EntityPlayer player)
    {
        // 已移除锁定存储卡检查
        return true;
    }

    protected void notifyBlockUpdate(BlockPos pos)
    {
        net.minecraft.block.state.IBlockState state = this.getWorld().getBlockState(pos);
        this.getWorld().notifyBlockUpdate(pos, state, state, 3);
    }

    public void setInventoryName(String name)
    {
        this.customInventoryName = name;
    }

    public boolean hasCustomName()
    {
        return this.customInventoryName != null && this.customInventoryName.length() > 0;
    }

    public String getName()
    {
        return this.hasCustomName() ? this.customInventoryName : ModInfo.MOD_ID + ".container." + this.tileEntityName;
    }

    public BasicHandler getBaseItemHandler()
    {
        return this.itemHandlerBase;
    }

    public IItemHandler getWrappedInventoryForContainer(EntityPlayer player)
    {
        if (this.itemInventory == null)
        {
            this.initStorage(INV_SIZES[this.chestTier], this.getWorld() != null && this.getWorld().isRemote);
        }
        return this.itemInventory;
    }

    protected void readItemsFromNBT(NBTTagCompound nbt)
    {
        this.getBaseItemHandler().deserializeNBT(nbt);
    }

    public void writeItemsToNBT(NBTTagCompound nbt)
    {
        nbt.merge(this.getBaseItemHandler().serializeNBT());
    }

    private void initStorage(int invSize, boolean isRemote)
    {
        this.itemInventory = new InventoryItemCallback(null, invSize, true, isRemote, this);
        this.itemInventory.setContainerItemStack(this.getContainerStack());
        this.itemHandlerExternal = this.itemInventory;
    }

    @Override
    public void readFromNBT(NBTTagCompound nbt)
    {
        super.readFromNBT(nbt);

        if (nbt.hasKey("Rotation", Constants.NBT.TAG_BYTE))
        {
            this.facing = EnumFacing.byIndex(nbt.getByte("Rotation"));
        }

        if (nbt.hasKey("CustomName", Constants.NBT.TAG_STRING))
        {
            this.customInventoryName = nbt.getString("CustomName");
        }

        this.readItemsFromNBT(nbt);

        this.chestTier = MathHelper.clamp(nbt.getByte("ChestTier"), 0, MAX_TIER);
        this.setSelectedMemoryCard(nbt.getByte("SelMemoryCard"));
        this.invSize = INV_SIZES[this.chestTier];

    }

    @Override
    public NBTTagCompound writeToNBT(NBTTagCompound nbt)
    {
        nbt = super.writeToNBT(nbt);

        nbt.setByte("Rotation", (byte) this.facing.getIndex());

        this.writeItemsToNBT(nbt);

        if (this.hasCustomName())
        {
            nbt.setString("CustomName", this.customInventoryName);
        }

        nbt.setByte("ChestTier", (byte)this.chestTier);
        nbt.setByte("SelMemoryCard", (byte)this.selectedMemoryCard);

        return nbt;
    }

    public NBTTagCompound getUpdatePacketTag(NBTTagCompound nbt)
    {
        nbt.setByte("r", (byte)(this.facing.getIndex() & 0x07));

        nbt.setByte("tier", (byte)this.chestTier);
        nbt.setByte("msel", (byte)this.selectedMemoryCard);

        return nbt;
    }

    @Override
    public NBTTagCompound getUpdateTag()
    {
        NBTTagCompound nbt = new NBTTagCompound();
        nbt.setInteger("x", this.getPos().getX());
        nbt.setInteger("y", this.getPos().getY());
        nbt.setInteger("z", this.getPos().getZ());

        return this.getUpdatePacketTag(nbt);
    }

    @Override
    @Nullable
    public SPacketUpdateTileEntity getUpdatePacket()
    {
        if (this.getWorld() != null)
        {
            return new SPacketUpdateTileEntity(this.getPos(), 0, this.getUpdatePacketTag(new NBTTagCompound()));
        }

        return null;
    }

    @Override
    public void handleUpdateTag(NBTTagCompound tag)
    {
        EnumFacing oldFacing = this.facing;
        
        if (tag.hasKey("r"))
        {
            this.setFacing(EnumFacing.byIndex((byte)(tag.getByte("r") & 0x07)));
        }

        this.chestTier = tag.getByte("tier");
        this.selectedMemoryCard = tag.getByte("msel");
        this.invSize = INV_SIZES[this.chestTier];

        this.initStorage(this.invSize, this.getWorld().isRemote);

        if (oldFacing != this.facing)
        {
            this.getWorld().checkLightFor(net.minecraft.world.EnumSkyBlock.BLOCK, this.getPos());
            this.notifyBlockUpdate(this.getPos());
        }
        else
        {
            this.getWorld().markBlockRangeForRenderUpdate(this.getPos(), this.getPos());
        }
    }

    @Override
    public void onDataPacket(NetworkManager net, SPacketUpdateTileEntity packet)
    {
        this.handleUpdateTag(packet.getNbtCompound());
    }

    @Override
    public String toString()
    {
        return this.getClass().getSimpleName() + "(" + this.getPos() + ")@" + System.identityHashCode(this);
    }

    public boolean hasGui()
    {
        return true;
    }

    @Override
    public boolean hasCapability(Capability<?> capability, EnumFacing facing)
    {
        if (capability == CapabilityItemHandler.ITEM_HANDLER_CAPABILITY)
        {
            return this.itemHandlerExternal != null;
        }

        return super.hasCapability(capability, facing);
    }

    @Override
    public <T> T getCapability(Capability<T> capability, EnumFacing facing)
    {
        if (capability == CapabilityItemHandler.ITEM_HANDLER_CAPABILITY)
        {
            return CapabilityItemHandler.ITEM_HANDLER_CAPABILITY.cast(this.itemHandlerExternal);
        }

        return super.getCapability(capability, facing);
    }

    public boolean isInventoryAccessible(EntityPlayer player)
    {
        return !getSelectedMemoryCard().isEmpty();
    }

    public IItemHandler getMemoryCardInventory()
    {
        return this.itemHandlerMemoryCards;
    }

    public int getSelectedMemoryCardIndex()
    {
        return this.selectedMemoryCard;
    }

    public void setSelectedMemoryCard(int index)
    {
        this.selectedMemoryCard = MathHelper.clamp(index, 0, this.itemHandlerMemoryCards.getSlots() - 1);
    }

    public int getMaxMemoryCards()
    {
        return MAX_MEMORY_CARDS;
    }

    public ItemStack getSelectedMemoryCard()
    {
        return this.itemHandlerMemoryCards.getStackInSlot(this.selectedMemoryCard);
    }

    public ItemStack getContainerStack()
    {
        return this.getSelectedMemoryCard();
    }

    public void inventoryChanged(int inventoryId, int slot)
    {
        this.itemInventory.setContainerItemStack(this.getContainerStack());
    }

    public int getStorageTier()
    {
        return this.chestTier;
    }

    public void setStorageTier(World world, int tier)
    {
        this.chestTier = MathHelper.clamp(tier, 0, MAX_TIER);
        this.invSize = INV_SIZES[this.chestTier];

        this.initStorage(this.invSize, world.isRemote);
    }

    public void onLoad()
    {
        super.onLoad();

        this.initStorage(this.invSize, this.getWorld().isRemote);
    }
    
    public void performGuiAction(EntityPlayer player, int action, int element)
    {
        if (action == GUI_ACTION_SELECT_MEMORY_CARD && element >= 0 && element < MAX_MEMORY_CARDS)
        {
            this.setSelectedMemoryCard(element);
            this.inventoryChanged(0, element);
            this.markDirty();
        }
        else if (action == GUI_ACTION_MOVE_ITEMS)

        {
            int op = element & 0x7FFF;
            boolean shift = (element & 0x8000) != 0;

            if (op >= 0 && op < 6)
            {
                IItemHandler inventory = this.getWrappedInventoryForContainer(player);

                net.minecraftforge.items.IItemHandlerModifiable playerMainInv = new net.minecraftforge.items.wrapper.PlayerMainInvWrapper(player.inventory);
                net.minecraftforge.items.IItemHandlerModifiable offhandInv = new net.minecraftforge.items.wrapper.PlayerOffhandInvWrapper(player.inventory);
                net.minecraftforge.items.IItemHandler playerInv = new net.minecraftforge.items.wrapper.CombinedInvWrapper(playerMainInv, offhandInv);

                tf.storage.inventory.container.base.SlotRange chestSlotRange = new tf.storage.inventory.container.base.SlotRange(inventory);
                tf.storage.inventory.container.base.SlotRange playerSlotRange = new tf.storage.inventory.container.base.SlotRange(9, 27);

                switch (op)
                {
                    case 0:
                        if (shift)
                            tf.storage.util.StackHelper.tryMoveAllItems(playerInv, inventory);
                        else
                            tf.storage.util.StackHelper.tryMoveAllItemsWithinSlotRange(playerInv, inventory, playerSlotRange, chestSlotRange);
                        break;
                    case 1:
                        if (shift)
                            tf.storage.util.StackHelper.tryMoveMatchingItems(playerInv, inventory);
                        else
                            tf.storage.util.StackHelper.tryMoveMatchingItemsWithinSlotRange(playerInv, inventory, playerSlotRange, chestSlotRange);
                        break;
                    case 2:
                        tf.storage.util.StackHelper.leaveOneFullStackOfEveryItem(playerInv, inventory, true);
                        break;
                    case 3:
                        tf.storage.util.StackHelper.fillStacksOfMatchingItems(inventory, playerInv);
                        break;
                    case 4:
                        tf.storage.util.StackHelper.tryMoveMatchingItems(inventory, playerInv);
                        break;
                    case 5:
                        tf.storage.util.StackHelper.tryMoveAllItems(inventory, playerInv);
                        break;
                    default:
                        break;
                }

                this.markDirty();
            }
        }
        else if (action == GUI_ACTION_SORT_ITEMS && element >= 0 && element <= 1)
        {
            if (element == 0)
            {
                IItemHandler inventory = this.getWrappedInventoryForContainer(player);
                if (inventory instanceof net.minecraftforge.items.IItemHandlerModifiable) {
                    tf.storage.util.StackHelper.sortInventoryWithinRange((net.minecraftforge.items.IItemHandlerModifiable)inventory, new tf.storage.inventory.container.base.SlotRange(0, inventory.getSlots()));
                }
            }
            else
            {
                net.minecraftforge.items.IItemHandlerModifiable inv = (net.minecraftforge.items.IItemHandlerModifiable) player.getCapability(CapabilityItemHandler.ITEM_HANDLER_CAPABILITY, EnumFacing.UP);
                tf.storage.util.StackHelper.sortInventoryWithinRange(inv, new tf.storage.inventory.container.base.SlotRange(9, 27));
            }
        }
    }

    public ChestContainer getContainer(EntityPlayer player)
    {
        return new ChestContainer(player, this);
    }

    public Object getGui(EntityPlayer player)
    {
        return new ChestGui(this.getContainer(player), this);
    }

    private static class InventoryItemCallback extends tf.storage.inventory.handler.ItemHandler
    {
        private final IMemoryCardHolder callback;

        public InventoryItemCallback(ItemStack containerStack, int invSize, boolean allowCustomStackSizes,
                boolean isRemote, IMemoryCardHolder callback)
        {
            this(containerStack, invSize, 64, allowCustomStackSizes, isRemote, callback, "Items");
        }

        public InventoryItemCallback(ItemStack containerStack, int invSize, int maxStackSize, boolean allowCustomStackSizes,
                boolean isRemote, IMemoryCardHolder callback, String tagName)
        {
            super(containerStack, invSize, maxStackSize, allowCustomStackSizes, tagName);
            this.callback = callback;
        }

        @Override
        public ItemStack getContainerItemStack()
        {
            if (this.callback != null)
            {
                return this.callback.getContainerStack();
            }

            return super.getContainerItemStack();
        }

        @Override
        public void onContentsChanged(int slot)
        {
            super.onContentsChanged(slot);

            if (this.callback instanceof net.minecraft.tileentity.TileEntity)
            {
                ((net.minecraft.tileentity.TileEntity) this.callback).markDirty();
            }
        }
    }
}
