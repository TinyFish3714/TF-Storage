package tf.storage.platform.forge.compat;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.Optional;
import java.util.UUID;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.common.util.LazyOptional;
import net.minecraftforge.fml.ModList;
import net.minecraftforge.items.IItemHandler;
import net.minecraftforge.network.PacketDistributor;
import tf.storage.core.util.StackHelper;
import tf.storage.platform.forge.TFStorageMod;
import tf.storage.platform.forge.item.TFBagItem;

public final class CuriosCompat {

    private static final String CURIOS_MODID = "curios";
    private static final String CURIOS_API = "top.theillusivec4.curios.api.CuriosApi";
    private static final String CURIOS_NET = "top.theillusivec4.curios.common.network.NetworkHandler";
    private static final String CURIOS_PACKET_OPEN =
        "top.theillusivec4.curios.common.network.client.CPacketOpenCurios";
    private static final String CURIOS_SCREEN =
        "top.theillusivec4.curios.client.gui.CuriosScreen";
    private static final String CURIOS_SCREEN_V2 =
        "top.theillusivec4.curios.client.gui.CuriosScreenV2";

    private CuriosCompat() {
    }

    public static boolean isLoaded() {
        return ModList.get().isLoaded(CURIOS_MODID);
    }

    public static boolean isCuriosScreen(Screen screen) {
        if (screen == null || !isLoaded()) {
            return false;
        }
        String name = screen.getClass().getName();
        return CURIOS_SCREEN.equals(name) || CURIOS_SCREEN_V2.equals(name);
    }

    public static IItemHandler getEquippedCurios(Player player) {
        if (!isLoaded() || player == null) {
            return null;
        }
        try {
            Class<?> curiosApi = Class.forName(CURIOS_API);
            Method getCuriosInventory = curiosApi.getMethod("getCuriosInventory", LivingEntity.class);
            Object lazy = getCuriosInventory.invoke(null, player);
            if (lazy instanceof LazyOptional<?> lazyOptional) {
                Optional<?> handlerOpt = lazyOptional.resolve();
                if (handlerOpt.isEmpty()) {
                    return null;
                }
                Object handler = handlerOpt.get();
                Method getEquippedCurios = handler.getClass().getMethod("getEquippedCurios");
                Object equipped = getEquippedCurios.invoke(handler);
                if (equipped instanceof IItemHandler itemHandler) {
                    return itemHandler;
                }
            }
        } catch (Throwable t) {
            TFStorageMod.LOGGER.debug("Curios compat: failed to access curios inventory", t);
        }
        return null;
    }

    public static ItemStack findStackByUUID(Player player, UUID uuid) {
        if (uuid == null) {
            return ItemStack.EMPTY;
        }
        IItemHandler curiosInv = getEquippedCurios(player);
        if (curiosInv == null) {
            return ItemStack.EMPTY;
        }
        return StackHelper.getItemStackByUUID(curiosInv, uuid, "UUID");
    }

    public static ItemStack findOpenableBag(Player player) {
        IItemHandler curiosInv = getEquippedCurios(player);
        if (curiosInv == null) {
            return ItemStack.EMPTY;
        }

        for (int i = 0; i < curiosInv.getSlots(); i++) {
            ItemStack stack = curiosInv.getStackInSlot(i);
            if (stack.isEmpty()) {
                continue;
            }
            if (stack.getItem() instanceof TFBagItem && TFBagItem.bagIsAutoOpenable(stack)) {
                return stack;
            }
        }

        return ItemStack.EMPTY;
    }

    public static boolean openCuriosScreen(ItemStack carried) {
        if (!isLoaded()) {
            return false;
        }
        try {
            Class<?> packetClass = Class.forName(CURIOS_PACKET_OPEN);
            Constructor<?> ctor = packetClass.getConstructor(ItemStack.class);
            Object packet = ctor.newInstance(carried == null ? ItemStack.EMPTY : carried);

            Class<?> netClass = Class.forName(CURIOS_NET);
            Field instanceField = netClass.getField("INSTANCE");
            Object channel = instanceField.get(null);

            Method send = channel.getClass().getMethod("send", PacketDistributor.PacketTarget.class,
                Object.class);
            send.invoke(channel, PacketDistributor.SERVER.noArg(), packet);
            return true;
        } catch (Throwable t) {
            TFStorageMod.LOGGER.debug("Curios compat: failed to open curios screen", t);
            return false;
        }
    }
}
