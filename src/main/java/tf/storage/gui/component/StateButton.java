package tf.storage.gui.component;

import java.util.ArrayList;
import java.util.List;
import net.minecraft.util.ResourceLocation;
import tf.storage.item.base.BaseItem;

/**
 * 状态回调按钮类，继承自悬停文本按钮，添加了状态回调功能
 *
 * <p>此类整合了按钮状态管理和悬停文本功能，提供了更简洁的按钮实现方式。</p>
 */
public class StateButton extends HoverButton
{
    protected static final ButtonState STATE_INVALID = ButtonState.create(0, 0, "INVALID");
    protected final IButtonStateCallback callback;
    protected final ButtonState[] states;

    public StateButton(int id, int x, int y, int w, int h, int hoverOffsetU, int hoverOffsetV,
            ResourceLocation texture, IButtonStateCallback callback, ButtonState... states)
    {
        super(id, x, y, w, h, 0, 0, texture, hoverOffsetU, hoverOffsetV);

        this.callback = callback;
        this.states = states;
    }

    @Override
    protected int getU()
    {
        return this.getState(this.callback.getButtonStateIndex(this.id)).getU();
    }

    @Override
    protected int getV()
    {
        return this.getState(this.callback.getButtonStateIndex(this.id)).getV();
    }

    @Override
    protected boolean isEnabled()
    {
        return this.callback.isButtonEnabled(this.id);
    }

    @Override
    public List<String> getHoverStrings()
    {
        return this.getState(this.callback.getButtonStateIndex(this.id)).getHoverStrings();
    }

    /**
     * 获取指定索引的按钮状态
     *
     * @param index 状态索引
     * @return 按钮状态对象，如果索引无效则返回无效状态
     */
    protected ButtonState getState(int index)
    {
        return index >= 0 && index < this.states.length ? this.states[index] : STATE_INVALID;
    }

    /**
     * 按钮状态类，封装了按钮的视觉状态和悬停文本
     */
    public static class ButtonState
    {
        private final int u;
        private final int v;
        private final List<String> hoverText;

        private ButtonState(int u, int v, boolean translate, String... hoverStrings)
        {
            this.u = u;
            this.v = v;
            this.hoverText = new ArrayList<String>();

            for (String key : hoverStrings)
            {
                if (translate)
                {
                    BaseItem.addTranslatedTooltip(key, this.hoverText, false);
                }
                else
                {
                    this.hoverText.add(key);
                }
            }
        }

        public int getU()
        {
            return this.u;
        }

        public int getV()
        {
            return this.v;
        }

        public List<String> getHoverStrings()
        {
            return this.hoverText;
        }

        /**
         * 创建一个不翻译的按钮状态
         *
         * @param u U坐标
         * @param v V坐标
         * @param hoverStrings 悬停文本
         * @return 按钮状态对象
         */
        public static ButtonState create(int u, int v, String... hoverStrings)
        {
            return new ButtonState(u, v, false, hoverStrings);
        }

        /**
         * 创建一个翻译的按钮状态
         *
         * @param u U坐标
         * @param v V坐标
         * @param hoverStrings 悬停文本键
         * @return 按钮状态对象
         */
        public static ButtonState createTranslate(int u, int v, String... hoverStrings)
        {
            return new ButtonState(u, v, true, hoverStrings);
        }
    }
}
