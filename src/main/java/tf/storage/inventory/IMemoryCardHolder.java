package tf.storage.inventory;

import net.minecraft.item.ItemStack;

/**
 * Memory card holder interface, specifically used to define the basic behavior of memory card holders
 * This interface replaces the original modular inventory holder interface and focuses on memory card functionality
 * 
 * <p>Classes implementing this interface should be able to:
 * <ul>
 *   <li>Store and manage multiple memory cards</li>
 *   <li>Track the currently selected memory card</li>
 *   <li>Provide maximum capacity limit for memory cards</li>
 * </ul>
 * 
 * @author TinyFish3714
 * @version 1.0
 */
public interface IMemoryCardHolder
{
    /**
     * Get the currently selected memory card
     * 
     * @return The currently selected memory card ItemStack, or an empty ItemStack if no memory card is selected
     */
    ItemStack getSelectedMemoryCard();
    
    /**
     * Set the memory card at the specified index position as the currently selected state
     * 
     * @param index The index position of the memory card to select, should be between 0 and getMaxMemoryCards()-1
     * @throws IndexOutOfBoundsException If the index is out of valid range
     */
    void setSelectedMemoryCard(int index);
    
    /**
     * Get the index position of the currently selected memory card
     * 
     * @return The index of the currently selected memory card, or -1 if no memory card is selected
     */
    int getSelectedMemoryCardIndex();
    
    /**
     * Get the maximum number of memory cards this holder can store
     *
     * @return Maximum memory card capacity
     */
    int getMaxMemoryCards();
    
    /**
     * Get the container item stack
     *
     * @return The container item stack
     */
    ItemStack getContainerStack();
}
