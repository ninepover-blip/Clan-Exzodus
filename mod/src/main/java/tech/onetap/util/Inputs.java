package tech.onetap.util;

import java.util.Locale;

import org.lwjgl.glfw.GLFW;

public final class Inputs {
    private static final int MOUSE_OFFSET = 100;

    private Inputs() {
    }

    public static int mouseButtonCode(int button) {
        return -MOUSE_OFFSET - button;
    }

    public static boolean isMouseCode(int code) {
        return code <= -MOUSE_OFFSET;
    }

    public static boolean isKeyboardCode(int code) {
        return code >= GLFW.GLFW_KEY_SPACE;
    }

    public static int mouseButton(int code) {
        return -code - MOUSE_OFFSET;
    }

    public static boolean isDown(long windowHandle, int code) {
        if (isMouseCode(code)) {
            return GLFW.glfwGetMouseButton(windowHandle, mouseButton(code)) == GLFW.GLFW_PRESS;
        }

        return code != GLFW.GLFW_KEY_UNKNOWN && GLFW.glfwGetKey(windowHandle, code) == GLFW.GLFW_PRESS;
    }

    public static String name(int code) {
        if (code == GLFW.GLFW_KEY_UNKNOWN) {
            return "None";
        }

        if (isMouseCode(code)) {
            return switch (mouseButton(code)) {
                case GLFW.GLFW_MOUSE_BUTTON_LEFT -> "LMB";
                case GLFW.GLFW_MOUSE_BUTTON_RIGHT -> "RMB";
                case GLFW.GLFW_MOUSE_BUTTON_MIDDLE -> "MMB";
                default -> "MOUSE" + (mouseButton(code) + 1);
            };
        }

        if (code == GLFW.GLFW_KEY_RIGHT_SHIFT) {
            return "RSHIFT";
        }

        if (code == GLFW.GLFW_KEY_LEFT_SHIFT) {
            return "LSHIFT";
        }

        if (code == GLFW.GLFW_KEY_RIGHT_CONTROL) {
            return "RCTRL";
        }

        if (code == GLFW.GLFW_KEY_LEFT_CONTROL) {
            return "LCTRL";
        }

        if (code == GLFW.GLFW_KEY_RIGHT_ALT) {
            return "RALT";
        }

        if (code == GLFW.GLFW_KEY_LEFT_ALT) {
            return "LALT";
        }

        String name = GLFW.glfwGetKeyName(code, 0);
        return name == null ? "KEY " + code : name.toUpperCase(Locale.ROOT);
    }
}
