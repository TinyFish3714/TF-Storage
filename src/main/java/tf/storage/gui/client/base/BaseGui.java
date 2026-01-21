package tf.storage.gui.client.base;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Collection;
import org.lwjgl.input.Keyboard;
import org.lwjgl.opengl.GL11;
import com.google.common.collect.Ordering;
import net.minecraft.client.gui.GuiButton;
import net.minecraft.client.renderer.InventoryEffectRenderer;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.OpenGlHelper;
import net.minecraft.client.renderer.RenderHelper;
import net.minecraft.client.resources.I18n;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.inventory.ClickType;
import net.minecraft.inventory.Slot;
import net.minecraft.item.ItemStack;
import net.minecraft.potion.Potion;
import net.minecraft.potion.PotionEffect;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.math.BlockPos;
import net.minecraftforge.client.event.GuiScreenEvent;
import net.minecraftforge.client.event.GuiScreenEvent.MouseInputEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.eventhandler.EventPriority;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.relauncher.Side;
import tf.storage.TFStorage;
import tf.storage.gui.component.HoverButton;
import tf.storage.inventory.container.base.BaseContainer;
import tf.storage.item.base.BaseItem;
import tf.storage.network.PacketHandler;
import tf.storage.network.packet.ActionPacket;
import tf.storage.core.ModInfo;
import tf.storage.util.StackHelper;

@Mod.EventBusSubscriber(Side.CLIENT)
public class BaseGui extends InventoryEffectRenderer
{
    protected final BaseContainer container;
    protected final EntityPlayer player;
    protected final ResourceLocation guiTextureWidgets;
    protected ResourceLocation guiTexture;
    
    // 在本地添加缺失的纹理引用，因为其在父类中是私有的
    private static final ResourceLocation INVENTORY_BACKGROUND = new ResourceLocation("textures/gui/container/inventory.png");

    public BaseGui(BaseContainer container, int xSize, int ySize, String textureName)
    {
        super(container);
        this.container = container;
        this.player = container.player;
        this.xSize = xSize;
        this.ySize = ySize;
        this.guiTexture = ModInfo.getGuiTexture(textureName);
        this.guiTextureWidgets = ModInfo.getGuiTexture("gui.widgets");
    }

    @Override
    public void drawScreen(int mouseX, int mouseY, float gameTicks)
    {
        // 强制修正 guiLeft，防止药水效果导致的 GUI 偏移
        this.guiLeft = (this.width - this.xSize) / 2;

        this.drawDefaultBackground();
        
        // 临时禁用 hasActivePotionEffects 以阻止父类绘制默认的药水效果
        boolean originalState = this.hasActivePotionEffects;
        this.hasActivePotionEffects = false;
        
        super.drawScreen(mouseX, mouseY, gameTicks);
        
        this.hasActivePotionEffects = originalState;
        
        // 如果原本有药水效果，则调用自定义绘制
        if (originalState) {
            this.drawCustomPotionEffects();
        }

        this.drawTooltips(mouseX, mouseY);
    }

    // 移除了 @Override updateActivePotionEffects，因为该方法在当前 Mappings 中不可见或不存在

    protected void drawCustomPotionEffects()
    {
        // 自定义绘制逻辑：调整位置并缩放为 75%
        int x = this.guiLeft - 124 + 32;
        int y = this.guiTop + 49;
        Collection<PotionEffect> collection = this.mc.player.getActivePotionEffects();

        if (!collection.isEmpty())
        {
            GlStateManager.color(1.0F, 1.0F, 1.0F, 1.0F);
            GlStateManager.disableLighting();
            int l = 33;

            if (collection.size() > 5)
            {
                l = 132 / (collection.size() - 1);
            }

            GlStateManager.pushMatrix();
            float scale = 0.75F;
            GlStateManager.scale(scale, scale, scale);
            
            int renderX = (int)(x / scale);
            int renderY = (int)(y / scale);

            for (PotionEffect potioneffect : Ordering.natural().sortedCopy(collection))
            {
                Potion potion = potioneffect.getPotion();

                if (!potion.shouldRender(potioneffect)) continue;

                GlStateManager.color(1.0F, 1.0F, 1.0F, 1.0F);
                this.mc.getTextureManager().bindTexture(INVENTORY_BACKGROUND);
                this.drawTexturedModalRect(renderX, renderY, 0, 166, 140, 32);

                if (potion.hasStatusIcon())
                {
                    int i1 = potion.getStatusIconIndex();
                    this.drawTexturedModalRect(renderX + 6, renderY + 7, 0 + i1 % 8 * 18, 198 + i1 / 8 * 18, 18, 18);
                }
                
                potion.renderInventoryEffect(potioneffect, this, renderX, renderY, this.zLevel);
                
                if (!potion.shouldRenderInvText(potioneffect))
                {
                    renderY += l;
                    continue;
                }

                String s1 = I18n.format(potion.getName());

                if (potioneffect.getAmplifier() == 1)
                {
                    s1 = s1 + " " + I18n.format("enchantment.level.2");
                }
                else if (potioneffect.getAmplifier() == 2)
                {
                    s1 = s1 + " " + I18n.format("enchantment.level.3");
                }
                else if (potioneffect.getAmplifier() == 3)
                {
                    s1 = s1 + " " + I18n.format("enchantment.level.4");
                }

                this.fontRenderer.drawStringWithShadow(s1, (float)(renderX + 10 + 18), (float)(renderY + 6), 16777215);
                String s = Potion.getPotionDurationString(potioneffect, 1.0F);
                this.fontRenderer.drawStringWithShadow(s, (float)(renderX + 10 + 18), (float)(renderY + 6 + 10), 8355711);
                
                renderY += l;
            }
            
            GlStateManager.popMatrix();
        }
    }

    @Override
    protected void drawGuiContainerBackgroundLayer(float gameTicks, int mouseX, int mouseY)
    {
        GlStateManager.color(1.0f, 1.0f, 1.0f, 1.0f);
        this.bindTexture(this.guiTexture);
        this.drawTexturedModalRect(this.guiLeft, this.guiTop, 0, 0, this.xSize, this.ySize);


        this.bindTexture(this.guiTexture);
    }

    protected void drawTooltips(int mouseX, int mouseY)
    {
        for (int i = 0; i < this.buttonList.size(); i++)
        {
            GuiButton button = this.buttonList.get(i);

            if ((button instanceof HoverButton) && button.mousePressed(this.mc, mouseX, mouseY))
            {
                this.drawHoveringText(((HoverButton) button).getHoverStrings(), mouseX, mouseY, this.fontRenderer);
            }
        }

        this.renderHoveredToolTip(mouseX, mouseY);
    }

    @Override
    protected void mouseClicked(int mouseX, int mouseY, int mouseButton) throws IOException
    {
        super.mouseClicked(mouseX, mouseY, mouseButton);

        for (int l = 0; l < this.buttonList.size(); ++l)
        {
            GuiButton guibutton = (GuiButton)this.buttonList.get(l);

            if (guibutton.mousePressed(this.mc, mouseX, mouseY))
            {
                if (mouseButton != 0)
                {
                    guibutton.playPressSound(this.mc.getSoundHandler());
                }

                this.actionPerformedWithButton(guibutton, mouseButton);
            }
        }
    }

    @Override
    protected void keyTyped(char typedChar, int keyCode) throws IOException
    {
        if (keyCode == Keyboard.KEY_ESCAPE || this.mc.gameSettings.keyBindInventory.isActiveAndMatches(keyCode))
        {
            this.mc.player.closeScreen();
        }

        this.checkHotbarKeys(keyCode);

        Slot slot = this.getSlotUnderMouse();

        if (slot != null)
        {
            if (this.mc.gameSettings.keyBindPickBlock.isActiveAndMatches(keyCode))
            {
                this.handleMouseClick(slot, slot.slotNumber, 0, ClickType.CLONE);
            }
            else if (this.mc.gameSettings.keyBindDrop.isActiveAndMatches(keyCode))
            {
                this.handleMouseClick(slot, slot.slotNumber, isCtrlKeyDown() ? 1 : 0, ClickType.THROW);
            }
        }
    }

    protected void actionPerformedWithButton(GuiButton guiButton, int mouseButton) throws IOException
    {
    }

    protected void bindTexture(ResourceLocation rl)
    {
        this.mc.getTextureManager().bindTexture(rl);
    }


}
