package tf.storage.platform.forge.client.screen;

import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.ChatFormatting;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.gui.screens.inventory.InventoryScreen;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.client.extensions.common.IClientMobEffectExtensions;
import net.minecraftforge.items.SlotItemHandler;
import tf.storage.core.inventory.IItemHandlerSize;
import tf.storage.core.util.TextFormatter;
import tf.storage.core.util.CardHelper;
import tf.storage.core.util.NBTHelper;
import tf.storage.platform.forge.TFStorageMod;
import tf.storage.platform.forge.inventory.handler.ItemHandler;
import tf.storage.platform.forge.inventory.handler.ModularHandler;
import tf.storage.platform.forge.item.TFBagItem;
import tf.storage.platform.forge.menu.BagMenu;
import tf.storage.platform.forge.client.screen.GuiButtonIds.Bag;
import tf.storage.platform.forge.network.GuiActions;
import tf.storage.platform.forge.network.ModNetworking;
import tf.storage.platform.forge.network.packets.ActionPacket;
import tf.storage.platform.forge.compat.CuriosCompat;
import tf.storage.platform.forge.event.GuiEvents;

public class BagScreen extends AbstractContainerScreen<BagMenu> implements ButtonStateCallback {
    public static final int BTN_ID_FIRST_SELECT_MEMORY_CARD = Bag.SELECT_MEMORY_CARD_START;
    public static final int BTN_ID_FIRST_MOVE_ITEMS = Bag.MOVE_ITEMS_START;
    public static final int BTN_ID_FIRST_SORT = Bag.SORT_START;
    public static final int BTN_ID_FIRST_REGION_LOCK = Bag.REGION_LOCK_START;
    public static final int BTN_ID_FIRST_MODES = Bag.MODES_START;
    public static final int BTN_ID_BAUBLES = Bag.CURIOS;
    private static final String[] BUTTON_STRINGS = new String[] {
        "tfstorage.gui.label.moveallitemsexcepthotbar",
        "tfstorage.gui.label.movematchingitemsexcepthotbar",
        "tfstorage.gui.label.leaveonefilledstack",
        "tfstorage.gui.label.fillstacks",
        "tfstorage.gui.label.movematchingitems",
        "tfstorage.gui.label.moveallitems",
        "tfstorage.gui.label.sortitems"
    };
    private static final ResourceLocation TEXTURE_0 = ResourceLocation.parse(TFStorageMod.MOD_ID + ":textures/gui/gui.container.tfbag.0.png");
    private static final ResourceLocation TEXTURE_1 = ResourceLocation.parse(TFStorageMod.MOD_ID + ":textures/gui/gui.container.tfbag.1.png");
    private static final ResourceLocation WIDGETS = ResourceLocation.parse(TFStorageMod.MOD_ID + ":textures/gui/gui.widgets.png");
    private static final ResourceLocation CURIOS_BUTTON = ResourceLocation.parse("curios:textures/gui/inventory.png");
    private static final int WIDGETS_SIZE = 256;
    private final List<IconButton> bagButtons = new ArrayList<>();
    private final List<FormattedCharSequence> tooltipLines = new ArrayList<>();
    private int firstMemoryCardSlotX;
    private int firstMemoryCardSlotY;
    private int lastGuiLeft;
    private int lastGuiTop;
    private final boolean curiosLoaded;
    private final List<int[]> effectIconPositions = new ArrayList<>();

    public BagScreen(BagMenu menu, Inventory playerInventory, Component title) {
        super(menu, playerInventory, title);
        if (menu.getBagTier() == 1) {
            this.imageWidth = 256;
            this.imageHeight = 256;
        } else {
            this.imageWidth = 176;
            this.imageHeight = 256;
        }
        this.inventoryLabelY = this.imageHeight - 94;
        this.curiosLoaded = CuriosCompat.isLoaded();
    }

    @Override
    protected void init() {
        super.init();
        updatePositions();
    }

    protected void drawCustomPotionEffects(GuiGraphics guiGraphics) {
        LocalPlayer player = this.minecraft.player;
        if (player == null) return;
        Collection<MobEffectInstance> effects = player.getActiveEffects();
        if (effects.isEmpty()) return;
        List<MobEffectInstance> visibleEffects = new ArrayList<>();
        for (MobEffectInstance effect : effects) {
            IClientMobEffectExtensions renderer = IClientMobEffectExtensions.of(effect);
            if (renderer.isVisibleInInventory(effect)) {
                visibleEffects.add(effect);
            }
        }
        if (visibleEffects.isEmpty()) return;
        effectIconPositions.clear();
        int iconSize = 12;
        int spacing = 1;
        int startX = this.leftPos - iconSize - spacing - 4;
        int startY = this.topPos + 14;
        int currentY = startY;
        for (int i = 0; i < visibleEffects.size(); i++) {
            MobEffectInstance effect = visibleEffects.get(i);
            renderEffectIcon(guiGraphics, effect, startX, currentY);
            effectIconPositions.add(new int[]{startX, currentY, i});
            currentY += iconSize + spacing;
        }
    }

    protected void renderEffectTooltip(GuiGraphics guiGraphics, int mouseX, int mouseY) {
        for (int[] pos : effectIconPositions) {
            int iconX = pos[0];
            int iconY = pos[1];
            int iconSize = 12;
            if (mouseX >= iconX && mouseX < iconX + iconSize && mouseY >= iconY && mouseY < iconY + iconSize) {
                LocalPlayer player = this.minecraft.player;
                if (player == null) return;
                List<MobEffectInstance> effects = new ArrayList<>(player.getActiveEffects());
                List<MobEffectInstance> visibleEffects = new ArrayList<>();
                for (MobEffectInstance effect : effects) {
                    IClientMobEffectExtensions renderer = IClientMobEffectExtensions.of(effect);
                    if (renderer.isVisibleInInventory(effect)) {
                        visibleEffects.add(effect);
                    }
                }
                if (pos[2] < visibleEffects.size()) {
                    MobEffectInstance effect = visibleEffects.get(pos[2]);
                    List<Component> tooltip = getEffectTooltip(effect);
                    List<FormattedCharSequence> lines = new ArrayList<>();
                    for (Component line : tooltip) {
                        lines.add(line.getVisualOrderText());
                    }
                    guiGraphics.renderTooltip(this.font, lines, mouseX, mouseY);
                }
                break;
            }
        }
    }

    private List<Component> getEffectTooltip(MobEffectInstance effect) {
        List<Component> tooltip = new ArrayList<>();
        Component name = effect.getEffect().getDisplayName();
        String amplifiedName = name.getString();
        int amplifier = effect.getAmplifier();
        if (amplifier > 0) {
            amplifiedName += " " + ChatFormatting.WHITE + net.minecraft.locale.Language.getInstance().getOrDefault("enchantment.level." + (amplifier + 1));
        }
        tooltip.add(Component.literal(amplifiedName));
        int ticks = effect.getDuration();
        int seconds = ticks / 20;
        int minutes = seconds / 60;
        int secs = seconds % 60;
        String duration = minutes + ":" + (secs < 10 ? "0" : "") + secs;
        tooltip.add(Component.literal(ChatFormatting.GRAY + duration));
        return tooltip;
    }

    private void renderEffectIcon(GuiGraphics guiGraphics, MobEffectInstance effect, int x, int y) {
        // 绘制背景框（类似原版紧凑型药水效果）
        guiGraphics.fill(x, y, x + 12, y + 12, 0xFF000000);
        // 绘制边框
        guiGraphics.renderOutline(x, y, 12, 12, 0xFFC0C0C0);
        
        // 绘制药水效果图标
        net.minecraft.client.Minecraft mc = net.minecraft.client.Minecraft.getInstance();
        net.minecraft.client.renderer.texture.TextureAtlasSprite sprite = mc.getMobEffectTextures().get(effect.getEffect());
        RenderSystem.setShaderTexture(0, sprite.atlasLocation());
        guiGraphics.blit(x + 1, y + 1, 0, 10, 10, sprite);
    }

    @Override
    public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        this.leftPos = (this.width - this.imageWidth) / 2;
        this.renderBackground(guiGraphics);
        super.render(guiGraphics, mouseX, mouseY, partialTick);
        this.drawCustomPotionEffects(guiGraphics);
        renderLargeStackOverlays(guiGraphics);
        if (needsPositionUpdate()) updatePositions();
        this.renderTooltip(guiGraphics, mouseX, mouseY);
        renderButtonTooltips(guiGraphics, mouseX, mouseY);
        renderEffectTooltip(guiGraphics, mouseX, mouseY);
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        return super.keyPressed(keyCode, scanCode, modifiers);
    }

    @Override
    protected void renderBg(GuiGraphics guiGraphics, float partialTick, int mouseX, int mouseY) {
        RenderSystem.setShaderColor(1.0f, 1.0f, 1.0f, 1.0f);
        ResourceLocation texture = this.menu.getBagTier() == 1 ? TEXTURE_1 : TEXTURE_0;
        guiGraphics.blit(texture, this.leftPos, this.topPos, 0, 0, this.imageWidth, this.imageHeight);
        ModularHandler bagInv = this.menu.getInventoryItemWithMemoryCards();
        if (this.minecraft.player != null && bagInv != null && !bagInv.isAccessibleBy(this.minecraft.player)) {
            int invSize = bagInv.getSlots();
            for (int i = 0; i < invSize; i++) {
                Slot slot = this.menu.getSlot(i);
                guiGraphics.blit(WIDGETS, this.leftPos + slot.x - 1, this.topPos + slot.y - 1, 46, 0, 18, 18, WIDGETS_SIZE, WIDGETS_SIZE);
            }
        }
        int selectedIndex = this.menu.getSelectedMemoryCardIndex();
        if (selectedIndex >= 0 && bagInv != null) {
            ItemStack selectedCard = this.menu.getMemoryCardStack(selectedIndex);
            if (selectedCard.isEmpty()) {
                int invSize = bagInv.getSlots();
                for (int i = 0; i < invSize; i++) {
                    Slot slot = this.menu.getSlot(i);
                    guiGraphics.blit(WIDGETS, this.leftPos + slot.x - 1, this.topPos + slot.y - 1, 46, 0, 18, 18, WIDGETS_SIZE, WIDGETS_SIZE);
                }
            }
        }
        ItemHandler memoryCardInv = this.menu.getInventoryItemWithMemoryCards().getMemoryCardInventory();
        if (this.minecraft.player != null && memoryCardInv != null && !memoryCardInv.isAccessibleBy(this.minecraft.player)) {
            int slots = memoryCardInv.getSlots();
            for (int i = 0; i < slots; i++) {
                guiGraphics.blit(WIDGETS, this.firstMemoryCardSlotX - 1 + i * 18, this.firstMemoryCardSlotY - 1, 46, 0, 18, 18, WIDGETS_SIZE, WIDGETS_SIZE);
            }
        }
        int index = this.menu.getSelectedMemoryCardIndex();
        if (index >= 0) {
            guiGraphics.blit(WIDGETS, this.firstMemoryCardSlotX - 1 + index * 18, this.firstMemoryCardSlotY, 46, 18, 18, 18, WIDGETS_SIZE, WIDGETS_SIZE);
            guiGraphics.blit(WIDGETS, this.firstMemoryCardSlotX + 4 + index * 18, this.firstMemoryCardSlotY + 20, 56, 48, 8, 8, WIDGETS_SIZE, WIDGETS_SIZE);
        }
        for (int i = 0; i < 4; i++) {
            if (this.menu.getMemoryCardStack(i).isEmpty()) {
                guiGraphics.blit(WIDGETS, this.firstMemoryCardSlotX - 1 + i * 18, this.firstMemoryCardSlotY - 1, 46, 0, 18, 18, WIDGETS_SIZE, WIDGETS_SIZE);
                guiGraphics.blit(WIDGETS, this.firstMemoryCardSlotX + 1 + i * 18, this.firstMemoryCardSlotY + 1, 32, 48, 16, 16, WIDGETS_SIZE, WIDGETS_SIZE);
            }
        }
        ItemStack modularStack = this.menu.getContainerItem();
        if (!modularStack.isEmpty() && TFBagItem.ShiftMode.getEffectiveMode(modularStack) == TFBagItem.ShiftMode.TO_BAG) {
            int x = this.leftPos + (this.menu.getBagTier() == 1 ? 40 : 0) + 64;
            guiGraphics.blit(WIDGETS, x, this.topPos + 153, 32, 32, 12, 12, WIDGETS_SIZE, WIDGETS_SIZE);
        }
        int xOff = this.leftPos + 51 + (this.menu.getBagTier() == 1 ? 40 : 0);
        InventoryScreen.renderEntityInInventoryFollowsMouse(guiGraphics, xOff, this.topPos + 82, 30, xOff - mouseX, this.topPos + 25 - mouseY, this.minecraft.player);
    }

    @Override
    protected void renderLabels(GuiGraphics guiGraphics, int mouseX, int mouseY) {
        int offsetXTier = this.menu.getBagTier() == 1 ? 40 : 0;
        guiGraphics.drawString(this.font, Component.translatable("container.crafting"), offsetXTier + 97, 5, 0x404040, false);
        guiGraphics.drawString(this.font, this.title, offsetXTier + 8, 5, 0x404040, false);
    }

    private void updatePositions() {
        Slot firstSlot = this.menu.getSlot(0);
        this.firstMemoryCardSlotX = this.leftPos + firstSlot.x + 5 * 18;
        this.firstMemoryCardSlotY = this.topPos + firstSlot.y - 32;
        createButtons();
        this.lastGuiLeft = this.leftPos;
        this.lastGuiTop = this.topPos;
    }

    private boolean needsPositionUpdate() {
        return this.lastGuiLeft != this.leftPos || this.lastGuiTop != this.topPos;
    }

    private void createButtons() {
        this.clearWidgets();
        this.bagButtons.clear();
        int numMemoryCards = 4;
        for (int i = 0; i < numMemoryCards; i++) {
            this.bagButtons.add(new IconButton(BTN_ID_FIRST_SELECT_MEMORY_CARD + i, this.firstMemoryCardSlotX + 5 + i * 18, this.firstMemoryCardSlotY + 21, 6, 6, 14, 21, WIDGETS, 6, 0));
        }
        int x = this.leftPos + this.menu.getSlot(0).x + 2;
        int y = this.topPos + this.menu.getSlot(0).y + 55;
        this.bagButtons.add(new HoverButton(BTN_ID_FIRST_MOVE_ITEMS + 0, x - 2, y + 1, 7, 7, 0, 56, WIDGETS, 7, 0, "tfstorage.gui.label.moveallitemsexcepthotbar", "tfstorage.gui.label.holdshifttoincludehotbar"));
        this.bagButtons.add(new HoverButton(BTN_ID_FIRST_MOVE_ITEMS + 1, x + 14, y + 1, 7, 7, 0, 14, WIDGETS, 7, 0, "tfstorage.gui.label.movematchingitemsexcepthotbar", "tfstorage.gui.label.holdshifttoincludehotbar"));
        int[] xOff = new int[] { 31, 117, 134, 151 };
        int[] quickButtonUVs = new int[] { 14, 0, 14, 7, 0, 21, 14, 14 };
        for (int i = 2; i < 6; i++) {
            int u = quickButtonUVs[(i - 2) * 2];
            int v = quickButtonUVs[(i - 2) * 2 + 1];
            this.bagButtons.add(new HoverButton(BTN_ID_FIRST_MOVE_ITEMS + i, x + xOff[i - 2], y + 1, 7, 7, u, v, WIDGETS, 7, 0, BUTTON_STRINGS[i]));
        }
        y = this.topPos + this.menu.getSlot(0).y - 11;
        this.bagButtons.add(new StateButton(BTN_ID_FIRST_MODES + 0, x - 1, y, 7, 7, 7, 0, WIDGETS, this, StateButton.ButtonState.createTranslate(0, 42, "tfstorage.gui.label.bag.disabled"), StateButton.ButtonState.createTranslate(0, 0, "tfstorage.gui.label.bag.enabled")));
        this.bagButtons.add(new StateButton(BTN_ID_FIRST_MODES + 1, x + 21, y, 7, 7, 7, 0, WIDGETS, this, StateButton.ButtonState.createTranslate(0, 35, "tfstorage.gui.label.pickupmode.disabled"), StateButton.ButtonState.createTranslate(0, 49, "tfstorage.gui.label.pickupmode.matching"), StateButton.ButtonState.createTranslate(0, 0, "tfstorage.gui.label.pickupmode.all")));
        this.bagButtons.add(new StateButton(BTN_ID_FIRST_MODES + 2, x + 10, y, 7, 7, 7, 0, WIDGETS, this, StateButton.ButtonState.createTranslate(0, 35, "tfstorage.gui.label.restockmode.off"), StateButton.ButtonState.createTranslate(0, 0, "tfstorage.gui.label.restockmode.on")));
        this.bagButtons.add(new StateButton(23, x + 32, y, 7, 7, 7, 0, WIDGETS, this, StateButton.ButtonState.createTranslate(0, 0, TFBagItem.ShiftMode.TO_BAG.getDisplayNameKey()), StateButton.ButtonState.createTranslate(0, 35, TFBagItem.ShiftMode.INV_HOTBAR.getDisplayNameKey()), StateButton.ButtonState.createTranslate(0, 49, TFBagItem.ShiftMode.DOUBLE_TAP.getDisplayNameKey())));
        if (this.menu.getBagTier() == 0) {
            this.bagButtons.add(new HoverButton(10, x + 74, y, 7, 7, 0, 7, WIDGETS, 7, 0, BUTTON_STRINGS[6]));
            this.bagButtons.add(new HoverButton(13, x + 74, y + 67, 7, 7, 0, 7, WIDGETS, 7, 0, "tfstorage.gui.label.sortitems.player"));
        } else {
            this.bagButtons.add(new HoverButton(11, x - 15, y - 5, 7, 7, 0, 7, WIDGETS, 7, 0, BUTTON_STRINGS[6]));
            this.bagButtons.add(new HoverButton(10, x + 74, y, 7, 7, 0, 7, WIDGETS, 7, 0, BUTTON_STRINGS[6]));
            this.bagButtons.add(new HoverButton(12, x + 164, y - 5, 7, 7, 0, 7, WIDGETS, 7, 0, BUTTON_STRINGS[6]));
            this.bagButtons.add(new HoverButton(13, x + 74, y + 67, 7, 7, 0, 7, WIDGETS, 7, 0, "tfstorage.gui.label.sortitems.player"));
            this.bagButtons.add(new StateButton(15, x - 26, y - 5, 7, 7, 7, 0, WIDGETS, this, StateButton.ButtonState.createTranslate(0, 0, "tfstorage.gui.label.regionop.enabled"), StateButton.ButtonState.createTranslate(0, 35, "tfstorage.gui.label.regionop.disabled")));
            this.bagButtons.add(new StateButton(14, x + 63, y, 7, 7, 7, 0, WIDGETS, this, StateButton.ButtonState.createTranslate(0, 0, "tfstorage.gui.label.regionop.enabled"), StateButton.ButtonState.createTranslate(0, 35, "tfstorage.gui.label.regionop.disabled")));
            this.bagButtons.add(new StateButton(16, x + 175, y - 5, 7, 7, 7, 0, WIDGETS, this, StateButton.ButtonState.createTranslate(0, 0, "tfstorage.gui.label.regionop.enabled"), StateButton.ButtonState.createTranslate(0, 35, "tfstorage.gui.label.regionop.disabled")));
        }
        if (this.curiosLoaded) {
            int tierOffset = this.menu.getBagTier() == 1 ? 40 : 0;
            int buttonX = this.leftPos + 26 + tierOffset;
            int buttonY = this.topPos + 15;
            this.bagButtons.add(new HoverButton(BTN_ID_BAUBLES, buttonX, buttonY, 14, 14, 50, 0, CURIOS_BUTTON, 0, 14, "curios.name"));
        }
        for (IconButton button : this.bagButtons) {
            this.addRenderableWidget(button);
        }
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        for (IconButton btn : this.bagButtons) {
            if (btn.isMouseOver(mouseX, mouseY) && btn.mouseClicked(mouseX, mouseY, button)) return true;
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    private void renderButtonTooltips(GuiGraphics guiGraphics, int mouseX, int mouseY) {
        for (IconButton button : this.bagButtons) {
            if (button.isMouseOver(mouseX, mouseY) && button.getHoverText().isEmpty() == false) {
                tooltipLines.clear();
                for (Component line : button.getHoverText()) {
                    tooltipLines.add(line.getVisualOrderText());
                }
                guiGraphics.renderTooltip(this.font, tooltipLines, mouseX, mouseY);
                return;
            }
        }
    }

    @Override
    public int getButtonStateIndex(int callbackId) {
        ItemStack stack = this.menu.getContainerItem();
        if (!stack.isEmpty()) {
            if (callbackId == BTN_ID_FIRST_MODES) return NBTHelper.getBoolean(stack, "TFBag", "DisableOpen") ? 0 : 1;
            else if (callbackId == BTN_ID_FIRST_MODES + 1) {
                TFBagItem.PickupMode mode = TFBagItem.PickupMode.fromStack(stack);
                if (mode == TFBagItem.PickupMode.ALL) return 2;
                else if (mode == TFBagItem.PickupMode.MATCHING) return 1;
                return 0;
            } else if (callbackId == BTN_ID_FIRST_MODES + 2) {
                TFBagItem.RestockMode mode = TFBagItem.RestockMode.fromStack(stack);
                return mode == TFBagItem.RestockMode.ON ? 1 : 0;
            } else if (callbackId >= Bag.REGION_LOCK_START && callbackId <= Bag.REGION_LOCK_START + 2) return this.isMaskActiveForSection(callbackId - Bag.REGION_LOCK_START, "LockMask") ? 1 : 0;
            else if (callbackId == Bag.SHIFT_MODE) return Math.min(NBTHelper.getByte(stack, "TFBag", "ShiftMode") & 0x3, 2);
        }
        return 0;
    }

    @Override
    public boolean isButtonEnabled(int callbackId) { return true; }

    private boolean isMaskActiveForSection(int section, String tagName) {
        ItemStack stack = this.menu.getContainerItem();
        int selected = CardHelper.getStoredMemoryCardSelection(stack, 4);
        if (selected >= 0 && section >= 0 && section <= 2) {
            ItemStack cardStack = CardHelper.getSelectedMemoryCardStack(stack);
            if (!cardStack.isEmpty()) {
                long[] masks = new long[] { 0x1FFFFFFL, 0x1FFF8000000L, 0x7FFE0000000000L };
                long lockMask = NBTHelper.getLong(cardStack, "TFBag", tagName);
                return (lockMask & masks[section]) == masks[section];
            }
        }
        return false;
    }

    private void onButtonPressed(int id, int mouseButton) {
        if (id >= BTN_ID_FIRST_SELECT_MEMORY_CARD && id < (BTN_ID_FIRST_SELECT_MEMORY_CARD + 4)) {
            ModNetworking.sendToServer(ActionPacket.forBag(GuiActions.Bag.SELECT_MODULE, id - BTN_ID_FIRST_SELECT_MEMORY_CARD));
        } else if (id >= BTN_ID_FIRST_MOVE_ITEMS && id <= (BTN_ID_FIRST_MOVE_ITEMS + 5)) {
            int value = id - BTN_ID_FIRST_MOVE_ITEMS;
            if (hasShiftDown()) value |= 0x8000;
            ModNetworking.sendToServer(ActionPacket.forBag(GuiActions.Bag.MOVE_ITEMS, value));
        } else if (id >= BTN_ID_FIRST_SORT && id < (BTN_ID_FIRST_SORT + 4)) {
            ModNetworking.sendToServer(ActionPacket.forBag(GuiActions.Bag.SORT_ITEMS, id - BTN_ID_FIRST_SORT));
        } else if (id >= BTN_ID_FIRST_REGION_LOCK && id < (BTN_ID_FIRST_REGION_LOCK + 3)) {
            ModNetworking.sendToServer(ActionPacket.forBag(GuiActions.Bag.TOGGLE_REGION_LOCK, id - BTN_ID_FIRST_REGION_LOCK));
        } else if (id >= BTN_ID_FIRST_MODES && id < (BTN_ID_FIRST_MODES + 3)) {
            int data = id - BTN_ID_FIRST_MODES;
            if (mouseButton == 1) data |= 0x8000;
            ModNetworking.sendToServer(ActionPacket.forBag(GuiActions.Bag.TOGGLE_MODES, data));
        } else if (id == Bag.SHIFT_MODE) {
            ModNetworking.sendToServer(ActionPacket.forBag(GuiActions.Bag.TOGGLE_SHIFTCLICK, mouseButton));
        } else if (id == BTN_ID_BAUBLES && this.curiosLoaded) {
            GuiEvents.instance().setOpenedExternalFromBag(true);
            CuriosCompat.openCuriosScreen(ItemStack.EMPTY);
        }
    }

    private interface IHoverTextProvider { List<Component> getHoverText(); }

    private class IconButton extends AbstractWidget implements IHoverTextProvider {
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
        protected boolean isEnabled() { return true; }

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
            this.active = this.isEnabled();
            if (!this.visible) return;
            boolean hovered = mouseX >= this.getX() && mouseY >= this.getY() && mouseX < this.getX() + this.width && mouseY < this.getY() + this.height;
            int state = hovered ? 1 : 0;
            guiGraphics.blit(this.texture, this.getX(), this.getY(), this.getU() + state * this.hoverOffsetU, this.getV() + state * this.hoverOffsetV, this.width, this.height, WIDGETS_SIZE, WIDGETS_SIZE);
        }

        @Override
        protected void updateWidgetNarration(net.minecraft.client.gui.narration.NarrationElementOutput narration) {}

        @Override
        public List<Component> getHoverText() { return this.hoverText; }
    }

    private class HoverButton extends IconButton {
        public HoverButton(int id, int x, int y, int w, int h, int u, int v, ResourceLocation texture, int hoverOffsetU, int hoverOffsetV, String... hoverStrings) {
            super(id, x, y, w, h, u, v, texture, hoverOffsetU, hoverOffsetV);
            for (String text : hoverStrings) this.getHoverText().add(Component.translatable(text));
        }
    }

    private class StateButton extends HoverButton {
        protected static final ButtonState STATE_INVALID = ButtonState.create(0, 0, "INVALID");
        protected final ButtonStateCallback callback;
        protected final ButtonState[] states;

        public StateButton(int id, int x, int y, int w, int h, int hoverOffsetU, int hoverOffsetV, ResourceLocation texture, ButtonStateCallback callback, ButtonState... states) {
            super(id, x, y, w, h, 0, 0, texture, hoverOffsetU, hoverOffsetV);
            this.callback = callback;
            this.states = states;
        }

        @Override
        protected int getU() { return this.getState(this.callback.getButtonStateIndex(this.id)).getU(); }
        @Override
        protected int getV() { return this.getState(this.callback.getButtonStateIndex(this.id)).getV(); }
        @Override
        protected boolean isEnabled() { return this.callback.isButtonEnabled(this.id); }
        @Override
        public List<Component> getHoverText() { return this.getState(this.callback.getButtonStateIndex(this.id)).getHoverText(); }
        protected ButtonState getState(int index) { return index >= 0 && index < this.states.length ? this.states[index] : STATE_INVALID; }

        public static class ButtonState {
            private final int u;
            private final int v;
            private final List<Component> hoverText;

            private ButtonState(int u, int v, boolean translate, String... hoverStrings) {
                this.u = u;
                this.v = v;
                this.hoverText = new ArrayList<>();
                for (String key : hoverStrings) this.hoverText.add(translate ? Component.translatable(key) : Component.literal(key));
            }

            public int getU() { return this.u; }
            public int getV() { return this.v; }
            public List<Component> getHoverText() { return this.hoverText; }
            public static ButtonState create(int u, int v, String... hoverStrings) { return new ButtonState(u, v, false, hoverStrings); }
            public static ButtonState createTranslate(int u, int v, String... hoverStrings) { return new ButtonState(u, v, true, hoverStrings); }
        }
    }

    private void renderLargeStackOverlays(GuiGraphics guiGraphics) {
        for (Slot slot : this.menu.slots) {
            ItemStack stack = slot.getItem();
            if (stack.isEmpty()) continue;
            if (slot instanceof SlotItemHandler handlerSlot && handlerSlot.getItemHandler() instanceof IItemHandlerSize) {
                if (stack.getCount() != 1) renderLargeStackItemOverlay(guiGraphics, stack, this.leftPos + slot.x, this.topPos + slot.y);
            }
        }
    }

    private void renderLargeStackItemOverlay(GuiGraphics guiGraphics, ItemStack stack, int xPosition, int yPosition) {
        String str = TextFormatter.getStackSizeString(stack, 4);
        if (stack.getCount() < 1) str = ChatFormatting.RED + str;
        guiGraphics.pose().pushPose();
        guiGraphics.pose().translate(xPosition, yPosition, 200.0f);
        guiGraphics.pose().scale(0.5f, 0.5f, 0.5f);
        int textWidth = this.font.width(str);
        guiGraphics.drawString(this.font, str, (31 - textWidth), 23, 0xFFFFFF, true);
        guiGraphics.pose().popPose();
    }
}
