package tf.storage.platform.forge.item;

import java.util.ArrayList;
import java.util.List;
import javax.annotation.Nullable;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import tf.storage.core.util.NBTHelper;
import tf.storage.core.util.CardHelper;

/**
 * TFUnitItem - TF单元存储卡物品
 * 支持多种容量等级(6B/8B/10B/12B)
 * 
 * 适配 Minecraft 1.20.1
 */
public class TFUnitItem extends Item {
    
    private final Tier tier;
    
    public enum Tier {
        TIER_6B(6, 64),      // 2^6 = 64
        TIER_8B(8, 256),     // 2^8 = 256
        TIER_10B(10, 1024),  // 2^10 = 1024
        TIER_12B(12, 4096);  // 2^12 = 4096
        
        private final int bits;
        private final int maxStackSize;
        
        Tier(int bits, int maxStackSize) {
            this.bits = bits;
            this.maxStackSize = maxStackSize;
        }
        
        public int getBits() { return bits; }
        public int getMaxStackSize() { return maxStackSize; }
        
        public String getSuffix() {
            return bits + "b";
        }
    }
    
    public TFUnitItem(Tier tier, Properties properties) {
        super(properties);
        this.tier = tier;
    }
    
    public Tier getTier() {
        return tier;
    }
    
    public int getTFUnitTier(ItemStack stack) {
        return tier.getBits();
    }
    
    public int getMaxStorageStackSize() {
        return tier.getMaxStackSize();
    }
    
    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        return InteractionResultHolder.pass(player.getItemInHand(hand));
    }
    
    @Override
    public void appendHoverText(ItemStack stack, @Nullable Level level, List<Component> tooltip, TooltipFlag flag) {
        super.appendHoverText(stack, level, tooltip, flag);
        
        CompoundTag nbt = stack.getTag();
        if (nbt == null || nbt.isEmpty()) {
            tooltip.add(Component.translatable("tfstorage.tooltip.item.tfunit.nodata"));
            return;
        }
        
        // 物品列表提示信息
        ArrayList<String> lines = new ArrayList<>();
        int itemCount = CardHelper.getFormattedItemListFromContainerItem(stack, lines, 20);
        
        if (!lines.isEmpty()) {
            ListTag tagList = NBTHelper.getStoredItemsList(stack, false);
            int stackCount = tagList != null ? tagList.size() : 0;
            tooltip.add(Component.translatable("tfstorage.tooltip.item.tfunit.items.stackcount", stackCount, itemCount));
            for (String line : lines) {
                tooltip.add(Component.literal(line));
            }
        } else {
            tooltip.add(Component.translatable("tfstorage.tooltip.item.tfunit.noitems"));
        }
    }
    
    /**
     * 检查物品栈是否为有效的存储卡
     */
    public boolean isValidCard(ItemStack stack) {
        return stack.getItem() instanceof TFUnitItem;
    }
}