package tech.onetap.module.settings.impl;

import java.awt.Color;

public class ThemeManager {
    private static ThemeManager instance;

    // Создаем стандартную тему по умолчанию (Название, Цвет 1, Цвет 2 в HEX формате ARGB)
    // 0xFF4A90E2 - это синий цвет, 0xFF9013FE - фиолетовый.
    // Вы можете поменять эти HEX-коды на любые другие свои любимые цвета!
    private final Theme defaultTheme = new Theme("Default", 0xFF4A90E2, 0xFF9013FE);

    private ThemeManager() {
    }

    public static ThemeManager getInstance() {
        if (instance == null) {
            instance = new ThemeManager();
        }
        return instance;
    }

    public Theme getCurrentTheme() {
        return defaultTheme;
    }

    public void setBaseColor(int color) {
        Color base = new Color(color, true);
        float[] hsb = Color.RGBtoHSB(base.getRed(), base.getGreen(), base.getBlue(), null);
        int secondRgb = Color.HSBtoRGB((hsb[0] + 0.09f) % 1.0f,
                Math.max(0.55f, hsb[1]), Math.max(0.72f, hsb[2]));
        defaultTheme.setColors(0xFF000000 | (color & 0x00FFFFFF),
                0xFF000000 | (secondRgb & 0x00FFFFFF));
    }

}
