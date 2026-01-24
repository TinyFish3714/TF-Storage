package tf.storage.util;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import net.minecraft.init.Items;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;
import net.minecraft.nbt.NBTTagString;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.Mirror;
import net.minecraft.util.Rotation;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.ChunkCache;
import net.minecraft.world.IBlockAccess;
import net.minecraft.world.World;
import net.minecraft.world.chunk.Chunk;
import net.minecraftforge.common.util.Constants;

public class WorldHelper
{

    /**
     * 获取 TileEntity 的安全方法，适用于 ChunkCache (渲染线程安全)
     */
    @Nullable
    public static <T extends TileEntity> T getTileEntitySafely(IBlockAccess world, BlockPos pos, Class<T> tileClass)
    {
        TileEntity te;
        if (world instanceof ChunkCache)
        {
            ChunkCache chunkCache = (ChunkCache) world;
            te = chunkCache.getTileEntity(pos, Chunk.EnumCreateEntityType.CHECK);
        }
        else
        {
            te = world.getTileEntity(pos);
        }

        if (tileClass.isInstance(te))
        {
            return tileClass.cast(te);
        }
        else
        {
            return null;
        }
    }

}
