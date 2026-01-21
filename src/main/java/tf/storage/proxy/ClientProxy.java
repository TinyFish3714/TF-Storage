package tf.storage.proxy;

import net.minecraft.client.Minecraft;
import net.minecraft.client.audio.ISound.AttenuationType;
import net.minecraft.client.audio.PositionedSoundRecord;
import net.minecraft.client.audio.SoundHandler;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.client.renderer.block.model.ModelResourceLocation;
import net.minecraft.client.resources.I18n;
import net.minecraft.client.settings.KeyBinding;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.util.NonNullList;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.SoundCategory;
import net.minecraft.util.SoundEvent;
import net.minecraftforge.client.event.ModelRegistryEvent;
import net.minecraftforge.client.model.ModelLoader;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.fml.client.FMLClientHandler;
import net.minecraftforge.fml.client.registry.ClientRegistry;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.network.simpleimpl.MessageContext;
import net.minecraftforge.fml.relauncher.Side;
import tf.storage.TFStorage;
import tf.storage.block.TFChestBlock;
import tf.storage.config.ModConfig;
import tf.storage.event.GuiEvents;
import tf.storage.event.InputEvents;
import tf.storage.item.base.BaseItem;
import tf.storage.core.ModInfo;
import tf.storage.core.ModBlocks;
import tf.storage.core.ModItems;

@Mod.EventBusSubscriber(Side.CLIENT)
public class ClientProxy extends CommonProxy
{
    @Override
    public String format(String key, Object... args)
    {
        return I18n.format(key, args);
    }

    @Override
    public EntityPlayer getClientPlayer()
    {
        return FMLClientHandler.instance().getClientPlayerEntity();
    }

    @Override
    public EntityPlayer getPlayerFromMessageContext(MessageContext ctx)
    {
        switch (ctx.side)
        {
            case CLIENT:
                return FMLClientHandler.instance().getClientPlayerEntity();
            case SERVER:
                return super.getPlayerFromMessageContext(ctx);
            default:
                TFStorage.logger.warn("getPlayerFromMessageContext() 中的端无效: " + ctx.side);
                return null;
        }
    }


    @Override
    public void registerEventHandlers()
    {
        super.registerEventHandlers();
        MinecraftForge.EVENT_BUS.register(new ModConfig());
        MinecraftForge.EVENT_BUS.register(GuiEvents.instance());
        MinecraftForge.EVENT_BUS.register(new InputEvents());
    }



    @Override
    public boolean isShiftKeyDown()
    {
        return GuiScreen.isShiftKeyDown();
    }

    @Override
    public boolean isControlKeyDown()
    {
        return GuiScreen.isCtrlKeyDown();
    }

    @Override
    public boolean isAltKeyDown()
    {
        return GuiScreen.isAltKeyDown();
    }

    @SubscribeEvent
    public static void registerModels(ModelRegistryEvent event)
    {
        registerItemBlockModels();
        registerAllItemModels();
    }

    private static void registerAllItemModels()
    {
        registerItemModelWithVariants(ModItems.MEMORY_CARD);
        registerItemModelWithVariantsAndMeshDefinition(ModItems.TF_BAG);
        registerSimpleItemModel(ModItems.SYCORE);
        registerSimpleItemModel(ModItems.EDCORE);
        registerSimpleItemModel(ModItems.TFCORE);
    }

    private static void registerSimpleItemModel(BaseItem item)
    {
        ModelLoader.setCustomModelResourceLocation(item, 0, 
            new ModelResourceLocation(item.getRegistryName(), "inventory"));
    }

    private static void registerItemModelWithVariants(BaseItem item)
    {

        ResourceLocation[] variants = item.getItemVariants();
        
        // 对于 TFUnit，直接使用循环注册0-3 的metadata，避免创建NonNullList 和调用getSubItems
        for (int i = 0; i < variants.length; i++)
        {
            ModelResourceLocation mrl = (variants[i] instanceof ModelResourceLocation) ?
                                        (ModelResourceLocation)variants[i] : new ModelResourceLocation(variants[i], "inventory");
            ModelLoader.setCustomModelResourceLocation(item, i, mrl);
        }
    }

    private static void registerItemModelWithVariantsAndMeshDefinition(BaseItem item)
    {
        ModelLoader.registerItemVariants(item, item.getItemVariants());
        ModelLoader.setCustomMeshDefinition(item, stack -> item.getModelLocation(stack));
    }


    private static void registerItemBlockModels()
    {
        registerAllItemBlockModels(ModBlocks.TF_CHEST, "facing=north,type=", "");
    }

    private static void registerItemBlockModel(TFChestBlock blockIn, int meta, String fullVariant)
    {
        ModelLoader.setCustomModelResourceLocation(Item.getItemFromBlock(blockIn), meta,
            new ModelResourceLocation(blockIn.getRegistryName(), fullVariant));
    }

    private static void registerAllItemBlockModels(TFChestBlock blockIn, String variantPre, String variantPost)
    {
        String[] names = blockIn.getUnlocalizedNames();

        for (int meta = 0; meta < names.length; meta++)
        {
            Item item = Item.getItemFromBlock(blockIn);
            ModelResourceLocation mrl = new ModelResourceLocation(item.getRegistryName(), variantPre + names[meta] + variantPost);
            ModelLoader.setCustomModelResourceLocation(item, meta, mrl);
        }
    }


}
