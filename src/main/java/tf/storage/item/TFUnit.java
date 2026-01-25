package tf.storage.item;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import net.minecraft.client.renderer.block.model.ModelResourceLocation;
import net.minecraft.client.resources.I18n;
import net.minecraft.creativetab.CreativeTabs;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;
import net.minecraft.util.ActionResult;
import net.minecraft.util.EnumActionResult;
import net.minecraft.util.EnumHand;
import net.minecraft.util.NonNullList;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.text.TextFormatting;
import net.minecraft.world.World;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;
import tf.storage.item.base.BaseItem;
import tf.storage.core.ModInfo;
import tf.storage.core.ModItems;
import tf.storage.util.NBTHelper;
import tf.storage.util.CardHelper;

/**
 * TFUnit类是TF单元物品类，直接继承BaseItem。
 * 该类提供了简洁明了的TF单元管理功能。
 *
 * <p>此类支持多种TF单元容量等级，包括6B、8B、10B、12B。
 * 每种容量对应不同的元数据值。TF单元可以存储物品数据。</p>
 *
 * @author TinyFish3714
 * @version 2.0
 */
public class TFUnit extends BaseItem
{
    public static final int META_6B = 0;
    public static final int META_8B = 1;
    public static final int META_10B = 2;
    public static final int META_12B = 3;
    
    // TF单元容量常数
    public static final int TIER_6B = 6;
    public static final int TIER_8B = 8;
    public static final int TIER_10B = 10;
    public static final int TIER_12B = 12;

    public TFUnit(String name)
    {
        super(name);

        this.setMaxStackSize(64);
        this.setMaxDamage(0);
        this.setHasSubtypes(true);
    }

    @Override
    public String getTranslationKey(ItemStack stack)
    {
        int meta = stack.getMetadata();
        if (meta >= META_6B && meta <= META_12B)
        {
            return super.getTranslationKey() + "_items_" + meta;
        }
        return super.getTranslationKey();
    }

    @Override
    public ActionResult<ItemStack> onItemRightClick(World worldIn, EntityPlayer playerIn, EnumHand hand)
    {
        return new ActionResult<>(EnumActionResult.PASS, playerIn.getHeldItem(hand));
    }

    @Override
    @SideOnly(Side.CLIENT)
    public void addTooltipLines(ItemStack stack, EntityPlayer player, List<String> list, boolean verbose)
    {
        NBTTagCompound nbt = stack.getTagCompound();
        if (nbt == null || nbt.isEmpty())
        {
            list.add(I18n.format("tfstorage.tooltip.item.tfunit.nodata"));
            return;
        }

        // 物品列表提示信息
        ArrayList<String> lines = new ArrayList<>();
        int itemCount = CardHelper.getFormattedItemListFromContainerItem(stack, lines, 20);
        if (!lines.isEmpty())
        {
            NBTTagList tagList = NBTHelper.getStoredItemsList(stack, false);
            int stackCount = tagList != null ? tagList.tagCount() : 0;
            list.add(I18n.format("tfstorage.tooltip.item.tfunit.items.stackcount", stackCount, itemCount));
            list.addAll(lines);
        }
        else
        {
            list.add(I18n.format("tfstorage.tooltip.item.tfunit.noitems"));
        }
    }

    public boolean isValidCard(ItemStack stack)
    {
        int meta = stack.getMetadata();
        return meta >= META_6B && meta <= META_12B;
    }

    public int getTFUnitTier(ItemStack stack)
    {
        if (isValidCard(stack))
        {
            switch (stack.getMetadata())
            {
                case META_6B: return TIER_6B;
                case META_8B: return TIER_8B;
                case META_10B: return TIER_10B;
                case META_12B: return TIER_12B;
            }
        }
        return -1;
    }

    @Override
    public void getSubItemsCustom(CreativeTabs creativeTab, NonNullList<ItemStack> list)
    {
        list.add(new ItemStack(this, 1, META_6B));
        list.add(new ItemStack(this, 1, META_8B));
        list.add(new ItemStack(this, 1, META_10B));
        list.add(new ItemStack(this, 1, META_12B));
    }

    @Override
    @SideOnly(Side.CLIENT)
    public ResourceLocation[] getItemVariants()
    {
        String rl = ModInfo.MOD_ID + ":" + this.name;
        return new ResourceLocation[] {
            new ModelResourceLocation(rl, "tex=tfunit_items_6b"),
            new ModelResourceLocation(rl, "tex=tfunit_items_8b"),
            new ModelResourceLocation(rl, "tex=tfunit_items_10b"),
            new ModelResourceLocation(rl, "tex=tfunit_items_12b")
        };
    }
}
