package tf.storage.core;

import net.minecraft.item.Item;
import net.minecraftforge.event.RegistryEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.registries.IForgeRegistry;
import tf.storage.item.TFBag;
import tf.storage.item.base.BaseItem;
import tf.storage.item.TFUnit;

/**
 * ModItems - 物品注册
 */
@Mod.EventBusSubscriber(modid = ModInfo.MOD_ID)
public class ModItems
{
    public static final BaseItem MEMORY_CARD = new TFUnit(ModInfo.NAME_ITEM_MEMORY_CARD);
    public static final BaseItem TF_BAG = new TFBag(ModInfo.NAME_ITEM_TF_BAG);
    public static final BaseItem SYCORE = new BaseItem(ModInfo.NAME_ITEM_SYCORE);
    public static final BaseItem EDCORE = new BaseItem(ModInfo.NAME_ITEM_EDCORE);
    public static final BaseItem TFCORE = new BaseItem(ModInfo.NAME_ITEM_TFCORE);

    @SubscribeEvent
    public static void registerItems(RegistryEvent.Register<Item> event)
    {
        IForgeRegistry<Item> registry = event.getRegistry();

        registerItem(registry, MEMORY_CARD);
        registerItem(registry, TF_BAG);
        registerItem(registry, SYCORE);
        registerItem(registry, EDCORE);
        registerItem(registry, TFCORE);
        
        // 注册方块物品
        registerItemBlock(registry, ModBlocks.TF_CHEST, true);
    }

    private static void registerItem(IForgeRegistry<Item> registry, BaseItem item)
    {
        item.setRegistryName(ModInfo.MOD_ID + ":" + item.getBaseName());
        registry.register(item);
    }
    
    private static void registerItemBlock(IForgeRegistry<Item> registry, tf.storage.block.TFChestBlock block, boolean hasSubtypes)
    {
        Item item = block.createItemBlock().setRegistryName(ModInfo.MOD_ID, block.getBlockName()).setHasSubtypes(hasSubtypes);
        registry.register(item);
    }
}
