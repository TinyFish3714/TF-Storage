package tf.storage.platform.forge.inventory.slot;

import java.util.Optional;
import net.minecraft.core.NonNullList;
import net.minecraft.world.Container;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.CraftingContainer;
import net.minecraft.world.inventory.ResultContainer;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.CraftingRecipe;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.level.Level;
import net.minecraftforge.common.ForgeHooks;

/**
 * A slot for crafting results.
 * Handles recipe unlocking and remaining items distribution.
 */
public class ResultSlot extends Slot {
    
    private final Player player;
    private final CraftingContainer craftMatrix;
    private int amountCrafted;

    public ResultSlot(CraftingContainer craftMatrix, ResultContainer craftResult,
                      int index, int xPosition, int yPosition, Player player) {
        super(craftResult, index, xPosition, yPosition);

        this.player = player;
        this.craftMatrix = craftMatrix;
    }

    @Override
    public boolean mayPlace(ItemStack stack) {
        return false;
    }

    @Override
    public ItemStack remove(int amount) {
        if (this.hasItem()) {
            this.amountCrafted += Math.min(amount, this.getItem().getCount());
        }

        return super.remove(amount);
    }

    @Override
    protected void onQuickCraft(ItemStack stack, int amount) {
        this.amountCrafted += amount;
        this.checkTakeAchievements(stack);
    }

    @Override
    protected void onSwapCraft(int amount) {
        this.amountCrafted += amount;
    }

    @Override
    protected void checkTakeAchievements(ItemStack stack) {
        if (this.amountCrafted > 0) {
            stack.onCraftedBy(this.player.level(), this.player, this.amountCrafted);
            net.minecraftforge.event.ForgeEventFactory.firePlayerCraftingEvent(this.player, stack, this.craftMatrix);
        }

        this.amountCrafted = 0;

        if (this.container instanceof ResultContainer resultContainer) {
            resultContainer.awardUsedRecipes(this.player, this.craftMatrix.getItems());
        }
    }

    @Override
    public void onTake(Player player, ItemStack stack) {
        this.checkTakeAchievements(stack);

        ForgeHooks.setCraftingPlayer(player);
        Level level = player.level();
        
        NonNullList<ItemStack> remainingItems = NonNullList.withSize(this.craftMatrix.getContainerSize(), ItemStack.EMPTY);
        
        // Get the recipe and its remaining items
        Optional<CraftingRecipe> recipe = level.getRecipeManager()
            .getRecipeFor(RecipeType.CRAFTING, this.craftMatrix, level);
        
        if (recipe.isPresent()) {
            remainingItems = recipe.get().getRemainingItems(this.craftMatrix);
        }
        
        ForgeHooks.setCraftingPlayer(null);

        for (int i = 0; i < remainingItems.size(); i++) {
            ItemStack stackInSlot = this.craftMatrix.getItem(i);
            ItemStack remainingItemsInSlot = remainingItems.get(i);

            if (!stackInSlot.isEmpty()) {
                this.craftMatrix.removeItem(i, 1);
                stackInSlot = this.craftMatrix.getItem(i);
            }

            if (!remainingItemsInSlot.isEmpty()) {
                if (stackInSlot.isEmpty()) {
                    this.craftMatrix.setItem(i, remainingItemsInSlot);
                } else if (ItemStack.isSameItem(stackInSlot, remainingItemsInSlot) &&
                           ItemStack.isSameItemSameTags(stackInSlot, remainingItemsInSlot)) {
                    remainingItemsInSlot.grow(stackInSlot.getCount());
                    this.craftMatrix.setItem(i, remainingItemsInSlot);
                } else if (!this.player.getInventory().add(remainingItemsInSlot)) {
                    this.player.drop(remainingItemsInSlot, false);
                }
            }
        }

        // Trigger slotsChanged to update crafting result for continuous crafting
        this.craftMatrix.setChanged();
    }
}
