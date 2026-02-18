package tf.storage.platform.forge.event;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.inventory.InventoryScreen;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.client.event.ScreenEvent;
import net.minecraftforge.client.gui.overlay.VanillaGuiOverlay;
import net.minecraftforge.client.event.RenderGuiOverlayEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import tf.storage.platform.forge.TFStorageMod;
import tf.storage.platform.forge.client.screen.BagScreen;
import tf.storage.platform.forge.item.TFBagItem;
import tf.storage.platform.forge.compat.CuriosCompat;
import tf.storage.platform.forge.network.ModNetworking;
import tf.storage.platform.forge.network.packets.OpenBagPacket;

/**
 * GuiEvents - Client GUI event handler
 * Main function: intercept vanilla inventory open event, open TF bag instead
 * 
 * Adapted for Minecraft 1.20.1
 * Note: This is a client-only class, using @OnlyIn(Dist.CLIENT)
 */
@OnlyIn(Dist.CLIENT)
@Mod.EventBusSubscriber(modid = TFStorageMod.MOD_ID, bus = Mod.EventBusSubscriber.Bus.FORGE, value = Dist.CLIENT)
public class GuiEvents {
    
    private static final GuiEvents INSTANCE = new GuiEvents();
    private boolean tfBagShouldOpen;
    private boolean openedExternalFromBag; // Opened external GUI from TF bag (e.g. Curios)
    
    // Timer for delayed TF bag reopening
    private int reopenBagDelay = -1;

    // Private constructor (singleton pattern)
    private GuiEvents() {}

    public static GuiEvents instance() {
        return INSTANCE;
    }

    public void setTFBagShouldOpen(boolean shouldOpen) {
        this.tfBagShouldOpen = shouldOpen;
    }

    public void setOpenedExternalFromBag(boolean val) {
        this.openedExternalFromBag = val;
    }

    /**
     * Listen for GUI open events
     * Intercept vanilla inventory screen, open TF bag instead
     */
    @SubscribeEvent
    public static void onScreenOpenEvent(ScreenEvent.Opening event) {
        Minecraft mc = Minecraft.getInstance();
        Player player = mc.player;
        GuiEvents guiEvents = instance();

        if (guiEvents.openedExternalFromBag) {
            boolean isClosing = (event.getNewScreen() == null);
            boolean isGoingToInv = (event.getNewScreen() instanceof InventoryScreen);

            boolean currentIsExternal = CuriosCompat.isCuriosScreen(mc.screen);

            if (currentIsExternal && (isClosing || isGoingToInv)) {
                guiEvents.openedExternalFromBag = false;
                event.setCanceled(true);
                guiEvents.reopenBagDelay = 1;
            }
        }

        // Intercept vanilla inventory screen, open TF bag instead
        if (event.getNewScreen() != null && event.getNewScreen().getClass() == InventoryScreen.class) {
            if (guiEvents.tfBagShouldOpen && player != null && !TFBagItem.getOpenableBag(player).isEmpty()) {
                // Cancel opening vanilla inventory
                event.setCanceled(true);
                
                // Send request to server to open TF bag
                ModNetworking.sendToServer(new OpenBagPacket());
            }
        }
    }
    
    /**
     * RenderGuiOverlayEvent - Hide potion effects when TF bag is open
     */
    @SubscribeEvent
    public static void onRenderGuiOverlayPre(RenderGuiOverlayEvent.Pre event) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.screen instanceof BagScreen) {
            // Hide potion effects in the top-right corner when TF bag is open
            if (event.getOverlay().id().equals(VanillaGuiOverlay.POTION_ICONS.id())) {
                event.setCanceled(true);
            }
        }
    }
    
    /**
     * Client Tick event
     * Used for delayed TF bag reopening
     */
    @SubscribeEvent
    public static void onClientTick(TickEvent.ClientTickEvent event) {
        if (event.phase == TickEvent.Phase.END) {
            GuiEvents guiEvents = instance();
            
            if (guiEvents.reopenBagDelay >= 0) {
                guiEvents.reopenBagDelay--;
                
                if (guiEvents.reopenBagDelay == 0) {
                    // Timer expired, reopen TF bag
                    Minecraft mc = Minecraft.getInstance();
                    Player player = mc.player;
                    
                    if (player != null) {
                        if (!TFBagItem.getOpenableBag(player).isEmpty()) {
                            ModNetworking.sendToServer(new OpenBagPacket());
                        } else {
                            mc.setScreen(new InventoryScreen(player));
                        }
                    }
                    
                    // Reset timer
                    guiEvents.reopenBagDelay = -1;
                }
            }
        }
    }
}
