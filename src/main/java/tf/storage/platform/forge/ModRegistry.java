package tf.storage.platform.forge;

import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraftforge.common.extensions.IForgeMenuType;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;
import tf.storage.platform.forge.crafting.PreserveNbtRecipe;
import tf.storage.platform.forge.crafting.PreserveNbtRecipeSerializer;
import tf.storage.platform.forge.item.TFUnitItem;
import tf.storage.platform.forge.item.TFBagItem;
import tf.storage.platform.forge.item.CoreItem;
import tf.storage.platform.forge.block.TFChestBlock;
import tf.storage.platform.forge.block.TFChestBlockEntity;
import tf.storage.platform.forge.menu.BagMenu;
import tf.storage.platform.forge.menu.ChestMenu;

/**
 * ModRegistry - 统一的注册管理器
 * 使用DeferredRegister进行所有游戏对象的注册
 */
public class ModRegistry {
    
    // Deferred Registers
    public static final DeferredRegister<Item> ITEMS = DeferredRegister.create(ForgeRegistries.ITEMS, TFStorageMod.MOD_ID);
    public static final DeferredRegister<Block> BLOCKS = DeferredRegister.create(ForgeRegistries.BLOCKS, TFStorageMod.MOD_ID);
    public static final DeferredRegister<BlockEntityType<?>> BLOCK_ENTITIES = DeferredRegister.create(ForgeRegistries.BLOCK_ENTITY_TYPES, TFStorageMod.MOD_ID);
    public static final DeferredRegister<MenuType<?>> MENUS = DeferredRegister.create(ForgeRegistries.MENU_TYPES, TFStorageMod.MOD_ID);
    public static final DeferredRegister<RecipeSerializer<?>> RECIPE_SERIALIZERS = DeferredRegister.create(ForgeRegistries.RECIPE_SERIALIZERS, TFStorageMod.MOD_ID);
    public static final DeferredRegister<CreativeModeTab> CREATIVE_TABS = DeferredRegister.create(Registries.CREATIVE_MODE_TAB, TFStorageMod.MOD_ID);
    
    // ==================== Items ====================
    
    // TF单元 (存储卡) - 4种容量等级
    public static final RegistryObject<Item> TF_UNIT_6B = ITEMS.register("tfunit_6b", 
        () -> new TFUnitItem(TFUnitItem.Tier.TIER_6B, new Item.Properties().stacksTo(64)));
    public static final RegistryObject<Item> TF_UNIT_8B = ITEMS.register("tfunit_8b", 
        () -> new TFUnitItem(TFUnitItem.Tier.TIER_8B, new Item.Properties().stacksTo(64)));
    public static final RegistryObject<Item> TF_UNIT_10B = ITEMS.register("tfunit_10b", 
        () -> new TFUnitItem(TFUnitItem.Tier.TIER_10B, new Item.Properties().stacksTo(64)));
    public static final RegistryObject<Item> TF_UNIT_12B = ITEMS.register("tfunit_12b", 
        () -> new TFUnitItem(TFUnitItem.Tier.TIER_12B, new Item.Properties().stacksTo(64)));
    
    // TF包 - 2种等级
    public static final RegistryObject<Item> TF_BAG = ITEMS.register("tfbag", 
        () -> new TFBagItem(TFBagItem.Tier.TIER_1, new Item.Properties().stacksTo(1)));
    public static final RegistryObject<Item> TF_BAG_LARGE = ITEMS.register("tfbag_large", 
        () -> new TFBagItem(TFBagItem.Tier.TIER_2, new Item.Properties().stacksTo(1)));
    
    // 核心材料物品
    public static final RegistryObject<Item> SYCORE = ITEMS.register("sycore", 
        () -> new CoreItem(new Item.Properties()));
    public static final RegistryObject<Item> EDCORE = ITEMS.register("edcore", 
        () -> new CoreItem(new Item.Properties()));
    public static final RegistryObject<Item> TFCORE = ITEMS.register("tfcore", 
        () -> new CoreItem(new Item.Properties()));
    
    // ==================== Blocks ====================
    
    // TF箱子 - 4种等级
    public static final RegistryObject<Block> TF_CHEST_0 = BLOCKS.register("tf_chest_0",
        () -> new TFChestBlock(TFChestBlock.Tier.TIER_0));
    public static final RegistryObject<Block> TF_CHEST_1 = BLOCKS.register("tf_chest_1",
        () -> new TFChestBlock(TFChestBlock.Tier.TIER_1));
    public static final RegistryObject<Block> TF_CHEST_2 = BLOCKS.register("tf_chest_2",
        () -> new TFChestBlock(TFChestBlock.Tier.TIER_2));
    public static final RegistryObject<Block> TF_CHEST_3 = BLOCKS.register("tf_chest_3",
        () -> new TFChestBlock(TFChestBlock.Tier.TIER_3));
    
    // TF箱子方块物品
    public static final RegistryObject<Item> TF_CHEST_0_ITEM = ITEMS.register("tf_chest_0",
        () -> new net.minecraft.world.item.BlockItem(TF_CHEST_0.get(), new Item.Properties()));
    public static final RegistryObject<Item> TF_CHEST_1_ITEM = ITEMS.register("tf_chest_1",
        () -> new net.minecraft.world.item.BlockItem(TF_CHEST_1.get(), new Item.Properties()));
    public static final RegistryObject<Item> TF_CHEST_2_ITEM = ITEMS.register("tf_chest_2",
        () -> new net.minecraft.world.item.BlockItem(TF_CHEST_2.get(), new Item.Properties()));
    public static final RegistryObject<Item> TF_CHEST_3_ITEM = ITEMS.register("tf_chest_3",
        () -> new net.minecraft.world.item.BlockItem(TF_CHEST_3.get(), new Item.Properties()));
    
    // ==================== Block Entities ====================
    
    public static final RegistryObject<BlockEntityType<TFChestBlockEntity>> TF_CHEST_BE = BLOCK_ENTITIES.register("tf_chest",
        () -> BlockEntityType.Builder.of(TFChestBlockEntity::new, 
            TF_CHEST_0.get(), TF_CHEST_1.get(), TF_CHEST_2.get(), TF_CHEST_3.get()).build(null));
    
    // ==================== Menus ====================
    
    public static final RegistryObject<MenuType<ChestMenu>> TF_CHEST_MENU = MENUS.register("tf_chest",
        () -> IForgeMenuType.create(ChestMenu::new));

    public static final RegistryObject<MenuType<BagMenu>> TF_BAG_MENU = MENUS.register("tf_bag",
        () -> IForgeMenuType.create(BagMenu::new));

    // ==================== Recipes ====================

    public static final RegistryObject<RecipeSerializer<PreserveNbtRecipe>> PRESERVE_NBT_RECIPE = RECIPE_SERIALIZERS.register("preserve_nbt",
        PreserveNbtRecipeSerializer::new);
    
    // ==================== Creative Tab ====================
    
    public static final RegistryObject<CreativeModeTab> TF_STORAGE_TAB = CREATIVE_TABS.register("tfstorage_tab",
        () -> CreativeModeTab.builder()
            .title(Component.translatable("itemGroup.tfstorage"))
            .icon(() -> new ItemStack(TF_BAG.get()))
            .displayItems((params, output) -> {
                // TF单元
                output.accept(TF_UNIT_6B.get());
                output.accept(TF_UNIT_8B.get());
                output.accept(TF_UNIT_10B.get());
                output.accept(TF_UNIT_12B.get());
                // TF包
                output.accept(TF_BAG.get());
                output.accept(TF_BAG_LARGE.get());
                // TF箱子
                output.accept(TF_CHEST_0_ITEM.get());
                output.accept(TF_CHEST_1_ITEM.get());
                output.accept(TF_CHEST_2_ITEM.get());
                output.accept(TF_CHEST_3_ITEM.get());
                // 核心材料
                output.accept(SYCORE.get());
                output.accept(EDCORE.get());
                output.accept(TFCORE.get());
            })
            .build());
    
    /**
     * 注册所有内容到事件总线
     */
    public static void register(IEventBus modEventBus) {
        ITEMS.register(modEventBus);
        BLOCKS.register(modEventBus);
        BLOCK_ENTITIES.register(modEventBus);
        MENUS.register(modEventBus);
        RECIPE_SERIALIZERS.register(modEventBus);
        CREATIVE_TABS.register(modEventBus);
        
        TFStorageMod.LOGGER.info("TF Storage registries initialized");
    }
}
