package tf.storage.item;

import java.util.ArrayList;
import java.util.List;
import javax.annotation.Nullable;
import net.minecraft.client.resources.I18n;
import net.minecraft.client.util.ITooltipFlag;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemBlock;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.world.World;
import net.minecraftforge.common.util.Constants;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;
import tf.storage.TFStorage;
import tf.storage.block.TFChestBlock;
import tf.storage.item.base.BaseItem;
import tf.storage.core.ModInfo;
import tf.storage.util.NBTHelper;

public class ItemBlockStorage extends ItemBlock
{
    protected String[] blockNames;
    protected String[] tooltipNames;

    public ItemBlockStorage(TFChestBlock block)
    {
        super(block);

        this.setHasSubtypes(true);
        this.setMaxDamage(0);

        this.setBlockNames(block.getUnlocalizedNames());
        this.setTooltipNames(block.getTooltipNames());
    }

    public void setBlockNames(String[] names)
    {
        this.blockNames = names;
    }

    public void setTooltipNames(String[] names)
    {
        this.tooltipNames = names;
    }

    @Override
    public int getMetadata(int meta)
    {
        return meta;
    }

    @Override
    public String getTranslationKey(ItemStack stack)
    {
        if (this.blockNames != null && stack.getMetadata() < this.blockNames.length)
        {
            return "tile." + ModInfo.getDotPrefixedName(this.blockNames[stack.getMetadata()]);
        }

        return super.getTranslationKey(stack);
    }

    public String getTooltipName(ItemStack stack)
    {
        if (this.tooltipNames != null)
        {
            if (stack.getMetadata() < this.tooltipNames.length)
            {
                return "tile." + ModInfo.getDotPrefixedName(this.tooltipNames[stack.getMetadata()]);
            }
            else if (this.tooltipNames.length == 1)
            {
                return "tile." + ModInfo.getDotPrefixedName(this.tooltipNames[0]);
            }
        }

        return this.getTranslationKey(stack);
    }

    @Override
    @SideOnly(Side.CLIENT)
    public void addInformation(ItemStack stack, @Nullable World world, List<String> list, ITooltipFlag advanced)
    {
        this.addTooltips(stack, list, TFStorage.proxy.isShiftKeyDown());
    }

    @SideOnly(Side.CLIENT)
    public void addTooltips(ItemStack stack, List<String> list, boolean verbose)
    {
        BaseItem.addTranslatedTooltip(this.getTooltipName(stack) + ".tooltips", list, verbose);
    }

    @Override
    public boolean isFull3D()
    {
        return true;
    }
}
