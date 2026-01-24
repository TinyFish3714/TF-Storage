package tf.storage.gui.component;

/**
 * 按钮状态回调接口
 *
 * <p>此接口定义了按钮状态回调的基本方法，用于实现动态按钮状态管理。</p>
 */
public interface IButtonStateCallback
{
    /**
     * 获取指定ID按钮的当前状态索引
     *
     * @param callbackId 按钮ID
     * @return 当前状态索引
     */
    int getButtonStateIndex(int callbackId);

    /**
     * 获取指定ID按钮是否启用
     *
     * @param callbackId 按钮ID
     * @return 如果按钮启用则返回true，否则返回false
     */
    boolean isButtonEnabled(int callbackId);
}
