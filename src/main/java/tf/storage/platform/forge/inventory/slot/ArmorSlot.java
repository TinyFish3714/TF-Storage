package tf.storage.platform.forge.inventory.slot;

import javax.annotation.Nullable;
import com.mojang.datafixers.util.Pair;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.InventoryMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraftforge.items.IItemHandler;

/**
 * A slot specifically for armor items.
 */
public class ArmorSlot extends GenericSlot {
    
    private static final ResourceLocation[] ARMOR_SLOT_TEXTURES = new ResourceLocation[] {
        InventoryMenu.EMPTY_ARMOR_SLOT_BOOTS,
        InventoryMenu.EMPTY_ARMOR_SLOT_LEGGINGS,
        InventoryMenu.EMPTY_ARMOR_SLOT_CHESTPLATE,
        InventoryMenu.EMPTY_ARMOR_SLOT_HELMET
    };
    
    public static final EquipmentSlot[] EQUIPMENT_SLOT_TYPES = new EquipmentSlot[] {
        EquipmentSlot.FEET,
        EquipmentSlot.LEGS,
        EquipmentSlot.CHEST,
        EquipmentSlot.HEAD
    };
    
    protected final Player player;
    protected final int armorSlotIndex;

    public ArmorSlot(Player player, IItemHandler itemHandler, int armorSlotIndex, int index, int xPosition, int yPosition) {
        super(itemHandler, index, xPosition, yPosition);
        this.player = player;
        this.armorSlotIndex = armorSlotIndex;
    }

    @Override
    public int getMaxStackSize() {
        return 1;
    }

    @Override
    public int getMaxStackSize(ItemStack stack) {
        return this.getMaxStackSize();
    }

    @Override
    public boolean mayPlace(ItemStack stack) {
        if (stack.isEmpty()) {
            return false;
        }

        EquipmentSlot slot = EQUIPMENT_SLOT_TYPES[this.armorSlotIndex];
        return stack.canEquip(slot, this.player);
    }

    @Override
    public boolean mayPickup(Player player) {
        ItemStack stack = this.getItem();
        return (stack.isEmpty() || player.isCreative() || !EnchantmentHelper.hasBindingCurse(stack)) 
               && super.mayPickup(player);
    }

    @Override
    @Nullable
    public Pair<ResourceLocation, ResourceLocation> getNoItemIcon() {
        return Pair.of(InventoryMenu.BLOCK_ATLAS, ARMOR_SLOT_TEXTURES[this.armorSlotIndex]);
    }
}
