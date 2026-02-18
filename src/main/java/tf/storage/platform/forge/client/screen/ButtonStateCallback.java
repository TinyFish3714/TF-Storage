package tf.storage.platform.forge.client.screen;

/**
 * Button state callback interface for bag screen buttons.
 */
public interface ButtonStateCallback {
    int getButtonStateIndex(int callbackId);
    boolean isButtonEnabled(int callbackId);
}
