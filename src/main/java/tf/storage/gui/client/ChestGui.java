package tf.storage.gui.client;

import java.io.IOException;
import net.minecraft.client.gui.GuiButton;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.resources.I18n;
import net.minecraft.inventory.Slot;
import net.minecraft.util.math.MathHelper;
import org.lwjgl.input.Mouse;
import tf.storage.gui.client.base.LargeStackGui;
import tf.storage.gui.component.HoverButton;
import tf.storage.gui.component.IconButton;
import tf.storage.inventory.container.ChestContainer;
import tf.storage.network.PacketHandler;
import tf.storage.network.packet.ActionPacket;
import tf.storage.core.ModInfo;
import tf.storage.tile.TileChest;

public class ChestGui extends LargeStackGui
{
    public static final int BTN_ID_SORT_CHEST       = 10;
    public static final int BTN_ID_SORT_PLAYER      = 11;

    private static final String[] BUTTON_STRINGS = new String[] {
            "tfstorage.gui.label.moveallitemsexcepthotbar",
            "tfstorage.gui.label.movematchingitemsexcepthotbar",
            "tfstorage.gui.label.leaveonefilledstack",
            "tfstorage.gui.label.fillstacks",
            "tfstorage.gui.label.movematchingitems",
            "tfstorage.gui.label.moveallitems",
            "tfstorage.gui.label.sortitems"
    };

    private final TileChest tetfc;

    private float currentScroll;
    private boolean isScrolling;
    private boolean needsScrollbar;
    private int totalRows;
    private static final int visibleRows = 5;
    private boolean cleanedButtons = false;

    public ChestGui(ChestContainer container, TileChest te)
    {
        super(container, 176, 256, "gui.container.tf_chest." + Math.min(te.getStorageTier(), 1));
        this.tetfc = te;
        this.scaledStackSizeTextInventories.add(container.inventory);

        int tier = te.getStorageTier();
        if (tier >= 2) {
            this.needsScrollbar = true;
            this.totalRows = (tier == 2) ? 8 : 12;
        } else {
            this.needsScrollbar = false;
            this.totalRows = (tier == 0) ? 3 : 5;
        }
    }

    @Override
    public void initGui()
    {
        this.cleanedButtons = false;
        this.setGuiSize();

        super.initGui();

        this.createButtons();

        if (this.needsScrollbar) {
            this.updateSlotPositions();
        }
    }

    @Override
    public void drawScreen(int mouseX, int mouseY, float gameTicks)
    {
        if (!this.cleanedButtons)
        {
            this.buttonList.removeIf(b -> b.id >= 200);
            this.cleanedButtons = true;
        }
        
        super.drawScreen(mouseX, mouseY, gameTicks);
    }

    protected void setGuiSize()
    {
        int chestTier = this.tetfc.getStorageTier();
        switch (chestTier)
        {
            case 0:
                this.xSize = 176;
                this.ySize = 186;
                break;
            case 1:
                this.xSize = 176;
                this.ySize = 222;
                break;
            case 2:
            case 3:
                this.xSize = 187;
                this.ySize = 222;
                break;
            default:
                this.xSize = 176;
                this.ySize = 222;
        }
    }

    private static final int SCROLLBAR_BG_X = 171;
    private static final int SCROLLBAR_BG_Y = 0;
    private static final int SCROLLBAR_BG_WIDTH = 17;
    private static final int SCROLLBAR_BG_HEIGHT = 132;
    private static final int SCROLLBAR_BG_U = 0;
    private static final int SCROLLBAR_BG_V = 64;
    
    private static final int TRACK_X_OFFSET = 2;
    private static final int TRACK_Y_OFFSET = 37;
    private static final int TRACK_WIDTH = 7;
    private static final int TRACK_HEIGHT = 88;
    
    private static final int HANDLE_WIDTH = 7;
    private static final int HANDLE_MIN_HEIGHT = 15;
    private static final int HANDLE_U = 17;
    private static final int HANDLE_V = 64;
    private static final int HANDLE_TOP = 3;
    private static final int HANDLE_BOTTOM = 3;

    private int getHandleHeight() {
        if (!this.needsScrollbar) {
            return 0;
        }
        float handleProportion = (float)this.visibleRows / (float)this.totalRows;
        int handleHeight = (int)(TRACK_HEIGHT * handleProportion);
        return MathHelper.clamp(handleHeight, HANDLE_MIN_HEIGHT, TRACK_HEIGHT);
    }

    @Override
    public void handleMouseInput() throws IOException {
        super.handleMouseInput();
        if (this.needsScrollbar) {
            int dWheel = Mouse.getEventDWheel();
            if (dWheel != 0) {
                int scrollableRows = this.totalRows - this.visibleRows;
                if (scrollableRows > 0) {
                    int scrollDirection = dWheel > 0 ? -1 : 1;
                    float scrollStep = 1.0f / (float)scrollableRows;
                    this.currentScroll = MathHelper.clamp(this.currentScroll + (scrollDirection * scrollStep), 0.0f, 1.0f);
                    this.updateSlotPositions();
                }
            }
        }
    }

    @Override
    protected void mouseClicked(int mouseX, int mouseY, int mouseButton) throws IOException {
        super.mouseClicked(mouseX, mouseY, mouseButton);
        if (this.needsScrollbar && mouseButton == 0) {
            int trackX = this.guiLeft + SCROLLBAR_BG_X + TRACK_X_OFFSET;
            int trackY = this.guiTop + SCROLLBAR_BG_Y + TRACK_Y_OFFSET;

            if (mouseX >= trackX && mouseX < trackX + TRACK_WIDTH && mouseY >= trackY && mouseY < trackY + TRACK_HEIGHT) {
                this.isScrolling = true;
                int handleHeight = this.getHandleHeight();
                float draggableTrackHeight = TRACK_HEIGHT - handleHeight;
                if (draggableTrackHeight > 0) {
                    this.currentScroll = MathHelper.clamp(((float)(mouseY - trackY) - (float)handleHeight / 2.0F) / draggableTrackHeight, 0.0F, 1.0F);
                } else {
                    this.currentScroll = 0.0f;
                }
                updateSlotPositions();
            }
        }
    }
    
    @Override
    protected void mouseReleased(int mouseX, int mouseY, int state) {
        super.mouseReleased(mouseX, mouseY, state);
        if (this.isScrolling) {
            this.isScrolling = false;
        }
    }

    @Override
    protected void mouseClickMove(int mouseX, int mouseY, int clickedMouseButton, long timeSinceLastClick) {
        super.mouseClickMove(mouseX, mouseY, clickedMouseButton, timeSinceLastClick);
        if (this.isScrolling) {
            int trackY = this.guiTop + SCROLLBAR_BG_Y + TRACK_Y_OFFSET;
            int handleHeight = this.getHandleHeight();
            float draggableTrackHeight = TRACK_HEIGHT - handleHeight;
            if (draggableTrackHeight > 0) {
                this.currentScroll = MathHelper.clamp(((float)(mouseY - trackY) - (float)handleHeight / 2.0F) / draggableTrackHeight, 0.0F, 1.0F);
            } else {
                this.currentScroll = 0.0f;
            }
            updateSlotPositions();
        }
    }

    private void updateSlotPositions() {
        if (!this.needsScrollbar) return;

        int scrollableRows = this.totalRows - this.visibleRows;
        int firstVisibleRow = (int)(this.currentScroll * scrollableRows + 0.5f);
        
        int invSize = this.totalRows * 9;
        for (int i = 0; i < invSize; ++i) {
            Slot slot = this.inventorySlots.getSlot(i);
            int row = i / 9;
            int col = i % 9;

            if (row >= firstVisibleRow && row < firstVisibleRow + this.visibleRows) {
                slot.yPos = 37 + (row - firstVisibleRow) * 18;
            } else {
                slot.yPos = -2000;
            }
            slot.xPos = 8 + col * 18;
        }
    }

    @Override
    protected void drawGuiContainerForegroundLayer(int mouseX, int mouseY)
    {
        int y = 6; // 统一Y坐标
        String str = this.tetfc.hasCustomName() ? this.tetfc.getName() : I18n.format(this.tetfc.getName());
        this.fontRenderer.drawString(str, 8, y, 0x404040);
    }

    @Override
    protected void drawGuiContainerBackgroundLayer(float gameTicks, int mouseX, int mouseY)
    {
        super.drawGuiContainerBackgroundLayer(gameTicks, mouseX, mouseY);

        this.bindTexture(this.guiTextureWidgets);

        if (this.needsScrollbar) {
            int bgX = this.guiLeft + SCROLLBAR_BG_X;
            int bgY = this.guiTop + SCROLLBAR_BG_Y;
            this.drawTexturedModalRect(bgX, bgY, SCROLLBAR_BG_U, SCROLLBAR_BG_V, SCROLLBAR_BG_WIDTH, SCROLLBAR_BG_HEIGHT);

            int handleHeight = this.getHandleHeight();
            float draggableTrackHeight = TRACK_HEIGHT - handleHeight;
            int trackX = bgX + TRACK_X_OFFSET;
            int trackY = bgY + TRACK_Y_OFFSET;
            int handleY = trackY + (int)(draggableTrackHeight * this.currentScroll);

            GlStateManager.color(1.0f, 1.0f, 1.0f, 1.0f);
            
            this.drawTexturedModalRect(trackX, handleY, HANDLE_U, HANDLE_V, HANDLE_WIDTH, HANDLE_TOP);
            
            int middleHeight = handleHeight - HANDLE_TOP - HANDLE_BOTTOM;
            if (middleHeight > 0) {
                int middleU = HANDLE_U;
                int middleV = HANDLE_V + HANDLE_TOP;
                for (int i = 0; i < middleHeight; i++) {
                    this.drawTexturedModalRect(trackX, handleY + HANDLE_TOP + i, middleU, middleV, HANDLE_WIDTH, 1);
                }
            }
            
            this.drawTexturedModalRect(trackX, handleY + handleHeight - HANDLE_BOTTOM, HANDLE_U, HANDLE_V + HANDLE_MIN_HEIGHT - HANDLE_BOTTOM, HANDLE_WIDTH, HANDLE_BOTTOM);
        }

        Slot slot;
        GuiButton button = this.buttonList.get(this.tetfc.getSelectedMemoryCardIndex());
        int x = button.x;
        int y = button.y;

        this.drawTexturedModalRect(x - 1, y - 1, 56, 48, 8, 8);

        int tier = this.tetfc.getStorageTier();
        int invSize = (tier == 0) ? 27 : (tier == 1) ? 45 : (tier == 2) ? 72 : 108;

        slot = this.inventorySlots.getSlot(invSize + this.tetfc.getSelectedMemoryCardIndex());
        this.drawTexturedModalRect(this.guiLeft + slot.xPos - 1, this.guiTop + slot.yPos - 1, 46, 18, 18, 18);

        if (this.tetfc.isInventoryAccessible(this.container.getPlayer()) == false)
        {
            if (this.needsScrollbar) {
                int scrollableRows = this.totalRows - this.visibleRows;
                int firstVisibleRow = (int)(this.currentScroll * scrollableRows + 0.5f);
                for (int i = 0; i < this.visibleRows * 9; i++) {
                    int actualSlot = firstVisibleRow * 9 + i;
                    if (actualSlot < invSize) {
                        slot = this.inventorySlots.getSlot(actualSlot);
                        if (slot.yPos > 0) {
                            x = this.guiLeft + slot.xPos - 1;
                            y = this.guiTop + slot.yPos - 1;
                            this.drawTexturedModalRect(x, y, 46, 0, 18, 18);
                        }
                    }
                }
            } else {
                for (int i = 0; i < invSize; i++)
                {
                    slot = this.inventorySlots.getSlot(i);
                    x = this.guiLeft + slot.xPos - 1;
                    y = this.guiTop + slot.yPos - 1;
                    this.drawTexturedModalRect(x, y, 46, 0, 18, 18);
                }
            }
        }

        // int mask = this.tetfc.getLockedMemoryCards(); // 已移除

        for (int i = 0; i < 4; i++)
        {
            if (this.tetfc.getMemoryCardInventory().getStackInSlot(i).isEmpty())
            {
                slot = this.inventorySlots.getSlot(invSize + i);
                this.drawTexturedModalRect(this.guiLeft + slot.xPos + 1, this.guiTop + slot.yPos + 1, 32, 48, 16, 16);
            }

            // if ((mask & (1 << i)) != 0) // 已移除
            // {
            //     button = this.buttonList.get(i);
            //     // Draw lock icon logic was here? No, button is selected logic? 
            //     // The original code assigned 'button' but didn't seem to draw anything extra on it here?
            //     // Ah, GuiButtonIcon might render differently if it knows it's locked? 
            //     // But we are removing the feature anyway.
            // }
        }
    }

    protected void createButtons()
    {
        this.buttonList.clear();

        int x = (this.width - this.xSize) / 2;
        int y = (this.height - this.ySize) / 2;
        int tier = this.tetfc.getStorageTier();

        for (int i = 0; i < 4; i++)
        {
            this.buttonList.add(new IconButton(i, x + 103 + i * 18, y + 27, 6, 6, 14, 21, this.guiTextureWidgets, 6, 0));
        }

        int yOff;
        if (tier == 0) {
            yOff = 78;
        } else {
            yOff = 114;
        }
        
        int xOffs[] = new int[] { 7, 23, 40, 126, 143, 160 };
        int[] quickButtonUVs = new int[] { 0, 56, 0, 14, 14, 0, 14, 7, 0, 21, 14, 14 };

        for (int i = 0; i < 6; i++)
        {
            int u = quickButtonUVs[i * 2];
            int v = quickButtonUVs[i * 2 + 1];
            this.buttonList.add(new HoverButton(i + 4, x + xOffs[i] + 1, y + yOff + 15, 7, 7, u, v,
                    this.guiTextureWidgets, 7, 0, BUTTON_STRINGS[i]));
        }

        // Add the sort button for the TF Chest inventory
        this.buttonList.add(new HoverButton(BTN_ID_SORT_CHEST, x + 9, y + 26, 7, 7, 0, 7, this.guiTextureWidgets, 7, 0, BUTTON_STRINGS[6]));

        this.buttonList.add(new HoverButton(BTN_ID_SORT_PLAYER, x + 84, y + yOff + 15, 7, 7, 0, 7,
                this.guiTextureWidgets, 7, 0, "tfstorage.gui.label.sortitems.player"));
    }

    protected void updateActivePotionEffects()
    {
        // 禁用 TF Chest 的药水效果和 GUI 偏移
    }

    protected void drawActivePotionEffects()
    {
        // 禁用 TF Chest 的药水效果渲染
    }

    @Override
    protected void actionPerformedWithButton(GuiButton button, int mouseButton) throws IOException
    {
        if (button.id >= 0 && button.id < 4)
        {
            if (mouseButton == 0)
            {
                PacketHandler.INSTANCE.sendToServer(
                    new ActionPacket(this.tetfc.getWorld().provider.getDimension(), this.tetfc.getPos(),
                        ModInfo.GUI_ID_TILE_ENTITY_GENERIC, TileChest.GUI_ACTION_SELECT_MEMORY_CARD, button.id));
            }
            // else if (mouseButton == 1) // 已移除右键锁定功能
            // {
            //     PacketHandler.INSTANCE.sendToServer(
            //         new ActionPacket(this.tetfc.getWorld().provider.getDimension(), this.tetfc.getPos(),
            //             ModInfo.GUI_ID_TILE_ENTITY_GENERIC, TileChest.GUI_ACTION_LOCK_MEMORY_CARD, button.id));
            // }
        }
        else if (button.id >= 4 && button.id < 10)
        {
            PacketHandler.INSTANCE.sendToServer(
                new ActionPacket(this.tetfc.getWorld().provider.getDimension(), this.tetfc.getPos(),
                    ModInfo.GUI_ID_TILE_ENTITY_GENERIC, TileChest.GUI_ACTION_MOVE_ITEMS, button.id - 4));
        }
        else if (button.id >= BTN_ID_SORT_CHEST && button.id <= BTN_ID_SORT_PLAYER)
        {
            PacketHandler.INSTANCE.sendToServer(new ActionPacket(this.tetfc.getWorld().provider.getDimension(), this.tetfc.getPos(),
                ModInfo.GUI_ID_TILE_ENTITY_GENERIC, TileChest.GUI_ACTION_SORT_ITEMS, button.id - BTN_ID_SORT_CHEST));
        }
    }
}
