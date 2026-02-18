package tf.storage.platform.forge.inventory.handler;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import tf.storage.platform.forge.block.TFChestBlockEntity;

/**
 * Item handler for TFChest block entity.
 * Notifies the block entity when contents change.
 */
public class TileHandler extends BasicHandler {
    protected final TFChestBlockEntity blockEntity;
    protected final int inventoryId;

    public TileHandler(int invSize, TFChestBlockEntity blockEntity) {
        this(0, invSize, blockEntity);
    }

    public TileHandler(int inventoryId, int invSize, TFChestBlockEntity blockEntity) {
        super(invSize);
        this.blockEntity = blockEntity;
        this.inventoryId = inventoryId;
    }

    public TileHandler(int inventoryId, int invSize, int stackLimit, boolean allowCustomStackSizes, String tagName, TFChestBlockEntity blockEntity) {
        super(invSize, stackLimit, allowCustomStackSizes, tagName);
        this.blockEntity = blockEntity;
        this.inventoryId = inventoryId;
    }

    @Override
    public void onContentsChanged(int slot) {
        super.onContentsChanged(slot);

        this.blockEntity.inventoryChanged(this.inventoryId, slot);
        this.blockEntity.setChanged();

        Level level = this.blockEntity.getLevel();
        if (level != null && !level.isClientSide) {
            BlockPos pos = this.blockEntity.getBlockPos();
            BlockState state = level.getBlockState(pos);
            level.updateNeighbourForOutputSignal(pos, state.getBlock());
        }
    }

    public int getInventoryId() {
        return this.inventoryId;
    }
}
