package tf.storage.item.base;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;
import javax.annotation.Nullable;
import net.minecraft.client.renderer.block.model.ModelResourceLocation;
import net.minecraft.client.util.ITooltipFlag;
import net.minecraft.creativetab.CreativeTabs;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.util.NonNullList;
import net.minecraft.util.ResourceLocation;
import net.minecraft.world.World;
import net.minecraftforge.fml.common.registry.ForgeRegistries;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;
import tf.storage.TFStorage;
import tf.storage.creativetab.CreativeTab;
import tf.storage.core.ModInfo;

/**
 * BaseItem - TF Storage 物品基类
 * 重命名自 ItemTFStorage 以保持命名简洁
 */
public class BaseItem extends Item
{
    protected final String name;
    protected String commonTooltip = null;

    public BaseItem(String name)
    {
        super();

        this.name = name;

        this.setCreativeTab(CreativeTab.TF_STORAGE_TAB);
        this.setTranslationKey(name);
    }

    public String getBaseName()
    {
        return this.name;
    }

    @Override
    public Item setTranslationKey(String name)
    {
        return super.setTranslationKey(ModInfo.getDotPrefixedName(name));
    }

    @Override
    public boolean shouldCauseReequipAnimation(ItemStack oldStack, ItemStack newStack, boolean slotChanged)
    {
        return slotChanged || oldStack.getItem() != newStack.getItem() || oldStack.getMetadata() != newStack.getMetadata();
    }

    /**
     * 自定义 addInformation() 方法，允许选择提示信息子集。
     */
    @SideOnly(Side.CLIENT)
    public void addTooltipLines(ItemStack stack, EntityPlayer player, List<String> list, boolean verbose)
    {
    }

    @Override
    @SideOnly(Side.CLIENT)
    public void addInformation(ItemStack stack, @Nullable World world, List<String> list, ITooltipFlag advanced)
    {
        ArrayList<String> tmpList = new ArrayList<String>();
        boolean verbose = TFStorage.proxy.isShiftKeyDown();

        // 无 NBT 数据的“新”物品：在常规提示数据前显示提示
        if (stack.getTagCompound() == null)
        {
            this.addTooltips(stack, tmpList, verbose);

            if (verbose == false && tmpList.size() > 2)
            {
                list.add(TFStorage.proxy.format("tfstorage.tooltip.item.holdshiftfordescription"));
            }
            else
            {
                list.addAll(tmpList);
            }
        }

        tmpList.clear();

        EntityPlayer player = TFStorage.proxy.getClientPlayer();

        this.addTooltipLines(stack, player, tmpList, true);

        if (verbose == false && tmpList.size() > 2)
        {
            tmpList.clear();
            this.addTooltipLines(stack, player, tmpList, false);

            if (tmpList.size() > 0)
            {
                list.add(tmpList.get(0));
            }

            list.add(TFStorage.proxy.format("tfstorage.tooltip.item.holdshift"));
        }
        else
        {
            list.addAll(tmpList);
        }
    }

    @SideOnly(Side.CLIENT)
    public static void addTranslatedTooltip(String key, List<String> list, boolean verbose, Object... args)
    {
        String translated = TFStorage.proxy.format(key, args);

        if (translated.equals(key) == false)
        {
            if (translated.contains("|lf"))
            {
                String[] lines = translated.split(Pattern.quote("|lf"));

                for (String line : lines)
                {
                    list.add(line);
                }
            }
            else
            {
                list.add(translated);
            }
        }
    }

    @SideOnly(Side.CLIENT)
    public void addTooltips(ItemStack stack, List<String> list, boolean verbose)
    {
        addTranslatedTooltip(this.getTranslationKey(stack) + ".tooltips", list, verbose);

        if (this.commonTooltip != null)
        {
            addTranslatedTooltip(this.commonTooltip, list, verbose);
        }
    }

    public void getSubItemsCustom(CreativeTabs tab, NonNullList<ItemStack> items)
    {
        super.getSubItems(tab, items);
    }

    @Override
    public void getSubItems(CreativeTabs tab, NonNullList<ItemStack> items)
    {
        if (this.isInCreativeTab(tab))
        {
            this.getSubItemsCustom(tab, items);
        }
    }

    @SideOnly(Side.CLIENT)
    public ResourceLocation[] getItemVariants()
    {
        return new ResourceLocation[] { ForgeRegistries.ITEMS.getKey(this) };
    }

    @SideOnly(Side.CLIENT)
    public ModelResourceLocation getModelLocation(ItemStack stack)
    {
        return new ModelResourceLocation(this.getRegistryName(), "inventory");
    }
}
