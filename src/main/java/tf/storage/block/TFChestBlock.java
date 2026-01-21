package tf.storage.block;

import java.util.Random;
import javax.annotation.Nullable;

import net.minecraft.block.Block;
import net.minecraft.block.BlockDirectional;
import net.minecraft.block.BlockHorizontal;
import net.minecraft.block.SoundType;
import net.minecraft.block.material.Material;
import net.minecraft.block.properties.IProperty;
import net.minecraft.block.properties.PropertyDirection;
import net.minecraft.block.properties.PropertyEnum;
import net.minecraft.block.state.BlockFaceShape;
import net.minecraft.block.state.BlockStateContainer;
import net.minecraft.block.state.IBlockState;
import net.minecraft.creativetab.CreativeTabs;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemBlock;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.BlockRenderLayer;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.EnumHand;
import net.minecraft.util.IStringSerializable;
import net.minecraft.util.NonNullList;
import net.minecraft.util.Rotation;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.IBlockAccess;
import net.minecraft.world.World;
import net.minecraftforge.common.util.Constants;
import net.minecraftforge.items.CapabilityItemHandler;
import net.minecraftforge.items.IItemHandler;
import net.minecraftforge.items.ItemHandlerHelper;

import tf.storage.TFStorage;
import tf.storage.creativetab.CreativeTab;
import tf.storage.item.ItemBlockStorage;
import tf.storage.core.ModInfo;
import tf.storage.tile.TileChest;
import tf.storage.util.StackHelper;
import tf.storage.util.WorldHelper;

public class TFChestBlock extends Block
{
    protected static final AxisAlignedBB SINGLE_CHEST_AABB = new AxisAlignedBB(0.0625D, 0.0D, 0.0625D, 0.9375D, 0.875D, 0.9375D);
    
    public static final PropertyEnum<TFChestBlock.EnumStorageType> TYPE = PropertyEnum.create("type", TFChestBlock.EnumStorageType.class);
    public static final PropertyDirection FACING = BlockHorizontal.FACING;
    
    protected String blockName;
    protected String[] unlocalizedNames;
    protected String[] tooltipNames;

    public TFChestBlock(String name, float hardness, float resistance, int harvestLevel, Material material)
    {
        super(material);
        this.setHardness(hardness);
        this.setResistance(resistance);
        this.setHarvestLevel("pickaxe", harvestLevel);
        this.setCreativeTab(CreativeTab.TF_STORAGE_TAB);
        this.setSoundType(SoundType.STONE);
        this.blockName = name;
        this.unlocalizedNames = this.generateUnlocalizedNames();
        this.tooltipNames = this.generateTooltipNames();
        
        this.setDefaultState(this.getBlockState().getBaseState()
                .withProperty(TYPE, TFChestBlock.EnumStorageType.TF_CHEST_0)
                .withProperty(FACING, EnumFacing.NORTH));
    }

    @Override
    public boolean hasTileEntity(IBlockState state)
    {
        return true;
    }

    @Override
    public TileEntity createTileEntity(World world, IBlockState state)
    {
        TileChest tehc = new TileChest();
        tehc.setStorageTier(world, state.getValue(TYPE).getTier());
        return tehc;
    }

    @Override
    public void onBlockPlacedBy(World world, BlockPos pos, IBlockState state, EntityLivingBase placer, ItemStack stack)
    {
        TileChest te = WorldHelper.getTileEntitySafely(world, pos, TileChest.class);
        if (te == null) return;

        NBTTagCompound nbt = stack.getTagCompound();
        if (nbt != null && nbt.hasKey("BlockEntityTag", Constants.NBT.TAG_COMPOUND))
        {
            te.readFromNBT(nbt.getCompoundTag("BlockEntityTag"));
        }
        else
        {
            if (stack.hasDisplayName())
            {
                te.setInventoryName(stack.getDisplayName());
            }
        }

        te.setFacing(placer.getHorizontalFacing().getOpposite());
        te.onLoad();
    }



    @Override
    public boolean onBlockActivated(World world, BlockPos pos, IBlockState state, EntityPlayer player, EnumHand hand,
                                    EnumFacing side, float hitX, float hitY, float hitZ)
    {
        if (world.isRemote) return true;

        TileChest te = WorldHelper.getTileEntitySafely(world, pos, TileChest.class);
        if (te != null && !te.isInvalid())
        {
            if (te.hasGui())
            {
                player.openGui(TFStorage.instance, ModInfo.GUI_ID_TILE_ENTITY_GENERIC, world, pos.getX(), pos.getY(), pos.getZ());
            }
            return true;
        }
        return false;
    }

    @Override
    public void onBlockClicked(World world, BlockPos pos, EntityPlayer playerIn)
    {
        if (!world.isRemote)
        {
            TileChest te = WorldHelper.getTileEntitySafely(world, pos, TileChest.class);
            if (te != null)
            {
            }
        }
    }




    @Override
    public void breakBlock(World world, BlockPos pos, IBlockState state)
    {
        TileChest te = WorldHelper.getTileEntitySafely(world, pos, TileChest.class);
        if (te != null)
        {
            IItemHandler handler = te.getBaseItemHandler();
            if (handler != null)
            {
                StackHelper.dropInventoryContentsInWorld(world, pos, handler);
                world.updateComparatorOutputLevel(pos, this);
            }
        }
        super.breakBlock(world, pos, state);
    }

    @Override
    public boolean removedByPlayer(IBlockState state, World world, BlockPos pos, EntityPlayer player, boolean willHarvest)
    {
        return super.removedByPlayer(state, world, pos, player, willHarvest);
    }

    @Override
    public void harvestBlock(World worldIn, EntityPlayer player, BlockPos pos, IBlockState state, TileEntity te, ItemStack stack)
    {
        super.harvestBlock(worldIn, player, pos, state, te, stack);
        worldIn.setBlockToAir(pos);
    }

    @Override
    public boolean hasComparatorInputOverride(IBlockState state)
    {
        return true;
    }

    @Override
    public int getComparatorInputOverride(IBlockState state, World world, BlockPos pos)
    {
        TileChest te = WorldHelper.getTileEntitySafely(world, pos, TileChest.class);
        if (te != null)
        {
            IItemHandler handler = te.getCapability(CapabilityItemHandler.ITEM_HANDLER_CAPABILITY, null);
            if (handler != null)
            {
                return ItemHandlerHelper.calcRedstoneFromInventory(handler);
            }
        }
        return 0;
    }


    @Override
    protected BlockStateContainer createBlockState()
    {
        return new BlockStateContainer(this, new IProperty[] { TYPE, FACING });
    }

    @Override
    public IBlockState getStateFromMeta(int meta)
    {
        return this.getDefaultState().withProperty(TYPE, EnumStorageType.fromMeta(meta));
    }

    @Override
    public int getMetaFromState(IBlockState state)
    {
        return state.getValue(TYPE).getMeta();
    }

    @Override
    public IBlockState getActualState(IBlockState state, IBlockAccess world, BlockPos pos)
    {
        TileChest te = WorldHelper.getTileEntitySafely(world, pos, TileChest.class);
        if (te != null)
        {
            EnumFacing facing = te.getFacing();
            if (facing.getAxis() != EnumFacing.Axis.Y)
            {
                state = state.withProperty(FACING, facing);
            }
        }
        return state;
    }

    @Override
    public boolean rotateBlock(World world, BlockPos pos, EnumFacing axis)
    {
        TileChest te = WorldHelper.getTileEntitySafely(world, pos, TileChest.class);
        if (te != null)
        {
            te.rotate(Rotation.CLOCKWISE_90);
            IBlockState state = world.getBlockState(pos).getActualState(world, pos);
            world.notifyBlockUpdate(pos, state, state, 3);
            return true;
        }
        return false;
    }

    @Override
    public boolean canRenderInLayer(IBlockState state, BlockRenderLayer layer)
    {
        return layer == this.getRenderLayer();
    }


    public String getBlockName()
    {
        return this.blockName;
    }

    @Override
    public int damageDropped(IBlockState state)
    {
        return this.getMetaFromState(state);
    }

    protected String[] generateUnlocalizedNames()
    {
        EnumStorageType[] types = EnumStorageType.values();
        String[] names = new String[types.length];
        
        for (int i = 0; i < types.length; i++)
        {
            names[i] = types[i].getName();
        }
        
        return names;
    }

    /**
     * 生成用于查找 ItemBlock 提示信息的名称。
     * 若要为方块的所有变体使用通用的提示信息，请返回一个仅包含一个条目的数组。
     * @return 提示信息名称数组
     */
    protected String[] generateTooltipNames()
    {
        return new String[] {
            ModInfo.NAME_TILE_ENTITY_TF_CHEST,
            ModInfo.NAME_TILE_ENTITY_TF_CHEST,
            ModInfo.NAME_TILE_ENTITY_TF_CHEST,
            ModInfo.NAME_TILE_ENTITY_TF_CHEST
        };
    }

    public String[] getUnlocalizedNames()
    {
        return this.unlocalizedNames;
    }

    public String[] getTooltipNames()
    {
        return this.tooltipNames;
    }

    public ItemBlock createItemBlock()
    {
        return new ItemBlockStorage(this);
    }

    @Override
    @Deprecated
    public float getBlockHardness(IBlockState state, World world, BlockPos pos)
    {
        // 已移除锁定检查逻辑
        return super.getBlockHardness(state, world, pos);
    }


    @Override
    public BlockFaceShape getBlockFaceShape(IBlockAccess worldIn, IBlockState state, BlockPos pos, EnumFacing face)
    {
        return state.getValue(TYPE).isFullCube() ? BlockFaceShape.SOLID : BlockFaceShape.UNDEFINED;
    }

    @Override
    public boolean isNormalCube(IBlockState state) { return state.getValue(TYPE).isFullCube(); }

    @Override
    public boolean isNormalCube(IBlockState state, IBlockAccess world, BlockPos pos) { return state.getValue(TYPE).isFullCube(); }

    @Override
    public boolean isOpaqueCube(IBlockState state) { return state.getValue(TYPE).isFullCube(); }

    @Override
    public boolean isFullCube(IBlockState state) { return state.getValue(TYPE).isFullCube(); }

    @Override
    public boolean isSideSolid(IBlockState state, IBlockAccess world, BlockPos pos, EnumFacing side) { return state.getValue(TYPE).isFullCube(); }

    @Override
    public AxisAlignedBB getBoundingBox(IBlockState state, IBlockAccess source, BlockPos pos)
    {
        return SINGLE_CHEST_AABB;
    }

    @Override
    public void getSubBlocks(CreativeTabs tab, NonNullList<ItemStack> list)
    {
        for (EnumStorageType type : EnumStorageType.values())
        {
            list.add(new ItemStack(this, 1, type.getMeta()));
        }
    }


    public static enum EnumStorageType implements IStringSerializable
    {
        TF_CHEST_0 (0, 0, ModInfo.NAME_TILE_ENTITY_TF_CHEST),
        TF_CHEST_1 (1, 1, ModInfo.NAME_TILE_ENTITY_TF_CHEST),
        TF_CHEST_2 (2, 2, ModInfo.NAME_TILE_ENTITY_TF_CHEST),
        TF_CHEST_3 (3, 3, ModInfo.NAME_TILE_ENTITY_TF_CHEST);

        private final int tier;
        private final String nameBase;
        private final int meta;
        private final boolean isFullCube;

        private EnumStorageType(int meta, int tier, String nameBase)
        {
            this(meta, tier, nameBase, false);
        }

        private EnumStorageType(int meta, int tier, String nameBase, boolean fullCube)
        {
            this.meta = meta;
            this.tier = tier;
            this.nameBase = nameBase;
            this.isFullCube = fullCube;
        }

        @Override
        public String getName()
        {
            return this.tier < 0 ? this.nameBase : this.nameBase + "_" + this.tier;
        }

        public int getTier() { return this.tier; }
        public int getMeta() { return this.meta; }
        public boolean isFullCube() { return this.isFullCube; }

        public static EnumStorageType fromMeta(int meta)
        {
            return values()[meta % values().length];
        }
    }
}
