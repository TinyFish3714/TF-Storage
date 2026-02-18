package tf.storage.platform.forge.compat.jei;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import mezz.jei.api.constants.RecipeTypes;
import mezz.jei.api.recipe.RecipeType;
import mezz.jei.api.recipe.transfer.IRecipeTransferInfo;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.crafting.CraftingRecipe;
import tf.storage.platform.forge.menu.BagMenu;

public class BagMenuRecipeTransferInfo implements IRecipeTransferInfo<BagMenu, CraftingRecipe> {

    @Override
    public Class<? extends BagMenu> getContainerClass() {
        return BagMenu.class;
    }

    @Override
    public Optional<MenuType<BagMenu>> getMenuType() {
        return Optional.empty();
    }

    @Override
    public RecipeType<CraftingRecipe> getRecipeType() {
        return RecipeTypes.CRAFTING;
    }

    @Override
    public boolean canHandle(BagMenu container, CraftingRecipe recipe) {
        return true;
    }

    @Override
    public List<Slot> getRecipeSlots(BagMenu container, CraftingRecipe recipe) {
        int start = container.getCraftingSlotStart();
        int count = container.getCraftingSlotCount();
        List<Slot> slots = new ArrayList<>(count);
        for (int i = 0; i < count; i++) {
            slots.add(container.getSlot(start + i));
        }
        return slots;
    }

    @Override
    public List<Slot> getInventorySlots(BagMenu container, CraftingRecipe recipe) {
        List<Slot> slots = new ArrayList<>();
        int customStart = container.getCustomInventorySlotStart();
        int customCount = container.getCustomInventorySlotCount();
        for (int i = 0; i < customCount; i++) {
            slots.add(container.getSlot(customStart + i));
        }
        int playerStart = container.getPlayerInventorySlotStart();
        int playerCount = container.getPlayerInventorySlotCount();
        for (int i = 0; i < playerCount; i++) {
            slots.add(container.getSlot(playerStart + i));
        }
        return slots;
    }
}
