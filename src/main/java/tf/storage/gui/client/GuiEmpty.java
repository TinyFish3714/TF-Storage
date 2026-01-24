package tf.storage.gui.client;

import net.minecraft.client.gui.GuiScreen;

public class GuiEmpty extends GuiScreen
{
    @Override
    public void drawDefaultBackground()
    {
        // Draw nothing to avoid flashing dirt background
    }

    @Override
    public void drawScreen(int mouseX, int mouseY, float partialTicks)
    {
        // Draw a dark background to hide the game world flickering
        this.drawGradientRect(0, 0, this.width, this.height, -1072689136, -804253680);
    }
}
