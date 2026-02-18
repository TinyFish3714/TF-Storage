package tf.storage.platform.forge.menu.provider;

import net.minecraft.network.chat.Component;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;
import tf.storage.platform.forge.menu.BagMenu;

/**
 * Menu provider for TF Bag.
 */
public class BagMenuProvider implements MenuProvider {

    private final ItemStack bagStack;

    public BagMenuProvider(ItemStack bagStack) {
        this.bagStack = bagStack;
    }

    @Override
    public Component getDisplayName() {
        return bagStack.getHoverName();
    }

    @Override
    public AbstractContainerMenu createMenu(int containerId, Inventory playerInventory, Player player) {
        return new BagMenu(containerId, playerInventory, bagStack);
    }
}
