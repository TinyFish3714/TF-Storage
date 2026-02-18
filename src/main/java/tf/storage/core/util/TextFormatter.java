package tf.storage.core.util;

import net.minecraft.world.item.ItemStack;

/**
 * Text formatting utilities.
 */
public class TextFormatter {
    public static String getStackSizeString(ItemStack stack, int maxChars) {
        return String.valueOf(stack.getCount());
    }

    public static String formatNumberWithKSeparators(long value) {
        return String.valueOf(value);
    }

    public static String getInitialsWithDots(String str) {
        if (str == null || str.isEmpty()) return "";
        StringBuilder sb = new StringBuilder();
        String[] split = str.split(" ");
        for (String s : split) {
            if (!s.isEmpty() && Character.isAlphabetic(s.charAt(0))) {
                sb.append(s.charAt(0)).append('.');
            }
        }
        return sb.toString();
    }
}
