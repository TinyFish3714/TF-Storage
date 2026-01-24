package tf.storage.inventory.handler;

import tf.storage.tile.TileChest;

public class TileHandler extends BasicHandler
{
    protected final TileChest te;
    protected final int inventoryId;

    public TileHandler(int invSize, TileChest te)
    {
        this(0, invSize, te);
    }

    public TileHandler(int inventoryId, int invSize, TileChest te)
    {
        super(invSize);
        this.te = te;
        this.inventoryId = inventoryId;
    }

    public TileHandler(int inventoryId, int invSize, int stackLimit, boolean allowCustomStackSizes, String tagName, TileChest te)
    {
        super(invSize, stackLimit, allowCustomStackSizes, tagName);
        this.te = te;
        this.inventoryId = inventoryId;
    }

    @Override
    public void onContentsChanged(int slot)
    {
        super.onContentsChanged(slot);

        this.te.inventoryChanged(this.inventoryId, slot);
        this.te.markDirty();
        
        if (this.te.getWorld() != null && !this.te.getWorld().isRemote)
        {
            net.minecraft.world.World world = this.te.getWorld();
            net.minecraft.util.math.BlockPos pos = this.te.getPos();
            net.minecraft.block.state.IBlockState state = world.getBlockState(pos);
            world.updateComparatorOutputLevel(pos, state.getBlock());
        }
    }

    public int getInventoryId()
    {
        return this.inventoryId;
    }
}
