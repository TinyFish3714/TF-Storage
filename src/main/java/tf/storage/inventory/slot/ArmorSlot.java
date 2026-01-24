package tf.storage.inventory.slot;

import javax.annotation.Nullable;
import net.minecraft.inventory.EntityEquipmentSlot;
import net.minecraft.item.ItemArmor;
import net.minecraft.item.ItemStack;
import net.minecraftforge.items.IItemHandler;
import tf.storage.inventory.container.BagContainer;
import tf.storage.inventory.container.base.BaseContainer;

public class ArmorSlot extends GenericSlot
{
    protected final BaseContainer container;
    protected final int armorSlotIndex;

    public ArmorSlot(BaseContainer container, IItemHandler itemHandler, int armorSlotIndex, int index, int xPosition, int yPosition)
    {
        super(itemHandler, index, xPosition, yPosition);
        this.container = container;
        this.armorSlotIndex = armorSlotIndex;
    }

    @Override
    public int getSlotStackLimit()
    {
        return 1;
    }

    @Override
    public int getItemStackLimit(ItemStack stack)
    {
        return this.getSlotStackLimit();
    }

    @Override
    public boolean isItemValid(ItemStack stack)
    {
        if (stack.isEmpty())
        {
            return false;
        }

        EntityEquipmentSlot slot = BagContainer.EQUIPMENT_SLOT_TYPES[this.armorSlotIndex];
        return stack.getItem().isValidArmor(stack, slot, this.container.player);
    }

    @Override
    @Nullable
    public String getSlotTexture()
    {
        return ItemArmor.EMPTY_SLOT_NAMES[BagContainer.EQUIPMENT_SLOT_TYPES[this.armorSlotIndex].getIndex()];
    }
}
