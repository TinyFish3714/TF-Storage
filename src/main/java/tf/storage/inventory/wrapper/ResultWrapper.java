package tf.storage.inventory.wrapper;

import javax.annotation.Nullable;
import net.minecraft.item.crafting.IRecipe;
import tf.storage.inventory.handler.BasicHandler;

public class ResultWrapper extends BasicHandler
{
    @Nullable
    private IRecipe recipe;

    public ResultWrapper()
    {
        super(1);
    }

    public void setRecipe(@Nullable IRecipe recipe)
    {
        this.recipe = recipe;
    }

    @Nullable
    public IRecipe getRecipe()
    {
        return this.recipe;
    }
}
