package tf.storage.gui.component;

import java.util.ArrayList;
import java.util.List;
import net.minecraft.client.resources.I18n;
import net.minecraft.util.ResourceLocation;

/**
 * 悬停文本按钮类，继承自图标按钮，添加了悬停文本功能
 *
 * <p>此类在图标按钮的基础上添加了悬停时显示文本的功能，提供了更丰富的用户交互体验。</p>
 */
public class HoverButton extends IconButton
{
    protected final ArrayList<String> hoverStrings;

    public HoverButton(int id, int x, int y, int w, int h, int u, int v,
            ResourceLocation texture, int hoverOffsetU, int hoverOffsetV, String ... hoverStrings)
    {
        super(id, x, y, w, h, u, v, texture, hoverOffsetU, hoverOffsetV);
        this.hoverStrings = new ArrayList<String>();

        for (String text : hoverStrings)
        {
            this.hoverStrings.add(I18n.format(text));
        }
    }

    /**
     * 获取悬停文本列表
     *
     * @return 悬停文本列表
     */
    public List<String> getHoverStrings()
    {
        return this.hoverStrings;
    }
}
