package tf.storage.platform.forge.block;

import javax.annotation.Nullable;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.DirectionProperty;
import net.minecraft.world.level.material.MapColor;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraftforge.network.NetworkHooks;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.minecraft.nbt.CompoundTag;
import net.minecraftforge.items.IItemHandler;
import tf.storage.core.util.StackHelper;

/**
 * TFChestBlock - TF箱子方块
 * 支持4种等级，每种等级有不同的存储容量
 * 
 * 适配 Minecraft 1.20.1
 */
public class TFChestBlock extends Block implements EntityBlock {
    
    public static final DirectionProperty FACING = HorizontalDirectionalBlock.FACING;
    
    protected static final VoxelShape SHAPE = Block.box(1.0D, 0.0D, 1.0D, 15.0D, 14.0D, 15.0D);
    
    private final Tier tier;
    
    public enum Tier {
        TIER_0(0, 27),   // 小型 - 27格
        TIER_1(1, 45),   // 正常 - 45格
        TIER_2(2, 72),   // 大型 - 72格
        TIER_3(3, 108);  // 超大 - 108格
        
        private final int tierIndex;
        private final int invSize;
        
        Tier(int tierIndex, int invSize) {
            this.tierIndex = tierIndex;
            this.invSize = invSize;
        }
        
        public int getTierIndex() { return tierIndex; }
        public int getInvSize() { return invSize; }
    }
    
    public TFChestBlock(Tier tier) {
        super(BlockBehaviour.Properties.of()
            .mapColor(MapColor.STONE)
            .strength(6.0f, 60.0f)
            .sound(SoundType.STONE)
            .requiresCorrectToolForDrops());
        
        this.tier = tier;
        this.registerDefaultState(this.stateDefinition.any().setValue(FACING, Direction.NORTH));
    }
    
    public Tier getTier() {
        return tier;
    }
    
    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(FACING);
    }
    
    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        return this.defaultBlockState().setValue(FACING, context.getHorizontalDirection().getOpposite());
    }
    
    @Override
    public VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return SHAPE;
    }
    
    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new TFChestBlockEntity(pos, state, tier);
    }
    
    @Override
    public InteractionResult use(BlockState state, Level level, BlockPos pos, Player player, 
            InteractionHand hand, BlockHitResult hit) {
        if (level.isClientSide) {
            return InteractionResult.SUCCESS;
        }
        
        BlockEntity be = level.getBlockEntity(pos);
        if (be instanceof TFChestBlockEntity tfChest) {
            if (player instanceof ServerPlayer serverPlayer) {
                NetworkHooks.openScreen(serverPlayer, tfChest, buf -> buf.writeBlockPos(pos));
            }
            return InteractionResult.CONSUME;
        }
        
        return InteractionResult.PASS;
    }
    
    @Override
    public void setPlacedBy(Level level, BlockPos pos, BlockState state, @Nullable LivingEntity placer, ItemStack stack) {
        super.setPlacedBy(level, pos, state, placer, stack);
        
        BlockEntity be = level.getBlockEntity(pos);
        if (be instanceof TFChestBlockEntity tfChest) {
            // 从物品NBT恢复数据
            CompoundTag nbt = stack.getTag();
            if (nbt != null && nbt.contains("BlockEntityTag")) {
                tfChest.load(nbt.getCompound("BlockEntityTag"));
            } else if (stack.hasCustomHoverName()) {
                tfChest.setCustomName(stack.getHoverName());
            }
            
            if (placer != null) {
                tfChest.setFacing(placer.getDirection().getOpposite());
            }
        }
    }
    
    @Override
    public void onRemove(BlockState state, Level level, BlockPos pos, BlockState newState, boolean isMoving) {
        if (!state.is(newState.getBlock())) {
            BlockEntity be = level.getBlockEntity(pos);
            if (be instanceof TFChestBlockEntity tfChest) {
                IItemHandler handler = tfChest.getBaseItemHandler();
                if (handler != null) {
                    StackHelper.dropInventoryContentsInWorld(level, pos, handler);
                }
            }
        }
        super.onRemove(state, level, pos, newState, isMoving);
    }
    
    @Override
    public boolean hasAnalogOutputSignal(BlockState state) {
        return true;
    }
    
    @Override
    @SuppressWarnings("deprecation")
    public int getAnalogOutputSignal(BlockState state, Level level, BlockPos pos) {
        BlockEntity be = level.getBlockEntity(pos);
        if (be instanceof TFChestBlockEntity tfChest) {
            IItemHandler handler = tfChest.getBaseItemHandler();
            if (handler != null) {
                return net.minecraftforge.items.ItemHandlerHelper.calcRedstoneFromInventory(handler);
            }
        }
        return 0;
    }
}
