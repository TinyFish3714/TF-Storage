package tf.storage.gui.client;

import java.io.IOException;
import java.util.Collection;
import org.lwjgl.input.Keyboard;
import com.google.common.collect.Ordering;
import net.minecraft.client.gui.GuiButton;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.client.gui.inventory.GuiInventory;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.resources.I18n;
import net.minecraft.inventory.Slot;
import net.minecraft.item.ItemStack;
import net.minecraft.potion.Potion;
import net.minecraft.potion.PotionEffect;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.MathHelper;
import tf.storage.gui.client.base.LargeStackGui;
import tf.storage.gui.component.HoverButton;
import tf.storage.gui.component.IconButton;
import tf.storage.gui.component.StateButton;
import tf.storage.gui.component.StateButton.ButtonState;
import tf.storage.gui.component.IButtonStateCallback;
import tf.storage.inventory.container.BagContainer;
import tf.storage.inventory.handler.ModularHandler;
import tf.storage.item.TFBag;
import tf.storage.item.TFBag.PickupMode;
import tf.storage.item.TFBag.RestockMode;
import tf.storage.item.TFBag.ShiftMode;
import tf.storage.network.PacketHandler;
import tf.storage.network.packet.ActionPacket;
import tf.storage.core.ModInfo;
import tf.storage.core.ModCompat;
import tf.storage.util.NBTHelper;

public class BagGui extends LargeStackGui implements IButtonStateCallback
{
    public static final int BTN_ID_FIRST_SELECT_MEMORY_CARD = 0;
    public static final int BTN_ID_FIRST_MOVE_ITEMS         = 4;
    public static final int BTN_ID_FIRST_SORT               = 10;
    public static final int BTN_ID_FIRST_REGION_LOCK        = 14;
    public static final int BTN_ID_FIRST_MODES              = 17;
    public static final int BTN_ID_BAUBLES                  = 100;

    private static final String[] BUTTON_STRINGS = new String[] {
            "tfstorage.gui.label.moveallitemsexcepthotbar",
            "tfstorage.gui.label.movematchingitemsexcepthotbar",
            "tfstorage.gui.label.leaveonefilledstack",
            "tfstorage.gui.label.fillstacks",
            "tfstorage.gui.label.movematchingitems",
            "tfstorage.gui.label.moveallitems",
            "tfstorage.gui.label.sortitems"
    };

    private final BagContainer containerTFB;
    private final ModularHandler invWithMemoryCards;
    private final int invSize;
    private final int numMemoryCardSlots;
    private final int bagTier;
    private final int offsetXTier;
    private int firstMemoryCardSlotX;
    private int firstMemoryCardSlotY;
    // private boolean hasActivePotionEffects; // 已移除：继承自 InventoryEffectRenderer
    private int lastGuiLeft;
    private int lastGuiTop;
    private final boolean baublesLoaded;
    public static final ResourceLocation RESOURCES_BAUBLES_BUTTON
            = new ResourceLocation(ModCompat.MODID_BAUBLES.toLowerCase(), "textures/gui/expanded_inventory.png");

    public BagGui(BagContainer container)
    {
        super(container, container.getBagTier() == 1 ? 256 : 176, 256, "gui.container.tfbag." + container.getBagTier());

        this.containerTFB = container;
        this.invWithMemoryCards = container.inventoryItemWithMemoryCards;
        this.invSize = this.invWithMemoryCards.getSlots();
        this.numMemoryCardSlots = this.invWithMemoryCards.getMemoryCardInventory().getSlots();
        this.bagTier = this.containerTFB.getBagTier();
        this.offsetXTier = this.bagTier == 1 ? 40 : 0;
        this.baublesLoaded = ModCompat.isBaublesLoaded();

        this.scaledStackSizeTextInventories.add(this.invWithMemoryCards);
    }

    private void updatePositions()
    {
        this.firstMemoryCardSlotX  = this.guiLeft + this.containerTFB.getSlot(0).xPos + 5 * 18;
        this.firstMemoryCardSlotY  = this.guiTop  + this.containerTFB.getSlot(0).yPos - 32;

        this.createButtons();

        this.lastGuiLeft = this.guiLeft;
        this.lastGuiTop = this.guiTop;
    }

    private boolean needsPositionUpdate()
    {
        return this.lastGuiLeft != this.guiLeft || this.lastGuiTop != this.guiTop;
    }

    @Override
    public void initGui()
    {
        super.initGui();
        this.updatePositions();


        Keyboard.enableRepeatEvents(false);
    }

    @Override
    public void updateScreen()
    {
        super.updateScreen();

    }

    @Override
    public void drawScreen(int mouseX, int mouseY, float gameTicks)
    {
        super.drawScreen(mouseX, mouseY, gameTicks);

        if (this.needsPositionUpdate())
        {
            this.updatePositions();
        }


        this.drawTooltips(mouseX, mouseY);
    }

    @Override
    protected void drawGuiContainerBackgroundLayer(float gameTicks, int mouseX, int mouseY)
    {
        GlStateManager.color(1.0f, 1.0f, 1.0f, 1.0f);
        this.bindTexture(this.guiTexture);
        this.drawTexturedModalRect(this.guiLeft, this.guiTop, 0, 0, this.xSize, this.ySize);

        this.bindTexture(this.guiTextureWidgets);

        if (this.invWithMemoryCards.isAccessibleBy(this.player) == false)
        {
            for (int i = 0; i < this.invSize; i++)
            {
                Slot slot = this.containerTFB.getSlot(i);
                this.drawTexturedModalRect(this.guiLeft + slot.xPos - 1, this.guiTop + slot.yPos - 1, 46, 0, 18, 18);
            }
        }

        int index = this.invWithMemoryCards.getSelectedMemoryCardIndex();
        if (index >= 0)
        {
            this.drawTexturedModalRect(this.firstMemoryCardSlotX - 1 + index * 18, this.firstMemoryCardSlotY, 46, 18, 18, 18);
            this.drawTexturedModalRect(this.firstMemoryCardSlotX + 4 + index * 18, this.firstMemoryCardSlotY + 20, 56, 48, 8, 8);
        }

        for (int i = 0; i < this.numMemoryCardSlots; i++)
        {
            if (this.invWithMemoryCards.getMemoryCardInventory().getStackInSlot(i).isEmpty())
            {
                this.drawTexturedModalRect(this.firstMemoryCardSlotX + 1 + i * 18, this.firstMemoryCardSlotY + 1, 32, 48, 16, 16);
            }
        }

        ItemStack modularStack = this.containerTFB.inventoryItemWithMemoryCards.getModularItemStack();

        if (modularStack.isEmpty() == false && ShiftMode.getEffectiveMode(modularStack) == ShiftMode.TO_BAG)
        {
            int x = this.guiLeft + this.offsetXTier + 64;
            this.drawTexturedModalRect(x, this.guiTop + 153, 32, 32, 12, 12);
        }

        int xOff = this.guiLeft + 51 + this.offsetXTier;
        GuiInventory.drawEntityOnScreen(xOff, this.guiTop + 82, 30, xOff - (float)mouseX, this.guiTop + 25 - (float)mouseY, this.mc.player);
    }

    @Override
    protected void drawGuiContainerForegroundLayer(int mouseX, int mouseY)
    {
        int xOff = this.offsetXTier;
        this.fontRenderer.drawString(I18n.format("container.crafting"), xOff + 97, 5, 0x404040);
        this.fontRenderer.drawString(I18n.format("tfstorage.container.tfbag"), xOff + 8, 5, 0x404040);
    }

    protected void createButtons()
    {
        this.buttonList.removeIf(b -> b.id < 200);

        int numMemoryCards = this.invWithMemoryCards.getMemoryCardInventory().getSlots();

        for (int i = 0; i < numMemoryCards; i++)
        {
            this.buttonList.add(new IconButton(BTN_ID_FIRST_SELECT_MEMORY_CARD + i,
                this.firstMemoryCardSlotX + 5 + i * 18, this.firstMemoryCardSlotY + 21,
                6, 6, 14, 21, this.guiTextureWidgets, 6, 0));
        }

        int x = this.guiLeft + this.containerTFB.getSlot(0).xPos + 2;
        int y = this.guiTop + this.containerTFB.getSlot(0).yPos + 55;

        this.buttonList.add(new HoverButton(BTN_ID_FIRST_MOVE_ITEMS + 0, x -   2, y + 1, 7, 7, 0, 56, this.guiTextureWidgets, 7, 0,
                "tfstorage.gui.label.moveallitemsexcepthotbar",
                "tfstorage.gui.label.holdshifttoincludehotbar"));

        this.buttonList.add(new HoverButton(BTN_ID_FIRST_MOVE_ITEMS + 1, x + 14, y + 1, 7, 7, 0, 14, this.guiTextureWidgets, 7, 0,
                "tfstorage.gui.label.movematchingitemsexcepthotbar",
                "tfstorage.gui.label.holdshifttoincludehotbar"));

        int[] xOff = new int[] { 31, 117, 134, 151 };
        int[] quickButtonUVs = new int[] { 14, 0, 14, 7, 0, 21, 14, 14 };

        for (int i = 2; i < 6; i++)
        {
            int u = quickButtonUVs[(i - 2) * 2];
            int v = quickButtonUVs[(i - 2) * 2 + 1];
            this.buttonList.add(new HoverButton(BTN_ID_FIRST_MOVE_ITEMS + i, x + xOff[i - 2], y + 1,
                7, 7, u, v, this.guiTextureWidgets, 7, 0, BUTTON_STRINGS[i]));
        }

        y = this.guiTop + this.containerTFB.getSlot(0).yPos - 11;

        this.buttonList.add(new StateButton(BTN_ID_FIRST_MODES + 0, x - 1, y, 7, 7, 7, 0,
                this.guiTextureWidgets, this,
                ButtonState.createTranslate(0, 42, "tfstorage.gui.label.bag.disabled"),
                ButtonState.createTranslate(0,  0, "tfstorage.gui.label.bag.enabled")));
        this.buttonList.add(new StateButton(BTN_ID_FIRST_MODES + 1, x + 21, y, 7, 7, 7, 0,
                this.guiTextureWidgets, this,
                ButtonState.createTranslate(0, 35, "tfstorage.gui.label.pickupmode.disabled"),
                ButtonState.createTranslate(0, 49, "tfstorage.gui.label.pickupmode.matching"),
                ButtonState.createTranslate(0, 0, "tfstorage.gui.label.pickupmode.all")));
        this.buttonList.add(new StateButton(BTN_ID_FIRST_MODES + 2, x + 10, y, 7, 7, 7, 0,
                this.guiTextureWidgets, this,
                ButtonState.createTranslate(0, 35, "tfstorage.gui.label.restockmode.off"),
                ButtonState.createTranslate(0, 0, "tfstorage.gui.label.restockmode.on")));
        this.buttonList.add(new StateButton(23, x + 32, y, 7, 7, 7, 0, this.guiTextureWidgets, this,
                ButtonState.createTranslate(0, 0, ShiftMode.TO_BAG.getUnlocName()),
                ButtonState.createTranslate(0, 35, ShiftMode.INV_HOTBAR.getUnlocName()),
                ButtonState.createTranslate(0, 49, ShiftMode.DOUBLE_TAP.getUnlocName())));

        if (this.bagTier == 0)
        {
            this.buttonList.add(new HoverButton(10, x + 74, y, 7, 7, 0, 7, this.guiTextureWidgets, 7, 0, BUTTON_STRINGS[6]));
            this.buttonList.add(new HoverButton(13, x + 74, y + 67, 7, 7, 0, 7,
                    this.guiTextureWidgets, 7, 0, "tfstorage.gui.label.sortitems.player"));
        }
        else
        {
            this.buttonList.add(new HoverButton(11, x - 15, y - 5, 7, 7, 0, 7, this.guiTextureWidgets, 7, 0, BUTTON_STRINGS[6]));
            this.buttonList.add(new HoverButton(10, x + 74, y, 7, 7, 0, 7, this.guiTextureWidgets, 7, 0, BUTTON_STRINGS[6]));
            this.buttonList.add(new HoverButton(12, x + 164, y - 5, 7, 7, 0, 7, this.guiTextureWidgets, 7, 0, BUTTON_STRINGS[6]));
            this.buttonList.add(new HoverButton(13, x + 74, y + 67, 7, 7, 0, 7,
                    this.guiTextureWidgets, 7, 0, "tfstorage.gui.label.sortitems.player"));

            this.buttonList.add(new StateButton(15, x - 26, y - 5, 7, 7, 7, 0, this.guiTextureWidgets, this,
                    ButtonState.createTranslate(0, 0, "tfstorage.gui.label.regionop.enabled"),
                    ButtonState.createTranslate(0, 35, "tfstorage.gui.label.regionop.disabled")));
            this.buttonList.add(new StateButton(14, x + 63, y, 7, 7, 7, 0, this.guiTextureWidgets, this,
                    ButtonState.createTranslate(0, 0, "tfstorage.gui.label.regionop.enabled"),
                    ButtonState.createTranslate(0, 35, "tfstorage.gui.label.regionop.disabled")));
            this.buttonList.add(new StateButton(16, x + 175, y - 5, 7, 7, 7, 0, this.guiTextureWidgets, this,
                    ButtonState.createTranslate(0, 0, "tfstorage.gui.label.regionop.enabled"),
                    ButtonState.createTranslate(0, 35, "tfstorage.gui.label.regionop.disabled")));
        }

        if (this.baublesLoaded)
        {
            this.buttonList.add(new HoverButton(BTN_ID_BAUBLES, this.guiLeft + 68 + this.offsetXTier, this.guiTop + 15,
                    10, 10, 200, 48, RESOURCES_BAUBLES_BUTTON, 10, 0, "Baubles"));
        }
    }

    @Override
    protected void actionPerformedWithButton(GuiButton button, int mouseButton) throws IOException
    {
        super.actionPerformed(button);

        if (button.id >= BTN_ID_FIRST_SELECT_MEMORY_CARD && button.id < (BTN_ID_FIRST_SELECT_MEMORY_CARD + this.numMemoryCardSlots))
        {
            PacketHandler.INSTANCE.sendToServer(new ActionPacket(0, new BlockPos(0, 0, 0),
                ModInfo.GUI_ID_TF_BAG, TFBag.GUI_ACTION_SELECT_MODULE, button.id - BTN_ID_FIRST_SELECT_MEMORY_CARD));
        }
        else if (button.id >= BTN_ID_FIRST_MOVE_ITEMS && button.id <= (BTN_ID_FIRST_MOVE_ITEMS + 5))
        {
            int value = button.id - BTN_ID_FIRST_MOVE_ITEMS;

            if (GuiScreen.isShiftKeyDown())
            {
                value |= 0x8000;
            }

            PacketHandler.INSTANCE.sendToServer(new ActionPacket(0, new BlockPos(0, 0, 0),
                ModInfo.GUI_ID_TF_BAG, TFBag.GUI_ACTION_MOVE_ITEMS, value));
        }
        else if (button.id >= BTN_ID_FIRST_SORT && button.id < (BTN_ID_FIRST_SORT + 4))
        {
            PacketHandler.INSTANCE.sendToServer(new ActionPacket(0, new BlockPos(0, 0, 0),
                ModInfo.GUI_ID_TF_BAG, TFBag.GUI_ACTION_SORT_ITEMS, button.id - BTN_ID_FIRST_SORT));
        }
        else if (button.id >= BTN_ID_FIRST_REGION_LOCK && button.id < (BTN_ID_FIRST_REGION_LOCK + 3))
        {
            PacketHandler.INSTANCE.sendToServer(new ActionPacket(0, new BlockPos(0, 0, 0),
                ModInfo.GUI_ID_TF_BAG, TFBag.GUI_ACTION_TOGGLE_REGION_LOCK, button.id - BTN_ID_FIRST_REGION_LOCK));
        }
        else if (button.id >= BTN_ID_FIRST_MODES && button.id < (BTN_ID_FIRST_MODES + 3))
        {
            int data = button.id - BTN_ID_FIRST_MODES;

            if (mouseButton == 1)
            {
                data |= 0x8000;
            }

            PacketHandler.INSTANCE.sendToServer(new ActionPacket(0, new BlockPos(0, 0, 0),
                ModInfo.GUI_ID_TF_BAG, TFBag.GUI_ACTION_TOGGLE_MODES, data));
        }
        else if (button.id == 23)
        {
            PacketHandler.INSTANCE.sendToServer(new ActionPacket(0, new BlockPos(0, 0, 0),
                ModInfo.GUI_ID_TF_BAG, TFBag.GUI_ACTION_TOGGLE_SHIFTCLICK, mouseButton));
        }
        else if (button.id == 100 && this.baublesLoaded)
        {
            tf.storage.event.GuiEvents.instance().setOpenedBaublesFromBag(true);
            PacketHandler.INSTANCE.sendToServer(new ActionPacket(0, new BlockPos(0, 0, 0),
                    ModInfo.GUI_ID_TF_BAG, TFBag.GUI_ACTION_OPEN_BAUBLES, 0));
        }
    }




    @Override
    public int getButtonStateIndex(int callbackId)
    {
        ItemStack stack = this.containerTFB.getContainerItem();

        if (stack.isEmpty() == false)
        {
            if (callbackId == BTN_ID_FIRST_MODES)
            {
                return NBTHelper.getBoolean(stack, "TFBag", "DisableOpen") ? 0 : 1;
            }
            else if (callbackId == BTN_ID_FIRST_MODES + 1)
            {
                PickupMode mode = TFBag.PickupMode.fromStack(stack);
                if (mode == PickupMode.ALL) { return 2; }
                else if (mode == PickupMode.MATCHING) { return 1; }
                return 0;
            }
            else if (callbackId == BTN_ID_FIRST_MODES + 2)
            {
                RestockMode mode = TFBag.RestockMode.fromStack(stack);
                return mode == RestockMode.ON ? 1 : 0;
            }
            else if (callbackId >= 14 && callbackId <= 16)
            {
                return this.isMaskActiveForSection(callbackId - 14, "LockMask") ? 1 : 0;
            }
            else if (callbackId == 23)
            {
                return MathHelper.clamp(NBTHelper.getByte(stack, "TFBag", "ShiftMode") & 0x3, 0, 2);
            }
        }

        return 0;
    }

    @Override
    public boolean isButtonEnabled(int callbackId)
    {
        return true;
    }

    private boolean isMaskActiveForSection(int section, String tagName)
    {
        int selected = this.invWithMemoryCards.getSelectedMemoryCardIndex();

        if (selected >= 0 && section >= 0 && section <= 2)
        {
            ItemStack stack = this.invWithMemoryCards.getMemoryCardInventory().getStackInSlot(selected);

            if (stack.isEmpty() == false)
            {
                long[] masks = new long[] { 0x1FFFFFFL, 0x1FFF8000000L, 0x7FFE0000000000L };
                long lockMask = NBTHelper.getLong(stack, "TFBag", tagName);

                if ((lockMask & masks[section]) == masks[section])
                {
                    return true;
                }
            }
        }

        return false;
    }
}
