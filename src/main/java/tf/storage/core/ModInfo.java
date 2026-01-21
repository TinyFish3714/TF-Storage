package tf.storage.core;

import net.minecraft.util.ResourceLocation;
import net.minecraft.util.text.TextFormatting;

/**
 * ModInfo - 模组ID、资源路径、常量定义
 */
public class ModInfo
{
    // === 模组标识 ===
    public static final String MOD_ID = "tfstorage";
    public static final String MOD_NAME = "TF Storage";
    public static final String MOD_VERSION = "@MOD_VERSION@";

    // === 代理类 ===
    public static final String PROXY_CLASS_CLIENT = "tf.storage.proxy.ClientProxy";
    public static final String PROXY_CLASS_SERVER = "tf.storage.proxy.CommonProxy";

    // === 物品注册名 (请勿修改 - 保持存档兼容性) ===
    public static final String NAME_ITEM_MEMORY_CARD = "tfunit";
    public static final String NAME_ITEM_TF_BAG = "tfbag";
    public static final String NAME_ITEM_SYCORE = "sycore";
    public static final String NAME_ITEM_EDCORE = "edcore";
    public static final String NAME_ITEM_TFCORE = "tfcore";

    // === 方块注册名 (请勿修改 - 保持存档兼容性) ===
    public static final String NAME_BLOCK_TF_CHEST = "tf_chest";
    public static final String NAME_TILE_ENTITY_TF_CHEST = "tf_chest";

    // === GUI ID ===
    public static final int GUI_ID_TILE_ENTITY_GENERIC = 0;
    public static final int GUI_ID_TF_BAG = 1001;
    public static final int GUI_ID_TF_BAG_RIGHT_CLICK = 1002;

    // === 资源路径 ===
    public static final String GUI_LOCATION = "textures/gui/";

    public static ResourceLocation getGuiTexture(String name)
    {
        return getResourceLocation(GUI_LOCATION + name + ".png");
    }

    public static ResourceLocation getResourceLocation(String path)
    {
        return new ResourceLocation(MOD_ID, path);
    }

    public static ResourceLocation getResourceLocation(String modId, String path)
    {
        return new ResourceLocation(modId, path);
    }

    public static String getPrefixedName(String name)
    {
        return MOD_ID + "_" + name;
    }

    public static String getDotPrefixedName(String name)
    {
        return MOD_ID + "." + name;
    }

    // === 文本颜色 ===
    public static final class Colors
    {
        public static final String SUCCESS = TextFormatting.GREEN.toString();
        public static final String ERROR = TextFormatting.RED.toString();
        public static final String WARNING = TextFormatting.YELLOW.toString();
        public static final String INFO = TextFormatting.WHITE.toString();
        public static final String HIGHLIGHT = TextFormatting.BLUE.toString();
        public static final String RESET = TextFormatting.RESET.toString();
        public static final String GRAY = TextFormatting.GRAY.toString();
        public static final String ITALIC = TextFormatting.ITALIC.toString();
    }
}
