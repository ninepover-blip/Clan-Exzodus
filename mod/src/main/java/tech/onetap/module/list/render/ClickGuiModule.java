package tech.onetap.module.list.render;

import net.minecraft.client.MinecraftClient;
import org.lwjgl.glfw.GLFW;
import tech.onetap.module.Module;
import tech.onetap.module.ModuleCategory;
import tech.onetap.module.ModuleInformation;
import tech.onetap.module.settings.BooleanSetting;
import tech.onetap.module.settings.ColorSetting;
import tech.onetap.module.settings.ModeSetting;
import tech.onetap.module.settings.SliderSetting;
import tech.onetap.module.settings.impl.ThemeManager;
import tech.onetap.ui.ClickGuiScreen;

import java.awt.Color;

@ModuleInformation(moduleName = "Click Gui", moduleCategory = ModuleCategory.RENDER, moduleKeybind = GLFW.GLFW_KEY_RIGHT_SHIFT)
public class ClickGuiModule extends Module {

    public final ColorSetting themeColor = new ColorSetting("Theme Color", new Color(255, 65, 69).getRGB());
    public final ModeSetting guiStyle = new ModeSetting("GUI Style", "Infinyty", "Infinyty", "Glass");
    public final SliderSetting animationSpeed = new SliderSetting("Animation Speed", 0.25d, 0.12d, 0.42d, 0.01d);
    public final SliderSetting backgroundDim = new SliderSetting("Background Dim", 0.0d, 0.0d, 180.0d, 1.0d);
    public final SliderSetting roundness = new SliderSetting("Roundness", 8.0d, 2.0d, 14.0d, 1.0d);
    public final BooleanSetting accentGlow = new BooleanSetting("Accent Glow", true);
    public final BooleanSetting descriptions = new BooleanSetting("Descriptions", true);
    public final BooleanSetting searchDescriptions = new BooleanSetting("Search Descriptions", true);

    public String getGuiStyle() {
        return guiStyle.getValue();
    }

    public float getAnimationSpeed() {
        return (float) animationSpeed.getValue();
    }

    public int getBackgroundDim() {
        return (int) backgroundDim.getValue();
    }

    public float getRoundness() {
        return (float) roundness.getValue();
    }

    public boolean hasAccentGlow() {
        return accentGlow.getValue();
    }

    public boolean shouldShowDescriptions() {
        return descriptions.getValue();
    }

    public boolean shouldSearchDescriptions() {
        return searchDescriptions.getValue();
    }

    public Color getThemeColorValue() {
        return getThemeColorValue(0.0f);
    }

    public Color getThemeColorValue(float offset) {
        return gradientColor(new Color(themeColor.getValue()), offset, 255);
    }

    public Color getThemeBaseColorValue() {
        return new Color(themeColor.getValue());
    }

    public void setThemeBaseColor(Color color) {
        themeColor.setValue(color.getRGB());
        ThemeManager.getInstance().setBaseColor(color.getRGB());
    }

    public Color[] getThemeGradient(float offset, int alpha) {
        return gradientColors(new Color(themeColor.getValue()), offset, alpha);
    }

    public static Color[] gradientColors(Color base, float offset, int alpha) {
        return new Color[] {
                gradientColor(base, offset, alpha),
                gradientColor(base, offset + 0.18f, alpha),
                gradientColor(base, offset + 0.36f, alpha)
        };
    }

    public static Color gradientColor(Color base, float offset, int alpha) {
        float[] hsb = Color.RGBtoHSB(base.getRed(), base.getGreen(), base.getBlue(), null);
        float phase = (System.currentTimeMillis() % 5200L) / 5200.0f;
        float wave = (float) Math.sin((phase + offset) * Math.PI * 2.0d);
        float hue = wrap(hsb[0] + offset * 0.09f + wave * 0.035f);
        float saturation = clamp(Math.max(0.58f, hsb[1]) + wave * 0.055f, 0.46f, 1.0f);
        float brightness = clamp(Math.max(0.78f, hsb[2]) + (float) Math.cos((phase + offset) * Math.PI * 2.0d) * 0.045f, 0.62f, 1.0f);
        Color color = new Color(Color.HSBtoRGB(hue, saturation, brightness));
        return new Color(color.getRed(), color.getGreen(), color.getBlue(), clamp(alpha, 0, 255));
    }

    private static float wrap(float value) {
        float wrapped = value % 1.0f;
        return wrapped < 0.0f ? wrapped + 1.0f : wrapped;
    }

    private static float clamp(float value, float min, float max) {
        return Math.max(min, Math.min(max, value));
    }

    private static int clamp(int value, int min, int max) {
        return Math.max(min, Math.min(max, value));
    }

    @Override
    public void onEnable() {
        super.onEnable();
        MinecraftClient client = MinecraftClient.getInstance();
        if (!(client.currentScreen instanceof ClickGuiScreen)) {
            client.setScreen(new ClickGuiScreen(this));
        }
    }

    @Override
    public void onDisable() {
        super.onDisable();
        MinecraftClient client = MinecraftClient.getInstance();
        if (client.currentScreen instanceof ClickGuiScreen clickGuiScreen) {
            clickGuiScreen.requestClose();
        } else {
            client.setScreen(null);
        }
    }
}
