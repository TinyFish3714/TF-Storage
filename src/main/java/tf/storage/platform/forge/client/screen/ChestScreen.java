package tf.storage.platform.forge.client.screen;

import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.ChatFormatting;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.world.inventory.Slot;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.FormattedCharSequence;
import net.minecraftforge.items.SlotItemHandler;
import tf.storage.core.inventory.IItemHandlerSize;
import tf.storage.core.util.TextFormatter;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;
import tf.storage.platform.forge.TFStorageMod;
import tf.storage.platform.forge.block.TFChestBlockEntity;
import tf.storage.platform.forge.menu.ChestMenu;
import tf.storage.platform.forge.client.screen.GuiButtonIds.Chest;
import tf.storage.platform.forge.network.GuiActions;
import tf.storage.platform.forge.network.ModNetworking;
import tf.storage.platform.forge.network.packets.ActionPacket;

/**
 * Screen for TF Chest.
 */
public class ChestScreen extends AbstractContainerScreen<ChestMenu> {

    private static final ResourceLocation TEXTURE_0 = ResourceLocation.parse(TFStorageMod.MOD_ID + ":textures/gui/gui.container.tf_chest.0.png");
    private static final ResourceLocation TEXTURE_1 = ResourceLocation.parse(TFStorageMod.MOD_ID + ":textures/gui/gui.container.tf_chest.1.png");

    private static final ResourceLocation WIDGETS = ResourceLocation.parse(TFStorageMod.MOD_ID + ":textures/gui/gui.widgets.png");
    private static final int WIDGETS_SIZE = 256;

    private static final String[] BUTTON_STRINGS = new String[] {
        "tfstorage.gui.label.moveallitemsexcepthotbar",
        "tfstorage.gui.label.movematchingitemsexcepthotbar",
        "tfstorage.gui.label.leaveonefilledstack",
        "tfstorage.gui.label.fillstacks",
        "tfstorage.gui.label.movematchingitems",
        "tfstorage.gui.label.moveallitems",
        "tfstorage.gui.label.sortitems"
    };

    private final List<IconButton> buttons = new ArrayList<>();
    private final List<FormattedCharSequence> tooltipLines = new ArrayList<>();
    private int lastGuiLeft;
    private int lastGuiTop;
    private int totalRows;
    private boolean needsScrollbar;
    private float currentScroll;
    private boolean isScrolling;
    private static final int visibleRows = 5;

    private int lastTier = -1;
    private static Field slotXField;
    private static Field slotYField;

    static {
        initSlotFields();
    }

    public ChestScreen(ChestMenu menu, Inventory playerInventory, Component title) {
        super(menu, playerInventory, title);
        
        switch (menu.getChestTier()) {
            case 0 -> { this.imageWidth = 176; this.imageHeight = 186; }
            case 1 -> { this.imageWidth = 176; this.imageHeight = 222; }
            default -> { this.imageWidth = 187; this.imageHeight = 222; }
        }

        this.inventoryLabelY = this.imageHeight - 94;

        int tier = menu.getChestTier();
        if (tier >= 2) {
            this.needsScrollbar = true;
            this.totalRows = (tier == 2) ? 8 : 12;
        } else {
            this.needsScrollbar = false;
            this.totalRows = (tier == 0) ? 3 : 5;
        }
    }

    private void refreshLayoutFromInventory() {
        int tier = this.menu.getChestTier();
        this.totalRows = (tier == 0) ? 3 : (tier == 1) ? 5 : (tier == 2) ? 8 : 12;
        this.needsScrollbar = this.totalRows > visibleRows;

        if (this.totalRows <= 3) {
            this.imageWidth = 176;
            this.imageHeight = 186;
        } else if (this.totalRows <= 5) {
            this.imageWidth = 176;
            this.imageHeight = 222;
        } else {
            this.imageWidth = 187;
            this.imageHeight = 222;
        }
        this.inventoryLabelY = this.imageHeight - 94;
    }

    @Override
    protected void init() {
        refreshLayoutFromInventory();
        super.init();
        updatePositions();
    }

    @Override
    public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        this.renderBackground(guiGraphics);
        super.render(guiGraphics, mouseX, mouseY, partialTick);
        this.renderTooltip(guiGraphics, mouseX, mouseY);
        renderLargeStackOverlays(guiGraphics);

        if (this.menu.getChestTier() != this.lastTier) {
            refreshLayoutFromInventory();
            super.init();
            updatePositions();
            this.lastTier = this.menu.getChestTier();
        }
        if (needsPositionUpdate()) {
            updatePositions();
        }

        renderButtonTooltips(guiGraphics, mouseX, mouseY);
    }

    @Override
    protected void renderBg(GuiGraphics guiGraphics, float partialTick, int mouseX, int mouseY) {
        RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
        
        ResourceLocation texture = this.totalRows <= 3 ? TEXTURE_0 : TEXTURE_1;
        
        int x = (this.width - this.imageWidth) / 2;
        int y = (this.height - this.imageHeight) / 2;
        
        guiGraphics.blit(texture, x, y, 0, 0, this.imageWidth, this.imageHeight);

        if (this.needsScrollbar) {
            int bgX = this.leftPos + 171;
            int bgY = this.topPos + 0;
            guiGraphics.blit(WIDGETS, bgX, bgY, 0, 64, 17, 132, WIDGETS_SIZE, WIDGETS_SIZE);

            int trackX = bgX + 2;
            int trackY = bgY + 37;
            int handleHeight = getHandleHeight();
            float draggableTrackHeight = 88 - handleHeight;
            int handleY = trackY + (int)(draggableTrackHeight * this.currentScroll);

            guiGraphics.blit(WIDGETS, trackX, handleY, 17, 64, 7, 3, WIDGETS_SIZE, WIDGETS_SIZE);

            int middleHeight = handleHeight - 3 - 3;
            if (middleHeight > 0) {
                for (int i = 0; i < middleHeight; i++) {
                    guiGraphics.blit(WIDGETS, trackX, handleY + 3 + i, 17, 67, 7, 1, WIDGETS_SIZE, WIDGETS_SIZE);
                }
            }

            guiGraphics.blit(WIDGETS, trackX, handleY + handleHeight - 3, 17, 76, 7, 3, WIDGETS_SIZE, WIDGETS_SIZE);
        }

        int invSize = (this.menu.getChestTier() == 0) ? 27 : (this.menu.getChestTier() == 1) ? 45 : (this.menu.getChestTier() == 2) ? 72 : 108;
        int selected = this.menu.getBlockEntity().getSelectedMemoryCardIndex();

        if (selected >= 0 && selected < 4) {
            int buttonX = this.leftPos + 103 + selected * 18;
            int buttonY = this.topPos + 27;
            guiGraphics.blit(WIDGETS, buttonX - 1, buttonY - 1, 56, 48, 8, 8, WIDGETS_SIZE, WIDGETS_SIZE);

            int slotIndex = invSize + selected;
            if (slotIndex < this.menu.slots.size()) {
                var slot = this.menu.getSlot(slotIndex);
                guiGraphics.blit(WIDGETS, this.leftPos + slot.x - 1, this.topPos + slot.y - 1, 46, 18, 18, 18, WIDGETS_SIZE, WIDGETS_SIZE);
            }
        }

        if (!this.menu.getBlockEntity().isInventoryAccessible(this.minecraft.player)) {
            if (this.needsScrollbar) {
                int scrollableRows = this.totalRows - visibleRows;
                int firstVisibleRow = (int)(this.currentScroll * scrollableRows + 0.5f);
                for (int i = 0; i < visibleRows * 9; i++) {
                    int actualSlot = firstVisibleRow * 9 + i;
                    if (actualSlot < invSize) {
                        var slot = this.menu.getSlot(actualSlot);
                        if (slot.y > 0) {
                            guiGraphics.blit(WIDGETS, this.leftPos + slot.x - 1, this.topPos + slot.y - 1, 46, 0, 18, 18, WIDGETS_SIZE, WIDGETS_SIZE);
                        }
                    }
                }
            } else {
                for (int i = 0; i < invSize; i++) {
                    var slot = this.menu.getSlot(i);
                    guiGraphics.blit(WIDGETS, this.leftPos + slot.x - 1, this.topPos + slot.y - 1, 46, 0, 18, 18, WIDGETS_SIZE, WIDGETS_SIZE);
                }
            }
        }

        for (int i = 0; i < 4; i++) {
            if (this.menu.getBlockEntity().getMemoryCardInventory().getStackInSlot(i).isEmpty()) {
                int slotIndex = invSize + i;
                var slot = this.menu.getSlot(slotIndex);
                guiGraphics.blit(WIDGETS, this.leftPos + slot.x + 1, this.topPos + slot.y + 1, 32, 48, 16, 16, WIDGETS_SIZE, WIDGETS_SIZE);
            }
        }
    }

    private int getHandleHeight() {
        if (!this.needsScrollbar) {
            return 0;
        }
        float handleProportion = (float)visibleRows / (float)totalRows;
        int handleHeight = (int)(88 * handleProportion);
        return Math.max(15, Math.min(handleHeight, 88));
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (handleButtonClick(mouseX, mouseY, button)) {
            return true;
        }
        if (this.needsScrollbar && button == 0) {
            int trackX = this.leftPos + 171 + 2;
            int trackY = this.topPos + 0 + 37;
            if (mouseX >= trackX && mouseX < trackX + 7 && mouseY >= trackY && mouseY < trackY + 88) {
                this.isScrolling = true;
                int handleHeight = getHandleHeight();
                float draggableTrackHeight = 88 - handleHeight;
                if (draggableTrackHeight > 0) {
                    this.currentScroll = Math.max(0.0f, Math.min(1.0f,
                        ((float)(mouseY - trackY) - (float)handleHeight / 2.0f) / draggableTrackHeight));
                } else {
                    this.currentScroll = 0.0f;
                }
                updateSlotPositions();
                return true;
            }
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    public boolean mouseReleased(double mouseX, double mouseY, int button) {
        if (this.isScrolling) {
            this.isScrolling = false;
        }
        return super.mouseReleased(mouseX, mouseY, button);
    }


    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double delta) {
        if (this.needsScrollbar) {
            int scrollableRows = this.totalRows - visibleRows;
            if (scrollableRows > 0) {
                int direction = delta > 0 ? -1 : 1;
                float step = 1.0f / (float) scrollableRows;
                this.currentScroll = Math.max(0.0f, Math.min(1.0f, this.currentScroll + direction * step));
                updateSlotPositions();
                return true;
            }
        }
        return super.mouseScrolled(mouseX, mouseY, delta);
    }

    @Override
    public boolean mouseDragged(double mouseX, double mouseY, int button, double dragX, double dragY) {
        if (this.isScrolling) {
            int trackY = this.topPos + 0 + 37;
            int handleHeight = getHandleHeight();
            float draggableTrackHeight = 88 - handleHeight;
            if (draggableTrackHeight > 0) {
                this.currentScroll = Math.max(0.0f, Math.min(1.0f,
                    ((float)(mouseY - trackY) - (float)handleHeight / 2.0f) / draggableTrackHeight));
            } else {
                this.currentScroll = 0.0f;
            }
            updateSlotPositions();
            return true;
        }
        return super.mouseDragged(mouseX, mouseY, button, dragX, dragY);
    }

    private void updateSlotPositions() {
        if (!this.needsScrollbar) {
            return;
        }
        int scrollableRows = this.totalRows - visibleRows;
        int firstVisibleRow = (int)(this.currentScroll * scrollableRows + 0.5f);
        int invSize = this.totalRows * 9;
        for (int i = 0; i < invSize; ++i) {
            Slot slot = this.menu.getSlot(i);
            int row = i / 9;
            int col = i % 9;
            if (row >= firstVisibleRow && row < firstVisibleRow + visibleRows) {
                setSlotPos(slot, 8 + col * 18, 37 + (row - firstVisibleRow) * 18);
            } else {
                setSlotPos(slot, 8 + col * 18, -2000);
            }
        }
    }

    private static void initSlotFields() {
        try {
            Field x = Slot.class.getDeclaredField("f_40220_");
            Field y = Slot.class.getDeclaredField("f_40221_");
            x.setAccessible(true);
            y.setAccessible(true);
            slotXField = x;
            slotYField = y;
            return;
        } catch (NoSuchFieldException ignored) {
        }
        try {
            Field x = Slot.class.getDeclaredField("x");
            Field y = Slot.class.getDeclaredField("y");
            x.setAccessible(true);
            y.setAccessible(true);
            slotXField = x;
            slotYField = y;
            return;
        } catch (NoSuchFieldException ignored) {
        }
        try {
            Field x = Slot.class.getDeclaredField("xPos");
            Field y = Slot.class.getDeclaredField("yPos");
            x.setAccessible(true);
            y.setAccessible(true);
            slotXField = x;
            slotYField = y;
        } catch (NoSuchFieldException ignored) {
            slotXField = null;
            slotYField = null;
        }
    }


    private static void setSlotPos(Slot slot, int x, int y) {
        if (slotXField == null || slotYField == null) {
            return;
        }
        try {
            slotXField.setInt(slot, x);
            slotYField.setInt(slot, y);
        } catch (IllegalAccessException ignored) {
        }
    }

    private void updatePositions() {
        createButtons();
        if (this.needsScrollbar) {
            updateSlotPositions();
        }
        this.lastGuiLeft = this.leftPos;
        this.lastGuiTop = this.topPos;
    }

    private boolean needsPositionUpdate() {
        return this.lastGuiLeft != this.leftPos || this.lastGuiTop != this.topPos;
    }

    private void createButtons() {
        this.clearWidgets();
        this.buttons.clear();

        int x = this.leftPos;
        int y = this.topPos;
        int tier = this.menu.getChestTier();

        for (int i = 0; i < Chest.SELECT_MEMORY_CARD_COUNT; i++) {
            this.buttons.add(new IconButton(Chest.SELECT_MEMORY_CARD_START + i, x + 103 + i * 18, y + 27, 6, 6, 14, 21, WIDGETS, 6, 0));
        }

        int yOff = (tier == 0) ? 78 : 114;
        int[] xOffs = new int[] { 7, 23, 40, 126, 143, 160 };
        int[] quickButtonUVs = new int[] { 0, 56, 0, 14, 14, 0, 14, 7, 0, 21, 14, 14 };

        for (int i = 0; i < 6; i++) {
            int u = quickButtonUVs[i * 2];
            int v = quickButtonUVs[i * 2 + 1];
            this.buttons.add(new HoverButton(Chest.MOVE_ITEMS_START + i, x + xOffs[i] + 1, y + yOff + 15, 7, 7, u, v, WIDGETS, 7, 0, BUTTON_STRINGS[i]));
        }

        this.buttons.add(new HoverButton(Chest.SORT_START, x + 9, y + 26, 7, 7, 0, 7, WIDGETS, 7, 0, BUTTON_STRINGS[6]));
        this.buttons.add(new HoverButton(Chest.SORT_START + 1, x + 84, y + yOff + 15, 7, 7, 0, 7, WIDGETS, 7, 0, "tfstorage.gui.label.sortitems.player"));

        for (IconButton button : this.buttons) {
            this.addRenderableWidget(button);
        }
    }

    private boolean handleButtonClick(double mouseX, double mouseY, int button) {
        for (IconButton btn : this.buttons) {
            if (btn.isMouseOver(mouseX, mouseY) && btn.mouseClicked(mouseX, mouseY, button)) {
                return true;
            }
        }
        return false;
    }

    private void renderButtonTooltips(GuiGraphics guiGraphics, int mouseX, int mouseY) {
        for (IconButton button : this.buttons) {
            if (button.isMouseOver(mouseX, mouseY) && !button.getHoverText().isEmpty()) {
                tooltipLines.clear();
                for (Component line : button.getHoverText()) {
                    tooltipLines.add(line.getVisualOrderText());
                }
                guiGraphics.renderTooltip(this.font, tooltipLines, mouseX, mouseY);
                return;
            }
        }
    }

    private void onButtonPressed(int id, int mouseButton) {
        if (id >= Chest.SELECT_MEMORY_CARD_START && id < Chest.SELECT_MEMORY_CARD_START + Chest.SELECT_MEMORY_CARD_COUNT) {
            ModNetworking.sendToServer(ActionPacket.forChest(GuiActions.Chest.SELECT_MEMORY_CARD, id - Chest.SELECT_MEMORY_CARD_START, this.menu.getBlockEntity().getBlockPos()));
            // Client-side prediction for smoother UI
            this.menu.getBlockEntity().setSelectedMemoryCard(id - Chest.SELECT_MEMORY_CARD_START);
        } else if (id >= Chest.MOVE_ITEMS_START && id < Chest.MOVE_ITEMS_END_EXCLUSIVE) {
            ModNetworking.sendToServer(ActionPacket.forChest(GuiActions.Chest.MOVE_ITEMS, id - Chest.MOVE_ITEMS_START, this.menu.getBlockEntity().getBlockPos()));
        } else if (id >= Chest.SORT_START && id <= Chest.SORT_END_INCLUSIVE) {
            ModNetworking.sendToServer(ActionPacket.forChest(GuiActions.Chest.SORT_ITEMS, id - Chest.SORT_START, this.menu.getBlockEntity().getBlockPos()));
        }
    }

    private class IconButton extends AbstractWidget {
        protected final int id;
        protected final ResourceLocation texture;
        protected int u;
        protected int v;
        protected int hoverOffsetU;
        protected int hoverOffsetV;

        private final List<Component> hoverText = new ArrayList<>();

        public IconButton(int id, int x, int y, int w, int h, int u, int v, ResourceLocation texture, int hoverOffsetU, int hoverOffsetV) {
            super(x, y, w, h, Component.empty());
            this.id = id;
            this.u = u;
            this.v = v;
            this.texture = texture;
            this.hoverOffsetU = hoverOffsetU;
            this.hoverOffsetV = hoverOffsetV;
        }

        protected int getU() { return this.u; }
        protected int getV() { return this.v; }

        @Override
        public boolean mouseClicked(double mouseX, double mouseY, int button) {
            if (this.visible && this.isMouseOver(mouseX, mouseY)) {
                onButtonPressed(this.id, button);
                return true;
            }
            return false;
        }

        @Override
        protected void renderWidget(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
            if (!this.visible) {
                return;
            }
            boolean hovered = mouseX >= this.getX() && mouseY >= this.getY() &&
                mouseX < this.getX() + this.width && mouseY < this.getY() + this.height;
            int state = hovered ? 1 : 0;
            guiGraphics.blit(this.texture, this.getX(), this.getY(), this.getU() + state * this.hoverOffsetU,
                this.getV() + state * this.hoverOffsetV, this.width, this.height, WIDGETS_SIZE, WIDGETS_SIZE);
        }

        @Override
        protected void updateWidgetNarration(net.minecraft.client.gui.narration.NarrationElementOutput narration) {
        }

        public List<Component> getHoverText() {
            return hoverText;
        }
    }

    private class HoverButton extends IconButton {
        public HoverButton(int id, int x, int y, int w, int h, int u, int v, ResourceLocation texture,
                           int hoverOffsetU, int hoverOffsetV, String... hoverStrings) {
            super(id, x, y, w, h, u, v, texture, hoverOffsetU, hoverOffsetV);
            for (String key : hoverStrings) {
                this.getHoverText().add(Component.translatable(key));
            }
        }
    }

    private void renderLargeStackOverlays(GuiGraphics guiGraphics) {
        for (Slot slot : this.menu.slots) {
            ItemStack stack = slot.getItem();
            if (stack.isEmpty()) {
                continue;
            }
            if (slot instanceof SlotItemHandler handlerSlot && handlerSlot.getItemHandler() instanceof IItemHandlerSize) {
                if (stack.getCount() != 1) {
                    renderLargeStackItemOverlay(guiGraphics, stack, this.leftPos + slot.x, this.topPos + slot.y);
                }
            }
        }
    }

    private void renderLargeStackItemOverlay(GuiGraphics guiGraphics, ItemStack stack, int xPosition, int yPosition) {
        String str = TextFormatter.getStackSizeString(stack, 4);
        if (stack.getCount() < 1) {
            str = ChatFormatting.RED + str;
        }

        guiGraphics.pose().pushPose();
        guiGraphics.pose().translate(xPosition, yPosition, 200.0f);
        guiGraphics.pose().scale(0.5f, 0.5f, 0.5f);
        int textWidth = this.font.width(str);
        guiGraphics.drawString(this.font, str, (31 - textWidth), 23, 0xFFFFFF, true);
        guiGraphics.pose().popPose();
    }

}
