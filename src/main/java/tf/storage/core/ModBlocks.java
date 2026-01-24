package tf.storage.core;

import net.minecraft.block.Block;
import net.minecraft.block.material.Material;
import net.minecraft.tileentity.TileEntity;
import net.minecraftforge.event.RegistryEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.registry.GameRegistry;
import net.minecraftforge.registries.IForgeRegistry;
import tf.storage.block.TFChestBlock;
import tf.storage.tile.TileChest;

/**
 * ModBlocks - 方块注册
 */
@Mod.EventBusSubscriber(modid = ModInfo.MOD_ID)
public class ModBlocks
{
    public static final TFChestBlock TF_CHEST = new TFChestBlock(ModInfo.NAME_BLOCK_TF_CHEST, 6.0f, 60f, 1, Material.ROCK);

    @SubscribeEvent
    public static void registerBlocks(RegistryEvent.Register<Block> event)
    {
        IForgeRegistry<Block> registry = event.getRegistry();

        // 注册方块
        registerBlock(registry, TF_CHEST);
        registerTileEntities();
    }

    private static void registerTileEntities()
    {
        registerTileEntity(TileChest.class, ModInfo.NAME_TILE_ENTITY_TF_CHEST);
    }

    private static void registerTileEntity(Class<? extends TileEntity> clazz, String id)
    {
        GameRegistry.registerTileEntity(clazz, id);
    }

    private static void registerBlock(IForgeRegistry<Block> registry, TFChestBlock block)
    {
        block.setRegistryName(ModInfo.MOD_ID + ":" + block.getBlockName());
        registry.register(block);
    }
}
