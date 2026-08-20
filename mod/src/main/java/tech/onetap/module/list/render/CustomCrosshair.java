package tech.onetap.module.list.render;

import com.google.common.eventbus.Subscribe;
import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.gl.ShaderProgramKeys;
import net.minecraft.client.render.BufferBuilder;
import net.minecraft.client.render.BufferRenderer;
import net.minecraft.client.render.Tessellator;
import net.minecraft.client.render.VertexFormat;
import net.minecraft.client.render.VertexFormats;
import org.joml.Matrix4f;
import tech.onetap.event.list.EventHUD;
import tech.onetap.module.Module;
import tech.onetap.module.ModuleCategory;
import tech.onetap.module.ModuleInformation;
import tech.onetap.module.settings.BooleanSetting;
import tech.onetap.module.settings.ColorSetting;
import tech.onetap.module.settings.ModeSetting;
import tech.onetap.module.settings.SliderSetting;
import tech.onetap.util.render.providers.ColorProvider;
import tech.onetap.util.render.renderers.DrawUtil;

@ModuleInformation(moduleName = "CustomCrosshair", moduleDesc = "Кастомный прицел", moduleCategory = ModuleCategory.RENDER)
public class CustomCrosshair extends Module {

    private final ModeSetting style = new ModeSetting("Стиль", "Cross", "Cross", "Dot", "Circle");
    private final SliderSetting size = new SliderSetting("Размер", 5f, 2f, 15f, 0.5f);
    private final SliderSetting gap = new SliderSetting("Отступ", 3f, 0f, 10f, 0.5f);
    private final SliderSetting thickness = new SliderSetting("Толщина", 1.5f, 1f, 4f, 0.5f);
    private final ColorSetting color = new ColorSetting("Цвет", 0xFFFFFFFF);
    private final BooleanSetting dynamic = new BooleanSetting("Менять при атаке", true);

    private int attackTicks = 0;

    @Subscribe
    public void onEventHUD(EventHUD event) {
        if (!isEnabled() || mc.player == null) return;

        float centerX = mc.getWindow().getScaledWidth() / 2f;
        float centerY = mc.getWindow().getScaledHeight() / 2f;

        int col = color.getValue();
        if (dynamic.getValue() && mc.options.attackKey.isPressed()) {
            col = ColorProvider.rgba(255, 80, 80, 255);
        }

        float s = size.getFloatValue();
        float g = gap.getFloatValue();
        float t = thickness.getFloatValue();

        switch (style.getValue()) {
            case "Cross" -> {
                DrawUtil.drawRound(centerX - t / 2f, centerY - g - s, t, s, t / 2f, col);
                DrawUtil.drawRound(centerX - t / 2f, centerY + g, t, s, t / 2f, col);
                DrawUtil.drawRound(centerX - g - s, centerY - t / 2f, s, t, t / 2f, col);
                DrawUtil.drawRound(centerX + g, centerY - t / 2f, s, t, t / 2f, col);
            }
            case "Dot" -> DrawUtil.drawRound(centerX - 1.5f, centerY - 1.5f, 3, 3, 1.5f, col);
            case "Circle" -> drawRing(centerX, centerY, s, t, col);
        }
    }

    private void drawRing(float centerX, float centerY, float radius, float thickness, int color) {
        int r = (color >> 16) & 0xFF;
        int g = (color >> 8) & 0xFF;
        int b = color & 0xFF;
        int a = (color >> 24) & 0xFF;

        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        RenderSystem.disableCull();
        RenderSystem.setShader(ShaderProgramKeys.POSITION_COLOR);
        Matrix4f matrix = new Matrix4f();

        float inner = radius;
        float outer = radius + thickness;
        int segments = 48;

        BufferBuilder buffer = Tessellator.getInstance().begin(VertexFormat.DrawMode.TRIANGLE_STRIP, VertexFormats.POSITION_COLOR);
        for (int i = 0; i <= segments; i++) {
            double angle = Math.toRadians((360.0 / segments) * i);
            float cos = (float) Math.cos(angle);
            float sin = (float) Math.sin(angle);
            buffer.vertex(matrix, centerX + cos * outer, centerY + sin * outer, 0).color(r, g, b, a);
            buffer.vertex(matrix, centerX + cos * inner, centerY + sin * inner, 0).color(r, g, b, a);
        }
        BufferRenderer.drawWithGlobalProgram(buffer.end());

        RenderSystem.enableCull();
        RenderSystem.disableBlend();
    }
}
