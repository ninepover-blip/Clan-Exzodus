package tech.onetap.module.settings;

import org.lwjgl.glfw.GLFW;

public final class ActionBindSetting extends Setting {
    private int keyCode;

    public ActionBindSetting(String name) {
        this(name, GLFW.GLFW_KEY_UNKNOWN);
    }

    public ActionBindSetting(String name, int keyCode) {
        super(name);
        this.keyCode = keyCode;
    }

    public int getKeyCode() {
        return keyCode;
    }

    public void setKeyCode(int keyCode) {
        this.keyCode = keyCode;
    }

    public boolean isBound() {
        return keyCode != GLFW.GLFW_KEY_UNKNOWN;
    }

    @Override
    public String getValueAsString() {
        return Integer.toString(keyCode);
    }

    @Override
    public void setValueFromString(String value) {
        try {
            this.keyCode = Integer.parseInt(value);
        } catch (NumberFormatException ex) {
            this.keyCode = GLFW.GLFW_KEY_UNKNOWN;
        }
    }

    @Override
    public ActionBindSetting setVisible(java.util.function.Supplier<Boolean> visible) {
        this.visible = visible;
        return this;
    }
}
